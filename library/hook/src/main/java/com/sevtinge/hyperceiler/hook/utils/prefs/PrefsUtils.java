/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.utils.prefs;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.sevtinge.hyperceiler.hook.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsChangeObserver.PrefToUri;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;

import de.robv.android.xposed.XposedBridge;

public class PrefsUtils {
    public static SharedPreferences mSharedPreferences = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();

    public static String mPrefsPathCurrent = null;
    public static String mPrefsFileCurrent = null;
    public static String mPrefsName = "hyperceiler_prefs";
    public static String mPrefsPath = "/data/user_de/0/" + ProjectApi.mAppModulePkg + "/shared_prefs";
    public static String mPrefsFile = mPrefsPath + "/" + mPrefsName + ".xml";

    public static SharedPreferences getSharedPrefs(Context context, boolean multiProcess) {
        context = AppsTool.getProtectedContext(context);
        try {
            return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_WORLD_READABLE : Context.MODE_WORLD_READABLE);
        } catch (Throwable t) {
            return context.getSharedPreferences(mPrefsName, multiProcess ? Context.MODE_MULTI_PROCESS | Context.MODE_PRIVATE : Context.MODE_PRIVATE);
        }
    }

    /**
     * 模块端调用：让配置文件对其他进程可读（供 zygote 阶段读取快照）。
     * 不依赖 New XSharedPreferences，失败时静默忽略。
     */
    public static void makePrefsReadable(Context context) {
        try {
            Context protectedContext = AppsTool.getProtectedContext(context);
            File dataDir = protectedContext.getDataDir();
            File prefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, mPrefsName + ".xml");
            dataDir.setExecutable(true, false);
            prefsDir.setExecutable(true, false);
            prefsDir.setReadable(true, false);
            if (prefsFile.exists()) prefsFile.setReadable(true, false);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 获取系统 Context（ActivityThread.getSystemContext），用于在被 hook 进程中访问模块的 ContentProvider。
     */
    public static Context getSystemContext() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            return (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 通过模块的 SharedPrefsProvider 一次性读取全部配置并写入 mPrefsMap。
     *
     * @return 是否成功读取到配置
     */
    public static boolean loadAllFromProvider(Context context) {
        if (context == null) return false;
        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/all");
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) return false;

            Map<String, Object> loaded = new HashMap<>();
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                String type = cursor.getString(1);
                String value = cursor.getString(2);
                if (key == null || type == null || value == null) continue;
                switch (type) {
                    case "boolean" -> loaded.put(key, "1".equals(value));
                    case "int" -> loaded.put(key, Integer.parseInt(value));
                    case "long" -> loaded.put(key, Long.parseLong(value));
                    case "float" -> loaded.put(key, Float.parseFloat(value));
                    case "string" -> loaded.put(key, value);
                    case "stringset" -> {
                        JSONArray array = new JSONArray(value);
                        Set<String> set = new LinkedHashSet<>();
                        for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
                        loaded.put(key, set);
                    }
                }
            }
            if (loaded.isEmpty()) return false;
            mPrefsMap.putAll(loaded);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return getSharedPrefs(context, false);
    }

    public static String getSharedPrefsPath() {
        if (mPrefsPathCurrent == null) try {
            Field mFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            mFile.setAccessible(true);
            mPrefsPathCurrent = ((File) mFile.get(mSharedPreferences)).getParentFile().getAbsolutePath();
            return mPrefsPathCurrent;
        } catch (Throwable t) {
            System.out.print("Test" + t);
            return mPrefsPath;
        }
        else return mPrefsPathCurrent;
    }

    public static String getSharedPrefsFile() {
        if (mPrefsFileCurrent == null) try {
            Field fFile = mSharedPreferences.getClass().getDeclaredField("mFile");
            fFile.setAccessible(true);
            mPrefsFileCurrent = ((File) fFile.get(mSharedPreferences)).getAbsolutePath();
            System.out.println("Test: mPrefsFileCurrent");
            return mPrefsFileCurrent;
        } catch (Throwable t) {
            System.out.println("Test: mPrefsFile" + t);
            return mPrefsFile;
        }
        else
            System.out.println("Test: mPrefsFileCurrent2");
        return mPrefsFileCurrent;
    }

    public static boolean contains(String key) {
        return mSharedPreferences.contains(key);
    }

    public static SharedPreferences.Editor editor() {
        return mSharedPreferences.edit();
    }

    public static void putString(String key, String defValue) {
        mSharedPreferences.edit().putString(key, defValue).apply();
    }

    public static String getSharedStringPrefs(Context context, String name, String defValue) {
        Uri uri = PrefToUri.stringPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String prefValue = cursor.getString(0);
                cursor.close();
                return prefValue;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (mPrefsMap.containsKey(name))
            return (String) mPrefsMap.getObject(name, defValue);
        else return defValue;
    }

    public static Set<String> getSharedStringSetPrefs(Context context, String name) {
        Uri uri = PrefToUri.stringSetPrefToUri(name);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                Set<String> prefValue = new LinkedHashSet<>();
                while (cursor.moveToNext()) {
                    prefValue.add(cursor.getString(0));
                }
                cursor.close();
                return prefValue;
            } else {
                XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: null");
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        LinkedHashSet<String> empty = new LinkedHashSet<>();
        if (mPrefsMap.containsKey(name)) {
            return (Set<String>) mPrefsMap.getObject(name, empty);
        } else {
            return empty;
        }
    }

    public static int getSharedIntPrefs(Context context, String name, int defValue) {
        Uri uri = PrefToUri.intPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (mPrefsMap.containsKey(name))
            return (int) mPrefsMap.getObject(name, defValue);
        else return defValue;
    }

    public static boolean getSharedBoolPrefs(Context context, String name, boolean defValue) {
        Uri uri = PrefToUri.boolPrefToUri(name, defValue);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int prefValue = cursor.getInt(0);
                cursor.close();
                return prefValue == 1;
            } else XposedLogUtils.logI("ContentResolver", "[" + name + "] Cursor fail: " + cursor);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

        if (mPrefsMap.containsKey(name))
            return (boolean) mPrefsMap.getObject(name, false);
        else
            return defValue;
    }

    public static void registerOnSharedPreferenceChangeListener(Context context) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            Log.i("prefs", "Changed: " + key);
            AppsTool.requestBackup(context);
            Object val = sharedPreferences.getAll().get(key);
            String path = "";
            if (val instanceof String)
                path = "string/";
            else if (val instanceof Set<?>)
                path = "stringset/";
            else if (val instanceof Integer)
                path = "integer/";
            else if (val instanceof Boolean)
                path = "boolean/";

            ContentResolver resolver = context.getContentResolver();
            resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/" + path + key), null);
            if (!path.isEmpty()) resolver.notifyChange(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/pref/" + path + key), null);
        });
    }
}
