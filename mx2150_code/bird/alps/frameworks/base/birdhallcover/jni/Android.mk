LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := liblog libnativehelper libcutils

LOCAL_SRC_FILES := clam_jni.cpp
LOCAL_MODULE := libclamjni

#LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
