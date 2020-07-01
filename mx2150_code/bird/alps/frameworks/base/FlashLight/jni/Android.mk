
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_SRC_FILES := flashligh_jni.cpp
                  

LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_PRELINK_MODULE := false
LOCAL_CERTIFICATE := platform 
LOCAL_MODULE := libbirdflashlight_jni
include $(BUILD_SHARED_LIBRARY)


