/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
**
** Copyright 2010, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


//#define LOG_NDEBUG 0
#define LOG_TAG "MediaProfiles"

#include <stdlib.h>
#include <utils/Log.h>
#include <utils/Vector.h>
#include <cutils/properties.h>
#include <expat.h>
#include <media/MediaProfiles.h>
#include <media/stagefright/foundation/ADebug.h>
#include <OMX_Video.h>

#ifdef MTK_AOSP_ENHANCEMENT
#include <sys/sysconf.h>
//#include <asm/page.h> //64bit workaround

#include "venc_drv_if_public.h"
#include "val_types_public.h"

#include <fcntl.h>
#include <sys/ioctl.h>
//for devinfo
#define DEV_IOC_MAGIC       'd'
#define READ_DEV_DATA       _IOR(DEV_IOC_MAGIC,  1, unsigned int)
#endif//MTK_AOSP_ENHANCEMENT

namespace android {

#ifdef MTK_AOSP_ENHANCEMENT
static int getVideoCapability(int i4VideoFormat,unsigned int *pu4Width, unsigned int *pu4Height, unsigned int *pu4BitRatem, unsigned int *pu4FrameRate, int slowmotion=0);
MediaProfiles *MediaProfiles::sInstanceMtkDefault= NULL;
#endif

Mutex MediaProfiles::sLock;
bool MediaProfiles::sIsInitialized = false;
MediaProfiles *MediaProfiles::sInstance = NULL;

const MediaProfiles::NameToTagMap MediaProfiles::sVideoEncoderNameMap[] = {
    {"h263", VIDEO_ENCODER_H263},
    {"h264", VIDEO_ENCODER_H264},
    {"m4v",  VIDEO_ENCODER_MPEG_4_SP},
    {"hevc", VIDEO_ENCODER_HEVC}
};

const MediaProfiles::NameToTagMap MediaProfiles::sAudioEncoderNameMap[] = {
    {"amrnb",  AUDIO_ENCODER_AMR_NB},
    {"amrwb",  AUDIO_ENCODER_AMR_WB},
    {"aac",    AUDIO_ENCODER_AAC},
    {"heaac",  AUDIO_ENCODER_HE_AAC},
    {"aaceld", AUDIO_ENCODER_AAC_ELD}
};

const MediaProfiles::NameToTagMap MediaProfiles::sFileFormatMap[] = {
    {"3gp", OUTPUT_FORMAT_THREE_GPP},
    {"mp4", OUTPUT_FORMAT_MPEG_4}
};

const MediaProfiles::NameToTagMap MediaProfiles::sVideoDecoderNameMap[] = {
    {"wmv", VIDEO_DECODER_WMV}
};

const MediaProfiles::NameToTagMap MediaProfiles::sAudioDecoderNameMap[] = {
    {"wma", AUDIO_DECODER_WMA}
};

const MediaProfiles::NameToTagMap MediaProfiles::sCamcorderQualityNameMap[] = {
    {"low", CAMCORDER_QUALITY_LOW},
    {"high", CAMCORDER_QUALITY_HIGH},
    {"qcif", CAMCORDER_QUALITY_QCIF},
    {"cif", CAMCORDER_QUALITY_CIF},
    {"480p", CAMCORDER_QUALITY_480P},
    {"720p", CAMCORDER_QUALITY_720P},
    {"1080p", CAMCORDER_QUALITY_1080P},
    {"2160p", CAMCORDER_QUALITY_2160P},
    {"qvga", CAMCORDER_QUALITY_QVGA},

    {"timelapselow",  CAMCORDER_QUALITY_TIME_LAPSE_LOW},
    {"timelapsehigh", CAMCORDER_QUALITY_TIME_LAPSE_HIGH},
    {"timelapseqcif", CAMCORDER_QUALITY_TIME_LAPSE_QCIF},
    {"timelapsecif", CAMCORDER_QUALITY_TIME_LAPSE_CIF},
    {"timelapse480p", CAMCORDER_QUALITY_TIME_LAPSE_480P},
    {"timelapse720p", CAMCORDER_QUALITY_TIME_LAPSE_720P},
    {"timelapse1080p", CAMCORDER_QUALITY_TIME_LAPSE_1080P},
    {"timelapse2160p", CAMCORDER_QUALITY_TIME_LAPSE_2160P},
    {"timelapseqvga", CAMCORDER_QUALITY_TIME_LAPSE_QVGA},

    {"highspeedlow",  CAMCORDER_QUALITY_HIGH_SPEED_LOW},
    {"highspeedhigh", CAMCORDER_QUALITY_HIGH_SPEED_HIGH},
    {"highspeed480p", CAMCORDER_QUALITY_HIGH_SPEED_480P},
    {"highspeed720p", CAMCORDER_QUALITY_HIGH_SPEED_720P},
    {"highspeed1080p", CAMCORDER_QUALITY_HIGH_SPEED_1080P},
    {"highspeed2160p", CAMCORDER_QUALITY_HIGH_SPEED_2160P},

#if defined(MTK_AOSP_ENHANCEMENT) || defined(MTK_EMULATOR_SUPPORT)
    {"mtklow", CAMCORDER_QUALITY_MTK_LOW},
    {"mtkmedium", CAMCORDER_QUALITY_MTK_MEDIUM},
    {"mtkhigh", CAMCORDER_QUALITY_MTK_HIGH},
    {"mtkfine", CAMCORDER_QUALITY_MTK_FINE},

    {"mtknightlow", CAMCORDER_QUALITY_MTK_NIGHT_LOW},
    {"mtknightmedium", CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM},
    {"mtknighthigh", CAMCORDER_QUALITY_MTK_NIGHT_HIGH},
    {"mtknightfine", CAMCORDER_QUALITY_MTK_NIGHT_FINE},

    {"mtkliveeffect", CAMCORDER_QUALITY_MTK_LIVE_EFFECT},
    {"mtkh264high", CAMCORDER_QUALITY_MTK_H264_HIGH},
    {"mtkmpeg41080p", CAMCORDER_QUALITY_MTK_MPEG4_1080P},
    {"mtkfine4k2k", CAMCORDER_QUALITY_MTK_FINE_4K2K},

    {"mtktimelapselow", CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW},
    {"mtktimelapsemedium", CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM},
    {"mtktimelapsehigh", CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH},
    {"mtktimelapsefine", CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE},

    {"mtktimelapsenightlow", CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW},
    {"mtktimelapsenightmedium", CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM},
    {"mtktimelapsenighthigh", CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH},
    {"mtktimelapsenightfine", CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE},

    {"mtktimelapseliveeffect", CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT},
    {"mtktimelapseh264high", CAMCORDER_QUALITY_MTK_TIME_LAPSE_H264_HIGH},
    {"mtktimelapsempeg41080p", CAMCORDER_QUALITY_MTK_TIME_LAPSE_MPEG4_1080P},
    {"mtktimelapsefine4k2k", CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE_4K2K},

    {"mtkslowmotionlow", CAMCORDER_QUALITY_MTK_SLOW_MOTION_LOW},
    {"mtkslowmotionmedium", CAMCORDER_QUALITY_MTK_SLOW_MOTION_MEDIUM},
    {"mtkslowmotionhigh", CAMCORDER_QUALITY_MTK_SLOW_MOTION_HIGH},
    {"mtkslowmotionfine", CAMCORDER_QUALITY_MTK_SLOW_MOTION_FINE},

    {"mtkvga120", CAMCORDER_QUALITY_MTK_VGA_120},
    {"mtk720p60", CAMCORDER_QUALITY_MTK_720P_60},
    {"mtk720p120", CAMCORDER_QUALITY_MTK_720P_120},
    {"mtk720p180", CAMCORDER_QUALITY_MTK_720P_180},
    {"mtk720p240", CAMCORDER_QUALITY_MTK_720P_240},
    {"mtk1080p60", CAMCORDER_QUALITY_MTK_1080P_60},
    {"mtk1080p120", CAMCORDER_QUALITY_MTK_1080P_120},
#endif//not (MTK_AOSP_ENHANCEMENT || MTK_EMULATOR_SUPPORT)
};

#if LOG_NDEBUG
#define UNUSED __unused
#else
#define UNUSED
#endif

/*static*/ void
MediaProfiles::logVideoCodec(const MediaProfiles::VideoCodec& codec UNUSED)
{
    ALOGV("video codec:");
    ALOGV("codec = %d", codec.mCodec);
    ALOGV("bit rate: %d", codec.mBitRate);
    ALOGV("frame width: %d", codec.mFrameWidth);
    ALOGV("frame height: %d", codec.mFrameHeight);
    ALOGV("frame rate: %d", codec.mFrameRate);
}

/*static*/ void
MediaProfiles::logAudioCodec(const MediaProfiles::AudioCodec& codec UNUSED)
{
    ALOGV("audio codec:");
    ALOGV("codec = %d", codec.mCodec);
    ALOGV("bit rate: %d", codec.mBitRate);
    ALOGV("sample rate: %d", codec.mSampleRate);
    ALOGV("number of channels: %d", codec.mChannels);
}

/*static*/ void
MediaProfiles::logVideoEncoderCap(const MediaProfiles::VideoEncoderCap& cap UNUSED)
{
    ALOGV("video encoder cap:");
    ALOGV("codec = %d", cap.mCodec);
    ALOGV("bit rate: min = %d and max = %d", cap.mMinBitRate, cap.mMaxBitRate);
    ALOGV("frame width: min = %d and max = %d", cap.mMinFrameWidth, cap.mMaxFrameWidth);
    ALOGV("frame height: min = %d and max = %d", cap.mMinFrameHeight, cap.mMaxFrameHeight);
    ALOGV("frame rate: min = %d and max = %d", cap.mMinFrameRate, cap.mMaxFrameRate);
}

/*static*/ void
MediaProfiles::logAudioEncoderCap(const MediaProfiles::AudioEncoderCap& cap UNUSED)
{
    ALOGV("audio encoder cap:");
    ALOGV("codec = %d", cap.mCodec);
    ALOGV("bit rate: min = %d and max = %d", cap.mMinBitRate, cap.mMaxBitRate);
    ALOGV("sample rate: min = %d and max = %d", cap.mMinSampleRate, cap.mMaxSampleRate);
    ALOGV("number of channels: min = %d and max = %d", cap.mMinChannels, cap.mMaxChannels);
}

/*static*/ void
MediaProfiles::logVideoDecoderCap(const MediaProfiles::VideoDecoderCap& cap UNUSED)
{
    ALOGV("video decoder cap:");
    ALOGV("codec = %d", cap.mCodec);
}

/*static*/ void
MediaProfiles::logAudioDecoderCap(const MediaProfiles::AudioDecoderCap& cap UNUSED)
{
    ALOGV("audio codec cap:");
    ALOGV("codec = %d", cap.mCodec);
}

/*static*/ int
MediaProfiles::findTagForName(const MediaProfiles::NameToTagMap *map, size_t nMappings,
        const char *name)
{
    int tag = -1;
    for (size_t i = 0; i < nMappings; ++i) {
        if (!strcmp(map[i].name, name)) {
            tag = map[i].tag;
            break;
        }
    }
    return tag;
}

/*static*/ MediaProfiles::VideoCodec*
MediaProfiles::createVideoCodec(const char **atts, MediaProfiles *profiles)
{
    CHECK(!strcmp("codec",     atts[0]) &&
          !strcmp("bitRate",   atts[2]) &&
          !strcmp("width",     atts[4]) &&
          !strcmp("height",    atts[6]) &&
          !strcmp("frameRate", atts[8]));

    const size_t nMappings = sizeof(sVideoEncoderNameMap)/sizeof(sVideoEncoderNameMap[0]);
    const int codec = findTagForName(sVideoEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(static_cast<video_encoder>(codec),
            atoi(atts[3]), atoi(atts[5]), atoi(atts[7]), atoi(atts[9]));
    logVideoCodec(*videoCodec);

    size_t nCamcorderProfiles;
    CHECK((nCamcorderProfiles = profiles->mCamcorderProfiles.size()) >= 1);
    profiles->mCamcorderProfiles[nCamcorderProfiles - 1]->mVideoCodec = videoCodec;
    return videoCodec;
}

/*static*/ MediaProfiles::AudioCodec*
MediaProfiles::createAudioCodec(const char **atts, MediaProfiles *profiles)
{
    CHECK(!strcmp("codec",      atts[0]) &&
          !strcmp("bitRate",    atts[2]) &&
          !strcmp("sampleRate", atts[4]) &&
          !strcmp("channels",   atts[6]));
    const size_t nMappings = sizeof(sAudioEncoderNameMap)/sizeof(sAudioEncoderNameMap[0]);
    const int codec = findTagForName(sAudioEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::AudioCodec *audioCodec =
        new MediaProfiles::AudioCodec(static_cast<audio_encoder>(codec),
            atoi(atts[3]), atoi(atts[5]), atoi(atts[7]));
    logAudioCodec(*audioCodec);

    size_t nCamcorderProfiles;
    CHECK((nCamcorderProfiles = profiles->mCamcorderProfiles.size()) >= 1);
    profiles->mCamcorderProfiles[nCamcorderProfiles - 1]->mAudioCodec = audioCodec;
    return audioCodec;
}
/*static*/ MediaProfiles::AudioDecoderCap*
MediaProfiles::createAudioDecoderCap(const char **atts)
{
    CHECK(!strcmp("name",    atts[0]) &&
          !strcmp("enabled", atts[2]));

    const size_t nMappings = sizeof(sAudioDecoderNameMap)/sizeof(sAudioDecoderNameMap[0]);
    const int codec = findTagForName(sAudioDecoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::AudioDecoderCap *cap =
        new MediaProfiles::AudioDecoderCap(static_cast<audio_decoder>(codec));
    logAudioDecoderCap(*cap);
    return cap;
}

/*static*/ MediaProfiles::VideoDecoderCap*
MediaProfiles::createVideoDecoderCap(const char **atts)
{
    CHECK(!strcmp("name",    atts[0]) &&
          !strcmp("enabled", atts[2]));

    const size_t nMappings = sizeof(sVideoDecoderNameMap)/sizeof(sVideoDecoderNameMap[0]);
    const int codec = findTagForName(sVideoDecoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::VideoDecoderCap *cap =
        new MediaProfiles::VideoDecoderCap(static_cast<video_decoder>(codec));
    logVideoDecoderCap(*cap);
    return cap;
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createVideoEncoderCap(const char **atts)
{
    CHECK(!strcmp("name",           atts[0])  &&
          !strcmp("enabled",        atts[2])  &&
          !strcmp("minBitRate",     atts[4])  &&
          !strcmp("maxBitRate",     atts[6])  &&
          !strcmp("minFrameWidth",  atts[8])  &&
          !strcmp("maxFrameWidth",  atts[10]) &&
          !strcmp("minFrameHeight", atts[12]) &&
          !strcmp("maxFrameHeight", atts[14]) &&
          !strcmp("minFrameRate",   atts[16]) &&
          !strcmp("maxFrameRate",   atts[18]));

    const size_t nMappings = sizeof(sVideoEncoderNameMap)/sizeof(sVideoEncoderNameMap[0]);
    const int codec = findTagForName(sVideoEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::VideoEncoderCap *cap =
        new MediaProfiles::VideoEncoderCap(static_cast<video_encoder>(codec),
            atoi(atts[5]), atoi(atts[7]), atoi(atts[9]), atoi(atts[11]), atoi(atts[13]),
            atoi(atts[15]), atoi(atts[17]), atoi(atts[19]));
    logVideoEncoderCap(*cap);
    return cap;
}

/*static*/ MediaProfiles::AudioEncoderCap*
MediaProfiles::createAudioEncoderCap(const char **atts)
{
    CHECK(!strcmp("name",          atts[0])  &&
          !strcmp("enabled",       atts[2])  &&
          !strcmp("minBitRate",    atts[4])  &&
          !strcmp("maxBitRate",    atts[6])  &&
          !strcmp("minSampleRate", atts[8])  &&
          !strcmp("maxSampleRate", atts[10]) &&
          !strcmp("minChannels",   atts[12]) &&
          !strcmp("maxChannels",   atts[14]));

    const size_t nMappings = sizeof(sAudioEncoderNameMap)/sizeof(sAudioEncoderNameMap[0]);
    const int codec = findTagForName(sAudioEncoderNameMap, nMappings, atts[1]);
    CHECK(codec != -1);

    MediaProfiles::AudioEncoderCap *cap =
        new MediaProfiles::AudioEncoderCap(static_cast<audio_encoder>(codec), atoi(atts[5]),
            atoi(atts[7]), atoi(atts[9]), atoi(atts[11]), atoi(atts[13]), atoi(atts[15]));
    logAudioEncoderCap(*cap);
    return cap;
}

/*static*/ output_format
MediaProfiles::createEncoderOutputFileFormat(const char **atts)
{
    CHECK(!strcmp("name", atts[0]));

    const size_t nMappings =sizeof(sFileFormatMap)/sizeof(sFileFormatMap[0]);
    const int format = findTagForName(sFileFormatMap, nMappings, atts[1]);
    CHECK(format != -1);

    return static_cast<output_format>(format);
}

static bool isCameraIdFound(int cameraId, const Vector<int>& cameraIds) {
    for (int i = 0, n = cameraIds.size(); i < n; ++i) {
        if (cameraId == cameraIds[i]) {
            return true;
        }
    }
    return false;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createCamcorderProfile(int cameraId, const char **atts, Vector<int>& cameraIds)
{
    CHECK(!strcmp("quality",    atts[0]) &&
          !strcmp("fileFormat", atts[2]) &&
          !strcmp("duration",   atts[4]));

    const size_t nProfileMappings = sizeof(sCamcorderQualityNameMap)/
            sizeof(sCamcorderQualityNameMap[0]);
    const int quality = findTagForName(sCamcorderQualityNameMap, nProfileMappings, atts[1]);
    CHECK(quality != -1);

    const size_t nFormatMappings = sizeof(sFileFormatMap)/sizeof(sFileFormatMap[0]);
    const int fileFormat = findTagForName(sFileFormatMap, nFormatMappings, atts[3]);
    CHECK(fileFormat != -1);

    MediaProfiles::CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = cameraId;
    if (!isCameraIdFound(cameraId, cameraIds)) {
        cameraIds.add(cameraId);
    }
    profile->mFileFormat = static_cast<output_format>(fileFormat);
    profile->mQuality = static_cast<camcorder_quality>(quality);
    profile->mDuration = atoi(atts[5]);
    return profile;
}

MediaProfiles::ImageEncodingQualityLevels*
MediaProfiles::findImageEncodingQualityLevels(int cameraId) const
{
    int n = mImageEncodingQualityLevels.size();
    for (int i = 0; i < n; i++) {
        ImageEncodingQualityLevels *levels = mImageEncodingQualityLevels[i];
        if (levels->mCameraId == cameraId) {
            return levels;
        }
    }
    return NULL;
}

void MediaProfiles::addImageEncodingQualityLevel(int cameraId, const char** atts)
{
    CHECK(!strcmp("quality", atts[0]));
    int quality = atoi(atts[1]);
    ALOGV("%s: cameraId=%d, quality=%d", __func__, cameraId, quality);
    ImageEncodingQualityLevels *levels = findImageEncodingQualityLevels(cameraId);

    if (levels == NULL) {
        levels = new ImageEncodingQualityLevels();
        levels->mCameraId = cameraId;
        mImageEncodingQualityLevels.add(levels);
    }

    levels->mLevels.add(quality);
}

/*static*/ int
MediaProfiles::getCameraId(const char** atts)
{
    if (!atts[0]) return 0;  // default cameraId = 0
    CHECK(!strcmp("cameraId", atts[0]));
    return atoi(atts[1]);
}

void MediaProfiles::addStartTimeOffset(int cameraId, const char** atts)
{
    int offsetTimeMs = 1000;
    if (atts[2]) {
        CHECK(!strcmp("startOffsetMs", atts[2]));
        offsetTimeMs = atoi(atts[3]);
    }

    ALOGV("%s: cameraId=%d, offset=%d ms", __func__, cameraId, offsetTimeMs);
    mStartTimeOffsets.replaceValueFor(cameraId, offsetTimeMs);
}

/*static*/ void
MediaProfiles::startElementHandler(void *userData, const char *name, const char **atts)
{
    MediaProfiles *profiles = (MediaProfiles *) userData;
    if (strcmp("Video", name) == 0) {
        createVideoCodec(atts, profiles);
    } else if (strcmp("Audio", name) == 0) {
        createAudioCodec(atts, profiles);
    } else if (strcmp("VideoEncoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mVideoEncoders.add(createVideoEncoderCap(atts));
    } else if (strcmp("AudioEncoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mAudioEncoders.add(createAudioEncoderCap(atts));
    } else if (strcmp("VideoDecoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mVideoDecoders.add(createVideoDecoderCap(atts));
    } else if (strcmp("AudioDecoderCap", name) == 0 &&
               strcmp("true", atts[3]) == 0) {
        profiles->mAudioDecoders.add(createAudioDecoderCap(atts));
    } else if (strcmp("EncoderOutputFileFormat", name) == 0) {
        profiles->mEncoderOutputFileFormats.add(createEncoderOutputFileFormat(atts));
    } else if (strcmp("CamcorderProfiles", name) == 0) {
        profiles->mCurrentCameraId = getCameraId(atts);
        profiles->addStartTimeOffset(profiles->mCurrentCameraId, atts);
    } else if (strcmp("EncoderProfile", name) == 0) {
        profiles->mCamcorderProfiles.add(
            createCamcorderProfile(profiles->mCurrentCameraId, atts, profiles->mCameraIds));
    } else if (strcmp("ImageEncoding", name) == 0) {
        profiles->addImageEncodingQualityLevel(profiles->mCurrentCameraId, atts);
    }
}

static bool isCamcorderProfile(camcorder_quality quality) {
    return quality >= CAMCORDER_QUALITY_LIST_START &&
           quality <= CAMCORDER_QUALITY_LIST_END;
}

static bool isTimelapseProfile(camcorder_quality quality) {
    return quality >= CAMCORDER_QUALITY_TIME_LAPSE_LIST_START &&
           quality <= CAMCORDER_QUALITY_TIME_LAPSE_LIST_END;
}

static bool isHighSpeedProfile(camcorder_quality quality) {
    return quality >= CAMCORDER_QUALITY_HIGH_SPEED_LIST_START &&
           quality <= CAMCORDER_QUALITY_HIGH_SPEED_LIST_END;
}

void MediaProfiles::initRequiredProfileRefs(const Vector<int>& cameraIds) {
    ALOGV("Number of camera ids: %zu", cameraIds.size());
    CHECK(cameraIds.size() > 0);
    mRequiredProfileRefs = new RequiredProfiles[cameraIds.size()];
    for (size_t i = 0, n = cameraIds.size(); i < n; ++i) {
        mRequiredProfileRefs[i].mCameraId = cameraIds[i];
        for (size_t j = 0; j < kNumRequiredProfiles; ++j) {
            mRequiredProfileRefs[i].mRefs[j].mHasRefProfile = false;
            mRequiredProfileRefs[i].mRefs[j].mRefProfileIndex = -1;
            if ((j & 1) == 0) {  // low resolution
                mRequiredProfileRefs[i].mRefs[j].mResolutionProduct = 0x7FFFFFFF;
            } else {             // high resolution
                mRequiredProfileRefs[i].mRefs[j].mResolutionProduct = 0;
            }
        }
    }
}

int MediaProfiles::getRequiredProfileRefIndex(int cameraId) {
    for (size_t i = 0, n = mCameraIds.size(); i < n; ++i) {
        if (mCameraIds[i] == cameraId) {
            return i;
        }
    }
    return -1;
}

void MediaProfiles::checkAndAddRequiredProfilesIfNecessary() {
    if (sIsInitialized) {
        return;
    }

    initRequiredProfileRefs(mCameraIds);

    for (size_t i = 0, n = mCamcorderProfiles.size(); i < n; ++i) {
        int product = mCamcorderProfiles[i]->mVideoCodec->mFrameWidth *
                      mCamcorderProfiles[i]->mVideoCodec->mFrameHeight;

        camcorder_quality quality = mCamcorderProfiles[i]->mQuality;
        int cameraId = mCamcorderProfiles[i]->mCameraId;
        int index = -1;
        int refIndex = getRequiredProfileRefIndex(cameraId);
        CHECK(refIndex != -1);
        RequiredProfileRefInfo *info;
        camcorder_quality refQuality;

        // Check high and low from either camcorder profile, timelapse profile
        // or high speed profile, but not all of them. Default, check camcorder profile
        size_t j = 0;
        size_t o = 2;
        if (isTimelapseProfile(quality)) {
            // Check timelapse profile instead.
            j = 2;
            o = kNumRequiredProfiles;
        } else if (isHighSpeedProfile(quality)) {
            // Skip the check for high speed profile.
            continue;
        } else {
            // Must be camcorder profile.
            CHECK(isCamcorderProfile(quality));
        }
        for (; j < o; ++j) {
            info = &(mRequiredProfileRefs[refIndex].mRefs[j]);
            if ((j % 2 == 0 && product > info->mResolutionProduct) ||  // low
                (j % 2 != 0 && product < info->mResolutionProduct)) {  // high
                continue;
            }
            switch (j) {
                case 0:
                   refQuality = CAMCORDER_QUALITY_LOW;
                   break;
                case 1:
                   refQuality = CAMCORDER_QUALITY_HIGH;
                   break;
                case 2:
                   refQuality = CAMCORDER_QUALITY_TIME_LAPSE_LOW;
                   break;
                case 3:
                   refQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
                   break;
                default:
                    CHECK(!"Should never reach here");
            }

            if (!info->mHasRefProfile) {
                index = getCamcorderProfileIndex(cameraId, refQuality);
            }
            if (index == -1) {
                // New high or low quality profile is found.
                // Update its reference.
                info->mHasRefProfile = true;
                info->mRefProfileIndex = i;
                info->mResolutionProduct = product;
            }
        }
    }

    for (size_t cameraId = 0; cameraId < mCameraIds.size(); ++cameraId) {
        for (size_t j = 0; j < kNumRequiredProfiles; ++j) {
            int refIndex = getRequiredProfileRefIndex(cameraId);
            CHECK(refIndex != -1);
            RequiredProfileRefInfo *info =
                    &mRequiredProfileRefs[refIndex].mRefs[j];

            if (info->mHasRefProfile) {

                CamcorderProfile *profile =
                    new CamcorderProfile(
                            *mCamcorderProfiles[info->mRefProfileIndex]);

                // Overwrite the quality
                switch (j % kNumRequiredProfiles) {
                    case 0:
                        profile->mQuality = CAMCORDER_QUALITY_LOW;
                        break;
                    case 1:
                        profile->mQuality = CAMCORDER_QUALITY_HIGH;
                        break;
                    case 2:
                        profile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_LOW;
                        break;
                    case 3:
                        profile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
                        break;
                    default:
                        CHECK(!"Should never come here");
                }

                int index = getCamcorderProfileIndex(cameraId, profile->mQuality);
                if (index != -1) {
                    ALOGV("Profile quality %d for camera %zu already exists",
                        profile->mQuality, cameraId);
                    CHECK(index == refIndex);
                    continue;
                }

                // Insert the new profile
                ALOGV("Add a profile: quality %d=>%d for camera %zu",
                        mCamcorderProfiles[info->mRefProfileIndex]->mQuality,
                        profile->mQuality, cameraId);

                mCamcorderProfiles.add(profile);
            }
        }
    }
}

/*static*/ MediaProfiles*
MediaProfiles::getInstance()
{
    ALOGV("getInstance");
    Mutex::Autolock lock(sLock);
    if (!sIsInitialized) {
        char value[PROPERTY_VALUE_MAX];
        if (property_get("media.settings.xml", value, NULL) <= 0) {
            const char *defaultXmlFile = "/etc/media_profiles.xml";
            FILE *fp = fopen(defaultXmlFile, "r");
            if (fp == NULL) {
                ALOGW("could not find media config xml file");
                sInstance = createDefaultInstance();
            } else {
                fclose(fp);  // close the file first.
                sInstance = createInstanceFromXmlFile(defaultXmlFile);
            }
        } else {
            sInstance = createInstanceFromXmlFile(value);
        }
        CHECK(sInstance != NULL);
        sInstance->checkAndAddRequiredProfilesIfNecessary();
        sIsInitialized = true;
    }

    return sInstance;
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultH263VideoEncoderCap()
{
#ifdef MTK_AOSP_ENHANCEMENT
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;
    int iRet;
    iRet = getVideoCapability(VIDEO_ENCODER_H263, &u4Width, &u4Height, &u4BitRate, &u4FrameRate );
    ALOGI("[ %s ], support ret:%d maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",
            __FUNCTION__, iRet, u4Width, u4Height, u4BitRate, u4FrameRate);
    if (iRet > 0) {
        return new MediaProfiles::VideoEncoderCap(
                VIDEO_ENCODER_H263, 75*1000, u4BitRate,
                176, u4Width, 144, u4Height, 15, u4FrameRate);
    }
    else {
        ALOGE("[ERROR] don't support H263!!");
        return NULL;
    }
#else//not MTK_AOSP_ENHANCEMENT
    return new MediaProfiles::VideoEncoderCap(
        VIDEO_ENCODER_H263, 192000, 420000, 176, 352, 144, 288, 1, 20);
#endif//MTK_AOSP_ENHANCEMENT
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultM4vVideoEncoderCap()
{
#ifdef MTK_AOSP_ENHANCEMENT
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;
    int iRet;
    iRet = getVideoCapability(VIDEO_ENCODER_MPEG_4_SP, &u4Width, &u4Height, &u4BitRate, &u4FrameRate );
    ALOGI("[ %s ], support ret:%d maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",
            __FUNCTION__, iRet, u4Width, u4Height, u4BitRate, u4FrameRate);
    if (iRet > 0) {
        return new MediaProfiles::VideoEncoderCap(
                VIDEO_ENCODER_MPEG_4_SP, 75*1000, u4BitRate,
                176, u4Width, 144, u4Height, 15, u4FrameRate);
    }
    else {
        ALOGE("[ERROR] don't support MPEG4!!");
        return NULL;
    }
#else//not MTK_AOSP_ENHANCEMENT
    return new MediaProfiles::VideoEncoderCap(
        VIDEO_ENCODER_MPEG_4_SP, 192000, 420000, 176, 352, 144, 288, 1, 20);
#endif//MTK_AOSP_ENHANCEMENT
}

/*static*/ void
MediaProfiles::createDefaultVideoEncoders(MediaProfiles *profiles)
{
#ifdef MTK_AOSP_ENHANCEMENT
    MediaProfiles::VideoEncoderCap *ptmp = NULL;
    ptmp = createDefaultH264VideoEncoderCap();
    if(ptmp != NULL)
    {
        profiles->mVideoEncoders.add(ptmp);
    }
#ifdef MTK_VIDEO_HEVC_SUPPORT
    ptmp = createDefaultHEVCVideoEncoderCap();
    if(ptmp != NULL)
    {
        profiles->mVideoEncoders.add(ptmp);
    }
#endif//MTK_VIDEO_HEVC_SUPPORT

    ptmp = createDefaultH263VideoEncoderCap();
    if(ptmp != NULL)
    {
        profiles->mVideoEncoders.add(ptmp);
    }

    ptmp = createDefaultM4vVideoEncoderCap();
    if(ptmp != NULL)
    {
        profiles->mVideoEncoders.add(ptmp);
    }
#else
    profiles->mVideoEncoders.add(createDefaultH263VideoEncoderCap());
    profiles->mVideoEncoders.add(createDefaultM4vVideoEncoderCap());
#endif

}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderTimeLapseQcifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 1000000, 176, 144, 20);

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderTimeLapse480pProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 20000000, 720, 480, 20);

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ void
MediaProfiles::createDefaultCamcorderTimeLapseLowProfiles(
        MediaProfiles::CamcorderProfile **lowTimeLapseProfile,
        MediaProfiles::CamcorderProfile **lowSpecificTimeLapseProfile) {
    *lowTimeLapseProfile = createDefaultCamcorderTimeLapseQcifProfile(
            CAMCORDER_QUALITY_TIME_LAPSE_LOW);
    *lowSpecificTimeLapseProfile = createDefaultCamcorderTimeLapseQcifProfile(
            CAMCORDER_QUALITY_TIME_LAPSE_QCIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderTimeLapseHighProfiles(
        MediaProfiles::CamcorderProfile **highTimeLapseProfile,
        MediaProfiles::CamcorderProfile **highSpecificTimeLapseProfile) {
    *highTimeLapseProfile = createDefaultCamcorderTimeLapse480pProfile(
            CAMCORDER_QUALITY_TIME_LAPSE_HIGH);
    *highSpecificTimeLapseProfile = createDefaultCamcorderTimeLapse480pProfile(
            CAMCORDER_QUALITY_TIME_LAPSE_480P);
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderQcifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 192000, 176, 144, 20);

    MediaProfiles::AudioCodec *audioCodec =
        new MediaProfiles::AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);

    MediaProfiles::CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 30;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createDefaultCamcorderCifProfile(camcorder_quality quality)
{
    MediaProfiles::VideoCodec *videoCodec =
        new MediaProfiles::VideoCodec(VIDEO_ENCODER_H263, 360000, 352, 288, 20);

    AudioCodec *audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = 0;
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
    profile->mQuality = quality;
    profile->mDuration = 60;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ void
MediaProfiles::createDefaultCamcorderLowProfiles(
        MediaProfiles::CamcorderProfile **lowProfile,
        MediaProfiles::CamcorderProfile **lowSpecificProfile) {
    *lowProfile = createDefaultCamcorderQcifProfile(CAMCORDER_QUALITY_LOW);
    *lowSpecificProfile = createDefaultCamcorderQcifProfile(CAMCORDER_QUALITY_QCIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderHighProfiles(
        MediaProfiles::CamcorderProfile **highProfile,
        MediaProfiles::CamcorderProfile **highSpecificProfile) {
    *highProfile = createDefaultCamcorderCifProfile(CAMCORDER_QUALITY_HIGH);
    *highSpecificProfile = createDefaultCamcorderCifProfile(CAMCORDER_QUALITY_CIF);
}

/*static*/ void
MediaProfiles::createDefaultCamcorderProfiles(MediaProfiles *profiles)
{
#ifdef MTK_AOSP_ENHANCEMENT
    createMTKCamcorderProfiles(profiles);
#else//not MTK_AOSP_ENHANCEMENT
    // low camcorder profiles.
    MediaProfiles::CamcorderProfile *lowProfile, *lowSpecificProfile;
    createDefaultCamcorderLowProfiles(&lowProfile, &lowSpecificProfile);
    profiles->mCamcorderProfiles.add(lowProfile);
    profiles->mCamcorderProfiles.add(lowSpecificProfile);

    // high camcorder profiles.
    MediaProfiles::CamcorderProfile* highProfile, *highSpecificProfile;
    createDefaultCamcorderHighProfiles(&highProfile, &highSpecificProfile);
    profiles->mCamcorderProfiles.add(highProfile);
    profiles->mCamcorderProfiles.add(highSpecificProfile);

    // low camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *lowTimeLapseProfile, *lowSpecificTimeLapseProfile;
    createDefaultCamcorderTimeLapseLowProfiles(&lowTimeLapseProfile, &lowSpecificTimeLapseProfile);
    profiles->mCamcorderProfiles.add(lowTimeLapseProfile);
    profiles->mCamcorderProfiles.add(lowSpecificTimeLapseProfile);

    // high camcorder time lapse profiles.
    MediaProfiles::CamcorderProfile *highTimeLapseProfile, *highSpecificTimeLapseProfile;
    createDefaultCamcorderTimeLapseHighProfiles(&highTimeLapseProfile,
            &highSpecificTimeLapseProfile);
    profiles->mCamcorderProfiles.add(highTimeLapseProfile);
    profiles->mCamcorderProfiles.add(highSpecificTimeLapseProfile);

    // For emulator and other legacy devices which does not have a
    // media_profiles.xml file, We assume that the default camera id
    // is 0 and that is the only camera available.
    profiles->mCameraIds.push(0);
#endif//MTK_AOSP_ENHANCEMENT
}

/*static*/ void
MediaProfiles::createDefaultAudioEncoders(MediaProfiles *profiles)
{
    profiles->mAudioEncoders.add(createDefaultAmrNBEncoderCap());
//MTK80721 2011-12-14
#ifdef MTK_AOSP_ENHANCEMENT
    MediaProfiles::AudioEncoderCap* mAwbCap = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_AMR_WB, 6600, 28500, 16000, 16000, 1, 1);
    profiles->mAudioEncoders.add(mAwbCap);

    MediaProfiles::AudioEncoderCap* mAacCap = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_AAC, 8000, 160000, 12000, 48000, 1, 2);
    profiles->mAudioEncoders.add(mAacCap);

    //2012-10-09
    MediaProfiles::AudioEncoderCap* mAacCapHE = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_HE_AAC, 8000, 320000, 16000, 48000, 1, 2);
    profiles->mAudioEncoders.add(mAacCapHE);

    MediaProfiles::AudioEncoderCap* mAacCapELD = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_AAC_ELD, 16000, 320000, 16000, 48000, 1, 2);
    profiles->mAudioEncoders.add(mAacCapELD);
    //
    MediaProfiles::AudioEncoderCap* mVorbisCap = new MediaProfiles::AudioEncoderCap(AUDIO_ENCODER_VORBIS, 31980, 202960, 8000, 48000,  1, 2);
    profiles->mAudioEncoders.add(mVorbisCap);
#endif//MTK_AOSP_ENHANCEMENT
//
}

/*static*/ void
MediaProfiles::createDefaultVideoDecoders(MediaProfiles *profiles)
{
    MediaProfiles::VideoDecoderCap *cap =
        new MediaProfiles::VideoDecoderCap(VIDEO_DECODER_WMV);

    profiles->mVideoDecoders.add(cap);
}

/*static*/ void
MediaProfiles::createDefaultAudioDecoders(MediaProfiles *profiles)
{
    MediaProfiles::AudioDecoderCap *cap =
        new MediaProfiles::AudioDecoderCap(AUDIO_DECODER_WMA);

    profiles->mAudioDecoders.add(cap);
}

/*static*/ void
MediaProfiles::createDefaultEncoderOutputFileFormats(MediaProfiles *profiles)
{
    profiles->mEncoderOutputFileFormats.add(OUTPUT_FORMAT_THREE_GPP);
    profiles->mEncoderOutputFileFormats.add(OUTPUT_FORMAT_MPEG_4);
}

/*static*/ MediaProfiles::AudioEncoderCap*
MediaProfiles::createDefaultAmrNBEncoderCap()
{
    return new MediaProfiles::AudioEncoderCap(
        AUDIO_ENCODER_AMR_NB, 5525, 12200, 8000, 8000, 1, 1);
}

/*static*/ void
MediaProfiles::createDefaultImageEncodingQualityLevels(MediaProfiles *profiles)
{
    ImageEncodingQualityLevels *levels = new ImageEncodingQualityLevels();
    levels->mCameraId = 0;
#ifdef MTK_AOSP_ENHANCEMENT
    levels->mLevels.add(75);
    levels->mLevels.add(85);
    levels->mLevels.add(95);
#else//not MTK_AOSP_ENHANCEMENT
    levels->mLevels.add(70);
    levels->mLevels.add(80);
    levels->mLevels.add(90);
#endif//MTK_AOSP_ENHANCEMENT
    profiles->mImageEncodingQualityLevels.add(levels);

#ifdef MTK_AOSP_ENHANCEMENT
    ALOGD("FrontCameraLevels Setting\n");
    ImageEncodingQualityLevels *FrontCameraLevels = new ImageEncodingQualityLevels();
    FrontCameraLevels->mCameraId = 1;
    FrontCameraLevels->mLevels.add(75);
    FrontCameraLevels->mLevels.add(85);
    FrontCameraLevels->mLevels.add(95);
    profiles->mImageEncodingQualityLevels.add(FrontCameraLevels);

    ALOGD("Main2 CameraLevels Setting");
    ImageEncodingQualityLevels *Main2CameraLevels = new ImageEncodingQualityLevels();
    Main2CameraLevels->mCameraId = 2;
    Main2CameraLevels->mLevels.add(75);
    Main2CameraLevels->mLevels.add(85);
    Main2CameraLevels->mLevels.add(95);
    profiles->mImageEncodingQualityLevels.add(Main2CameraLevels);
#endif//MTK_AOSP_ENHANCEMENT
}

/*static*/ MediaProfiles*
MediaProfiles::createDefaultInstance()
{
    MediaProfiles *profiles = new MediaProfiles;
    createDefaultCamcorderProfiles(profiles);
    createDefaultVideoEncoders(profiles);
    createDefaultAudioEncoders(profiles);
    createDefaultVideoDecoders(profiles);
    createDefaultAudioDecoders(profiles);
    createDefaultEncoderOutputFileFormats(profiles);
    createDefaultImageEncodingQualityLevels(profiles);
    return profiles;
}

/*static*/ MediaProfiles*
MediaProfiles::createInstanceFromXmlFile(const char *xml)
{
    FILE *fp = NULL;
    CHECK((fp = fopen(xml, "r")));

    XML_Parser parser = ::XML_ParserCreate(NULL);
    CHECK(parser != NULL);

    MediaProfiles *profiles = new MediaProfiles();
    ::XML_SetUserData(parser, profiles);
    ::XML_SetElementHandler(parser, startElementHandler, NULL);

    /*
      FIXME:
      expat is not compiled with -DXML_DTD. We don't have DTD parsing support.

      if (!::XML_SetParamEntityParsing(parser, XML_PARAM_ENTITY_PARSING_ALWAYS)) {
          ALOGE("failed to enable DTD support in the xml file");
          return UNKNOWN_ERROR;
      }

    */

    const int BUFF_SIZE = 512;
    for (;;) {
        void *buff = ::XML_GetBuffer(parser, BUFF_SIZE);
        if (buff == NULL) {
            ALOGE("failed to in call to XML_GetBuffer()");
            delete profiles;
            profiles = NULL;
            goto exit;
        }

        int bytes_read = ::fread(buff, 1, BUFF_SIZE, fp);
        if (bytes_read < 0) {
            ALOGE("failed in call to read");
            delete profiles;
            profiles = NULL;
            goto exit;
        }

        CHECK(::XML_ParseBuffer(parser, bytes_read, bytes_read == 0));

        if (bytes_read == 0) break;  // done parsing the xml file
    }

exit:
    ::XML_ParserFree(parser);
    ::fclose(fp);
#ifdef MTK_AOSP_ENHANCEMENT
    xmlEnhancement(profiles);
    profiles->dumpProfiles();
#endif//MTK_AOSP_ENHANCEMENT
    return profiles;
}

Vector<output_format> MediaProfiles::getOutputFileFormats() const
{
    return mEncoderOutputFileFormats;  // copy out
}

Vector<video_encoder> MediaProfiles::getVideoEncoders() const
{
    Vector<video_encoder> encoders;
    for (size_t i = 0; i < mVideoEncoders.size(); ++i) {
        encoders.add(mVideoEncoders[i]->mCodec);
    }
    return encoders;  // copy out
}

int MediaProfiles::getVideoEncoderParamByName(const char *name, video_encoder codec) const
{
    ALOGV("getVideoEncoderParamByName: %s for codec %d", name, codec);
    int index = -1;
    for (size_t i = 0, n = mVideoEncoders.size(); i < n; ++i) {
        if (mVideoEncoders[i]->mCodec == codec) {
            index = i;
            break;
        }
    }
    if (index == -1) {
        ALOGE("The given video encoder %d is not found", codec);
        return -1;
    }

    if (!strcmp("enc.vid.width.min", name)) return mVideoEncoders[index]->mMinFrameWidth;
    if (!strcmp("enc.vid.width.max", name)) return mVideoEncoders[index]->mMaxFrameWidth;
    if (!strcmp("enc.vid.height.min", name)) return mVideoEncoders[index]->mMinFrameHeight;
    if (!strcmp("enc.vid.height.max", name)) return mVideoEncoders[index]->mMaxFrameHeight;
    if (!strcmp("enc.vid.bps.min", name)) return mVideoEncoders[index]->mMinBitRate;
    if (!strcmp("enc.vid.bps.max", name)) return mVideoEncoders[index]->mMaxBitRate;
    if (!strcmp("enc.vid.fps.min", name)) return mVideoEncoders[index]->mMinFrameRate;
    if (!strcmp("enc.vid.fps.max", name)) return mVideoEncoders[index]->mMaxFrameRate;

    ALOGE("The given video encoder param name %s is not found", name);
    return -1;
}

Vector<audio_encoder> MediaProfiles::getAudioEncoders() const
{
    Vector<audio_encoder> encoders;
    for (size_t i = 0; i < mAudioEncoders.size(); ++i) {
        encoders.add(mAudioEncoders[i]->mCodec);
    }
    return encoders;  // copy out
}

int MediaProfiles::getAudioEncoderParamByName(const char *name, audio_encoder codec) const
{
    ALOGV("getAudioEncoderParamByName: %s for codec %d", name, codec);
    int index = -1;
    for (size_t i = 0, n = mAudioEncoders.size(); i < n; ++i) {
        if (mAudioEncoders[i]->mCodec == codec) {
            index = i;
            break;
        }
    }
    if (index == -1) {
        ALOGE("The given audio encoder %d is not found", codec);
        return -1;
    }

    if (!strcmp("enc.aud.ch.min", name)) return mAudioEncoders[index]->mMinChannels;
    if (!strcmp("enc.aud.ch.max", name)) return mAudioEncoders[index]->mMaxChannels;
    if (!strcmp("enc.aud.bps.min", name)) return mAudioEncoders[index]->mMinBitRate;
    if (!strcmp("enc.aud.bps.max", name)) return mAudioEncoders[index]->mMaxBitRate;
    if (!strcmp("enc.aud.hz.min", name)) return mAudioEncoders[index]->mMinSampleRate;
    if (!strcmp("enc.aud.hz.max", name)) return mAudioEncoders[index]->mMaxSampleRate;

    ALOGE("The given audio encoder param name %s is not found", name);
    return -1;
}

Vector<video_decoder> MediaProfiles::getVideoDecoders() const
{
    Vector<video_decoder> decoders;
    for (size_t i = 0; i < mVideoDecoders.size(); ++i) {
        decoders.add(mVideoDecoders[i]->mCodec);
    }
    return decoders;  // copy out
}

Vector<audio_decoder> MediaProfiles::getAudioDecoders() const
{
    Vector<audio_decoder> decoders;
    for (size_t i = 0; i < mAudioDecoders.size(); ++i) {
        decoders.add(mAudioDecoders[i]->mCodec);
    }
    return decoders;  // copy out
}

int MediaProfiles::getCamcorderProfileIndex(int cameraId, camcorder_quality quality) const
{
    int index = -1;
    for (size_t i = 0, n = mCamcorderProfiles.size(); i < n; ++i) {
        if (mCamcorderProfiles[i]->mCameraId == cameraId &&
            mCamcorderProfiles[i]->mQuality == quality) {
            index = i;
            break;
        }
    }
    return index;
}

int MediaProfiles::getCamcorderProfileParamByName(const char *name,
                                                  int cameraId,
                                                  camcorder_quality quality) const
{
    ALOGV("getCamcorderProfileParamByName: %s for camera %d, quality %d",
        name, cameraId, quality);

    int index = getCamcorderProfileIndex(cameraId, quality);
    if (index == -1) {
        ALOGE("The given camcorder profile camera %d quality %d is not found",
            cameraId, quality);
        return -1;
    }

    if (!strcmp("duration", name)) return mCamcorderProfiles[index]->mDuration;
    if (!strcmp("file.format", name)) return mCamcorderProfiles[index]->mFileFormat;
    if (!strcmp("vid.codec", name)) return mCamcorderProfiles[index]->mVideoCodec->mCodec;
    if (!strcmp("vid.width", name)) return mCamcorderProfiles[index]->mVideoCodec->mFrameWidth;
    if (!strcmp("vid.height", name)) return mCamcorderProfiles[index]->mVideoCodec->mFrameHeight;
    if (!strcmp("vid.bps", name)) return mCamcorderProfiles[index]->mVideoCodec->mBitRate;
    if (!strcmp("vid.fps", name)) return mCamcorderProfiles[index]->mVideoCodec->mFrameRate;
    if (!strcmp("aud.codec", name)) return mCamcorderProfiles[index]->mAudioCodec->mCodec;
    if (!strcmp("aud.bps", name)) return mCamcorderProfiles[index]->mAudioCodec->mBitRate;
    if (!strcmp("aud.ch", name)) return mCamcorderProfiles[index]->mAudioCodec->mChannels;
    if (!strcmp("aud.hz", name)) return mCamcorderProfiles[index]->mAudioCodec->mSampleRate;

    ALOGE("The given camcorder profile param id %d name %s is not found", cameraId, name);
    return -1;
}

bool MediaProfiles::hasCamcorderProfile(int cameraId, camcorder_quality quality) const
{
    return (getCamcorderProfileIndex(cameraId, quality) != -1);
}

Vector<int> MediaProfiles::getImageEncodingQualityLevels(int cameraId) const
{
    Vector<int> result;
    ImageEncodingQualityLevels *levels = findImageEncodingQualityLevels(cameraId);
    if (levels != NULL) {
        result = levels->mLevels;  // copy out
    }
    return result;
}

int MediaProfiles::getStartTimeOffsetMs(int cameraId) const {
    int offsetTimeMs = -1;
    ssize_t index = mStartTimeOffsets.indexOfKey(cameraId);
    if (index >= 0) {
        offsetTimeMs = mStartTimeOffsets.valueFor(cameraId);
    }
    ALOGV("offsetTime=%d ms and cameraId=%d", offsetTimeMs, cameraId);
    return offsetTimeMs;
}

MediaProfiles::~MediaProfiles()
{
    CHECK("destructor should never be called" == 0);
#if 0
    for (size_t i = 0; i < mAudioEncoders.size(); ++i) {
        delete mAudioEncoders[i];
    }
    mAudioEncoders.clear();

    for (size_t i = 0; i < mVideoEncoders.size(); ++i) {
        delete mVideoEncoders[i];
    }
    mVideoEncoders.clear();

    for (size_t i = 0; i < mVideoDecoders.size(); ++i) {
        delete mVideoDecoders[i];
    }
    mVideoDecoders.clear();

    for (size_t i = 0; i < mAudioDecoders.size(); ++i) {
        delete mAudioDecoders[i];
    }
    mAudioDecoders.clear();

    for (size_t i = 0; i < mCamcorderProfiles.size(); ++i) {
        delete mCamcorderProfiles[i];
    }
    mCamcorderProfiles.clear();
#endif
}

#ifdef MTK_AOSP_ENHANCEMENT
//MTK CameraProfile Handling Functions
#define UNUSEDP(x) (void)(x)

uint32_t MediaProfiles::eChipName = 0;
video_encoder MediaProfiles::eHighestCodec = VIDEO_ENCODER_H264;
uint32_t MediaProfiles::sMaxWdith = 0;
uint32_t MediaProfiles::sMaxHeight = 0;
uint32_t MediaProfiles::sMaxBitrate = 0;
uint32_t MediaProfiles::sMaxFramerate = 0;
uint32_t MediaProfiles::sMemoryIsLarge = 1;

static uint32_t getChipName(void);
//Utility Functions
static int getVideoCapability(
        int i4VideoFormat,
        unsigned int *pu4Width,
        unsigned int *pu4Height,
        unsigned int *pu4BitRatem,
        unsigned int *pu4FrameRate,
        int slowmotion)
{
    int i4RetValue = 1;
    VENC_DRV_QUERY_VIDEO_FORMAT_T qinfo;
    VENC_DRV_QUERY_VIDEO_FORMAT_T outinfo;
    VENC_DRV_MRESULT_T ret;

    if((NULL == pu4Width) || (NULL == pu4Height) || (NULL == pu4BitRatem) || (NULL == pu4FrameRate)){
        return -1;
    }

    memset(&qinfo,0,sizeof(VENC_DRV_QUERY_VIDEO_FORMAT_T));
    memset(&outinfo,0,sizeof(VENC_DRV_QUERY_VIDEO_FORMAT_T));
    switch (i4VideoFormat)
    {
        /*
        case VIDEO_ENCODER_H263 :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_H263;
        break;*/
#ifdef MTK_VIDEO_HEVC_SUPPORT
        case VIDEO_ENCODER_HEVC :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_HEVC;
            qinfo.u4Profile = slowmotion;
            ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_VIDEO_CAMCORDER_CAP, &qinfo, &outinfo);
            if(ret ==  VENC_DRV_MRESULT_OK){
                (*pu4Width) = outinfo.u4Width;
                (*pu4Height) = outinfo.u4Height;
                (*pu4BitRatem) = outinfo.u4Bitrate;
                (*pu4FrameRate) = outinfo.u4FrameRate;
                ALOGI("[VIDEO_ENCODER_HEVC] checkVideoCapability, format=%d,support maxwidth=%u,maxheight=%u, bitrate %u, framerate %u",i4VideoFormat,outinfo.u4Width,outinfo.u4Height, outinfo.u4Bitrate, outinfo.u4FrameRate);
            }
            else{
                i4RetValue = -1;
            }
        break;
#endif
        case VIDEO_ENCODER_H264 :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_H264;
            qinfo.u4Profile = slowmotion;
            ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_VIDEO_CAMCORDER_CAP, &qinfo, &outinfo);
            if(ret ==  VENC_DRV_MRESULT_OK){
                (*pu4Width)= outinfo.u4Width;
                (*pu4Height) = outinfo.u4Height;
                (*pu4BitRatem) = outinfo.u4Bitrate;
                (*pu4FrameRate) = outinfo.u4FrameRate;
                ALOGI("checkVideoCapability, format=%d,support maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",i4VideoFormat,outinfo.u4Width,outinfo.u4Height, outinfo.u4Bitrate, outinfo.u4FrameRate);
            }
            else{
                i4RetValue = -1;
            }
        break;
        case VIDEO_ENCODER_MPEG_4_SP :
        case VIDEO_ENCODER_H263 :
            qinfo.eVideoFormat = VENC_DRV_VIDEO_FORMAT_MPEG4;
            qinfo.u4Profile = slowmotion;
            ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_VIDEO_CAMCORDER_CAP, &qinfo, &outinfo);
            if(ret ==  VENC_DRV_MRESULT_OK){
                (*pu4Width)= outinfo.u4Width;
                (*pu4Height) = outinfo.u4Height;
                (*pu4BitRatem) = outinfo.u4Bitrate;
                (*pu4FrameRate) = outinfo.u4FrameRate;
                ALOGI("checkVideoCapability, format=%d,support maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",i4VideoFormat,outinfo.u4Width,outinfo.u4Height, outinfo.u4Bitrate, outinfo.u4FrameRate);
            }
            else{
                i4RetValue = -1;
            }
        break;
        default:
            i4RetValue = -1;
            break;
    }
    //when query fail, give default value.
    if (i4RetValue == -1) {
        (*pu4Width) = 720;
        (*pu4Height) = 480;
        (*pu4BitRatem) = 4800000;
        (*pu4FrameRate) = 30;
    }
    return i4RetValue;
}

static uint32_t getHighestCode(
        video_encoder *pHighestCodec,
        uint32_t *pMaxWdith,
        uint32_t *pMaxHeight,
        uint32_t *pMaxBitrate,
        uint32_t *pMaxFramerate)
{
    uint32_t u4Width, u4Height, u4Bitrate, u4Framerate;
    int iRet;

    *pHighestCodec = VIDEO_ENCODER_H264;
    iRet = getVideoCapability(VIDEO_ENCODER_H264, pMaxWdith, pMaxHeight, pMaxBitrate, pMaxFramerate);
    if (iRet < 0)//fail
    {
        iRet = getVideoCapability(VIDEO_ENCODER_MPEG_4_SP, pMaxWdith, pMaxHeight, pMaxBitrate, pMaxFramerate);
        if (iRet < 0)//fail
        {
            ALOGE("[ERROR] Don't find any video codec!!");
            return -1;
        }
    }
    else
    {
        iRet = getVideoCapability(VIDEO_ENCODER_MPEG_4_SP, &u4Width, &u4Height, &u4Bitrate, &u4Framerate);
        //if MPEG4 is the highest codec
        if (iRet >= 0 && u4Width > *pMaxWdith) {
            *pHighestCodec = VIDEO_ENCODER_MPEG_4_SP;
            *pMaxWdith = u4Width;
            *pMaxHeight = u4Height;
            *pMaxBitrate = u4Bitrate;
            *pMaxHeight = u4Height;
        }
    }
    return 1;
}

static uint32_t getChipName(void)
{
    static bool fGetChipName = false;
    static VAL_UINT32_T outChipName = VAL_CHIP_NAME_MAX;

    if (!fGetChipName)
    {
        VENC_DRV_MRESULT_T ret = eVEncDrvQueryCapability(VENC_DRV_QUERY_TYPE_CHIP_NAME, NULL, &outChipName);
        if(ret ==  VENC_DRV_MRESULT_OK)
        {
            ALOGI("getChipName success = %d", outChipName);
        }
        fGetChipName = true;
    }

    return (uint32_t)outChipName;
}

static unsigned long queryLCA(void)
{
    static bool fGetLCA = false;
    static VAL_UINT32_T fgLCA = 0;

    if (!fGetLCA)
    {
        char value[PROPERTY_VALUE_MAX];
        property_get("ro.config.low_ram", value, "false");
        if (0 == strcmp(value, "true"))
        {
            fgLCA = 1;
            ALOGI("low_ram: queryLCA() return true");
        }
        fGetLCA = true;
    }

    return (unsigned long)fgLCA;
}

//Public methods
size_t  MediaProfiles::getCamcorderProfilesNum(int id)
{
    UNUSEDP(id);
    return mCamcorderProfiles.size();
}

String8  MediaProfiles::getCamcorderProfilesCaps(int id)
{
    char buff[256];
    memset(buff,0,256);

    for (size_t i = 0; i < mCamcorderProfiles.size();  ++i)
    {
        if (id == mCamcorderProfiles[i]->mCameraId)
        {
            char temp[10];
            memset(temp,0,10);
            sprintf(temp,"%d,",mCamcorderProfiles[i]->mQuality);
            strcat(buff,temp);
        }
    }

    ALOGD("[getCamcorderProfilesCaps] mCameraId = %d, buff = %s", id, buff);

    return String8(buff);
}

static camcorder_quality getMTKCamcorderFineID(uint32_t a_eChipName, bool a_fgIsTimeLapse)
{
    camcorder_quality fineID;

    if (a_fgIsTimeLapse == true)
    {
        fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
    }
    else
    {
        fineID = CAMCORDER_QUALITY_MTK_FINE;
    }

    switch(a_eChipName)
    {
        case VAL_CHIP_NAME_ROME:
        case VAL_CHIP_NAME_MT6795:
        case VAL_CHIP_NAME_MT6757:
        {
            if (a_fgIsTimeLapse == true)
            {
                fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE_4K2K;
            }
            else
            {
                fineID = CAMCORDER_QUALITY_MTK_FINE_4K2K;
            }
            break;
        }

        case VAL_CHIP_NAME_MT6797:
        {
#ifdef MTK_VIDEO_HEVC_SUPPORT
            unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;

            if(getVideoCapability(VIDEO_ENCODER_HEVC, &u4Width, &u4Height, &u4BitRate, &u4FrameRate) > 0)
            {
                if((u4Width >= 3840) && (u4Height >= 2160))
                {
                    if (a_fgIsTimeLapse == true)
                    {
                        fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE_4K2K;
                    }
                    else
                    {
                        fineID = CAMCORDER_QUALITY_MTK_FINE_4K2K;
                    }
                }
                else
                {
                    if (a_fgIsTimeLapse == true)
                    {
                        fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
                    }
                    else
                    {
                        fineID = CAMCORDER_QUALITY_MTK_FINE;
                    }
                }
            }
            else
            {
                ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
                if (a_fgIsTimeLapse == true)
                {
                    fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
                }
                else
                {
                    fineID = CAMCORDER_QUALITY_MTK_FINE;
                }
            }
#else
            if (a_fgIsTimeLapse == true)
            {
                fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
            }
            else
            {
                fineID = CAMCORDER_QUALITY_MTK_FINE;
            }
#endif
            break;
        }

        default:
        {
            ALOGI("[%s] video capability use default",__FUNCTION__);
            if (a_fgIsTimeLapse == true)
            {
                fineID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
            }
            else
            {
                fineID = CAMCORDER_QUALITY_MTK_FINE;
            }
            break;
        }
    }

    return fineID;
}

/*static*/ const char *
MediaProfiles::findNameForTag(const MediaProfiles::NameToTagMap *map, size_t nMappings, int tag, char *name)
{
    if (name == NULL) {
        return "NULL"; // error
    }

    for (size_t i = 0; i < nMappings; ++i) {
        if (tag == map[i].tag) {
            sprintf(name,"%s",map[i].name);
            return name;
        }
    }

    sprintf(name,"%d",tag);
    return name; // not found
}

void MediaProfiles::dumpProfiles()
{
    //for debug dump
    int i=0;
    ALOGD("there are %d profiles", (unsigned int)mCamcorderProfiles.size());

    char nameCQ[256], nameFF[256], nameVE[256], nameAE[256];
    memset(nameCQ,0,256);
    memset(nameFF,0,256);
    memset(nameVE,0,256);
    memset(nameAE,0,256);
    const size_t nMappingsCQ = sizeof(sCamcorderQualityNameMap)/sizeof(sCamcorderQualityNameMap[0]);
    const size_t nMappingsFF = sizeof(sFileFormatMap)/sizeof(sFileFormatMap[0]);
    const size_t nMappingsVE = sizeof(sVideoEncoderNameMap)/sizeof(sVideoEncoderNameMap[0]);
    const size_t nMappingsAE = sizeof(sAudioEncoderNameMap)/sizeof(sAudioEncoderNameMap[0]);

    for (i=0; i<(int)mCamcorderProfiles.size(); ++i)
    {
        CamcorderProfile *tmpC = mCamcorderProfiles[i];
        if (tmpC != NULL) {
            VideoCodec *tmpv = tmpC->mVideoCodec;
            AudioCodec *tmpa = tmpC->mAudioCodec;
            if (tmpv != NULL && tmpa != NULL) {

                ALOGD("camera_id:%d, quality:%s(%d), ffmt:%s, dur:%d, vcodec:%s, w:%d, h:%d, brate:%d, frate:%d, acodec:%s, brate:%d, srate:%d, ch:%d",
                        tmpC->mCameraId,
                        findNameForTag(sCamcorderQualityNameMap, nMappingsCQ, tmpC->mQuality, nameCQ),
                        tmpC->mQuality,
                        findNameForTag(sFileFormatMap, nMappingsFF, tmpC->mFileFormat, nameFF),
                        tmpC->mDuration,
                        findNameForTag(sVideoEncoderNameMap, nMappingsVE, tmpv->mCodec, nameVE),
                        tmpv->mFrameWidth,
                        tmpv->mFrameHeight,
                        tmpv->mBitRate,
                        tmpv->mFrameRate,
                        findNameForTag(sAudioEncoderNameMap, nMappingsAE, tmpa->mCodec, nameAE),
                        tmpa->mBitRate,
                        tmpa->mSampleRate,
                        tmpa->mChannels);
            }
            else if (tmpv != NULL) {
                ALOGD("camera_id:%d, quality:%s(%d), ffmt:%s, dur:%d, vcodec:%s, w:%d, h:%d, brate:%d, frate:%d, acodec=NULL",
                        tmpC->mCameraId,
                        findNameForTag(sCamcorderQualityNameMap, nMappingsCQ, tmpC->mQuality, nameCQ),
                        tmpC->mQuality,
                        findNameForTag(sFileFormatMap, nMappingsFF, tmpC->mFileFormat, nameFF),
                        tmpC->mDuration,
                        findNameForTag(sVideoEncoderNameMap, nMappingsVE, tmpv->mCodec, nameVE),
                        tmpv->mFrameWidth,
                        tmpv->mFrameHeight,
                        tmpv->mBitRate,
                        tmpv->mFrameRate);
            }
            else if (tmpa != NULL) {
                ALOGD("camera_id:%d, quality:%s(%d), ffmt:%s, dur:%d, acodec:%s, brate:%d, srate:%d, ch:%d, vcodec=NULL",
                        tmpC->mCameraId,
                        findNameForTag(sCamcorderQualityNameMap, nMappingsCQ, tmpC->mQuality, nameCQ),
                        tmpC->mQuality,
                        findNameForTag(sFileFormatMap, nMappingsFF, tmpC->mFileFormat, nameFF),
                        tmpC->mDuration,
                        findNameForTag(sAudioEncoderNameMap, nMappingsAE, tmpa->mCodec, nameAE),
                        tmpa->mBitRate,
                        tmpa->mSampleRate,
                        tmpa->mChannels);
            }
            else {
                ALOGD("camera_id:%d, quality:%s(%d), ffmt:%s, dur:%d, vcodec=NULL, acodec=NULL",
                        tmpC->mCameraId,
                        findNameForTag(sCamcorderQualityNameMap, nMappingsCQ, tmpC->mQuality, nameCQ),
                        tmpC->mQuality,
                        findNameForTag(sFileFormatMap, nMappingsFF, tmpC->mFileFormat, nameFF),
                        tmpC->mDuration);
            }
        }
        else {
            ALOGD("index %d is NULL", i);
        }
    }
}

//Private methods
/*static*/ MediaProfiles::CamcorderProfile*
MediaProfiles::createMTKCamcorderProfile(camcorder_quality quality, camcorder_mode CamMode, camera_id CamId)
{
    MediaProfiles::VideoCodec *videoCodec = NULL;
    MediaProfiles::AudioCodec *audioCodec = NULL;

    // Setting for VIDEO Profile
    switch(quality)
    {
        //Standard Quality
        case CAMCORDER_QUALITY_QVGA:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_MPEG_4_SP, 1000*1000, 320, 240, 30);
            }
            break;
        case CAMCORDER_QUALITY_CIF:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_MPEG_4_SP, 1250*1000, 352, 288, 30);
            }
            break;
        case CAMCORDER_QUALITY_480P:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        eHighestCodec, 4500*1000, 720, 480, 30);
            }
            break;
        case CAMCORDER_QUALITY_720P:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        eHighestCodec,
                        (eHighestCodec == VIDEO_ENCODER_H264) ? 9000*1000 : 12000*1000,
                        1280, 720, 30);
            }
            break;
        case CAMCORDER_QUALITY_1080P:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        eHighestCodec,
                        (eHighestCodec == VIDEO_ENCODER_H264) ? 17000*1000 : 26000*1000,
                        1920, 1080, 30);
            }
            break;
        case CAMCORDER_QUALITY_2160P:
            {
                //add this for passing checkAndAddRequiredProfilesIfNecessary()
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H264, 21000*1000, 1920, 1080, 30);
                ALOGE("Not define this quality (%d) yet!", quality);
            }
            break;
        case CAMCORDER_QUALITY_LOW:
        case CAMCORDER_QUALITY_QCIF:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H263, 192000, 176, 144, 20);
                //the value set is from google default functions
                //And CTS will check whether the supported standard lowest profile == low profile.
                //So LOW & QCIF MUST be the same.
            }
            break;
        //case CAMCORDER_QUALITY_HIGH:
            //videoCodec = createMTKFineVideoProfile(CamMode, CamId);
            ////CTS will check wether the highest supported profile == high-profile.
            ////So HIGH & (1080P or 720P or 480P) (depend on platform) MUST be the same.
            ////HIGH will add in createStandardCamcorderProfiles()
            //break;

        //Standard Time Lapse Quality
        case CAMCORDER_QUALITY_TIME_LAPSE_QVGA:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_MPEG_4_SP, 1000*1000, 320, 240, 30);
            }
            break;
        case CAMCORDER_QUALITY_TIME_LAPSE_CIF:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_MPEG_4_SP, 1250*1000, 352, 288, 30);
            }
            break;
        case CAMCORDER_QUALITY_TIME_LAPSE_480P:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        eHighestCodec, 4500*1000, 720, 480, 30);
            }
            break;
        case CAMCORDER_QUALITY_TIME_LAPSE_720P:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        eHighestCodec,
                        (eHighestCodec == VIDEO_ENCODER_H264) ? 9000*1000 : 12000*1000,
                        1280, 720, 30);
            }
            break;
        case CAMCORDER_QUALITY_TIME_LAPSE_1080P:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        eHighestCodec,
                        (eHighestCodec == VIDEO_ENCODER_H264) ? 17000*1000 : 26000*1000,
                        1920, 1080, 30);
            }
            break;
        case CAMCORDER_QUALITY_TIME_LAPSE_2160P:
            {
                //add this for passing checkAndAddRequiredProfilesIfNecessary()
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H264, 21000*1000, 1920, 1080, 30);
                ALOGE("Not define this time lapse quality (%d) yet!", quality);
            }
            break;
        case CAMCORDER_QUALITY_TIME_LAPSE_QCIF:
        case CAMCORDER_QUALITY_TIME_LAPSE_LOW:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H263, 1000000, 176, 144, 20);
                //the value set is from google default functions
            }
            break;
        //case CAMCORDER_QUALITY_TIME_LAPSE_HIGH:
            //{
                //videoCodec = new MediaProfiles::VideoCodec(
                        //VIDEO_ENCODER_MPEG_4_SP, 6000*1000, 640, 480, 20);
                ////for cam not support 720x480 in BSP case
                ////not sure if 480P profile is work
                ////HIGH will add in createStandardCamcorderProfiles()
            //}
            //break;

        //Standard High Speed Quality
        case CAMCORDER_QUALITY_HIGH_SPEED_LOW:
        case CAMCORDER_QUALITY_HIGH_SPEED_HIGH:
        case CAMCORDER_QUALITY_HIGH_SPEED_480P:
        case CAMCORDER_QUALITY_HIGH_SPEED_720P:
        case CAMCORDER_QUALITY_HIGH_SPEED_1080P:
            {
                //add this for passing checkAndAddRequiredProfilesIfNecessary()
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H264, 21000*1000, 1920, 1080, 30);
                ALOGE("Not define this high speed quality (%d) yet!", quality);
            }
            break;

        //MTK Profiles
        case CAMCORDER_QUALITY_MTK_LOW:
        case CAMCORDER_QUALITY_MTK_NIGHT_LOW:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW:
            videoCodec = createMTKLowVideoProfile(CamMode, CamId);
            break;
        case CAMCORDER_QUALITY_MTK_MEDIUM:
        case CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM:
            videoCodec = createMTKMediumVideoProfile(CamMode, CamId);
            break;
        case CAMCORDER_QUALITY_MTK_HIGH:
        case CAMCORDER_QUALITY_MTK_NIGHT_HIGH:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH:
            videoCodec = createMTKHighVideoProfile(CamMode, CamId);
            break;
        case CAMCORDER_QUALITY_MTK_FINE:
        case CAMCORDER_QUALITY_MTK_NIGHT_FINE:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE_4K2K:
        case CAMCORDER_QUALITY_MTK_FINE_4K2K:
            videoCodec = createMTKFineVideoProfile(CamMode, CamId);
            break;
        case CAMCORDER_QUALITY_MTK_LIVE_EFFECT:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT:
            videoCodec = new MediaProfiles::VideoCodec(
                    VIDEO_ENCODER_MPEG_4_SP, 2500*1000/CamMode, 480, 320, 30/CamMode);
            break;
        case CAMCORDER_QUALITY_MTK_H264_HIGH:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_H264_HIGH:
            videoCodec = new MediaProfiles::VideoCodec(
                    VIDEO_ENCODER_H264, 4000*1000/CamMode, 640, 480, 30/CamMode);
            break;
        case CAMCORDER_QUALITY_MTK_MPEG4_1080P:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_MPEG4_1080P:
            videoCodec = new MediaProfiles::VideoCodec(
                    VIDEO_ENCODER_MPEG_4_SP, 14000*1000/CamMode, 1920, 1088, 15/CamMode);
            break;

        //MTK Slowmotion Profiles
        case CAMCORDER_QUALITY_MTK_VGA_120:
            switch(eChipName)
            {
                default:
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 12000*1000, 640, 480, 120);
                    break;
            }
            break;
        case CAMCORDER_QUALITY_MTK_720P_60:
            switch(eChipName)
            {
                default:
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 18000*1000, 1280, 720, 60);
                    break;
            }
            break;
        case CAMCORDER_QUALITY_MTK_720P_120:
            switch(eChipName)
            {
                default:
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 36000*1000, 1280, 720, 120);
                    //videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_HEVC, 32000*1000, 1280, 736, 120);
                    //HEVC only for test
                    break;
            }
            break;
        case CAMCORDER_QUALITY_MTK_720P_180:
            switch(eChipName)
            {
                default:
#ifdef MTK_VIDEO_HEVC_SUPPORT
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_HEVC, 40000*1000, 1280, 736, 180);
#else//no HEVC
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 44000*1000, 1280, 720, 180);
#endif//MTK_VIDEO_HEVC_SUPPORT
                    break;
            }
            break;
        case CAMCORDER_QUALITY_MTK_720P_240:
            switch(eChipName)
            {
                default:
#ifdef MTK_VIDEO_HEVC_SUPPORT
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_HEVC, 43000*1000, 1280, 736, 240);
#else//no HEVC
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 60000*1000, 1280, 720, 240);
#endif//MTK_VIDEO_HEVC_SUPPORT
                    break;
            }
            break;
        case CAMCORDER_QUALITY_MTK_1080P_60:
            switch(eChipName)
            {
                default:
#ifdef MTK_VIDEO_HEVC_SUPPORT
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_HEVC, 21500*1000, 1920, 1088, 60);
#else//no HEVC
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 34000*1000, 1920, 1080, 60);
#endif//MTK_VIDEO_HEVC_SUPPORT
                    break;
            }
            break;
        case CAMCORDER_QUALITY_MTK_1080P_120:
            switch(eChipName)
            {
                default:
#ifdef MTK_VIDEO_HEVC_SUPPORT
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_HEVC, 43000*1000, 1920, 1088, 120);
#else//no HEVC
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 60000*1000, 1920, 1080, 120);
#endif//MTK_VIDEO_HEVC_SUPPORT
                    break;
            }
            break;
        default:
            videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 75*1000/CamMode, 96, 96, 30/CamMode);
            ALOGE("The given quality %d is not found", quality);
            break;
    }

    // Setting for AUDIO Profile
    switch(quality)
    {
#if 0
        //I am not sure the audio spec.
        case CAMCORDER_QUALITY_LOW:
        case CAMCORDER_QUALITY_HIGH:
        case CAMCORDER_QUALITY_TIME_LAPSE_HIGH:
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
            //the values are from google default funtion
            break;
#endif//
        case CAMCORDER_QUALITY_MTK_LOW:
        case CAMCORDER_QUALITY_MTK_NIGHT_LOW:
        case CAMCORDER_QUALITY_TIME_LAPSE_LOW:
        case CAMCORDER_QUALITY_TIME_LAPSE_QCIF:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW:
        case CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW:
#ifdef HAVE_AACENCODE_FEATURE
            audioCodec = new AudioCodec(AUDIO_ENCODER_AAC, 64000, 48000, 2);
#else
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
#endif
            break;

            //Fine quality AAC HE
        case CAMCORDER_QUALITY_MTK_FINE:
#ifdef HAVE_AACENCODE_FEATURE
            switch(eChipName)
            {
                case VAL_CHIP_NAME_MT6571:
                case VAL_CHIP_NAME_MT6575:
                    audioCodec = new AudioCodec(AUDIO_ENCODER_AAC, 128000, 48000, 2);
                    break;
                default:
                    //audioCodec = new AudioCodec(AUDIO_ENCODER_HE_AAC, 128000, 48000, 2);
                    audioCodec = new AudioCodec(AUDIO_ENCODER_AAC, 128000, 48000, 2);
                    break;
            }
#else
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
#endif
            break;

        default:
#ifdef HAVE_AACENCODE_FEATURE
            audioCodec = new AudioCodec(AUDIO_ENCODER_AAC, 128000, 48000, 2);
#else
            audioCodec = new AudioCodec(AUDIO_ENCODER_AMR_NB, 12200, 8000, 1);
#endif
            break;
    }

    CamcorderProfile *profile = new MediaProfiles::CamcorderProfile;
    profile->mCameraId = CamId;
