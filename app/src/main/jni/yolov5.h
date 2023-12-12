#ifndef NANODET_H
#define NANODET_H

#include <opencv2/core/core.hpp>
#include <jni.h>

#include "layer.h"
#include "net.h"
#include "benchmark.h"

class YoloV5Focus : public ncnn::Layer
{
public:
    YoloV5Focus()
    {
        one_blob_only = true;
    }

    virtual int forward(const ncnn::Mat& bottom_blob, ncnn::Mat& top_blob, const ncnn::Option& opt) const
    {
        int w = bottom_blob.w;
        int h = bottom_blob.h;
        int channels = bottom_blob.c;

        int outw = w / 2;
        int outh = h / 2;
        int outc = channels * 4;

        top_blob.create(outw, outh, outc, 4u, 1, opt.blob_allocator);
        if (top_blob.empty())
            return -100;

#pragma omp parallel for num_threads(opt.num_threads)
        for (int p = 0; p < outc; p++)
        {
            const float* ptr = bottom_blob.channel(p % channels).row((p / channels) % 2) + ((p / channels) / 2);
            float* outptr = top_blob.channel(p);

            for (int i = 0; i < outh; i++)
            {
                for (int j = 0; j < outw; j++)
                {
                    *outptr = *ptr;

                    outptr += 1;
                    ptr += 2;
                }

                ptr += w;
            }
        }

        return 0;
    }
};


struct Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
};

class Yolov5
{
public:
    Yolov5();

    int load(AAssetManager* mgr, bool use_gpu = false);

    int detect(const cv::Mat& rgb, std::vector<Object>& objects, float prob_threshold = 0.25f, float nms_threshold = 0.45f);

    int draw(cv::Mat& rgb, const std::vector<Object>& objects);

private:
    ncnn::Net yolov5;
    int target_size=320;
    ncnn::UnlockedPoolAllocator blob_pool_allocator;
    ncnn::PoolAllocator workspace_pool_allocator;
};
#endif