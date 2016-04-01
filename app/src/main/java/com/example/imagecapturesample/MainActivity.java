/*
 * Copyright (c) Olympus Imaging Corporation. All rights reserved.
 * Olympus Imaging Corp. licenses this software to you under EULA_OlympusCameraKit_ForDevelopers.pdf.
 */

package com.example.imagecapturesample;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCameraConnectionListener;
import jp.co.olympus.camerakit.OLYCameraKitException;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class MainActivity extends Activity implements OLYCameraConnectionListener {
	
	private final String TAG = this.toString();
	
	private boolean isActive = false;
	private Executor connectionExecutor = Executors.newFixedThreadPool(1);
	private BroadcastReceiver connectionReceiver;
	private OLYCamera camera = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getActionBar().hide();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		camera = new OLYCamera();
		camera.setContext(getApplicationContext());
		camera.setConnectionListener(this);
		connectionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				onReceiveBroadcastOfConnection(context, intent);
			}
		};
		
        ConnectingFragment fragment = new ConnectingFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment1, fragment);
		transaction.commitAllowingStateLoss();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		isActive = true;
		startScanningCamera();
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		isActive = false;
		unregisterReceiver(connectionReceiver);
		disconnectWithPowerOff(false);
	}

	private void onReceiveBroadcastOfConnection(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = wifiManager.getConnectionInfo();
			if (wifiManager.isWifiEnabled() && info != null && info.getNetworkId() != -1) {
				startConnectingCamera();
			}
		}
	}
	
	private void startScanningCamera() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(connectionReceiver, filter);
	}
	
	private void startConnectingCamera() {		
		connectionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

				try {
					camera.connect(OLYCamera.ConnectionType.WiFi);
				} catch (OLYCameraKitException e) {
					alertConnectingFailed(e);
					return;
				}
				try {
					camera.changeLiveViewSize(toLiveViewSize(preferences.getString("live_view_quality", "QVGA")));
				} catch (OLYCameraKitException e) {
					Log.w(TAG, "You had better uninstall this application and install it again.");
					alertConnectingFailed(e);
					return;
				}
				try {
					camera.changeRunMode(OLYCamera.RunMode.Recording);
				} catch (OLYCameraKitException e) {
					alertConnectingFailed(e);
					return;
				}
				
				// Restores my settings.
				if (camera.isConnected()) {
					Map<String, String> values = new HashMap<String, String>();
					for (String name : Arrays.asList(
							"TAKEMODE",
							"TAKE_DRIVE",
							"APERTURE",
							"SHUTTER",
							"EXPREV",
							"WB",
							"ISO",
							"RECVIEW"
					)) {
						String value = preferences.getString(name, null);
						if (value != null) {
							values.put(name, value);
						}
					}
					if (values.size() > 0) {
						try {
							camera.setCameraPropertyValues(values);
						} catch (OLYCameraKitException e) {
							Log.w(TAG, "To change the camera properties is failed: " + e.getMessage());
						}
					}
				}
				
				if (!camera.isAutoStartLiveView()) { // Please refer a document about OLYCamera.autoStartLiveView.
					// Start the live-view.
					// If you forget calling this method, live view will not be displayed on the screen.
					try {
						camera.startLiveView();
					} catch (OLYCameraKitException e) {
						Log.w(TAG, "To start the live-view is failed: " + e.getMessage());
						return;
					}
				}
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						onConnectedToCamera();
					}
				});
			}
		});
	}

	private void alertConnectingFailed(Exception e) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle("Connect failed")
			.setMessage(e.getMessage() != null ? e.getMessage() : "Unknown error")
			.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startScanningCamera();
				}
			});
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				builder.show();
			}
		});
	}
	
	public void disconnectWithPowerOff(final boolean powerOff) {		
		onDisconnectedFromCamera();
		
		connectionExecutor.execute(new Runnable() {
			@Override
			public void run() {
				// Stores current settings.
				if (camera.isConnected()) {
					Set<String> names = new HashSet<String>(Arrays.asList(
							"TAKEMODE",
							"TAKE_DRIVE",
							"APERTURE",
							"SHUTTER",
							"EXPREV",
							"WB",
							"ISO",
							"RECVIEW"
					));
					Map<String, String> values = null;
					try {
						values = camera.getCameraPropertyValues(names);
					} catch (OLYCameraKitException e) {
						Log.w(TAG, "To get the camera properties is failed: " + e.getMessage());
					}
					
					if (values != null) {
						SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
						Editor editor = preferences.edit();
						for (String key : values.keySet()) {
							editor.putString(key, values.get(key));
						}
						editor.commit();
					}
				}
				
				try {
					camera.disconnectWithPowerOff(powerOff);
				} catch (OLYCameraKitException e) {
					Log.w(this.toString(), "To disconnect from the camera is failed.");
				}
			}
		});
	}
	
	
	private void onConnectedToCamera() {
		if (isActive) {
			LiveViewFragment fragment = new LiveViewFragment();
			fragment.setCamera(camera);
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.fragment1, fragment);
			transaction.commitAllowingStateLoss();
		}
	}
	
	private void onDisconnectedFromCamera() {
		if (isActive) {
	        ConnectingFragment fragment = new ConnectingFragment();
	        FragmentTransaction transaction = getFragmentManager().beginTransaction();
	        transaction.replace(R.id.fragment1, fragment);
			transaction.commitAllowingStateLoss();
		}
	}
	
	@Override
	public void onDisconnectedByError(OLYCamera camera, OLYCameraKitException e) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				onDisconnectedFromCamera();
			}
		});
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
