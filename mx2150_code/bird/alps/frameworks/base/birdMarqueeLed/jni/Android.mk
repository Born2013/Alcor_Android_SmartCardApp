LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	marqueen_jni.cpp
	
LOCAL_C_INCLUDES := $(JNI_H_INCLUDE)

LOCAL_LDLIBS:=-L$(SYSROOT)/usr/lib -llog

LOCAL_SHARED_LIBRARIES := liblog libnativehelper libcutils
LOCAL_PRELINK_MODULE := false

LOCAL_MODULE := libmarqueen

include $(BUILD_SHARED_LIBRARY)


