/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.os;

import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
/*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
import android.os.Environment;
import android.os.SystemProperties;
import java.io.File;
import android.util.Log;
import android.text.TextUtils;
import android.app.ActivityThread;
import android.content.Context;
/*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
/*[BIRD][BIRD_SYSTEM_APP_DEFAULTRAMROM]huangzhangbin 20171123 begin*/
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
/*[BIRD][BIRD_SYSTEM_APP_DEFAULTRAMROM]huangzhangbin 20171123 end*/

/**
 * Retrieve overall information about the space on a filesystem. This is a
 * wrapper for Unix statvfs().
 */
public class StatFs {
    private StructStatVfs mStat;

    /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
    /**
     * @hide
    */
    private String mPath;
    /**
     * @hide
    */
    private static final String FAKE_ROM = SystemProperties.get("persist.sys.fake_rom","0");
    /**
     * @hide
    */
    private static final String MEMORY_FLASH = SystemProperties.get("ro.bird.memory.flash","BIRD_EMMC_64_8_DDR3");
    private static final boolean BIRD_SYSTEM_APP = SystemProperties.get("ro.bird_system_app").equals("1");
    private static final boolean BIRD_SYSTEM_APP_FOR_DM = SystemProperties.get("ro.bird_system_app_for_dm").equals("1");
    private static final boolean BIRD_SYSTEM_APP_DEFAULTRAMROM = SystemProperties.get("ro.bird_system_for_ramrom").equals("1");
    /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/

    /**
     * Construct a new StatFs for looking at the stats of the filesystem at
     * {@code path}. Upon construction, the stat of the file system will be
     * performed, and the values retrieved available from the methods on this
     * class.
     *
     * @param path path in the desired file system to stat.
     */
    public StatFs(String path) {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        mPath = path;
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
        mStat = doStat(path);
    }

    private static StructStatVfs doStat(String path) {
        try {
            return Os.statvfs(path);
        } catch (ErrnoException e) {
            throw new IllegalArgumentException("Invalid path: " + path, e);
        }
    }

    /**
     * Perform a restat of the file system referenced by this object. This is
     * the same as re-constructing the object with the same file system path,
     * and the new stat values are available upon return.
     */
    public void restat(String path) {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        mPath = path;
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
        mStat = doStat(path);
    }

    /**
     * @deprecated Use {@link #getBlockSizeLong()} instead.
     */
    @Deprecated
    public int getBlockSize() {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        if (mPath != null) {
            if (mPath.trim().equals("/data")) {
                Log.i("wushiyongRomTest", "data_blockSize = " + mStat.f_bsize);
            } else if (mPath.trim().equals("/storage/sdcard0")) {
                Log.i("wushiyongRomTest", "sdcard0_blockSize = " + mStat.f_bsize);
            } else if (mPath.trim().equals("/storage/sdcard1")) {
                Log.i("wushiyongRomTest", "sdcard1_blockSize = " + mStat.f_bsize);
            }
        }
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
        return (int) mStat.f_bsize;
    }

    /**
     * The size, in bytes, of a block on the file system. This corresponds to
     * the Unix {@code statvfs.f_bsize} field.
     */
    public long getBlockSizeLong() {
        return mStat.f_bsize;
    }

    /**
     * @deprecated Use {@link #getBlockCountLong()} instead.
     */
    @Deprecated
    public int getBlockCount() {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        if (mPath != null) {
            if (mPath.trim().equals("/data")) {
                Log.i("wushiyongRomTest", "data_blocksCount = " + mStat.f_blocks);
            } else if (mPath.trim().equals("/storage/sdcard0")) {
                Log.i("wushiyongRomTest", "sdcard0_blocksCount = " + mStat.f_blocks);
            } else if (mPath.trim().equals("/storage/sdcard1")) {
                Log.i("wushiyongRomTest", "sdcard1_blocksCount = " + mStat.f_blocks);
            }
        }
        if (BIRD_SYSTEM_APP) {
            boolean flag = SystemProperties.get("persist.sys.is_first_boot", "1").equals("1");
            if (flag) {
                initProperty();
                //SystemProperties.set("persist.sys.is_first_boot", "0");
            }
            if (!(mPath.trim().equals("/data")) && !(mPath.trim().equals("/storage/emulated/0"))) {
                return (int) mStat.f_blocks;
            }
            String currentInternalStr = SystemProperties.get("persist.sys.tt.rom_sd");
            if (TextUtils.isEmpty(currentInternalStr)) {
                return (int) mStat.f_blocks;
            } 
            boolean isSwitchDm = SystemProperties.get("persist.sys.dm_switch", "0").equals("1");
            if (isSwitchDm) {
                final Context context = ActivityThread.currentApplication().getApplicationContext();
                String packageName = context.getPackageName();
                Log.d("shujiang","packageName:"+packageName);
                if (packageName != null && (packageName.equals("cn.richinfo.dm"))) {
                    String dmcurrentInternalStr = SystemProperties.get("persist.sys.dm.rom_sd");
                    if (TextUtils.isEmpty(dmcurrentInternalStr)) {
                        return (int) mStat.f_blocks;
                    } 
                    if (mPath.trim().equals("/data")) {
                        return Integer.valueOf(dmcurrentInternalStr) * 256;
                    }
                }
            }
            return Integer.valueOf(currentInternalStr) * 256;
        }
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/ 
        return (int) mStat.f_blocks;
    }

