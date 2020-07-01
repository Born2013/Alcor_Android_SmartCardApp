ifneq ($(strip $(MTK_CTA_SUPPORT)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := achartengine:libs/achartengine-1.1.0.jar
include $(BUILD_MULTI_PREBUILT)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PRIVILEGED_MODULE := true

LOCAL_SRC_FILES := $(call all-java-files-under, src)

#LOCAL_JAVA_LIBRARIES += hwdroid
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13 android-support-v4 achartengine 


LOCAL_PACKAGE_NAME := com.sensortek.stkhealthcare2

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