/*[BIRD][BIRD_CAMERA_OUTPUT_FORMAT_MPEG_4][相机录像文件默认为.mp4格式][yangbo][20170317]BEGIN */
#ifdef BIRD_CAMERA_OUTPUT_FORMAT_MPEG_4
    profile->mFileFormat = OUTPUT_FORMAT_MPEG_4;
#else
    profile->mFileFormat = OUTPUT_FORMAT_THREE_GPP;
#endif
/*[BIRD][BIRD_CAMERA_OUTPUT_FORMAT_MPEG_4][相机录像文件默认为.mp4格式][yangbo][20170317]END */
    profile->mQuality = quality;
    profile->mDuration = 30;
    profile->mVideoCodec = videoCodec;
    profile->mAudioCodec = audioCodec;
    return profile;
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultH264VideoEncoderCap()
{
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;
    int slowmotion=0, iRet;
#ifdef MTK_SLOW_MOTION_VIDEO_SUPPORT
    slowmotion = 1;
#endif//MTK_SLOW_MOTION_VIDEO_SUPPORT
    iRet = getVideoCapability(VIDEO_ENCODER_H264, &u4Width, &u4Height, &u4BitRate, &u4FrameRate, slowmotion);
    ALOGI("[ %s ], support ret=%d maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",
            __FUNCTION__, iRet, u4Width, u4Height, u4BitRate, u4FrameRate);
    if (iRet > 0) {
        return new MediaProfiles::VideoEncoderCap(
                VIDEO_ENCODER_H264, 75*1000, u4BitRate,
                128, u4Width, 96, u4Height, 15, u4FrameRate);
    }
    else {
        ALOGE("[ERROR] don't support H264!!");
        return NULL;
    }
}

