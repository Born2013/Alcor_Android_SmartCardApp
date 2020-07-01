/*
 * Copyright Statement:
 * --------------------
 * This software is protected by Copyright and the information contained
 * herein is confidential. The software may not be copied and the information
 * contained herein may not be used or disclosed except with the written
 * permission of MediaTek Inc. (C) 2009
 *
 * BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 * NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
 * SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 * BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
 * LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE. 
 *
 * THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
 * WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
 * LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
 * RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
 * THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
 *
 */

/*
 *
 * Filename:
 * ---------
 *  libbirdbprotectpsd_jni
 *
 * Project:
 * --------
 *   Husijia
 *
 * Description:
 * ------------
 *   Bird B PROTECTPSD JNI Interface
 *
 * Author:
 * -------
 *  
 *
 */
#include <jni.h>
#include <utils/Log.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <pthread.h>
#include <linux/serial.h>
#include <string.h> 
#include <linux/serial.h>
#include <sys/system_properties.h>
#include<android/log.h>
#include <errno.h>
#include <utils/misc.h>
#include "jni.h"
#include "JNIHelp.h"
#include <linux/ioctl.h>
#include <string.h>  

#define TAG "marqueen_jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)

#ifdef _cplusplus
extern "C" {
#endif

#define IOCTL_MAGIC         0x1D
#define IOCTL_SET_ENABLE     _IOW(IOCTL_MAGIC, 1, int)
#define IOCTL_SET_LOOP_MODE   _IOW(IOCTL_MAGIC, 2, int)
#define IOCTL_SET_AUTO_PALY_MODE    _IOW(IOCTL_MAGIC, 3, int)
#define IOCTL_SET_AUDIO_MODE     _IOW(IOCTL_MAGIC, 4, int)


#define devpath  "/dev/marquee"
static int fd = -1;

static jint marqueen_open(JNIEnv *env, jobject clazz)
{

  fd = open(devpath, O_RDWR);
  if (fd < 0) 
  {	
    ALOGE("marqueen open faild\n");
    return -1;
  }
  return 0;
}

static jint marqueen_close(JNIEnv *env, jobject clazz)
{
  ALOGD("%s\n", __FUNCTION__);
  if(fd < 0)
    return -1;
  close(fd);
  return 0;
}


static jint marqueen_ioctl(JNIEnv *env, jobject clazz, jint cmd, jint data)
{
  int ret = 0;
  if(fd < 0)
    return -1;
  if(cmd == 1)
  {
     ret = ioctl(fd, IOCTL_SET_ENABLE, &data);
  } 
  else if(cmd == 2)
  {
     ret = ioctl(fd, IOCTL_SET_LOOP_MODE, &data);
  }
  else if(cmd == 3)
  {
     ret = ioctl(fd, IOCTL_SET_AUTO_PALY_MODE, &data);
  }
    else if(cmd == 4)
  {
     ret = ioctl(fd, IOCTL_SET_AUDIO_MODE, &data);
  }

  if(ret == -1)
   return -1;
   return 0;
}

static const char *classPathNameRx = "com/bird/marqueen/MarqueenNative";


static JNINativeMethod gNotify[] = {
	{ "marqueenOpen", "()I", (void*)marqueen_open},
        { "marqueenClose", "()I", (void*)marqueen_close},
        { "marqueenIoctl", "(II)I", (void*)marqueen_ioctl},
};


/*
 * Register several native methods for one class.
 */
static jint registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    if (clazz == NULL) {
        ALOGD("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        ALOGD("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    ALOGD("%s, success\n", __func__);
    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static jint registerNatives(JNIEnv* env)
{
    jint ret = JNI_FALSE;

    if (registerNativeMethods(env, classPathNameRx,gNotify,
        sizeof(gNotify) / sizeof(gNotify[0]))) {
        ret = JNI_TRUE;
    }

    ALOGD("%s, done\n", __func__);
    return ret;
}

// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    ALOGD("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        ALOGD("ERROR: GetEnv failed");
        goto fail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        ALOGD("ERROR: registerNatives failed");
        goto fail;
    }
    result = JNI_VERSION_1_4;

fail:
    return result;
}

