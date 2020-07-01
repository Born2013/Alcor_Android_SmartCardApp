#include <jni.h>
#include <stdio.h>
#include <utils/Log.h>
#include <android/log.h>
#include <fcntl.h>
#include <unistd.h>
#include <cutils/properties.h>

#ifdef _cplusplus
extern "C" {
#endif
//[BIRD][BIRD_FLASHLIGHT_JNI][wangjianping][手电筒接口][20140620] BEGIN
void turnOnFlashlight(JNIEnv *env, jobject thiz)
{
        int res = 0;
	int fd=0;
	char buf[4];
	__android_log_print(ANDROID_LOG_DEBUG, "maozexin", " open  /dev/mainled");
	fd = open("/dev/mainled", O_RDWR);
	buf[0]='C';
	res = write(fd,buf,1);


	if(fd != -1)
	{
	    close(fd) ;
	    fd = -1;
	}
}

void turnOffFlashlight(JNIEnv *env, jobject thiz)
{
        int res = 0;
	int fd=0;
	char buf[4];
	fd = open("/dev/mainled", O_RDWR);
	__android_log_print(ANDROID_LOG_DEBUG, "maozexin", " close  /dev/mainled");
	buf[0]='D';
	res = write(fd,buf,1);
        
	if(fd != -1)
	{
	    close(fd) ;
	    fd = -1;
	}
}


jboolean getState(JNIEnv *env, jobject thiz)
{
        int res = 0;
        int fd=0;
	char buf[4];
	char *p = NULL;
	fd = open("/dev/mainled", O_RDONLY);
        res = read(fd,buf,1);
        //__android_log_print(ANDROID_LOG_DEBUG, "wangjianping", " getState buf = %d",buf[0]);
        
	if( res==-1)
	{
	goto EXIT;
	}
	//p = strchr(buf, 'C');
	if(buf[0]==1)
	{
	 return JNI_TRUE;
	}
EXIT:

        if (fd != -1)

        close(fd);

        return JNI_FALSE;
  
}

#ifdef _cplusplus
}
#endif

static const char *classPathName = "com/bird/flashlight/FlashLightJni";

static JNINativeMethod methods[] =
{
  { "turn_on_flashlight", "()V", (void*)turnOnFlashlight } ,
  { "turn_off_flashlight", "()V", (void*)turnOffFlashlight } ,
  { "get_state_flashlight", "()Z",  (void*)getState }
};

static int registerNativeMethods(JNIEnv* env, const char* className,
   JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;


    //__android_log_print(ANDROID_LOG_DEBUG, "wangjianping", " registerNativeMethods");
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int registerNatives(JNIEnv* env)
{

  __android_log_print(ANDROID_LOG_DEBUG, "wangjianping", " registerNatives");
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

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
    
    //__android_log_print(ANDROID_LOG_DEBUG, "wangjianping", " JNI_OnLoad");
    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        goto bail;
    }
    env = uenv.env;
    if (registerNatives(env) != JNI_TRUE) {
        goto bail;
    }
    
    result = JNI_VERSION_1_4;
    
bail:
    return result;
}
//[BIRD][BIRD_FLASHLIGHT_JNI][wangjianping][手电筒接口][20140620] END