/*static*/ MediaProfiles::VideoEncoderCap*
MediaProfiles::createDefaultHEVCVideoEncoderCap()
{
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;
    int slowmotion=0, iRet;
#ifdef MTK_SLOW_MOTION_VIDEO_SUPPORT
    slowmotion = 1;
#endif//MTK_SLOW_MOTION_VIDEO_SUPPORT
    iRet = getVideoCapability(VIDEO_ENCODER_HEVC, &u4Width, &u4Height, &u4BitRate, &u4FrameRate, slowmotion);
    ALOGI("[ %s ], support ret=%d maxwidth=%d,maxheight=%d, bitrate %d, framerate %d",
            __FUNCTION__, iRet, u4Width, u4Height, u4BitRate, u4FrameRate);
    if (iRet > 0) {
        return new MediaProfiles::VideoEncoderCap(
                VIDEO_ENCODER_HEVC, 75*1000, u4BitRate,
                128, u4Width, 128, u4Height, 15, u4FrameRate);
    }
    else {
        ALOGE("[ERROR] don't support HEVC!!");
        return NULL;
    }
}

/*static*/ void
MediaProfiles::createMTKCamcorderProfiles(MediaProfiles *profiles)
{
    eChipName = getChipName();//1st time get ChipName
    //1st time get the highest codec
    getHighestCode(&eHighestCodec, &sMaxWdith, &sMaxHeight, &sMaxBitrate, &sMaxFramerate);
    //1st time get the memoryIsLarge
    int64_t memory_size_byte = (int64_t)sysconf(_SC_PHYS_PAGES) * PAGE_SIZE;
    sMemoryIsLarge = (memory_size_byte > 256*1024*1024) ? 1 : 0;

    createStandardCamcorderProfiles(profiles);
    createMTKLegacyCamcorderProfiles(profiles);
    createMTKSlowMotionCamcorderProfiles(profiles);

    profiles->mCameraIds.push(0);
    profiles->mCameraIds.push(1);

    profiles->dumpProfiles();
    return;
}

