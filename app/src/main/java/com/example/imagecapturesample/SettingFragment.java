/*
 * Copyright (c) Olympus Imaging Corporation. All rights reserved.
 * Olympus Imaging Corp. licenses this software to you under EULA_OlympusCameraKit_ForDevelopers.pdf.
 */

package com.example.imagecapturesample;

import java.util.HashMap;
import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraKitException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingFragment extends PreferenceFragment {
	
	private final String TAG = this.toString();
	
	private SharedPreferences preferences;
	
	private OLYCamera camera;
	
	public void setCamera(OLYCamera camera) {
		this.camera = camera;
	}
	
	private boolean isShowPreviewEnabled() {
		if (preferences != null) {
			return preferences.getBoolean("show_preview", true);
		}
		
		return false;
	}
	
	private String getLiveViewQuality() {
		if (preferences != null) {
			return preferences.getString("live_view_quality", "QVGA");
		}
		
		return "QVGA";
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		Map<String, ?> items = preferences.getAll();
		Editor editor = preferences.edit();
		if (!items.containsKey("touch_shutter")) {
			editor.putBoolean("touch_shutter", true);
		}
		if (!items.containsKey("show_preview")) {
			editor.putBoolean("show_preview", true);
		}
		editor.commit();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		{
			final HashMap<String, String> sizeTable = new HashMap<String, String>();
			sizeTable.put("QVGA", "(320x240)");
			sizeTable.put("VGA", "(640x480)");
			sizeTable.put("SVGA", "(800x600)");
			sizeTable.put("XGA", "(1024x768)");
			
			ListPreference liveViewQuality = (ListPreference)findPreference("live_view_quality");
			
			liveViewQuality.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					preference.setSummary((String)newValue + " " + sizeTable.get(newValue));
					return true;
				}
			});
			liveViewQuality.setSummary(liveViewQuality.getValue() + " " + sizeTable.get(liveViewQuality.getValue()));
		}
		
		findPreference("power_off").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				MainActivity activity = (MainActivity)getActivity();
				activity.disconnectWithPowerOff(true);
				return true;
			}
		});
		
		findPreference("camerakit_version").setSummary(OLYCamera.getVersion());
		
		try {
			Map<String, Object> hardwareInformation = camera.inquireHardwareInformation();
			findPreference("camera_version").setSummary((String)hardwareInformation.get(OLYCamera.HARDWARE_INFORMATION_CAMERA_FIRMWARE_VERSION_KEY));
		} catch (OLYCameraKitException e) {
			findPreference("camera_version").setSummary("Unknown");
		}
	}
	
	@Override
	public void onPause() {		
		super.onPause();
		
		if (camera.isConnected()) {
			// Applies the live preview quality.
			try {
				camera.changeLiveViewSize(toLiveViewSize(getLiveViewQuality()));
			} catch (OLYCameraKitException e1) {
				Log.w(TAG, "To change the live view size is failed.");
			}

			String recviewValue = isShowPreviewEnabled() ? "<RECVIEW/ON>" : "<RECVIEW/OFF>";
			try {
				camera.setCameraPropertyValue("RECVIEW", recviewValue);
			} catch (OLYCameraKitException e1) {
				Log.w(TAG, "To change the rec-view is failed.");
			}
		}
	}
	
	private OLYCamera.LiveViewSize toLiveViewSize(String quality) {
		if (quality.equalsIgnoreCase("QVGA")) {
			return OLYCamera.LiveViewSize.QVGA;
		} else if (quality.equalsIgnoreCase("VGA")) {
			return OLYCamera.LiveViewSize.VGA;
		} else if (quality.equalsIgnoreCase("SVGA")) {
			return OLYCamera.LiveViewSize.SVGA;
		} else if (quality.equalsIgnoreCase("XGA")) {
			return OLYCamera.LiveViewSize.XGA;
		}
		return OLYCamera.LiveViewSize.QVGA;
	}
	
}
