ifeq ($(strip $(BIRD_AIMEI_STUDENT)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := StudentSos
LOCAL_CERTIFICATE := platform
LOCAL_JAVA_LIBRARIES += telephony-common
LOCAL_RESOURCE_DIR := \
        $(LOCAL_PATH)/res
include $(BUILD_PACKAGE)
endif