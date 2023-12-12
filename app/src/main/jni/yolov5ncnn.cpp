#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolov5.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

jobject MatToBitmap(JNIEnv *env, cv::Mat src) {
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888FieldID = env->GetStaticFieldID(bitmapConfigClass, "ARGB_8888",
                                                     "Landroid/graphics/Bitmap$Config;");
    jobject argb8888Config = env->GetStaticObjectField(bitmapConfigClass, argb8888FieldID);

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethodID = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID, src.cols,
                                                 src.rows, argb8888Config);

    AndroidBitmapInfo bitmapInfo;
    void *pixels = 0;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) < 0) {
        return NULL;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        return NULL;
    }

    cv::Mat dst(bitmapInfo.height, bitmapInfo.width, CV_8UC4, pixels);

    if (src.type() == CV_8UC3) {
        cv::cvtColor(src, dst, cv::COLOR_RGB2RGBA);
    } else if (src.type() == CV_8UC1) {
        cv::cvtColor(src, dst, cv::COLOR_GRAY2RGBA);
    } else {
        src.copyTo(dst);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return bitmap;
}

std::vector<Object> createObjectsFromBoundingBoxString(const std::string &bboxString) {
    std::vector<Object> objects;
    std::istringstream iss(bboxString);
    std::string token;

    while (std::getline(iss, token, '/')) {
        Object obj;
        sscanf(token.c_str(), "%d,%f,%f,%f,%f,%f", &obj.label, &obj.prob, &obj.rect.x, &obj.rect.y,
               &obj.rect.width, &obj.rect.height);
        objects.push_back(obj);
    }

    return objects;
}


static Yolov5 *g_yolov5 = 0;
static ncnn::Mutex lock;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

//    ncnn::create_gpu_instance();

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

//    ncnn::destroy_gpu_instance();

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolov5;
        g_yolov5 = 0;
    }
}

// public native boolean Init(AssetManager mgr);
//JNIEXPORT jboolean JNICALL Java_com_tencent_yolov5ncnn_YoloV5Ncnn_Init(JNIEnv* env, jobject thiz, jobject assetManager)
JNIEXPORT jboolean JNICALL Java_com_example_demoproject_1master_YoloDetNcnn_loadModel(JNIEnv *env,
                                                                   jobject thiz,
                                                                   jobject assetManager,
                                                                   jint modelid,
                                                                   jint cpugpu)
{
//    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
//    {
//        return JNI_FALSE;
//    }

    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    bool use_gpu = (int) cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu /*&& ncnn::get_gpu_count()*/ == 0) {
            // no gpu
            delete g_yolov5;
            g_yolov5 = 0;
        } else {
            if (!g_yolov5)
                g_yolov5 = new Yolov5;
            g_yolov5->load(mgr, use_gpu);

        }
    }
    return JNI_TRUE;
}

// public native Obj[] Detect(Bitmap bitmap, boolean use_gpu);
JNIEXPORT jboolean JNICALL Java_com_example_demoproject_1master_YoloDetNcnn_predict(JNIEnv *env,
                                             jobject thiz,
                                             jobject imageView,
                                             jobject bitmap)
{
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        // Error handling
        return JNI_FALSE;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        // Only support RGBA_8888 format, you may need to convert other formats
        return JNI_FALSE;
    }

    // Get the pointer to bitmap pixels
    void *bitmapPixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0) {
        // Error handling
        return JNI_FALSE;
    }

    int width = info.width;
    int height = info.height;
    cv::Mat rgba(height, width, CV_8UC4, bitmapPixels);
    cv::Mat rgb;
    cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);

    jclass imageViewClass = env->GetObjectClass(imageView);
    jmethodID setImageBitmapMethod = env->GetMethodID(imageViewClass, "setImageBitmap",
                                                      "(Landroid/graphics/Bitmap;)V");
    ncnn::MutexLockGuard g(lock);

    if (g_yolov5)
    {
        std::vector<Object> objects;
        g_yolov5->detect(rgb, objects);
        g_yolov5->draw(rgb, objects);

        jobject jbitmap = MatToBitmap(env, rgb);
        env->CallVoidMethod(imageView, setImageBitmapMethod, jbitmap);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_example_demoproject_1master_YoloDetNcnn_draw_1Bbox(JNIEnv *env,
                                                jobject thiz,
                                                jobject image_view,
                                                jobject bitmap,
                                                jstring data,
                                                jstring data2) {
    // RGB형식으로 변경
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        // Error handling
        return JNI_FALSE;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        // Only support RGBA_8888 format, you may need to convert other formats
        return JNI_FALSE;
    }

    // Get the pointer to bitmap pixels
    void *bitmapPixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0) {
        // Error handling
        return JNI_FALSE;
    }

    // Create a cv::Mat from the bitmap data
    int width = info.width;
    int height = info.height;
    cv::Mat rgba(height, width, CV_8UC4, bitmapPixels);
    cv::Mat rgb;
    cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);

    jclass imageViewClass = env->GetObjectClass(image_view);
    jmethodID setImageBitmapMethod = env->GetMethodID(imageViewClass, "setImageBitmap",
                                                      "(Landroid/graphics/Bitmap;)V");

    ncnn::MutexLockGuard g(lock);

    if (g_yolov5) {
        const char *cstr = env->GetStringUTFChars(data, nullptr);
        if (cstr != nullptr) {
            std::string bboxString(cstr); // C 스타일 문자열을 C++의 std::string으로 복사
            env->ReleaseStringUTFChars(data, cstr); // 메모리 릴리스

            std::vector<Object> objects = createObjectsFromBoundingBoxString(cstr);
            g_yolov5->draw(rgb, objects);
        }
        jobject jbitmap = MatToBitmap(env, rgb);
        env->CallVoidMethod(image_view, setImageBitmapMethod, jbitmap);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    return JNI_TRUE;
}
}