/*static*/ void
MediaProfiles::createStandardCamcorderProfiles(MediaProfiles *profiles)
{
    //back Camera
    //CAMCORDER_QUALITY_LOW
    MediaProfiles::CamcorderProfile *BackLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackLowProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_LOW
    MediaProfiles::CamcorderProfile *BackTimeLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackTimeLowProfile);
    //CAMCORDER_QUALITY_QCIF
    MediaProfiles::CamcorderProfile *BackQcifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_QCIF, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackQcifProfile);
    //CAMCORDER_QUALITY_QVGA
    MediaProfiles::CamcorderProfile *BackQvgaProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_QVGA, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackQvgaProfile);
    //CAMCORDER_QUALITY_CIF
    MediaProfiles::CamcorderProfile *BackCifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_CIF, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackCifProfile);
    //CAMCORDER_QUALITY_480P
    if (sMaxWdith >= 720) {
        MediaProfiles::CamcorderProfile *Back480pProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        profiles->mCamcorderProfiles.add(Back480pProfile);
        if (sMaxWdith < 1280) {
            //CAMCORDER_QUALITY_HIGH
            //add below due to CTS will check wether HIGH == highest supported standard profile
            MediaProfiles::CamcorderProfile *BackHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, BACK_CAMERA);
            BackHighProfile->mQuality = CAMCORDER_QUALITY_HIGH;
            profiles->mCamcorderProfiles.add(BackHighProfile);
        }
    }
    //CAMCORDER_QUALITY_720P
    if (sMaxWdith >= 1280) {
        MediaProfiles::CamcorderProfile *Back720PProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_720P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        profiles->mCamcorderProfiles.add(Back720PProfile);
        if (sMaxWdith < 1920) {
            //CAMCORDER_QUALITY_HIGH
            //add below due to CTS will check wether HIGH == highest supported standard profile
            MediaProfiles::CamcorderProfile *BackHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_720P, CAMCORDER_DAY_MODE, BACK_CAMERA);
            BackHighProfile->mQuality = CAMCORDER_QUALITY_HIGH;
            profiles->mCamcorderProfiles.add(BackHighProfile);
        }
    }
    //CAMCORDER_QUALITY_1080P
    if (sMaxWdith >= 1920) {
        MediaProfiles::CamcorderProfile *Back1080PProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_1080P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        profiles->mCamcorderProfiles.add(Back1080PProfile);
        //CAMCORDER_QUALITY_HIGH
        //add below due to CTS will check wether HIGH == highest supported standard profile
        MediaProfiles::CamcorderProfile *BackHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_1080P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        BackHighProfile->mQuality = CAMCORDER_QUALITY_HIGH;
        profiles->mCamcorderProfiles.add(BackHighProfile);
    }
    //CAMCORDER_QUALITY_2160P

    //CAMCORDER_QUALITY_TIME_LAPSE_QCIF
    MediaProfiles::CamcorderProfile *BackTimeQcifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_QCIF, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackTimeQcifProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_QVGA
    MediaProfiles::CamcorderProfile *BackTimeQvgaProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_QVGA, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackTimeQvgaProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_CIF
    MediaProfiles::CamcorderProfile *BackTimeCifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_CIF, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(BackTimeCifProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_480P
    if (sMaxWdith >= 720) {
        MediaProfiles::CamcorderProfile *BackTime480pProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_480P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        profiles->mCamcorderProfiles.add(BackTime480pProfile);
        if (sMaxWdith < 1280) {
            //CAMCORDER_QUALITY_TIME_LAPSE_HIGH
            //add below due to CTS will check wether HIGH == highest supported standard profile
            MediaProfiles::CamcorderProfile *BackTimeHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_480P, CAMCORDER_DAY_MODE, BACK_CAMERA);
            BackTimeHighProfile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
            profiles->mCamcorderProfiles.add(BackTimeHighProfile);
        }
    }
    //CAMCORDER_QUALITY_TIME_LAPSE_720P
    if (sMaxWdith >= 1280) {
        MediaProfiles::CamcorderProfile *BackTime720pProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_720P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        profiles->mCamcorderProfiles.add(BackTime720pProfile);
        if (sMaxWdith < 1920) {
            //CAMCORDER_QUALITY_TIME_LAPSE_HIGH
            //add below due to CTS will check wether HIGH == highest supported standard profile
            MediaProfiles::CamcorderProfile *BackTimeHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_720P, CAMCORDER_DAY_MODE, BACK_CAMERA);
            BackTimeHighProfile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
            profiles->mCamcorderProfiles.add(BackTimeHighProfile);
        }
    }
    //CAMCORDER_QUALITY_TIME_LAPSE_1080P
    if (sMaxWdith >= 1920) {
        MediaProfiles::CamcorderProfile *BackTime1080pProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_1080P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        profiles->mCamcorderProfiles.add(BackTime1080pProfile);
        //CAMCORDER_QUALITY_TIME_LAPSE_HIGH
        //add below due to CTS will check wether HIGH == highest supported standard profile
        MediaProfiles::CamcorderProfile *BackTimeHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_1080P, CAMCORDER_DAY_MODE, BACK_CAMERA);
        BackTimeHighProfile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
        profiles->mCamcorderProfiles.add(BackTimeHighProfile);
    }
    //CAMCORDER_QUALITY_TIME_LAPSE_2160P

    //CAMCORDER_QUALITY_HIGH_SPEED_LOW
    //CAMCORDER_QUALITY_HIGH_SPEED_HIGH
    //CAMCORDER_QUALITY_HIGH_SPEED_480P
    //CAMCORDER_QUALITY_HIGH_SPEED_720P
    //CAMCORDER_QUALITY_HIGH_SPEED_1080P


    //front Camera
    //CAMCORDER_QUALITY_LOW
    MediaProfiles::CamcorderProfile *FrontLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontLowProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_LOW
    MediaProfiles::CamcorderProfile *FrontTimeLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontTimeLowProfile);

    //CAMCORDER_QUALITY_QCIF
    MediaProfiles::CamcorderProfile *FrontQcifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_QCIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontQcifProfile);
    //CAMCORDER_QUALITY_QVGA
    MediaProfiles::CamcorderProfile *FrontQvgaProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_QVGA, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontQvgaProfile);
    //CAMCORDER_QUALITY_CIF
    MediaProfiles::CamcorderProfile *FrontCifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_CIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontCifProfile);
    //CAMCORDER_QUALITY_480P
    if (sMaxWdith >= 720) {
        if ((VAL_CHIP_NAME_MT6580 == eChipName) || (VAL_CHIP_NAME_MT6570 == eChipName)) {
            /* 6580's front sensor can only support up to 640x480. */
            //CAMCORDER_QUALITY_HIGH
            MediaProfiles::CamcorderProfile *FrontHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_CIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
            FrontHighProfile->mQuality = CAMCORDER_QUALITY_HIGH;
            profiles->mCamcorderProfiles.add(FrontHighProfile);
        }
        else {
            MediaProfiles::CamcorderProfile *Front480pProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, FRONT_CAMERA);
            profiles->mCamcorderProfiles.add(Front480pProfile);
            //CAMCORDER_QUALITY_HIGH
            MediaProfiles::CamcorderProfile *FrontHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_480P, CAMCORDER_DAY_MODE, FRONT_CAMERA);
            FrontHighProfile->mQuality = CAMCORDER_QUALITY_HIGH;
            profiles->mCamcorderProfiles.add(FrontHighProfile);
        }
    }
    else
    {
        //CAMCORDER_QUALITY_HIGH
        MediaProfiles::CamcorderProfile *FrontHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_CIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
        FrontHighProfile->mQuality = CAMCORDER_QUALITY_HIGH;
        profiles->mCamcorderProfiles.add(FrontHighProfile);
    }
    //CAMCORDER_QUALITY_720P
    //CAMCORDER_QUALITY_1080P
    //CAMCORDER_QUALITY_2160P

    //CAMCORDER_QUALITY_TIME_LAPSE_QCIF
    MediaProfiles::CamcorderProfile *FrontTimeQcifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_QCIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontTimeQcifProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_QVGA
    MediaProfiles::CamcorderProfile *FrontTimeQvgaProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_QVGA, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontTimeQvgaProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_CIF
    MediaProfiles::CamcorderProfile *FrontTimeCifProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_CIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontTimeCifProfile);
    //CAMCORDER_QUALITY_TIME_LAPSE_480P
    if (sMaxWdith >= 720) {
        if ((VAL_CHIP_NAME_MT6580 == eChipName) || (VAL_CHIP_NAME_MT6570 == eChipName)) {
            /* 6580's front sensor can only support up to 640x480. */
            //CAMCORDER_QUALITY_TIME_LAPSE_HIGH
            MediaProfiles::CamcorderProfile *FrontTimeHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_CIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
            FrontTimeHighProfile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
            profiles->mCamcorderProfiles.add(FrontTimeHighProfile);
        }
        else {
            MediaProfiles::CamcorderProfile *FrontTime480pProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_480P, CAMCORDER_DAY_MODE, FRONT_CAMERA);
            profiles->mCamcorderProfiles.add(FrontTime480pProfile);
            //CAMCORDER_QUALITY_TIME_LAPSE_HIGH
            MediaProfiles::CamcorderProfile *FrontTimeHighProfile =
                createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_480P, CAMCORDER_DAY_MODE, FRONT_CAMERA);
            FrontTimeHighProfile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
            profiles->mCamcorderProfiles.add(FrontTimeHighProfile);
        }
    }
    else
    {
        //CAMCORDER_QUALITY_TIME_LAPSE_HIGH
        MediaProfiles::CamcorderProfile *FrontTimeHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_TIME_LAPSE_CIF, CAMCORDER_DAY_MODE, FRONT_CAMERA);
        FrontTimeHighProfile->mQuality = CAMCORDER_QUALITY_TIME_LAPSE_HIGH;
        profiles->mCamcorderProfiles.add(FrontTimeHighProfile);
    }
    //CAMCORDER_QUALITY_TIME_LAPSE_720P
    //CAMCORDER_QUALITY_TIME_LAPSE_1080P
    //CAMCORDER_QUALITY_TIME_LAPSE_2160P

    //CAMCORDER_QUALITY_HIGH_SPEED_LOW
    //CAMCORDER_QUALITY_HIGH_SPEED_HIGH
    //CAMCORDER_QUALITY_HIGH_SPEED_480P
    //CAMCORDER_QUALITY_HIGH_SPEED_720P
    //CAMCORDER_QUALITY_HIGH_SPEED_1080P

    return;
}