    /**
     * The total number of blocks on the file system. This corresponds to the
     * Unix {@code statvfs.f_blocks} field.
     */
    public long getBlockCountLong() {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        if (BIRD_SYSTEM_APP) {
            boolean flag = SystemProperties.get("persist.sys.is_first_boot", "1").equals("1");
            if (flag) {
                initProperty();
                //SystemProperties.set("persist.sys.is_first_boot", "0");
            }

            if (!(mPath.trim().equals("/data")) && !(mPath.trim().equals("/storage/emulated/0"))) {
                return mStat.f_blocks;
            }
            String currentInternalStr = SystemProperties.get("persist.sys.tt.rom_sd");
            if (TextUtils.isEmpty(currentInternalStr)) {
                return mStat.f_blocks;
            } 
            boolean isSwitchDm = SystemProperties.get("persist.sys.dm_switch", "0").equals("1");
            if (isSwitchDm) {
                final Context context = ActivityThread.currentApplication().getApplicationContext();
                String packageName = context.getPackageName();
                Log.d("shujiang","packageName:"+packageName);
                if (packageName != null && (packageName.equals("cn.richinfo.dm"))) {
                    String dmcurrentInternalStr = SystemProperties.get("persist.sys.dm.rom_sd");
                    if (TextUtils.isEmpty(dmcurrentInternalStr)) {
                        return mStat.f_blocks;
                    } 
                    if (mPath.trim().equals("/data")) {
                        return Long.valueOf(dmcurrentInternalStr) * 256;
                    }
                }
            }
            return Long.valueOf(currentInternalStr) * 256;
        } 
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
        return mStat.f_blocks;
    }

    /**
     * @deprecated Use {@link #getFreeBlocksLong()} instead.
     */
    @Deprecated
    public int getFreeBlocks() {
        return (int) mStat.f_bfree;
    }

    /**
     * The total number of blocks that are free on the file system, including
     * reserved blocks (that are not available to normal applications). This
     * corresponds to the Unix {@code statvfs.f_bfree} field. Most applications
     * will want to use {@link #getAvailableBlocks()} instead.
     */
    public long getFreeBlocksLong() {
        return mStat.f_bfree;
    }

    /**
     * The number of bytes that are free on the file system, including reserved
     * blocks (that are not available to normal applications). Most applications
     * will want to use {@link #getAvailableBytes()} instead.
     */
    public long getFreeBytes() {
        return mStat.f_bfree * mStat.f_bsize;
    }

    /**
     * @deprecated Use {@link #getAvailableBlocksLong()} instead.
     */
    @Deprecated
    public int getAvailableBlocks() {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        if (BIRD_SYSTEM_APP) {
            if (!(mPath.trim().equals("/data")) && !(mPath.trim().equals("/storage/emulated/0"))) {
                return (int) mStat.f_bavail;
            }
            String realInternalStr = SystemProperties.get("persist.sys.tt.rom_sd_b");
            String currentInternalStr = SystemProperties.get("persist.sys.tt.rom_sd");
            if (TextUtils.isEmpty(realInternalStr) || TextUtils.isEmpty(currentInternalStr)) {
                return (int) mStat.f_bavail;
            }
            long realValue = Long.valueOf(realInternalStr);
            long currentValue = Long.valueOf(currentInternalStr);
            if (BIRD_SYSTEM_APP_FOR_DM) {
                if (BIRD_SYSTEM_APP_DEFAULTRAMROM) {
                    String factoryRom = "";
                    if (MEMORY_FLASH.contains("32_4") ) {
                        factoryRom = "4096";
                    } else if (MEMORY_FLASH.contains("64_8") ) {
                        factoryRom = "8192";
                    } else  if (MEMORY_FLASH.contains("128_8") || MEMORY_FLASH.contains("128_16") || MEMORY_FLASH.contains("128_24")) {
                        factoryRom = "16384";
                    } else if (MEMORY_FLASH.contains("256_16") || MEMORY_FLASH.contains("256_24")) {
                        factoryRom = "32768";
                    }
                    return (int) (mStat.f_bavail + (currentValue - Long.valueOf(factoryRom)) * 256);
                } else {
                    return (int) (mStat.f_bavail + (currentValue - realValue) * 256);
                }
            }
            return (int) (mStat.f_bavail * currentValue / realValue);
        }
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
        return (int) mStat.f_bavail;
    }

