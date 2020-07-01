# [BIRD_KUSAI_MUSIC][Kusai风格音乐]XUJING 20170622
ifeq ($(BIRD_KUSAI_MUSIC),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	src/com/android/music/IMediaPlaybackService.aidl \
	src/com/mediatek/bluetooth/avrcp/IBTAvrcpMusic.aidl \
	src/com/mediatek/bluetooth/avrcp/IBTAvrcpMusicCallback.aidl
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-support-v4
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += voip-common

#[BIRD_WEIMI_SYSTEMUI]add by wangyueyue 20150320 begin 
LOCAL_JAVA_LIBRARIES += android-support-v4
#[BIRD_WEIMI_SYSTEMUI]add by wangyueyue 20150320 end 
LOCAL_PACKAGE_NAME := Music
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.music.ext
#[BIRD][MTK_CTA_SUPPORT][CTA软件应用可卸载][hongzhihao]20170911
ifeq ($(MTK_CTA_SUPPORT), yes)
   LOCAL_MODULE_PATH := $(PRODUCT_OUT)/data/app
else
   LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR)/app
endif
#[BIRD][MTK_CTA_SUPPORT][CTA软件应用可卸载][hongzhihao]20170911

LOCAL_CERTIFICATE := platform
#LOCAL_SDK_VERSION := current

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