/*static*/ void
MediaProfiles::createMTKLegacyCamcorderProfiles(MediaProfiles *profiles)
{
    //back Camera
    //CAMCORDER_QUALITY_MTK_LOW
    MediaProfiles::CamcorderProfile *LowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(LowProfile);
    //CAMCORDER_QUALITY_MTK_MEDIUM
    MediaProfiles::CamcorderProfile *MediumProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_MEDIUM, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(MediumProfile);
    //CAMCORDER_QUALITY_MTK_HIGH
    MediaProfiles::CamcorderProfile *HighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(HighProfile);
    //CAMCORDER_QUALITY_MTK_FINE
#if 0
    camcorder_quality fineID = CAMCORDER_QUALITY_MTK_FINE;
    fineID = ((VAL_CHIP_NAME_ROME == eChipName) || (VAL_CHIP_NAME_MT6795 == eChipName) || (VAL_CHIP_NAME_MT6797 == eChipName)) ?
        CAMCORDER_QUALITY_MTK_FINE_4K2K : CAMCORDER_QUALITY_MTK_FINE;
#else
    camcorder_quality fineID = getMTKCamcorderFineID(eChipName, false);
#endif
    MediaProfiles::CamcorderProfile *FineProfile =
        createMTKCamcorderProfile(fineID, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(FineProfile);

    //CAMCORDER_QUALITY_MTK_NIGHT_LOW
    MediaProfiles::CamcorderProfile *NightLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_LOW, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightLowProfile);
    //CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM
    MediaProfiles::CamcorderProfile *NightMediumProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_MEDIUM, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightMediumProfile);
    //CAMCORDER_QUALITY_MTK_NIGHT_HIGH
    MediaProfiles::CamcorderProfile *NightHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightHighProfile);
    //CAMCORDER_QUALITY_MTK_NIGHT_FINE
    MediaProfiles::CamcorderProfile *NightFineProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_FINE, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightFineProfile);
    //CAMCORDER_QUALITY_MTK_LIVE_EFFECT
    MediaProfiles::CamcorderProfile *LiveEffectProfile =
        createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LIVE_EFFECT, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(LiveEffectProfile);

    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW
    MediaProfiles::CamcorderProfile *LowTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(LowTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM
    MediaProfiles::CamcorderProfile *MediumTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_MEDIUM, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(MediumTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH
    MediaProfiles::CamcorderProfile *HighTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(HighTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE
#if 0
    camcorder_quality fineTimelapseID = CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
    fineTimelapseID = ((VAL_CHIP_NAME_ROME == eChipName) || (VAL_CHIP_NAME_MT6795 == eChipName) || (VAL_CHIP_NAME_MT6797 == eChipName)) ?
    CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE_4K2K : CAMCORDER_QUALITY_MTK_TIME_LAPSE_FINE;
#else
    camcorder_quality fineTimelapseID = getMTKCamcorderFineID(eChipName, true);
#endif
    MediaProfiles::CamcorderProfile *FineTimeLapseProfile =
    createMTKCamcorderProfile(fineTimelapseID, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(FineTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW
    MediaProfiles::CamcorderProfile *NightLowTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightLowTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM
    MediaProfiles::CamcorderProfile *NightMediumTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_MEDIUM,
                    CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightMediumTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH
    MediaProfiles::CamcorderProfile *NightHighTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightHighTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE
    MediaProfiles::CamcorderProfile *NightFineTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_FINE, CAMCORDER_NIGHT_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(NightFineTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT
    MediaProfiles::CamcorderProfile *LiveEffectTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT, CAMCORDER_DAY_MODE, BACK_CAMERA);
    profiles->mCamcorderProfiles.add(LiveEffectTimeLapseProfile);


    //front Camera
    //CAMCORDER_QUALITY_MTK_LOW
    MediaProfiles::CamcorderProfile *FrontLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontLowProfile);
    //CAMCORDER_QUALITY_MTK_HIGH
    MediaProfiles::CamcorderProfile *FrontHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontHighProfile);
    //CAMCORDER_QUALITY_MTK_NIGHT_LOW
    MediaProfiles::CamcorderProfile *FrontNightLowProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_LOW, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontNightLowProfile);
    //CAMCORDER_QUALITY_MTK_NIGHT_HIGH
    MediaProfiles::CamcorderProfile *FrontNightHighProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_NIGHT_HIGH, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontNightHighProfile);
    //CAMCORDER_QUALITY_MTK_LIVE_EFFECT
    MediaProfiles::CamcorderProfile *LiveEffectFrontProfile =
        createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_LIVE_EFFECT, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(LiveEffectFrontProfile);

    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW
    MediaProfiles::CamcorderProfile *FrontLowTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LOW, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontLowTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH
    MediaProfiles::CamcorderProfile *FrontHighTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_HIGH, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontHighTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW
    MediaProfiles::CamcorderProfile *FrontNightLowTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_LOW, CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontNightLowTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH
    MediaProfiles::CamcorderProfile *FrontNightHighTimeLapseProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_NIGHT_HIGH,
                    CAMCORDER_NIGHT_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(FrontNightHighTimeLapseProfile);
    //CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT
    MediaProfiles::CamcorderProfile *LiveEffectTimeLapseFrontProfile =
            createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_TIME_LAPSE_LIVE_EFFECT, CAMCORDER_DAY_MODE, FRONT_CAMERA);
    profiles->mCamcorderProfiles.add(LiveEffectTimeLapseFrontProfile);

    return;
}

