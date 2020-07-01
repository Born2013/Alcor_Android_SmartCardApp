#[BIRD][BIRD_KINGROOT][赋给KingRoot以root权限][yangbo][20171204]BEGIN
ifneq ($(strip $(BIRD_KINGROOT)), yes)
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS := -std=c11 -Wall -Werror

LOCAL_SRC_FILES:= su.c

LOCAL_MODULE:= su

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)
endif
#[BIRD][BIRD_KINGROOT][赋给KingRoot以root权限][yangbo][20171204]END
