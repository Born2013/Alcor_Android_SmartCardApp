#ifneq ($(BIRD_DEVICES_TEST),yes)
#ifeq ( $(BIRD_SMT_SW),yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
#[BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]begin
LOCAL_STATIC_JAVA_LIBRARIES := libsmreader
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v4
#[BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]end
LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := framework \
        mediatek-framework \
        mediatek-common \
        telephony-common
LOCAL_PROPRIETARY_MODULE := true
LOCAL_MODULE_OWNER := mtk
LOCAL_JNI_SHARED_LIBRARIES := libfmjni
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += ./java/api/cardread/com/cardreadso/CardReadApi.java
LOCAL_PACKAGE_NAME := SMT_SW
#[BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]begin
ifneq ($(BIRD_MMI_FACTORY_TEST),yes)
LOCAL_PREBUILT_JNI_LIBS := jni_libs/armeabi-v7a/libAlUSB.so
ifeq ($(TARGET_ARCH),arm64)
LOCAL_PREBUILT_JNI_LIBS += jni_libs/arm64-v8a/libAlUSB.so
endif
endif
#[BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]end
#[工厂贴片软件，要求把能去掉的应用都去掉]chenguangxiang 20171108 begin
ifeq ($(BIRD_MMI_FACTORY_TEST),yes)

LOCAL_OVERRIDES_PACKAGES := MtkBrowser MtkCalendar MtkQuickSearchBox Camera MtkMms Email FMRadio ExactCalculator Calculator Browser2 Calendar DeskClock QuickSearchBox Stk1 Gallery2 FileManager DownloadProvider DownloadProviderUi Music DocumentsUI Contacts

endif
#[工厂贴片软件，要求把能去掉的应用都去掉]chenguangxiang 20171108 end

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

#[BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]begin
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libsmreader:libs/SmartCardLib.jar
LOCAL_PREBUILT_LIBS += libSMTCardReadJni:libs/libCardReadJni/libCardReadJni.so
include $(BUILD_MULTI_PREBUILT) 
#[BIRD][BIRD_SMT_SW_SMARTCARD_TEST][贴片测试_读卡器测试][chengci][20180328]end

include $(call all-makefiles-under,$(LOCAL_PATH))
#endif
#endif