/*static*/ void
MediaProfiles::createMTKSlowMotionCamcorderProfiles(MediaProfiles *profiles)
{
#ifdef MTK_SLOW_MOTION_VIDEO_SUPPORT
    switch(eChipName)
    {
        case VAL_CHIP_NAME_MT6592:
            {
                //CAMCORDER_QUALITY_MTK_720P_120
                MediaProfiles::CamcorderProfile *SM720P120Profile =
                    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_720P_120, CAMCORDER_DAY_MODE, BACK_CAMERA);
                profiles->mCamcorderProfiles.add(SM720P120Profile);
            }
            break;
        case VAL_CHIP_NAME_MT6752:
        case VAL_CHIP_NAME_DENALI_1:
        case VAL_CHIP_NAME_DENALI_3:
        case VAL_CHIP_NAME_MT6755:
        case VAL_CHIP_NAME_MT6757:
            //CAMCORDER_QUALITY_MTK_VGA_120
            {
                MediaProfiles::CamcorderProfile *VGA120Profile =
                    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_VGA_120, CAMCORDER_DAY_MODE, BACK_CAMERA);
                profiles->mCamcorderProfiles.add(VGA120Profile);
            }
            break;
        case VAL_CHIP_NAME_ROME:
        case VAL_CHIP_NAME_MT6795:
        case VAL_CHIP_NAME_MT6797:
            //CAMCORDER_QUALITY_MTK_720P_120
            {
                MediaProfiles::CamcorderProfile *SM720P120Profile =
                    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_720P_120, CAMCORDER_DAY_MODE, BACK_CAMERA);
                profiles->mCamcorderProfiles.add(SM720P120Profile);
                //CAMCORDER_QUALITY_MTK_720P_180
                MediaProfiles::CamcorderProfile *SM720P180Profile =
                    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_720P_180, CAMCORDER_DAY_MODE, BACK_CAMERA);
                profiles->mCamcorderProfiles.add(SM720P180Profile);
                //CAMCORDER_QUALITY_MTK_720P_240
                MediaProfiles::CamcorderProfile *SM720P240Profile =
                    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_720P_240, CAMCORDER_DAY_MODE, BACK_CAMERA);
                profiles->mCamcorderProfiles.add(SM720P240Profile);
                //CAMCORDER_QUALITY_MTK_1080P_120
                MediaProfiles::CamcorderProfile *SM1080P120Profile =
                    createMTKCamcorderProfile(CAMCORDER_QUALITY_MTK_1080P_120, CAMCORDER_DAY_MODE, BACK_CAMERA);
                profiles->mCamcorderProfiles.add(SM1080P120Profile);
            }
            break;
        default:
            break;
    }
