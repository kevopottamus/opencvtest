LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include /Users/jun.chen/project/android/nvpack/OpenCV-2.4.5-Tegra-sdk-r2/sdk/native/jni/OpenCV-tegra3.mk

LOCAL_MODULE    := opencv_sample
LOCAL_SRC_FILES := jni_part.cpp
LOCAL_LDLIBS +=  -llog -ldl

include $(BUILD_SHARED_LIBRARY)
