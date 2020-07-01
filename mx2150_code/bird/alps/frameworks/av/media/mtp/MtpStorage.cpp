/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "MtpStorage"

#include "MtpDebug.h"
#include "MtpDatabase.h"
#include "MtpStorage.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/statfs.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <string.h>
#include <cstring>
#include <stdio.h>
#include <limits.h>
#include <inttypes.h>

// switch log level for user build
#ifdef MTK_USER_BUILD
#undef ALOGD
#define ALOGD ALOGV
#endif

#include <cutils/properties.h>
#include "cutils/xlog.h"

namespace android {

MtpStorage::MtpStorage(MtpStorageID id, const char* filePath,
        const char* description, uint64_t reserveSpace,
        bool removable, uint64_t maxFileSize)
    :   mStorageID(id),
        mFilePath(filePath),
        mDescription(description),
        mMaxCapacity(0),
        mMaxFileSize(maxFileSize),
        mReserveSpace(reserveSpace),
        mRemovable(removable)
{
    ALOGD("MtpStorage id: %d path: %s\n", id, filePath);
}

MtpStorage::~MtpStorage() {
}

int MtpStorage::getType() const {
    return (mRemovable ? MTP_STORAGE_REMOVABLE_RAM :  MTP_STORAGE_FIXED_RAM);
}

int MtpStorage::getFileSystemType() const {
    return MTP_STORAGE_FILESYSTEM_HIERARCHICAL;
}

int MtpStorage::getAccessCapability() const {
    return MTP_STORAGE_READ_WRITE;
}

uint64_t MtpStorage::getMaxCapacity() {
    if (mMaxCapacity == 0) {
        struct statfs   stat;
        if (statfs(getPath(), &stat))
            return -1;
        mMaxCapacity = (uint64_t)stat.f_blocks * (uint64_t)stat.f_bsize;
        //[BIRD][BIRD_SYSTEM_APP]: chl++ 20160913 for fake rom on pc display begin
        //TLB: chl++ 20160913 for fake rom on pc display begin 
        char value[255];
        property_get("persist.sys.fake_rom", value, "0");
        int fake_rom = atoi(value);
        char str_flash[255];
        int real_rom = 8;
        //float add_system_to_data = 0.0f;
        property_get("ro.bird.memory.flash", str_flash, "BIRD_EMMC_64_8");    
        ALOGI("MtpStorage::getMaxCapacity: 333  fake_rom = %d, real_rom = %d,  flash = %s \n", fake_rom, real_rom, str_flash);
        // strcpy_s(str_flash, strlen(str_flash.c_str()) + 1, str_flash.c_str());  
        // if (strncmp(str_flash, "BIRD_EMMC_32", 12)==0) {
        if (strstr(str_flash, "32_4") != NULL) {
            ALOGI("MtpStorage::getMaxCapacity: real_rom = 4 \n");
            real_rom = 4;
        } else if (strstr(str_flash, "64_8") != NULL) {
            ALOGI("MtpStorage::getMaxCapacity: real_rom = 8 \n");
            real_rom = 8;
        } else if (strstr(str_flash, "128_8") != NULL || strstr(str_flash, "128_16") != NULL ||strstr(str_flash, "128_24") != NULL) {
            ALOGI("MtpStorage::getMaxCapacity: real_rom = 16 \n");
            real_rom = 16;
        } else if (strstr(str_flash, "256_16") != NULL || strstr(str_flash, "256_24") != NULL) {
            ALOGI("MtpStorage::getMaxCapacity: real_rom = 32 \n");
            real_rom = 32;
        }
        ALOGI("MtpStorage::getMaxCapacity: 4444  fake_rom = %d, real_rom = %d,  flash = %s \n", fake_rom, real_rom, str_flash);

       ALOGI("MtpStorage::getMaxCapacity: getPath() = %s \n",getPath());

        if (fake_rom != 0) {
            if(strcmp(getPath(),"/storage/emulated/0") == 0){
                //shared sdcard
                ALOGI("MtpStorage::getMaxCapacity: 0000  \n");
                mMaxCapacity = (uint64_t) fake_rom * 1024 * 1024 * 1024L;
            }
        } else {
            if(strcmp(getPath(),"/storage/emulated/0") == 0){
                //shared sdcard
                ALOGI("MtpStorage::getMaxCapacity: 111  \n");
                mMaxCapacity = (uint64_t) real_rom * 1024 * 1024 * 1024L;
            }
        }
        //TLB: chl++ 20160913 for fake rom on pc display end 

        char value22[255];
        property_get("ro.bird_system_app", value22, "0");
        int isSystemAppFake = atoi(value22);

        if(isSystemAppFake != 0){
            property_get("persist.sys.tt.rom_sd", value22, "");
            uint64_t totalsize = atoi(value22);
            
            if (strcmp(getPath(),"/storage/emulated/0")==0) {
                //shared sdcard
                if (strlen(value22)!=0) {
                    ALOGI("BIRD_SYSTEM_APP MtpStorage::getMaxCapacity: 0000  \n");
                    mMaxCapacity = (uint64_t) totalsize * 1024 * 1024L;
                } 
            }
        }
    //[BIRD][BIRD_SYSTEM_APP]: chl++ 20160913 for fake rom on pc display end 
    }

    ALOGD("MtpStorage mMaxCapacity = %" PRIu64 "\n", mMaxCapacity);
    return mMaxCapacity;
}

uint64_t MtpStorage::getFreeSpace() {
    struct statfs   stat;
    if (statfs(getPath(), &stat))
        return -1;
    uint64_t freeSpace = (uint64_t)stat.f_bavail * (uint64_t)stat.f_bsize;

    //[BIRD][BIRD_SYSTEM_APP]: chl++ 20160913 for fake rom on pc display begin
    char value[255];
    property_get("ro.bird_system_app", value, "0");
    int isSystemAppFake = atoi(value);
    bool bflag = true;
        
    if (isSystemAppFake != 0) {
        char valueRom_sd_b[255];
        char valueRom_sd[255];
        property_get("persist.sys.tt.rom_sd_b", valueRom_sd_b, "");
        property_get("persist.sys.tt.rom_sd", valueRom_sd, "");
        if ((strlen(valueRom_sd_b)==0) ||(strlen(valueRom_sd)==0)) {
            bflag = false;
        }

        if (bflag && (strcmp(getPath(),"/storage/emulated/0")==0)) {
            //shared sdcard
            uint64_t realValue = atoi(valueRom_sd_b);
            uint64_t currentValue = atoi(valueRom_sd);
            freeSpace =  (uint64_t)(freeSpace * currentValue / realValue);

            //[BIRD] [BIRD_SYSTEM_APP_FOR_DM] chengshujiang 20170608 begin
            char bbb[255];
            property_get("ro.bird_system_app_for_dm", bbb, "0");
            int isSystemForDm = atoi(bbb);
            if (isSystemForDm != 0) {
                //[BIRD][BIRD_SYSTEM_APP_DEFAULTRAMROM]huangzhangbin 20171123 begin
                char defRom[255];
                property_get("ro.bird_system_for_ramrom", defRom, "0");
                int isDefRom = atoi(defRom);
                ALOGI("BIRD_SYSTEM_APP MtpStorage::getFreeSpace111: isDefRom = %d  \n",isDefRom);
                if (isDefRom) {
                    char str_flash[255];
                    int real_rom = 8;
                    property_get("ro.bird.memory.flash", str_flash, "BIRD_EMMC_64_8");    
                    if (strstr(str_flash, "32_4") != NULL) {
                        real_rom = 4;
                    } else if (strstr(str_flash, "64_8") != NULL) {
                        real_rom = 8;
                    } else if (strstr(str_flash, "128_8") != NULL || strstr(str_flash, "128_16") != NULL ||strstr(str_flash, "128_24") != NULL) {
                        real_rom = 16;
                    } else if (strstr(str_flash, "256_16") != NULL || strstr(str_flash, "256_24") != NULL) {
                        real_rom = 32;
                    }
                    freeSpace = (uint64_t)stat.f_bavail * (uint64_t)stat.f_bsize + (uint64_t) (currentValue - real_rom * 1024) * 1024 * 1024L;
                } else {
                    freeSpace = (uint64_t)stat.f_bavail * (uint64_t)stat.f_bsize + (uint64_t) (currentValue - realValue) * 1024 * 1024L;
                }
                //[BIRD][BIRD_SYSTEM_APP_DEFAULTRAMROM]huangzhangbin 20171123 end
            }
            //[BIRD] [BIRD_SYSTEM_APP_FOR_DM] chengshujiang 20170608 end
            ALOGI("BIRD_SYSTEM_APP MtpStorage::getFreeSpace: freeSpace = %lld  \n",freeSpace);
        }
    }
    //[BIRD][BIRD_SYSTEM_APP]: chl++ 20160913 for fake rom on pc display end 
    ALOGD("MtpStorage freeSpace = %" PRIu64 ", mReserveSpace = %" PRIu64 "\n", freeSpace, mReserveSpace);
    //[BIRD][BIRD_SYSTEM_APP]: chengshujiang 201601125 begin
    return freeSpace;//(freeSpace > mReserveSpace ? freeSpace - mReserveSpace : 0);
    //[BIRD][BIRD_SYSTEM_APP]: chengshujiang 201601125 end
}

const char* MtpStorage::getDescription() const {
    return (const char *)mDescription;
}

bool MtpStorage::setDescription(const char* description) {
    ALOGV("MtpStorage description = %s \n", description);

    mDescription.clear();
    mDescription.setTo(description);
    return true;
}

}  // namespace android