    /**
     * The number of blocks that are free on the file system and available to
     * applications. This corresponds to the Unix {@code statvfs.f_bavail} field.
     */
    public long getAvailableBlocksLong() {
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
        if (BIRD_SYSTEM_APP) {
            if (!(mPath.trim().equals("/data")) && !(mPath.trim().equals("/storage/emulated/0"))) {
                return mStat.f_bavail;
            }
            String realInternalStr = SystemProperties.get("persist.sys.tt.rom_sd_b");
            String currentInternalStr = SystemProperties.get("persist.sys.tt.rom_sd");
            if (TextUtils.isEmpty(realInternalStr) || TextUtils.isEmpty(currentInternalStr)) {
                return mStat.f_bavail;
            } 
            long realValue = Long.valueOf(realInternalStr);
            long currentValue = Long.valueOf(currentInternalStr);
            if (BIRD_SYSTEM_APP_FOR_DM) {
                if (BIRD_SYSTEM_APP_DEFAULTRAMROM) {
                    String factoryRom = "";
                    if (MEMORY_FLASH.contains("32_4") ) {
                        factoryRom = "4096";
                    } else if (MEMORY_FLASH.contains("64_8") ) {
                        factoryRom = "8192";
                    } else  if (MEMORY_FLASH.contains("128_8") || MEMORY_FLASH.contains("128_16") || MEMORY_FLASH.contains("128_24")) {
                        factoryRom = "16384";
                    } else if (MEMORY_FLASH.contains("256_16") || MEMORY_FLASH.contains("256_24")) {
                        factoryRom = "32768";
                    }
                    return mStat.f_bavail + (currentValue - Long.valueOf(factoryRom)) * 256;
                } else {
                    return mStat.f_bavail + (currentValue - realValue) * 256;
                }
            }
            return mStat.f_bavail * currentValue / realValue;  
        }
        /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/
        return mStat.f_bavail;
    }

    /**
     * The number of bytes that are free on the file system and available to
     * applications.
     */
    public long getAvailableBytes() {
        return mStat.f_bavail * mStat.f_bsize;
    }

    /**
     * The total number of bytes supported by the file system.
     */
    public long getTotalBytes() {
        return mStat.f_blocks * mStat.f_bsize;
    }

    /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 begin*/
    private void initProperty() {
        String factoryRom = "";
        String factoryRam = "";
        String factoryMinRam = "";

        if (MEMORY_FLASH.contains("32_4") ) {
            factoryRom = "4096";
        } else if (MEMORY_FLASH.contains("64_8") ) {
            factoryRom = "8192";
        } else  if (MEMORY_FLASH.contains("128_8") || MEMORY_FLASH.contains("128_16") || MEMORY_FLASH.contains("128_24")) {
            factoryRom = "16384";
        } else if (MEMORY_FLASH.contains("256_16") || MEMORY_FLASH.contains("256_24")) {
            factoryRom = "32768";
        }
        android.util.Log.i("huang1025", "initProperty MEMORY_FLASH = " + MEMORY_FLASH + ", factoryRom = " + factoryRom);
        if (MEMORY_FLASH.contains("_4") ) {
            factoryRam = "512";
            factoryMinRam = "100";
        } else if (MEMORY_FLASH.contains("_8")  ) {
            factoryRam = "1024";
            factoryMinRam = "300";
        } else if (MEMORY_FLASH.contains("_16") ) {
            factoryRam = "2048";
            factoryMinRam = "1024";
        } else if (MEMORY_FLASH.contains("_24")) {
            factoryRam = "3072";
            factoryMinRam = "1024";
        }
        if (BIRD_SYSTEM_APP_DEFAULTRAMROM) {
            factoryRam = SystemProperties.get("ro.bird_system_for_ram");//4096
            factoryRom = SystemProperties.get("ro.bird_system_for_rom");//32768
            factoryMinRam = SystemProperties.get("ro.bird_system_for_ram_min");//32768
            writeTotalRam(factoryRam);
            SystemProperties.set("persist.sys.tt.ram_max", factoryRam);
            SystemProperties.set("persist.sys.tt.ram_min", factoryMinRam);
        }

        SystemProperties.set("persist.sys.tt.ram_min_b", factoryMinRam);
        SystemProperties.set("persist.sys.tt.ram_max_b", factoryRam);
        SystemProperties.set("persist.sys.tt.rom_data_b", factoryRom);
        SystemProperties.set("persist.sys.tt.rom_sd_b", factoryRom);
        SystemProperties.set("persist.sys.tt.rom_data", factoryRom);
        SystemProperties.set("persist.sys.tt.rom_sd", factoryRom);
        SystemProperties.set("persist.sys.dm.rom_sd", factoryRom);
    }
    /*[BIRD][BIRD_SYSTEM_APP]huangzhangbin 20171021 end*/

    /*[BIRD][BIRD_SYSTEM_APP_DEFAULTRAMROM]huangzhangbin 20171123 begin*/
    private void writeTotalRam(String value) {
        android.util.Log.i("huang1030", "writeTotalRam value = " + value, new Exception("value"));
        File RamTimeFile2t = new File("/proc/meminfo");
        FileWriter mfr2t;
        try {
            mfr2t = new FileWriter(RamTimeFile2t);
            mfr2t.write(value);
            mfr2t.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*[BIRD][BIRD_SYSTEM_APP_DEFAULTRAMROM]huangzhangbin 20171123 end*/
}
