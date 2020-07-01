#[(BIRD_ELECTRONIC_CARD] wushiyong 20160524 begin
ifeq ($(BIRD_ELECTRONIC_CARD), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_JAVA_LIBRARIES += framework telephony-common mediatek-framework

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := BirdElectronicCard

LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)
endif
#[(BIRD_ELECTRONIC_CARD] wushiyong 20160524 end
