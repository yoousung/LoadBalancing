//
// Create by RangiLyu
// 2020 / 10 / 2
//

#include "nanodet_tstl.h"
#include <benchmark.h>

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>
// #include <iostream>

inline float fast_exp(float x)
{
    union {
        uint32_t i;
        float f;
    } v{};
    v.i = (1 << 23) * (1.4426950409 * x + 126.93490512f);
    return v.f;
}

inline float sigmoid(float x)
{
    return 1.0f / (1.0f + fast_exp(-x));
}

template<typename _Tp>
int activation_function_softmax(const _Tp* src, _Tp* dst, int length)
{
    const _Tp alpha = *std::max_element(src, src + length);
    _Tp denominator{ 0 };

    for (int i = 0; i < length; ++i) {
        dst[i] = fast_exp(src[i] - alpha);
        denominator += dst[i];
    }

    for (int i = 0; i < length; ++i) {
        dst[i] /= denominator;
    }

    return 0;
}


static void generate_grid_center_priors(const int input_height, const int input_width, std::vector<int>& strides, std::vector<CenterPrior>& center_priors)
{
    for (int i = 0; i < (int)strides.size(); i++)
    {
        int stride = strides[i];
        int feat_w = ceil((float)input_width / stride);
        int feat_h = ceil((float)input_height / stride);
        for (int y = 0; y < feat_h; y++)
        {
            for (int x = 0; x < feat_w; x++)
            {
                CenterPrior ct;
                ct.x = x;
                ct.y = y;
                ct.stride = stride;
                center_priors.push_back(ct);
            }
        }
    }
}


NanoDet2::NanoDet2()
{

}

int NanoDet2::load(AAssetManager* mgr, bool use_gpu)
{
    nanodet.clear();

    // opt
#if NCNN_VULKAN
    this->hasGPU = ncnn::get_gpu_count() > 0;
#endif
    nanodet.opt.use_vulkan_compute = use_gpu;
    nanodet.opt.use_fp16_arithmetic = true;

    nanodet.load_param(mgr, "nanodet_tstl.param");
    nanodet.load_model(mgr, "nanodet_tstl.bin");

    return 0;
}

int NanoDet2::detect(cv::Mat& rgb, std::vector<NanoDet2Object>& objects, float score_threshold, float nms_threshold)
{
    int img_w = rgb.cols;
    int img_h = rgb.rows;

    int w = img_w;
    int h = img_h;
    float scale = 1.f;
    if (w > h)
    {
        scale = (float)this->input_size[0] / w;
        w = this->input_size[0];
        h = h * scale;
    }
    else
    {
        scale = (float)this->input_size[1] / h;
        h = this->input_size[1];
        w = w * scale;
    }

    //    ncnn::Mat in = ncnn::Mat::from_pixels(rgb.data, ncnn::Mat::PIXEL_BGR, img_w, img_h);
    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb.data, ncnn::Mat::PIXEL_BGR, img_w, img_h, w, h);
    int wpad = input_size[0]-in.w;
    int hpad = input_size[1]-in.h;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 0.f);

//    cv::Mat in_pad_mat(in_pad.h, in_pad.w, CV_32FC1, in_pad.data);
//    cv::imshow("abc", in_pad_mat);
//    cv::waitKey(0);

    const float mean_vals[3] = { 103.53f, 116.28f, 123.675f };
    const float norm_vals[3] = { 0.017429f, 0.017507f, 0.017125f };

    in_pad.substract_mean_normalize(mean_vals, norm_vals);

    //double start = ncnn::get_current_time();

    auto ex = nanodet.create_extractor();
    ex.set_light_mode(true);
    ex.set_num_threads(4);
#if NCNN_VULKAN
    ex.set_vulkan_compute(this->hasGPU);
#endif
    ex.input("data", in_pad);

    std::vector<std::vector<NanoDet2Object>> results;
    results.resize(this->num_class);

    ncnn::Mat out;
    ex.extract("output", out);
    // printf("%d %d %d \n", out.w, out.h, out.c);

    // generate center priors in format of (x, y, stride)
    std::vector<CenterPrior> center_priors;
    generate_grid_center_priors(this->input_size[0], this->input_size[1], this->strides, center_priors);

    this->decode_infer(out, center_priors, score_threshold, results);

    for (int i = 0; i < (int)results.size(); i++)
    {
        this->nms(results[i], nms_threshold);

        for (auto box : results[i])
        {
            objects.push_back(box);
        }
    }

    //double end = ncnn::get_current_time();
    //double time = end - start;
    //printf("Detect Time:%7.2f \n", time);

    return 0;
}

void NanoDet2::decode_infer(ncnn::Mat& feats, std::vector<CenterPrior>& center_priors, float threshold, std::vector<std::vector<NanoDet2Object>>& results)
{
    const int num_points = center_priors.size();
    //printf("num_points:%d\n", num_points);

    //cv::Mat debug_heatmap = cv::Mat(feature_h, feature_w, CV_8UC3);
    for (int idx = 0; idx < num_points; idx++)
    {
        const int ct_x = center_priors[idx].x;
        const int ct_y = center_priors[idx].y;
        const int stride = center_priors[idx].stride;

        const float* scores = feats.row(idx);
        float score = 0;
        int cur_label = 0;
        for (int label = 0; label < this->num_class; label++)
        {
            if (scores[label] > score)
            {
                score = scores[label];
                cur_label = label;
            }
        }
        if (score > threshold)
        {
            //std::cout << "label:" << cur_label << " score:" << score << std::endl;
            const float* bbox_pred = feats.row(idx) + this->num_class;
            results[cur_label].push_back(this->disPred2Bbox(bbox_pred, cur_label, score, ct_x, ct_y, stride));
            //debug_heatmap.at<cv::Vec3b>(row, col)[0] = 255;
            //cv::imshow("debug", debug_heatmap);
        }
    }
}

