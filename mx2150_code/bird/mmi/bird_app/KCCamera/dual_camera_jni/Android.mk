LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := dual_camera_jni.cpp
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := liblog libnativehelper libcutils
LOCAL_MODULE := libdual_camera_jni
include $(BUILD_SHARED_LIBRARY)
