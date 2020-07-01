#ifeq ( $(BIRD_IUI_FLASHLIGHT), yes)
ifeq ($(strip $(BIRD_KUSAI_FLASHLIGHT)), yes)
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-common

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_AAPT_FLAGS = -c hdpi

LOCAL_PACKAGE_NAME := FlashLight
LOCAL_CERTIFICATE := platform
#[BIRD][MTK_CTA_SUPPORT][CTA软件应用可卸载][hongzhihao]20170911
ifeq ($(MTK_CTA_SUPPORT), yes)
   LOCAL_MODULE_PATH := $(TARGET_OUT)/vendor/operator/app
endif
#[BIRD][MTK_CTA_SUPPORT][CTA软件应用可卸载][hongzhihao]20170911
include $(BUILD_PACKAGE)
endif
#endif
