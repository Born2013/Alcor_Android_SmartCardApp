#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(my-dir)

########################
include $(CLEAR_VARS)

LOCAL_MODULE := platform.xml

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/permissions
#
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)


#[BIRD_GREEN_PROTECT] caoyuangui begin
ifeq ($(strip $(BIRD_GREEN_PROTECT)),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := restore_list
LOCAL_MODULE_CLASS := ETC
# This will install the file in /system/etc/green_protect
# $(shell mkdir -p  $(TARGET_OUT_ETC)/green_protect)
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/green_protect
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
#[BIRD_GREEN_PROTECT] caoyuangui end

#[BIRD][内单项目][BIRD_INTERNAL_SALE_STATISTICS]BEGIN
ifeq ($(strip $(BIRD_INTERNAL_SALE_STATISTICS)),yes)
include $(CLEAR_VARS)

LOCAL_MODULE := d

LOCAL_MODULE_CLASS := ETC

LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)
endif
#[BIRD][内单项目][BIRD_INTERNAL_SALE_STATISTICS]END

########################
#include $(CLEAR_VARS)

#LOCAL_MODULE := required_hardware.xml

#LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/permissions
#
#LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions

#LOCAL_SRC_FILES := $(LOCAL_MODULE)

#include $(BUILD_PREBUILT)
#为方便统计预装应用，将预装应用包名列表生成到手机的system/etc/custom/bird_prebuilts_app文件下: xujing 20170607 begin
ifeq ($(strip $(BIRD_INTERNAL_SALE_STATISTICS)),yes)
include $(CLEAR_VARS)
LOCAL_MODULE := bird_prebuilts_app.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/custom
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
#为方便统计预装应用，将预装应用包名列表生成到手机的system/etc/custom/bird_prebuilts_app文件下: xujing 20170607 end

ifeq ($(strip $(BIRD_TRAFFIC_MANAGE)), yes) 
include $(CLEAR_VARS)
LOCAL_MODULE := greenlist
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif

#[BIRD][BIRD_DEFAULT_ENABLE_PRIMISSION][应用商店默认打开权限][pangmeizhou][20180803]begin
ifeq ($(strip $(BIRD_NEUSOFT_ENABLE_PRIMISSION)), yes) 
include $(CLEAR_VARS)
LOCAL_MODULE := device_policies.xml
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)
LOCAL_SRC_FILES := $(LOCAL_MODULE)
include $(BUILD_PREBUILT)
endif
#[BIRD][BIRD_DEFAULT_ENABLE_PRIMISSION][应用商店默认打开权限][pangmeizhou][20180803]end
