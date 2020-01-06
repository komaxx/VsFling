package com.komaxx.vsfling;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.Activity;
import android.view.View;

import com.komaxx.komaxx_gl.util.KoLog;

public class CompatibilityUtils {
	/**
	 * Will set lightOutMode when available on device. </br>
	 * Do NOT call before "setContentView" was called!
	 */
	public static void setLightsOutMode(Activity a){
		try {
		
		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1){
			View rootView = a.findViewById(android.R.id.content);

			// find constant
			Field field = View.class.getField("SYSTEM_UI_FLAG_LOW_PROFILE");
			int SYSTEM_UI_FLAG_LOW_PROFILE = field.getInt(null);
			
			// find method and call it
			Method[] declaredMethods = View.class.getDeclaredMethods();
			for (Method m : declaredMethods){
				if (m.getName().equals("setSystemUiVisibility")){
					m.invoke(rootView, SYSTEM_UI_FLAG_LOW_PROFILE);
					KoLog.i("CompatibilityUtils", "lights out mode is set.");
				}
			}
		} // else: lights out mode not yet available
		
		} catch (Exception e){
			KoLog.w("CompatibilityUtils", "Setting lights out did not work :/ " + e.getMessage());
			e.printStackTrace();
		}
	}
}
