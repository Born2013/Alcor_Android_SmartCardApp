#[BIRD_NEW_FLASHLIGHT] add chengshujiang 20170307
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_SHARED_LIBRARIES := liblog libnativehelper libcutils

LOCAL_SRC_FILES := flash_light_jni.cpp
LOCAL_MODULE := libflash_light_jni

LOCAL_PRELINK_MODULE := false
LOCAL_CERTIFICATE := platform 
include $(BUILD_SHARED_LIBRARY)

