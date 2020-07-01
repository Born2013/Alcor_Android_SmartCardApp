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
#define LOG_TAG "ClamNative"

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



static jboolean readClamState(JNIEnv *env, jobject clazz)
{
	ALOGD("$$$%s\n", __FUNCTION__);

	int size = 0;
	static int fd = -1;
	unsigned char state[4];
	
	fd = open("/sys/class/switch/hall/state", O_RDWR);
    	if (fd < 0) {
		ALOGE("open : %s!!!\n", strerror(errno));
		return JNI_FALSE;
	} else {
		ALOGD("open success!!!\n");
	}
	
	
    	if(fd >= 0) {
        	size = read(fd, state, sizeof(state));

        	if(size <= 0) {
	    		close(fd);
            		return JNI_FALSE;
        	}

        	if (*state < 49) {
            		close(fd);
            	return JNI_FALSE;
        	}

        	close(fd);
        	return JNI_TRUE;  
    	} else {
		close(fd);
		return JNI_FALSE;
	}
}

	
static JNINativeMethod gNotify[] = {
	{ "readClamState", "()Z", (void*)readClamState },
};


jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	ALOGD("$$$%s\n", __FUNCTION__);
	
	
	jint result = -1;
	JNIEnv* env = NULL;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		ALOGE("GetEnv error!\n");
    	return result;
	}

	int ret = jniRegisterNativeMethods(
		env, "com/bird/clam/ClamNative", gNotify, NELEM(gNotify));
	if (ret < 0) {
		ALOGE("RegisterNatives error!\n");

		return -1;
	}
    
    return JNI_VERSION_1_4;
}

