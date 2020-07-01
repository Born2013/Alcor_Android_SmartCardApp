ifeq ($(BIRD_FIRMWARE_AGING_TEST), yes)
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_AAPT_FLAGS = -c hdpi

LOCAL_PACKAGE_NAME := BirdFirmwareTest
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
