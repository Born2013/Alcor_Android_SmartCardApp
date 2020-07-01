ifeq ($(strip $(BIRD_AIMEI_STUDENT)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := framework \
        mediatek-framework \
        mediatek-common \
        bouncycastle  telephony-common \
        

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_CERTIFICATE := platform
# LOCAL_PRIVILEGED_MODULE := true
LOCAL_PACKAGE_NAME := StudentTrafficManage
include $(BUILD_PACKAGE)
endif