#else//no MTK_SLOW_MOTION_VIDEO_SUPPORT
    UNUSEDP(profiles);
#endif//MTK_SLOW_MOTION_VIDEO_SUPPORT

    return;
}

/*static*/ MediaProfiles::VideoCodec *
MediaProfiles::createMTKLowVideoProfile (camcorder_mode CamMode, camera_id CamId)
{
    VideoCodec *videoCodec = NULL;
    switch(eChipName)
    {
        case VAL_CHIP_NAME_MT6572:
        case VAL_CHIP_NAME_MT6571:
        case VAL_CHIP_NAME_MT6575:
        case VAL_CHIP_NAME_MT6577:
        case VAL_CHIP_NAME_DENALI_2:
        case VAL_CHIP_NAME_MT6570:
        case VAL_CHIP_NAME_MT6580:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H263, 750*1000/CamMode, 176, 144, 30/CamMode);
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 150*1000/CamMode, 176, 144, 30/CamMode);
                }
            }
            break;
        default:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 750*1000/CamMode, 176, 144, 30/CamMode);
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 150*1000/CamMode, 176, 144, 30/CamMode);
                }
            }
            break;
    }
    return videoCodec;
}
/*static*/ MediaProfiles::VideoCodec *
MediaProfiles::createMTKMediumVideoProfile(camcorder_mode CamMode, camera_id CamId)
{
    VideoCodec *videoCodec = NULL;
#ifdef MTK_CAMCORDER_PROFILE_MID_MP4
    if (CamId == BACK_CAMERA){
        videoCodec = new MediaProfiles::VideoCodec(
                VIDEO_ENCODER_MPEG_4_SP, 4000*1000/CamMode, 480, 320, 30/CamMode);
    }
    else{
        videoCodec = new MediaProfiles::VideoCodec(
                VIDEO_ENCODER_MPEG_4_SP, 2000*1000/CamMode, 480, 320, 30/CamMode);
    }
#else//not MTK_CAMCORDER_PROFILE_MID_MP4
    UNUSEDP(CamId);
    switch(eChipName)
    {
        case VAL_CHIP_NAME_MT6572:
        case VAL_CHIP_NAME_MT6571:
        case VAL_CHIP_NAME_MT6575:
        case VAL_CHIP_NAME_MT6577:
        case VAL_CHIP_NAME_DENALI_2:
        case VAL_CHIP_NAME_MT6570:
        case VAL_CHIP_NAME_MT6580:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H264, 2500*1000/CamMode, 480, 320, 30/CamMode);
            }
            break;