NanoDet2Object NanoDet2::disPred2Bbox(const float*& dfl_det, int label, float score, int x, int y, int stride)
{
    float ct_x = x * stride;
    float ct_y = y * stride;
    std::vector<float> dis_pred;
    dis_pred.resize(4);
    for (int i = 0; i < 4; i++)
    {
        float dis = 0;
        float* dis_after_sm = new float[this->reg_max + 1];
        activation_function_softmax(dfl_det + i * (this->reg_max + 1), dis_after_sm, this->reg_max + 1);
        for (int j = 0; j < this->reg_max + 1; j++)
        {
            dis += j * dis_after_sm[j];
        }
        dis *= stride;
        //std::cout << "dis:" << dis << std::endl;
        dis_pred[i] = dis;
        delete[] dis_after_sm;
    }
    float xmin = (std::max)(ct_x - dis_pred[0], .0f);
    float ymin = (std::max)(ct_y - dis_pred[1], .0f);
    float xmax = (std::min)(ct_x + dis_pred[2], (float)this->input_size[0]);
    float ymax = (std::min)(ct_y + dis_pred[3], (float)this->input_size[1]);

    cv::Rect_<float> rect(xmin, ymin, xmax - xmin, ymax - ymin);

    //std::cout << xmin << "," << ymin << "," << xmax << "," << xmax << "," << std::endl;
    return NanoDet2Object { rect, label, score };
}

void NanoDet2::nms(std::vector<NanoDet2Object>& input_boxes, float NMS_THRESH)
{
    std::sort(input_boxes.begin(), input_boxes.end(), [](NanoDet2Object a, NanoDet2Object b) { return a.prob > b.prob; });
    std::vector<float> vArea(input_boxes.size());
    for (int i = 0; i < int(input_boxes.size()); ++i) {
        vArea[i] = (input_boxes.at(i).rect.width + 1)
                   * (input_boxes.at(i).rect.height + 1);
    }
    for (int i = 0; i < int(input_boxes.size()); ++i) {
        for (int j = i + 1; j < int(input_boxes.size());) {
            cv::Rect_<float> intersection = input_boxes[i].rect & input_boxes[j].rect;
            float w = std::max(0.0f, intersection.width);
            float h = std::max(0.0f, intersection.height);
            float inter = w * h;
            float ovr = inter / (vArea[i] + vArea[j] - inter);
            if (ovr >= NMS_THRESH) {
                input_boxes.erase(input_boxes.begin() + j);
                vArea.erase(vArea.begin() + j);
            }
            else {
                j++;
            }
        }
    }
}


int NanoDet2::draw(const cv::Mat& bgr, const std::vector<NanoDet2Object>& bboxes)
{
    static const unsigned char colors[19][3] = {
            {54,  67, 244},
            {99,  30, 233},
            {176, 39, 156}
    };

    int src_w = bgr.cols;
    int src_h = bgr.rows;
    int dst_w = effect_roi.width;
    int dst_h = effect_roi.height;
    float width_ratio = (float)src_w / (float)dst_w;
    float height_ratio = (float)src_h / (float)dst_h;


    for (size_t i = 0; i < bboxes.size(); i++)
    {
        const NanoDet2Object& bbox = bboxes[i];
        const unsigned char *color = colors[bbox.label];
        cv::Scalar cc(color[0], color[1], color[2]);

        cv::rectangle(bgr, cv::Rect(cv::Point((bbox.rect.x - effect_roi.x) * width_ratio, (bbox.rect.y - effect_roi.y) * height_ratio),
                                    cv::Point((bbox.rect.x+bbox.rect.width - effect_roi.x) * width_ratio, (bbox.rect.y+bbox.rect.height - effect_roi.y) * height_ratio)), cc, 2);
//        cv::rectangle(bgr, bbox.rect, cc, 2);

        char text[256];
        sprintf(text, "%s", class_names[bbox.label]);

        int baseLine = 0;
        cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.4, 1, &baseLine);

        int x = (bbox.rect.x - effect_roi.x) * width_ratio;
        int y = (bbox.rect.y - effect_roi.y) * height_ratio - label_size.height - baseLine;
//        int x = bbox.rect.x;
//        int y = bbox.rect.y - label_size.height - baseLine;
        if (y < 0)
            y = 0;
        if (x + label_size.width > bgr.cols)
            x = bgr.cols - label_size.width;

        cv::rectangle(bgr, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                      cc, -1);

        cv::putText(bgr, text, cv::Point(x, y + label_size.height),
                    cv::FONT_HERSHEY_SIMPLEX, 0.4, cv::Scalar(255, 255, 255));
    }

    return 0;
}