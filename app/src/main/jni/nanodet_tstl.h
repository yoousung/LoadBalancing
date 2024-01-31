//
// Create by RangiLyu
// 2020 / 10 / 2
//

#ifndef NANODET2_H
#define NANODET2_H

#include <opencv2/core/core.hpp>
#include <net.h>

typedef struct HeadInfo
{
    std::string cls_layer;
    std::string dis_layer;
    int stride;
};

struct CenterPrior
{
    int x;
    int y;
    int stride;
};

struct NanoDet2Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
};

struct object_rect {
    int x;
    int y;
    int width;
    int height;
};

class NanoDet2
{
public:
    NanoDet2();

    ncnn::Net nanodet;
    // modify these parameters to the same with your config if you want to use your own model
    int input_size[2] = {320, 320}; // input height and width
    static const int num_class = 3; // number of classes.
    int reg_max = 7; // `reg_max` set in the training config. Default: 7.
    std::vector<int> strides = { 8, 16, 32, 64 }; // strides of the multi-level feature.

    int load(AAssetManager* mgr, bool use_gpu = false);

    int detect(cv::Mat& rgb, std::vector<NanoDet2Object>& objects, float score_threshold = 0.4f, float nms_threshold = 0.5f);

    int draw(const cv::Mat& rgb, const std::vector<NanoDet2Object>& objects);

    const char* class_names[num_class] = {"sign", "light", "info" };
private:
    void decode_infer(ncnn::Mat& feats, std::vector<CenterPrior>& center_priors, float threshold, std::vector<std::vector<NanoDet2Object>>& results);
    NanoDet2Object disPred2Bbox(const float*& dfl_det, int label, float score, int x, int y, int stride);
    static void nms(std::vector<NanoDet2Object>& result, float nms_threshold);
    object_rect effect_roi = object_rect {0, 112, 320, 96};
};


#endif //NANODET2_H
