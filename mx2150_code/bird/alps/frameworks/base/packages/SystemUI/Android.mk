ifneq ($(strip $(BIRD_KUSAI_SYSTEMUI)), yes)
LOCAL_PATH:= $(call my-dir)


#BIRD_FACELOCK_SCREEN change begin
include $(CLEAR_VARS)
LOCAL_STATIC_JAVA_AAR_LIBRARIES := keyauard_aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := keyauard_aar:libs/FaceLockSDK_com_android_systemui.aar
include $(BUILD_MULTI_PREBUILT)
#BIRD_FACELOCK_SCREEN change end

include $(CLEAR_VARS)
LOCAL_MODULE := SystemUI-proto-tags

LOCAL_SRC_FILES := $(call all-proto-files-under,src) \
    src/com/android/systemui/EventLogTags.logtags

LOCAL_PROTOC_OPTIMIZE_TYPE := nano
LOCAL_PROTO_JAVA_OUTPUT_PARAMS := optional_field_style=accessors

include $(BUILD_STATIC_JAVA_LIBRARY)

# ------------------

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) $(call all-Iaidl-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
    com.mediatek.systemui.ext \
    Keyguard \
    android-support-v7-recyclerview \
    android-support-v7-preference \
    android-support-v7-appcompat \
    android-support-v14-preference \
    android-support-v17-leanback \
    framework-protos \
    SystemUI-proto-tags

LOCAL_JNI_SHARED_LIBRARIES := libyv12util

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += ims-common

# [BIRD][三指截屏][BIRD_THREE_POINTER_SCREENSHOT][qianliliang][20160607] BEGIN
ifeq ($(strip $(BIRD_THREE_POINTER_SCREENSHOT)), yes)
LOCAL_MANIFEST_FILE := three_pointer_screenshot/AndroidManifest.xml
endif
# [BIRD][三指截屏][BIRD_THREE_POINTER_SCREENSHOT][qianliliang][20160607] END

LOCAL_PACKAGE_NAME := SystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_RESOURCE_DIR := \
    frameworks/base/packages/Keyguard/res \
    frameworks/base/packages/Keyguard/res_ext \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/res_ext \
    frameworks/support/v7/preference/res \
    frameworks/support/v14/preference/res \
    frameworks/support/v7/appcompat/res \
    frameworks/support/v7/recyclerview/res \
    frameworks/support/v17/leanback/res

LOCAL_AAPT_FLAGS := --auto-add-overlay \
    --extra-packages com.android.keyguard:android.support.v7.recyclerview:android.support.v7.preference:android.support.v14.preference:android.support.v7.appcompat \
    --extra-packages android.support.v17.leanback \
    --extra-packages com.aiwinn.facelocksdk \
    --extra-packages com.aiwinn.bundlelibrary \
    --extra-packages com.aiwinn.initlibrary \
    --extra-packages com.adv.wt.library \
    --extra-packages com.wbtech.ums

#BIRD_FACELOCK_SCREEN change begin
LOCAL_SHARED_LIBRARIES := libArcSoft_FDEngine
LOCAL_SHARED_LIBRARIES += libArcSoft_FREngine
LOCAL_SHARED_LIBRARIES += libArcSoft_FTEngine
LOCAL_SHARED_LIBRARIES += libmpbase
LOCAL_SHARED_LIBRARIES += libtensorflow_demo
TARGET_ARCH_ABI := armeabi-v7a
APP_ABI := armeabi-v7a

LOCAL_STATIC_JAVA_AAR_LIBRARIES := keyauard_aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := keyauard_aar:libs/FaceLockSDK_com_android_systemui.aar
#BIRD_FACELOCK_SCREEN change end

ifneq ($(SYSTEM_UI_INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)

ifeq ($(EXCLUDE_SYSTEMUI_TESTS),)
    include $(call all-makefiles-under,$(LOCAL_PATH))
endif
endif
