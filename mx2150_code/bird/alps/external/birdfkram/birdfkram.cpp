#define LOG_TAG "fingerprintd"

#include <cutils/log.h>
#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/PermissionCache.h>
#include <utils/String16.h>
#include <cutils/properties.h>
#include <keystore/IKeystoreService.h>
#include <keystore/keystore.h> // for error codes

#include <hardware/hardware.h>
#include <hardware/fingerprint.h>
#include <hardware/hw_auth_token.h>
#include <fcntl.h>

int main() {
   
    static char fakeram[32];
    static char fakecpu[32];
    static char fakeswitchram[32];
    static char tmp[PROPERTY_VALUE_MAX];
    static char isSystemtmp[PROPERTY_VALUE_MAX];
    static char isSysForDmtmp[PROPERTY_VALUE_MAX];
    static char isSysForDmSwitch[PROPERTY_VALUE_MAX];
    static char switchtmp[PROPERTY_VALUE_MAX];
    static char cputmp[PROPERTY_VALUE_MAX];
    static char fakecpudm[32];
    static char cpudmtmp[PROPERTY_VALUE_MAX];
    long fdram = -1;
    long fdcpu = -1;
    long fdswitchram = -1;
    long fdcpudm = -1;
    ALOGE("birdfk is start\n");
  //[BIRD][BIRD_SYSTEM_APP]chengshujiang 20160828 begin
    property_get("ro.bird_system_app", isSystemtmp , "0");
    int isSystemApp = atoi(isSystemtmp);
    property_get("ro.bird_system_app_for_dm", isSysForDmtmp , "0");
    int isSystemAppForDm = atoi(isSysForDmtmp);
  
    property_get("persist.sys.dm_switch", isSysForDmSwitch , "0");
    int isDmSwitch = atoi(isSysForDmSwitch);
  
    if (isSystemApp == 1) {
          ALOGE("zxw property_get ram_max000 success");
          property_get("persist.sys.tt.ram_max", tmp , "1");
      } else {
          ALOGE("zxw property_get fake_ram_value000 success");
          property_get("persist.sys.fake_ram_value", tmp , "1");
      }
  //[BIRD][BIRD_SYSTEM_APP]chengshujiang 20160828 end
  
     {
        property_get("persist.sys.dm.ram_max", switchtmp , "1");
        ALOGE("zxw property_get dm ram_max success");
        strlcpy(fakeswitchram, switchtmp, sizeof(fakeswitchram));
     }
     {
        ALOGE("zxw property_get ram_max success");
        strlcpy(fakeram, tmp, sizeof(fakeram));
     }
    //if(strcmp(fakeram,"1") != 0)
    {
      if (isDmSwitch == 1) {
        fdswitchram = open("/proc/meminfotest",O_RDWR);
        if(fdswitchram < 0)
        {
            ALOGE("zxw open proc/meminfotest faile");
        }
        write(fdswitchram,fakeswitchram,sizeof(fakeswitchram));
      } else {
        fdswitchram = open("/proc/meminfotest",O_RDWR);
        if(fdswitchram < 0)
        {
            ALOGE("zxw open proc/meminfotest faile");
        }
        write(fdswitchram,fakeram,sizeof(fakeram));
      }
      
        fdram = open("/proc/meminfo",O_RDWR);
        if(fdram < 0)
        {
            ALOGE("zxw open proc/meminfo faile");
        }
        write(fdram,fakeram,sizeof(fakeram));  
    }
    //[BIRD] [BIRD_SYSTEM_APP_FOR_DM] chengshujiang 20170608 begin
    if (isSystemAppForDm == 1) {
      property_get("persist.sys.tt.cpu_model", cputmp , "ccc");
      property_get("persist.sys.dm.cpu_model", cpudmtmp , "ccc");
      
    if (strcmp(cputmp,"ccc") != 0) {
        strlcpy(fakecpu, cputmp, sizeof(fakecpu));
        fdcpu = open("/proc/cputestinfo",O_RDWR);
        if (fdcpu < 0) {
           ALOGE("zxw open proc/cputestinfo faile");
        }
        write(fdcpu,fakecpu,sizeof(fakecpu));
      }
      
     if (strcmp(cpudmtmp,"ccc") != 0) {
          strlcpy(fakecpudm, cpudmtmp, sizeof(fakecpudm));
          if (isDmSwitch == 1) {
            fdcpudm = open("/proc/cputestinfodm",O_RDWR);
            if(fdcpudm < 0)
            {
                ALOGE("zxw open proc/cputestinfodm faile");
            }
            write(fdcpudm,fakecpudm,sizeof(fakecpudm));
          } else {
            fdcpudm = open("/proc/cputestinfodm",O_RDWR);
            if(fdcpudm < 0)
            {
                ALOGE("zxw open proc/cputestinfodm faile");
            }
            write(fdcpudm,fakecpu,sizeof(fakecpu));
          }
      }
    }
    //[BIRD] [BIRD_SYSTEM_APP_FOR_DM] chengshujiang 20170608 end
    android::sp<android::IServiceManager> serviceManager = android::defaultServiceManager();
    android::IPCThreadState::self()->joinThreadPool();
    ALOGI("Done");
    return 0;
}
