/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */
 /*
 * [BIRD_NEW_FLASHLIGHT] add chengshujiang 20170307
 */
#define LOG_TAG "BirdFlashlightNative"

#include <stdio.h>
#include <errno.h>
#include <sys/ioctl.h>

#include <utils/misc.h>
#include <utils/Log.h>

#include "jni.h"
#include "JNIHelp.h"
#include <linux/ioctl.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>

static jboolean isFlashLightOn(JNIEnv *env, jobject clazz)
{
    int isOn = 0;
    static int fd = -1;
    int ret = 0;
    unsigned char state[1];
    
    fd = open("/sys/class/switch/toggle_switch/state", O_RDWR);

    if (fd < 0) {
        close(fd);
        ALOGD("csj state000");
        return JNI_FALSE;
    } else {
        isOn = read(fd, state, 1);
        ALOGD("csj state '%d'", isOn);
        if (isOn <= 0) {
            close(fd);
            ALOGD("csj state111");
            return JNI_FALSE;
        }
        
        if (*state < 49) {
            close(fd);
            ALOGD("csj state222");
            return JNI_FALSE;
        }
        close(fd);
        ALOGD("csj state333");
        return JNI_TRUE;
    }
}



static JNINativeMethod gNotify[] = {
    { "isFlashLightOn", "()Z", (void*)isFlashLightOn },
};


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    
    jint result = -1;
    JNIEnv* env = NULL;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return result;
    }

    int ret = jniRegisterNativeMethods(
        env, "com/bird/flashlight/BirdFlashlightNative", gNotify, NELEM(gNotify));
    if (ret < 0) {

        return -1;
    }
    
    return JNI_VERSION_1_4;
}

