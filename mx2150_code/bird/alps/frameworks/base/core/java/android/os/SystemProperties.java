/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.util.ArrayList;


/**
 * Gives access to the system properties store.  The system properties
 * store contains a list of string key-value pairs.
 *
 * {@hide}
 */
public class SystemProperties
{
    public static final int PROP_NAME_MAX = 31;
    public static final int PROP_VALUE_MAX = 91;

    private static final ArrayList<Runnable> sChangeCallbacks = new ArrayList<Runnable>();

    private static native String native_get(String key);
    private static native String native_get(String key, String def);
    private static native int native_get_int(String key, int def);
    private static native long native_get_long(String key, long def);
    private static native boolean native_get_boolean(String key, boolean def);
    private static native void native_set(String key, String def);
    private static native void native_add_change_callback();

    /*[BIRD][BIRD_SYSTEM_APP][品牌型号作假]huangzhangbin 20171031 begin*/
    private static final String PRODUCT_PROPERTY = "ro.product.model";
    private static final String BRANCH_PROPERTY = "ro.product.brand";
    private static final String PERSIST_PRODUCT_PROPERTY = "persist.sys.product.model";
    private static final String PERSIST_BRANCH_PROPERTY = "persist.sys.product.brand";
    /*[BIRD][BIRD_SYSTEM_APP][品牌型号作假]huangzhangbin 20171031 end*/

    /*[BIRD][BIRD_SYSTEM_MAIN_BORAD]huangzhangbin 20180319 begin*/
    private static final String MAIN_BOARD = "ro.product.board";
    private static final String PERSIST_MAIN_BOARD = "persist.sys.product.mainboard";
    /*[BIRD][BIRD_SYSTEM_MAIN_BORAD]huangzhangbin 20180319 end*/
    /**
     * Get the value for the given key.
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }

        /*[BIRD][BIRD_SYSTEM_APP][品牌型号作假]huangzhangbin 20171031 begin*/
        if(PRODUCT_PROPERTY.equals(key)) {
            String result = native_get(PERSIST_PRODUCT_PROPERTY);
            android.util.Log.i("huang1101", "result = " + result);
            if ("unknown".equals(result)) {
                return native_get(PRODUCT_PROPERTY);
            } else {
                return result;
            }
            //key = PERSIST_PRODUCT_PROPERTY;
        } else if (BRANCH_PROPERTY.equals(key)) {
            String result = native_get(PERSIST_BRANCH_PROPERTY);
            android.util.Log.i("huang1101", "result00 = " + result);
            if ("unknown".equals(result)) {
                return native_get(PRODUCT_PROPERTY);
            } else {
                return result;
            }
            //key = PERSIST_BRANCH_PROPERTY;
        }
        /*[BIRD][BIRD_SYSTEM_APP][品牌型号作假]huangzhangbin 20171031 end*/
        /*[BIRD][BIRD_SYSTEM_MAIN_BORAD]huangzhangbin 20180319 begin*/
        else if (MAIN_BOARD.equals(key)) {
            String result = native_get(PERSIST_MAIN_BOARD);
            android.util.Log.i("huang1101", "mainboard = " + result);
            if ("unknown".equals(result)) {
                return native_get(MAIN_BOARD);
            } else {
                return result;
            }
        }
        /*[BIRD][BIRD_SYSTEM_MAIN_BORAD]huangzhangbin 20180319 end*/
        return native_get(key);
    }

    /**
     * Get the value for the given key.
     * @return if the key isn't found, return def if it isn't null, or an empty string otherwise
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static String get(String key, String def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }

        /*[BIRD][BIRD_SYSTEM_APP][品牌型号作假]huangzhangbin 20171031 begin*/
        if(PRODUCT_PROPERTY.equals(key)) {
            String result = native_get(PERSIST_PRODUCT_PROPERTY, def);
            android.util.Log.i("huang1101", "result11 = " + result);
            if ("unknown".equals(result)) {
                return native_get(PRODUCT_PROPERTY, def);
            } else {
                return result;
            }
            //key = PERSIST_PRODUCT_PROPERTY;
        } else if (BRANCH_PROPERTY.equals(key)) {
            String result = native_get(PERSIST_BRANCH_PROPERTY, def);
            android.util.Log.i("huang1101", "result12 = " + result);
            if ("unknown".equals(result)) {
                return native_get(PRODUCT_PROPERTY, def);
            } else {
                return result;
            }
            //key = PERSIST_BRANCH_PROPERTY;
        }
        /*[BIRD][BIRD_SYSTEM_APP][品牌型号作假]huangzhangbin 20171031 end*/
        /*[BIRD][BIRD_SYSTEM_MAIN_BORAD]huangzhangbin 20180319 begin*/
        else if (MAIN_BOARD.equals(key)) {
            String result = native_get(PERSIST_MAIN_BOARD, def);
            android.util.Log.i("huang1101", "mainboard = " + result);
            if ("unknown".equals(result)) {
                return native_get(MAIN_BOARD, def);
            } else {
                return result;
            }
        }
        /*[BIRD][BIRD_SYSTEM_MAIN_BORAD]huangzhangbin 20180319 end*/
        return native_get(key, def);
    }

    /**
     * Get the value for the given key, and return as an integer.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static int getInt(String key, int def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_int(key, def);
    }

    /**
     * Get the value for the given key, and return as a long.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a long, or def if the key isn't found or
     *         cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static long getLong(String key, long def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_long(key, def);
    }

    /**
     * Get the value for the given key, returned as a boolean.
     * Values 'n', 'no', '0', 'false' or 'off' are considered false.
     * Values 'y', 'yes', '1', 'true' or 'on' are considered true.
     * (case sensitive).
     * If the key does not exist, or has any other value, then the default
     * result is returned.
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is
     *         not able to be parsed as a boolean.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    public static boolean getBoolean(String key, boolean def) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        return native_get_boolean(key, def);
    }

    /**
     * Set the value for the given key.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     * @throws IllegalArgumentException if the value exceeds 92 characters
     */
    public static void set(String key, String val) {
        if (key.length() > PROP_NAME_MAX) {
            throw new IllegalArgumentException("key.length > " + PROP_NAME_MAX);
        }
        if (val != null && val.length() > PROP_VALUE_MAX) {
            throw new IllegalArgumentException("val.length > " +
                PROP_VALUE_MAX);
        }
        native_set(key, val);
    }

    public static void addChangeCallback(Runnable callback) {
        synchronized (sChangeCallbacks) {
            if (sChangeCallbacks.size() == 0) {
                native_add_change_callback();
            }
            sChangeCallbacks.add(callback);
        }
    }

    static void callChangeCallbacks() {
        synchronized (sChangeCallbacks) {
            //Log.i("foo", "Calling " + sChangeCallbacks.size() + " change callbacks!");
            if (sChangeCallbacks.size() == 0) {
                return;
            }
            ArrayList<Runnable> callbacks = new ArrayList<Runnable>(sChangeCallbacks);
            for (int i=0; i<callbacks.size(); i++) {
                callbacks.get(i).run();
            }
        }
    }
}
