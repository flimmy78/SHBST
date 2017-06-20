package com.shbst.bst.tftdisplay_15_h.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharePreference 封装
 */
public class PrefUtils {

	public static boolean getBoolean(Context ctx, String key, boolean defaultValue) {
		SharedPreferences sp = ctx.getSharedPreferences(key,
				Context.MODE_PRIVATE);
		return sp.getBoolean(key, defaultValue);
	}

	public static void setBoolean(Context ctx, String key, boolean value) {
		SharedPreferences sp = ctx.getSharedPreferences(key,
				Context.MODE_PRIVATE);
		sp.edit().putBoolean(key, value).commit();
	}

	public static String getString(Context ctx, String key, String defaultValue) {
		SharedPreferences sp = ctx.getSharedPreferences(key,
				Context.MODE_PRIVATE);
		return sp.getString(key, defaultValue);
	}

	public static void setString(Context ctx, String key, String value) {
		SharedPreferences sp = ctx.getSharedPreferences(key,
				Context.MODE_PRIVATE);
		sp.edit().putString(key, value).commit();
	}
}
