ifneq ($(strip $(MTK_CTA_SUPPORT)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := framework \
        mediatek-framework \
        mediatek-common \
        telephony-common

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(call all-subdir-java-files) \

LOCAL_PACKAGE_NAME := Dualcamera
include $(BUILD_PACKAGE)
endif


