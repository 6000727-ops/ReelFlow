package uz.mirzaev.reelflow;

import android.content.Context;
import android.content.SharedPreferences;

final class Config {
  static final String INSTAGRAM="com.instagram.android", VK_CLIPS="com.vk.clips", VK_VIDEO="com.vk.vkvideo", VK="com.vkontakte.android";
  static final String INTERVAL="interval_seconds", SMART="smart_mode";
  static SharedPreferences prefs(Context c) { return c.getSharedPreferences("reelflow_settings",Context.MODE_PRIVATE); }
  static boolean allowed(Context c,String pkg) { if (pkg==null) return false; switch(pkg) { case INSTAGRAM: case VK_CLIPS: case VK_VIDEO: case VK: return prefs(c).getBoolean("app_"+pkg,true); default:return false; } }
  static int seconds(Context c) { return Math.max(10,Math.min(180,prefs(c).getInt(INTERVAL,30))); }
  static boolean smart(Context c) { return prefs(c).getBoolean(SMART,true); }
}