#ifdef MTK_VIDEO_HEVC_SUPPORT
        case VAL_CHIP_NAME_ROME:
        case VAL_CHIP_NAME_MT6795:
        case VAL_CHIP_NAME_MT6797:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
                else {
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264,
                            (CamMode == CAMCORDER_DAY_MODE) ? 3000*1000 : 1250*1000,
                            640, 480, 30/CamMode);
                }
            }
            break;
#endif  // MTK_VIDEO_HEVC_SUPPORT
        case VAL_CHIP_NAME_MT6752:
        case VAL_CHIP_NAME_DENALI_1:
        case VAL_CHIP_NAME_DENALI_3:
        case VAL_CHIP_NAME_MT6755:
            {
                if (0 == queryLCA())
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, (CamMode == CAMCORDER_DAY_MODE) ? 3000*1000 : 1250*1000, 640, 480, 30/CamMode);
                }
                else
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 2500*1000/CamMode, 480, 320, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_MT6757:
            {
                if (0 == queryLCA())
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
                else
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 2500*1000/CamMode, 480, 320, 30/CamMode);
                }
            }
            break;
        default:
            {
                videoCodec = new MediaProfiles::VideoCodec(
                        VIDEO_ENCODER_H264,
                        (CamMode == CAMCORDER_DAY_MODE) ? 3000*1000 : 1250*1000,
                        640, 480, 30/CamMode);
            }
            break;
    }
#endif  // MTK_CAMCORDER_PROFILE_MID_MP4

    return videoCodec;
}
/*static*/ MediaProfiles::VideoCodec *
MediaProfiles::createMTKHighVideoProfile(camcorder_mode CamMode, camera_id CamId)
{
    VideoCodec *videoCodec = NULL;
    switch(eChipName)
    {
        case VAL_CHIP_NAME_MT6572:
        case VAL_CHIP_NAME_MT6571:
        case VAL_CHIP_NAME_MT6575:
        case VAL_CHIP_NAME_MT6577:
        case VAL_CHIP_NAME_DENALI_2:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 6000*1000/CamMode, 640, 480, 30/CamMode);
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_MT6570:
        case VAL_CHIP_NAME_MT6580:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                }
            }
            break;
#ifdef MTK_VIDEO_HEVC_SUPPORT
        case VAL_CHIP_NAME_ROME:
        case VAL_CHIP_NAME_MT6795:
        case VAL_CHIP_NAME_MT6797:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                }
                else {
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                }
            }
            break;
#endif  // MTK_VIDEO_HEVC_SUPPORT
        case VAL_CHIP_NAME_MT6752:
        case VAL_CHIP_NAME_DENALI_1:
        case VAL_CHIP_NAME_DENALI_3:
        case VAL_CHIP_NAME_MT6755:
            {
                if (CamId == BACK_CAMERA)
                {
                    if (0 == queryLCA())
                    {
                        videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                    }
                    else
                    {
                        videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 3000*1000/CamMode, 640, 480, 30/CamMode);
                    }
                }
                else
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_MT6757:
            {
                if (CamId == BACK_CAMERA)
                {
                    if (0 == queryLCA())
                    {
                        videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                    }
                    else
                    {
                        videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 3000*1000/CamMode, 640, 480, 30/CamMode);
                    }
                }
                else
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                }
            }
            break;
        default:
            {
                if (CamId == BACK_CAMERA){
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 4200*1000/CamMode, 640, 480, 30/CamMode);
                }
            }
            break;
    }
    return videoCodec;
}
/*static*/ MediaProfiles::VideoCodec *
MediaProfiles::createMTKFineVideoProfile(camcorder_mode CamMode, camera_id CamId)
{
    VideoCodec *videoCodec = NULL;
    unsigned int u4Width, u4Height, u4FrameRate, u4BitRate;

    UNUSEDP(CamId);
    switch(eChipName)
    {
        case VAL_CHIP_NAME_MT6572:
        case VAL_CHIP_NAME_MT6571:
        case VAL_CHIP_NAME_MT6575:
        case VAL_CHIP_NAME_MT6577:
        case VAL_CHIP_NAME_DENALI_2:
        case VAL_CHIP_NAME_MT6570:
        case VAL_CHIP_NAME_MT6580:
            {
                if(getVideoCapability(
                            VIDEO_ENCODER_MPEG_4_SP, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0)
                {
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, (u4BitRate)/CamMode,
                            u4Width, u4Height, u4FrameRate/CamMode);
                }
                else
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_MPEG_4_SP, 12500*1000/CamMode, 1280, 720, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_MT6582:
        case VAL_CHIP_NAME_MT6592:
        case VAL_CHIP_NAME_MT8135:
        case VAL_CHIP_NAME_MT8127:
            {
                if (sMemoryIsLarge) {
                    if(getVideoCapability(VIDEO_ENCODER_H264, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0)
                    {
                        if((u4Width >= 1920) && (u4Height >= 1088)){
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1088, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_H264, (u4BitRate)/CamMode, u4Width, u4Height, u4FrameRate/CamMode);
                        }
                    }
                    else
                    {
                        ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
                        videoCodec = new MediaProfiles::VideoCodec(
                                VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1088, 30/CamMode);
                    }
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_MT6752:
        case VAL_CHIP_NAME_DENALI_1:
        case VAL_CHIP_NAME_DENALI_3:
        case VAL_CHIP_NAME_MT6755:
            {
                if (sMemoryIsLarge) {
                    if(getVideoCapability(VIDEO_ENCODER_H264, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0)
                    {
                        if((u4Width >= 1920) && (u4Height >= 1088)){
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_H264, (u4BitRate)/CamMode, u4Width, u4Height, u4FrameRate/CamMode);
                        }
                    }
                    else
                    {
                        ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
                        videoCodec = new MediaProfiles::VideoCodec(
                                VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                    }
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_MT6757:
            {
                if (0 == queryLCA())
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 20000*1000/CamMode, 3840, 2176, 30/CamMode);
                }
                else
                {
                    videoCodec = new MediaProfiles::VideoCodec(VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
            }
            break;
        case VAL_CHIP_NAME_ROME:
        case VAL_CHIP_NAME_MT6795:
        case VAL_CHIP_NAME_MT6797:
#ifdef MTK_VIDEO_HEVC_SUPPORT
            {
                if (CamId == BACK_CAMERA){
                    if(getVideoCapability(VIDEO_ENCODER_HEVC, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0)
                    {
                        if((u4Width >= 3840) && (u4Height >= 2160)){
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_HEVC, 43000*1000/CamMode, 3840, 2160, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_HEVC, (u4BitRate)/CamMode, u4Width, u4Height, u4FrameRate/CamMode);
                        }
                    }
                    else
                    {
                        ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
                        videoCodec = new MediaProfiles::VideoCodec(
                                VIDEO_ENCODER_HEVC, 43000*1000/CamMode, 3840, 2160, 30/CamMode);
                    }
                }
                else {
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                }
            }
            break;
#else//not MTK_VIDEO_HEVC_SUPPORT
            {
                if (sMemoryIsLarge) {
                    if(getVideoCapability(VIDEO_ENCODER_H264, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0)
                    {
                        if((u4Width >= 1920) && (u4Height >= 1088)){
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_H264, (u4BitRate)/CamMode, u4Width, u4Height, u4FrameRate/CamMode);
                        }
                    }
                    else
                    {
                        ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
                        videoCodec = new MediaProfiles::VideoCodec(
                                VIDEO_ENCODER_H264, 17000*1000/CamMode, 1920, 1080, 30/CamMode);
                    }
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_H264, 9000*1000/CamMode, 1280, 720, 30/CamMode);
                }
            }
            break;
#endif  // MTK_VIDEO_HEVC_SUPPORT
        default:
            {
                if (sMemoryIsLarge){
                    if(getVideoCapability(
                                VIDEO_ENCODER_MPEG_4_SP, &u4Width, &u4Height, &u4BitRate, &u4FrameRate ) > 0)
                    {
                        if((u4Width >= 1920) && (u4Height >= 1088))
                        {
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_MPEG_4_SP, 26000*1000/CamMode, 1920, 1088, 30/CamMode);
                        }
                        else{
                            videoCodec = new MediaProfiles::VideoCodec(
                                    VIDEO_ENCODER_MPEG_4_SP, (u4BitRate)/CamMode,
                                    u4Width, u4Height, u4FrameRate/CamMode);
                        }
                    }
                    else
                    {
                        ALOGI("[%s] Cannot get video capability use default",__FUNCTION__);
                        videoCodec = new MediaProfiles::VideoCodec(
                                VIDEO_ENCODER_MPEG_4_SP, 26000*1000/CamMode, 1920, 1088, 30/CamMode);
                    }
                }
                else{
                    videoCodec = new MediaProfiles::VideoCodec(
                            VIDEO_ENCODER_MPEG_4_SP, 12500*1000/CamMode, 1280, 720, 30/CamMode);
                }
            }
            break;
    }

    return videoCodec;
}

/*static*/ void
MediaProfiles::xmlEnhancement(MediaProfiles *xmlProfiles)
{
    ALOGD("XML->mCamcorderProfiles.empty: %d", xmlProfiles->mCamcorderProfiles.empty());
    ALOGD("XML->mAudioEncoders.empty: %d", xmlProfiles->mAudioEncoders.empty());
    ALOGD("XML->mAudioDecoders.empty: %d", xmlProfiles->mAudioDecoders.empty());
    ALOGD("XML->mVideoEncoders.empty: %d", xmlProfiles->mVideoEncoders.empty());
    ALOGD("XML->mVideoDecoders.empty: %d", xmlProfiles->mVideoDecoders.empty());
    ALOGD("XML->mEncoderOutputFileFormats.empty: %d", xmlProfiles->mEncoderOutputFileFormats.empty());
    ALOGD("XML->mImageEncodingQualityLevels.empty: %d", xmlProfiles->mImageEncodingQualityLevels.empty());

    if (xmlProfiles->mCamcorderProfiles.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mCamcorderProfiles = sInstanceMtkDefault->mCamcorderProfiles;
    }
    if (xmlProfiles->mAudioEncoders.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mAudioEncoders = sInstanceMtkDefault->mAudioEncoders;
    }
#if 0//ignore decoder
    if (xmlProfiles->mAudioDecoders.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mAudioDecoders = sInstanceMtkDefault->mAudioDecoders;
    }
#endif//0
    if (xmlProfiles->mVideoEncoders.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mVideoEncoders = sInstanceMtkDefault->mVideoEncoders;
    }
#if 0//ignore decoder
    if (xmlProfiles->mVideoDecoders.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mVideoDecoders = sInstanceMtkDefault->mVideoDecoders;
    }
#endif//0
    if (xmlProfiles->mEncoderOutputFileFormats.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mEncoderOutputFileFormats = sInstanceMtkDefault->mEncoderOutputFileFormats;
    }
    if (xmlProfiles->mImageEncodingQualityLevels.empty()) {
        if (NULL == sInstanceMtkDefault) {
            sInstanceMtkDefault = createDefaultInstance();
        }
        xmlProfiles->mImageEncodingQualityLevels = sInstanceMtkDefault->mImageEncodingQualityLevels;
    }
}
#endif//MTK_AOSP_ENHANCEMENT
} // namespace android
