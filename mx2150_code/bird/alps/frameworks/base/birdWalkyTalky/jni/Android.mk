LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	walkytalky_jni.cpp
	
LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_LDLIBS:=-L$(SYSROOT)/usr/lib -llog

LOCAL_SHARED_LIBRARIES := liblog libnativehelper libcutils
LOCAL_PRELINK_MODULE := false

LOCAL_MODULE := libwalkytalk

include $(BUILD_SHARED_LIBRARY)


