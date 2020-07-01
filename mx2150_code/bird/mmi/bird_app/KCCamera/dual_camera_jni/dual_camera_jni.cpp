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
 *xujing 20130730 added :   BIRD_LEATHER_COVER
 */
#define LOG_TAG "testNative"

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
#include <unistd.h>

static jboolean readSubCameraState(JNIEnv *env, jobject clazz)
{
    ALOGD("$$$%s\n", __FUNCTION__);

    int covered = 0;
    static int fd = -1;
    int ret = 0;
    unsigned char state[2];
    
    fd = open("/dev/camera_cover", O_RDWR);

    if (fd < 0) {
        ALOGE("csj open : %s!!!\n", strerror(errno));
        close(fd);
        return JNI_FALSE;
    } else {
        ALOGD("csj open success!!!\n");
        covered = read(fd, state, 2);
        ALOGD("yh state0:%d",state[0]);
        ALOGD("yh state1:%d",state[1]);
        ALOGD("yh covered:%d",covered);
        if (covered <= 0) {
            ALOGD("csj open covered");
            close(fd);
            return JNI_FALSE;
        }
       
        if (state[0] == 1) {
            ALOGD("state[0]:%d",state[0]);
            ALOGD("state[1]:%d",state[1]);
            close(fd);
            return JNI_TRUE;
        }
     
        ALOGD("csj state1111:%d",state[0]);
        close(fd);
        ALOGD("yangheng close");
        return JNI_FALSE;
    }
}


static jboolean readSubBackCameraState(JNIEnv *env, jobject clazz)
{
    ALOGD("$$$%s\n", __FUNCTION__);

    int covered = 0;
    static int fd = -1;
    int ret = 0;
    unsigned char read_cover[1];
    
    fd = open("/dev/camera_cover", O_RDWR);

    if (fd < 0) {
        ALOGE("csj open : %s!!!\n", strerror(errno));
        close(fd);
        return JNI_FALSE;
    } else {
        ALOGD("csj open success!!!\n");
        covered = read(fd,read_cover,1);
        if (covered <= 0) {
            ALOGD("csj open covered");
            close(fd);
            return JNI_FALSE;
        }
        if (read_cover[0] == 1) {
           // ALOGD("csj state000:%d"+read_cover[0]);
            close(fd);
            return JNI_TRUE;
        }
        //ALOGD("csj state1111:%d"+read_cover[0]);
        close(fd);
        return JNI_FALSE;
    }
}

static jboolean openSmain(JNIEnv *env, jobject clazz)
{
    ALOGD("$$$%s\n", __FUNCTION__);

    int fd = -1;
    int covered = 0;
    char read_cover[1]; //读取遮挡判断 1为遮挡；0为未遮挡，也是平常默认状态

    char open_smain[1];
    open_smain[0]=0x31; 

    
    fd = open("/dev/camera_cover", O_RDWR);

    if (fd < 0) {
        ALOGE("csj openSmain : %s!!!\n", strerror(errno));
        close(fd);
        return JNI_FALSE;
    } else {
        ALOGD("csj openSmain success!!!\n");
        covered = write(fd,open_smain,1);
        //ALOGD("csj openSmain1111:%d"+covered);
        if (covered <= 0) {
            ALOGD("csj openSmain covered");
            close(fd);
            return JNI_FALSE;
        }
        ALOGD("csj openSmain1111:%d");
        close(fd);
        return JNI_TRUE;
    }
}

static jboolean closeSmain(JNIEnv *env, jobject clazz)
{
    ALOGD("$$$%s\n", __FUNCTION__);

    int fd = -1;
    int covered = 0;
    char read_cover[1]; //读取遮挡判断 1为遮挡；0为未遮挡，也是平常默认状态

    char close_smain[1];
    close_smain[0]=0x30; 

    
    fd = open("/dev/camera_cover", O_RDWR);

    if (fd < 0) {
        ALOGE("csj closeSmain : %s!!!\n", strerror(errno));
        close(fd);
        return JNI_FALSE;
    } else {
        ALOGD("csj closeSmain success!!!\n");
        covered = write(fd,close_smain,1);
        //ALOGD("csj closeSmain1111:%d"+covered);
        if (covered <= 0) {
            ALOGD("csj closeSmain covered");
            close(fd);
            return JNI_FALSE;
        }
        ALOGD("csj closeSmain1111:%d");
        close(fd);
        return JNI_TRUE;
    }
}

static jboolean writeFrontCameraState(JNIEnv *env, jobject clazz)
{
    ALOGD("$$$%s\n", __FUNCTION__);

    int fd = -1;
    int covered = 0;
    char read_cover[1]; //读取遮挡判断 1为遮挡；0为未遮挡，也是平常默认状态

    char set_subcam[1];
    set_subcam[0]=0x30; 

    
    fd = open("/dev/camera_cover", O_RDWR);

    if (fd < 0) {
        ALOGE("csj writeFrontCameraState : %s!!!\n", strerror(errno));
        close(fd);
        return JNI_FALSE;
    } else {
        ALOGD("csj writeFrontCameraState success!!!\n");
        covered = write(fd, set_subcam, 1);
        ALOGD("csj writeFrontCameraState1111:%d",covered);
        if (covered <= 0) {
            ALOGD("csj writeFrontCameraState covered");
            close(fd);
            return JNI_FALSE;
        }
        ALOGD("csj writeFrontCameraState1111:%d");
        close(fd);
        return JNI_TRUE;
    }
}

static jboolean writeBackCameraState(JNIEnv *env, jobject clazz)
{
    ALOGD("$$$%s\n", __FUNCTION__);

    int fd = -1;
    int covered = 0;
    char read_cover[1]; //读取遮挡判断 1为遮挡；0为未遮挡，也是平常默认状态

    char set_subsmain[1];
    set_subsmain[0]=0x31;

    
    fd = open("/dev/camera_cover", O_RDWR);

    if (fd < 0) {
        ALOGE("csj writeBackCameraState : %s!!!\n", strerror(errno));
        close(fd);
        return JNI_FALSE;
    } else {
        ALOGD("csj writeBackCameraState success!!!\n");
        covered = write(fd, set_subsmain, 1);
        ALOGD("csj writeBackCameraState1111:%d",covered);
        if (covered <= 0) {
            ALOGD("csj writeBackCameraState covered");
            close(fd);
            return JNI_FALSE;
        }
        ALOGD("csj writeBackCameraState1111:%d");
        close(fd);
        return JNI_TRUE;
    }
}

static JNINativeMethod gNotify[] = {
    { "readSubCameraState", "()Z", (void*)readSubCameraState },
    { "writeFrontCameraState", "()Z", (void*)writeFrontCameraState },
    { "writeBackCameraState", "()Z", (void*)writeBackCameraState },
    { "openSmain", "()Z", (void*)openSmain },
    { "closeSmain", "()Z", (void*)closeSmain },
    { "readSubBackCameraState", "()Z", (void*)readSubBackCameraState },
};


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    ALOGD("$$$%s\n", __FUNCTION__);
    
    jint result = -1;
    JNIEnv* env = NULL;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGD("GetEnv error!\n");
        return result;
    }

    int ret = jniRegisterNativeMethods(
        env, "com/mediatek/camera/mode/gyfacebeauty/BirdDualCameraNative", gNotify, NELEM(gNotify));
    if (ret < 0) {
        ALOGD("RegisterNatives error!\n");

        return -1;
    }
    
    ALOGD("yangheng error!");
    return JNI_VERSION_1_4;
}

