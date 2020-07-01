#ifeq ($(strip $(BIRD_WRITE_IMEI)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES := framework
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += telephony-common 
#LOCAL_JAVA_LIBRARIES += mediatek-telephony-common

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := WriteIMEIApp

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
#endif
