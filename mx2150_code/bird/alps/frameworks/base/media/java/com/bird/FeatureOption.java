package com.bird;

import android.os.SystemProperties;
public final class FeatureOption {


	//[BIRD][BIRD_DEFAULT_RING_VOLUME_VALUE][默认铃声音量大小][luye][20160128]begin
	public static final int BIRD_DEFAULT_RING_VOLUME_VALUE = getIntValue("ro.bd_def_ring_volume");
	//[BIRD][BIRD_DEFAULT_RING_VOLUME_VALUE][默认铃声音量大小][luye][20160128]end

	//[BIRD][BIRD_DEFAULT_ALARM_VOLUME_VALUE][默认闹钟音量大小][luye][20160128]begin
	public static final int BIRD_DEFAULT_ALARM_VOLUME_VALUE = getIntValue("ro.bd_def_alarm_volume");
	//[BIRD][BIRD_DEFAULT_ALARM_VOLUME_VALUE][默认闹钟音量大小][luye][20160128]end
	
	//[BIRD][BIRD_DEFAULT_MEDIA_VOLUME_VALUE][默认媒体音量大小][luye][20160128]begin
	public static final int BIRD_DEFAULT_MEDIA_VOLUME_VALUE = getIntValue("ro.bd_def_media_volume");
	//[BIRD][BIRD_DEFAULT_MEDIA_VOLUME_VALUE][默认媒体音音量大小][luye][20160128]end

	//[BIRD][BIRD_DEFAULT_CALL_VOLUME_VALUE][默认通话音量大小][zhangaman][20170722]begin
	public static final int BIRD_DEFAULT_CALL_VOLUME_VALUE = getCallVolumeIntValue("ro.bd_def_call_volume");
	//[BIRD][BIRD_DEFAULT_CALL_VOLUME_VALUE][默认通话音量大小][zhangaman][20170722]end

    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }

    private static int getIntValue(String key) {
		final int value = SystemProperties.getInt(key, 15);
    	/*final String value = SystemProperties.get(key);
    	if(value != null && value.length() > 0){
    		return Integer.parseInt(value);
    	}*/
    	return value;
   }

    //[BIRD][BIRD_DEFAULT_CALL_VOLUME_VALUE][默认通话音量大小][zhangaman][20170722]begin
    private static int getCallVolumeIntValue(String key) {
        final int value = SystemProperties.getInt(key, 4);
    	return value;
    }
    //[BIRD][BIRD_DEFAULT_CALL_VOLUME_VALUE][默认通话音量大小][zhangaman][20170722]end
}
