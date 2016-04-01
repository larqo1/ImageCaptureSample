/*
 * Copyright (c) Olympus Imaging Corporation. All rights reserved.
 * Olympus Imaging Corp. licenses this software to you under EULA_OlympusCameraKit_ForDevelopers.pdf.
 */

package com.example.imagecapturesample;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.co.olympus.camerakit.OLYCamera;
import jp.co.olympus.camerakit.OLYCamera.ActionType;
import jp.co.olympus.camerakit.OLYCamera.TakingProgress;
import jp.co.olympus.camerakit.OLYCameraAutoFocusResult;
import jp.co.olympus.camerakit.OLYCameraKitException;
import jp.co.olympus.camerakit.OLYCameraLiveViewListener;
import jp.co.olympus.camerakit.OLYCameraPropertyListener;
import jp.co.olympus.camerakit.OLYCameraRecordingListener;
import jp.co.olympus.camerakit.OLYCameraRecordingSupportsListener;
import jp.co.olympus.camerakit.OLYCameraStatusListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class LiveViewFragment extends Fragment implements OLYCameraLiveViewListener, OLYCameraStatusListener, OLYCameraPropertyListener, OLYCameraRecordingListener, OLYCameraRecordingSupportsListener, View.OnClickListener, View.OnTouchListener {
	private static final String CAMERA_PROPERTY_TAKE_MODE = "TAKEMODE";
	private static final String CAMERA_PROPERTY_DRIVE_MODE = "TAKE_DRIVE";
	private static final String CAMERA_PROPERTY_APERTURE_VALUE = "APERTURE";
	private static final String CAMERA_PROPERTY_SHUTTER_SPEED = "SHUTTER";
	private static final String CAMERA_PROPERTY_EXPOSURE_COMPENSATION = "EXPREV";
	private static final String CAMERA_PROPERTY_ISO_SENSITIVITY = "ISO";
	private static final String CAMERA_PROPERTY_WHITE_BALANCE = "WB";
	private static final String CAMERA_PROPERTY_BATTERY_LEVEL = "BATTERY_LEVEL";
	
	private CameraLiveImageView imageView;
	private ImageView batteryLevelImageView;
	private TextView remainingRecordableImagesTextView;
	private ImageView drivemodeImageView;
	private TextView takemodeTextView;
	private TextView shutterSpeedTextView;
	private TextView apertureValueTextView;
	private TextView exposureCompensationTextView;
	private TextView isoSensitivityTextView;
	private ImageView whiteBalanceImageView;
	private ImageView shutterImageView;
	private ImageView settingImageView;
	private ImageView unlockImageView;
//	private RectF imageUserInteractionArea = new RectF(0, 0, 1, 1);
	private MediaPlayer focusedSoundPlayer;
	private MediaPlayer shutterSoundPlayer;
	private Boolean enabledTouchShutter;
	private Boolean enabledFocusLock;
	
	private OLYCamera camera;
	
	public void setCamera(OLYCamera camera) {
		this.camera = camera;
	}
	
	@SuppressWarnings("serial")
	private static final Map<String, Integer> drivemodeIconList = new HashMap<String, Integer>() {
		{
			put("<TAKE_DRIVE/DRIVE_NORMAL>"  , R.drawable.icn_drive_setting_single);
			put("<TAKE_DRIVE/DRIVE_CONTINUE>", R.drawable.icn_drive_setting_seq_l);
		}
	};
	
	@SuppressWarnings("serial")
	private static final Map<String, Integer> whiteBalanceIconList = new HashMap<String, Integer>() {
		{
			put("<WB/WB_AUTO>"          , R.drawable.icn_wb_setting_wbauto);
			put("<WB/MWB_SHADE>"        , R.drawable.icn_wb_setting_16);
			put("<WB/MWB_CLOUD>"        , R.drawable.icn_wb_setting_17);
			put("<WB/MWB_FINE>"         , R.drawable.icn_wb_setting_18);
			put("<WB/MWB_LAMP>"         , R.drawable.icn_wb_setting_20);
			put("<WB/MWB_FLUORESCENCE1>", R.drawable.icn_wb_setting_35);
			put("<WB/MWB_WATER_1>"      , R.drawable.icn_wb_setting_64);
			put("<WB/WB_CUSTOM1>"       , R.drawable.icn_wb_setting_512);
		}
	};

	@SuppressWarnings("serial")
	private static final Map<String, Integer> batteryIconList = new HashMap<String, Integer>() {
		{
			put("<BATTERY_LEVEL/UNKNOWN>"       , 0);
			put("<BATTERY_LEVEL/CHARGE>"        , R.drawable.tt_icn_battery_charge);
			put("<BATTERY_LEVEL/EMPTY>"         , R.drawable.tt_icn_battery_empty);
			put("<BATTERY_LEVEL/WARNING>"       , R.drawable.tt_icn_battery_half);
			put("<BATTERY_LEVEL/LOW>"           , R.drawable.tt_icn_battery_middle);
			put("<BATTERY_LEVEL/FULL>"          , R.drawable.tt_icn_battery_full);
			put("<BATTERY_LEVEL/EMPTY_AC>"      , R.drawable.tt_icn_battery_supply_empty);
			put("<BATTERY_LEVEL/SUPPLY_WARNING>", R.drawable.tt_icn_battery_supply_half);
			put("<BATTERY_LEVEL/SUPPLY_LOW>"    , R.drawable.tt_icn_battery_supply_middle);
			put("<BATTERY_LEVEL/SUPPLY_FULL>"   , R.drawable.tt_icn_battery_supply_full);
		}
	};
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		focusedSoundPlayer = MediaPlayer.create(activity, R.raw.focusedsound);
		shutterSoundPlayer = MediaPlayer.create(activity, R.raw.shuttersound);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_live_view, container, false);
		
		imageView = (CameraLiveImageView)view.findViewById(R.id.cameraLiveImageView);
		batteryLevelImageView = (ImageView)view.findViewById(R.id.batteryLevelImageView);
		remainingRecordableImagesTextView = (TextView)view.findViewById(R.id.remainingRecordableImagesTextView);
		drivemodeImageView = (ImageView)view.findViewById(R.id.drivemodeImageView);
		takemodeTextView = (TextView)view.findViewById(R.id.takemodeTextView);
		shutterSpeedTextView = (TextView)view.findViewById(R.id.shutterSpeedTextView);
		apertureValueTextView = (TextView)view.findViewById(R.id.apertureValueTextView);
		exposureCompensationTextView = (TextView)view.findViewById(R.id.exposureCompensationTextView);
		isoSensitivityTextView = (TextView)view.findViewById(R.id.isoSensitivityTextView);
		whiteBalanceImageView = (ImageView)view.findViewById(R.id.whiteBalaneImageView);
		shutterImageView = (ImageView)view.findViewById(R.id.shutterImageView);
		settingImageView = (ImageView)view.findViewById(R.id.settingImageView);
		unlockImageView = (ImageView)view.findViewById(R.id.unlockImageView);
		
		imageView.setOnTouchListener(this);
		shutterImageView.setOnTouchListener(this);
		drivemodeImageView.setOnClickListener(this);
		takemodeTextView.setOnClickListener(this);
		shutterSpeedTextView.setOnClickListener(this);
		apertureValueTextView.setOnClickListener(this);
		exposureCompensationTextView.setOnClickListener(this);
		isoSensitivityTextView.setOnClickListener(this);
		whiteBalanceImageView.setOnClickListener(this);
		settingImageView.setOnClickListener(this);
		unlockImageView.setOnClickListener(this);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		enabledTouchShutter = preferences.getBoolean("touch_shutter", false);
		unlockImageView.setVisibility(enabledTouchShutter ? View.INVISIBLE : View.VISIBLE);
		
		camera.setLiveViewListener(this);
		camera.setCameraPropertyListener(this);
		camera.setCameraStatusListener(this);
		camera.setRecordingListener(this);
		camera.setRecordingSupportsListener(this);
		
		updateDrivemodeImageView();
		updateTakemodeTextView();
		updateShutterSpeedTextView();
		updateApertureValueTextView();
		updateExposureCompensationTextView();
		updateIsoSensitivityTextView();
		updateWhiteBalanceImageView();
		updateBatteryLevelImageView();
		updateRemainingRecordableImagesTextView();
		
		try {
			camera.clearAutoFocusPoint();
			camera.unlockAutoFocus();
		} catch (OLYCameraKitException ee) {
		}
		enabledFocusLock = false;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		camera.setLiveViewListener(null);
		camera.setCameraPropertyListener(null);
		camera.setCameraStatusListener(null);
		camera.setRecordingListener(null);
		camera.setRecordingSupportsListener(null);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v == imageView) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				imageViewDidTouchDown(event);
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				imageViewDidTouchUp();
			}
		} else if (v == shutterImageView) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				shutterImageViewDidTouchDown();
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				shutterImageViewDidTouchUp();
			}
		}
		return true;
	}
	
	@Override
	public void onClick(View v) {		
		if (v == drivemodeImageView) {
			drivemodeImageViewDidTap();
		} else if (v == takemodeTextView) {
			takemodeTextViewDidTap();
		} else if (v == shutterSpeedTextView) {
			shutterSpeedTextViewDidTap();
		} else if (v == apertureValueTextView) {
			apertureValueTextViewDidTap();
		} else if (v == exposureCompensationTextView) {
			exposureCompensationTextViewDidTap();
		} else if (v == isoSensitivityTextView) {
			isoSensitivityTextViewDidTap();
		} else if (v == whiteBalanceImageView) {
			whiteBalanceImageViewDidTap();
		} else if (v == settingImageView) {
			settingImageViewDidTap();
		} else if (v == unlockImageView) {
			unlockImageViewDidTap();
		}
	}

	private void settingImageViewDidTap() {
		if (camera.isTakingPicture()) {
			stopTakingPicture();
		}
		if (camera.isRecordingVideo()) {
			stopRecordingVideo();
		}
		
        SettingFragment fragment = new SettingFragment();
        fragment.setCamera(camera);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(getId(), fragment);
       	transaction.addToBackStack(null);
       	transaction.commit();
	}

	
	// -------------------------------------------------------------------------
	// Camera actions
	// -------------------------------------------------------------------------

	//
	// Touch Shutter mode:
	//   - Tap a subject to focus and automatically release the shutter.
	//
	// Touch AF mode:
	//   - Tap to display a focus frame and focus on the subject in the selected area.
	//   - You can use the image view to choose the position of the focus frame.
	//   - Photographs can be taken by tapping the shutter button.
	//
	
	// UI events
	
	private void imageViewDidTouchDown(MotionEvent event) {
		OLYCamera.ActionType actionType = camera.getActionType();
		
		// If the focus point is out of area, ignore the touch.
		PointF point = imageView.getPointWithEvent(event);
		if (!imageView.isContainsPoint(point)) {
			return;
		}

		if (enabledTouchShutter) {
			// Touch Shutter mode
			if (actionType == OLYCamera.ActionType.Single) {
				takePictureWithPoint(point);
			} else if (actionType == OLYCamera.ActionType.Sequential) {
				startTakingPictureWithPoint(point);
			} else if (actionType == OLYCamera.ActionType.Movie) {
				if (camera.isRecordingVideo()) {
					stopRecordingVideo();
				} else {
					startRecordingVideo();
				}
			}
		} else {
			// Touch AF mode
			if (actionType == OLYCamera.ActionType.Single ||
				actionType == OLYCamera.ActionType.Sequential) {
				lockAutoFocus(point);
			}
		}
	}
	
	private void imageViewDidTouchUp() {
		OLYCamera.ActionType actionType = camera.getActionType();
		if (enabledTouchShutter) {
			// Touch Shutter mode
			if (actionType == OLYCamera.ActionType.Sequential) {
				stopTakingPicture();
			}
		}
	}
	
	private void shutterImageViewDidTouchDown() {
		OLYCamera.ActionType actionType = camera.getActionType();
		if (actionType == OLYCamera.ActionType.Single) {
			takePicture();
		} else if (actionType == OLYCamera.ActionType.Sequential) {
			startTakingPicture();
		} else if (actionType == OLYCamera.ActionType.Movie) {
			if (camera.isRecordingVideo()) {
				stopRecordingVideo();
			} else {
				startRecordingVideo();
			}
		}
	}
	
	private void shutterImageViewDidTouchUp() {
		OLYCamera.ActionType actionType = camera.getActionType();
		if (actionType == OLYCamera.ActionType.Sequential) {
			stopTakingPicture();
		}
	}
	
	private void unlockImageViewDidTap() {
		unlockAutoFocus();
	}
	
	// focus control
	
	private void lockAutoFocus(PointF point) {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}
		
		// Display a provisional focus frame at the touched point.
		final RectF preFocusFrameRect;
		{
			float focusWidth = 0.125f;  // 0.125 is rough estimate.
			float focusHeight = 0.125f;
			float imageWidth = imageView.getIntrinsicContentSizeWidth();
			float imageHeight = imageView.getIntrinsicContentSizeHeight();
			if (imageWidth > imageHeight) {
				focusHeight *= (imageWidth / imageHeight);
			} else {
				focusHeight *= (imageHeight / imageWidth);
			}
			preFocusFrameRect = new RectF(point.x - focusWidth / 2.0f, point.y - focusHeight / 2.0f,
			                 point.x + focusWidth / 2.0f, point.y + focusHeight / 2.0f);
		}
		imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Running);
		
		// Set auto-focus point.
		try {
			camera.setAutoFocusPoint(point);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			// Lock failed.
			try {
				camera.clearAutoFocusPoint();
				camera.unlockAutoFocus();
			} catch (OLYCameraKitException ee) {
			}
			enabledFocusLock = false;
			imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Failed, 1.0);
			return;
		}
		
		// Lock auto-focus.
		camera.lockAutoFocus(new OLYCamera.TakePictureCallback() {
			@Override
			public void onProgress(OLYCamera camera, TakingProgress progress, OLYCameraAutoFocusResult autoFocusResult) {
				if (progress == TakingProgress.EndFocusing) {
					if (autoFocusResult.getResult().equals("ok") && autoFocusResult.getRect() != null) {
						// Lock succeed.
						enabledFocusLock = true;
						focusedSoundPlayer.start();
						RectF postFocusFrameRect = autoFocusResult.getRect();
						imageView.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);
						
					} else if (autoFocusResult.getResult().equals("none")) {
						// Could not lock.
						try {
							camera.clearAutoFocusPoint();
							camera.unlockAutoFocus();
						} catch (OLYCameraKitException ee) {
						}
						enabledFocusLock = false;
						imageView.hideFocusFrame();
					} else {
						// Lock failed.
						try {
							camera.clearAutoFocusPoint();
							camera.unlockAutoFocus();
						} catch (OLYCameraKitException ee) {
						}
						enabledFocusLock = false;
						imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Failed, 1.0);
					}
				}
			}
			
			@Override
			public void onCompleted() {
				// No operation.
			}
			
			@Override
			public void onErrorOccurred(Exception e) {
				// Lock failed.
				try {
					camera.clearAutoFocusPoint();
					camera.unlockAutoFocus();
				} catch (OLYCameraKitException ee) {
				}
				enabledFocusLock = false;
				imageView.hideFocusFrame();

				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("AF failed", message);
					}
				});
			}
		});
	}
	
	private void unlockAutoFocus() {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}

		// Unlock auto-focus.
		try {
			camera.unlockAutoFocus();
			camera.clearAutoFocusPoint();
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
		}
		
		enabledFocusLock = false;
		imageView.hideFocusFrame();
	}
	
	// shutter control (still)
	
	private void takePicture() {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}
		
		HashMap<String, Object> options = new HashMap<String, Object>();
		camera.takePicture(options, new OLYCamera.TakePictureCallback() {
			@Override
			public void onProgress(OLYCamera camera, TakingProgress progress, OLYCameraAutoFocusResult autoFocusResult) {
				if (progress == TakingProgress.EndFocusing) {
					if (!enabledFocusLock) {
						if (autoFocusResult.getResult().equals("ok") && autoFocusResult.getRect() != null) {
							RectF postFocusFrameRect = autoFocusResult.getRect();
							imageView.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);
						} else if (autoFocusResult.getResult().equals("none")) {
							imageView.hideFocusFrame();
						} else {
							imageView.hideFocusFrame();
						}
					}
				} else if (progress == TakingProgress.BeginCapturing) {
					shutterSoundPlayer.start();
				}
			}
			
			@Override
			public void onCompleted() {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}
			}
			
			@Override
			public void onErrorOccurred(Exception e) {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}
				
				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Take failed", message);						
					}
				});
			}
		});
	}
	
	private void takePictureWithPoint(PointF point) {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}
		
		// Display a provisional focus frame at the touched point.
		final RectF preFocusFrameRect;
		{
			float focusWidth = 0.125f;  // 0.125 is rough estimate.
			float focusHeight = 0.125f;
			float imageWidth = imageView.getIntrinsicContentSizeWidth();
			float imageHeight = imageView.getIntrinsicContentSizeHeight();
			if (imageWidth > imageHeight) {
				focusHeight *= (imageWidth / imageHeight);
			} else {
				focusHeight *= (imageHeight / imageWidth);
			}
			preFocusFrameRect = new RectF(point.x - focusWidth / 2.0f, point.y - focusHeight / 2.0f,
			                 point.x + focusWidth / 2.0f, point.y + focusHeight / 2.0f);
		}
		imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Running);
		
		// Set auto-focus point.
		try {
			camera.setAutoFocusPoint(point);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			// Lock failed.
			try {
				camera.unlockAutoFocus();
			} catch (OLYCameraKitException ee) {
			}
			enabledFocusLock = false;
			imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Failed, 1.0);
			return;
		}
		
		HashMap<String, Object> options = new HashMap<String, Object>();
		camera.takePicture(options, new OLYCamera.TakePictureCallback() {
			@Override
			public void onProgress(OLYCamera camera, TakingProgress progress, OLYCameraAutoFocusResult autoFocusResult) {
				if (progress == TakingProgress.EndFocusing) {
					if (!enabledFocusLock) {
						if (autoFocusResult.getResult().equals("ok") && autoFocusResult.getRect() != null) {
							RectF postFocusFrameRect = autoFocusResult.getRect();
							imageView.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);
						} else if (autoFocusResult.getResult().equals("none")) {
							imageView.hideFocusFrame();
						} else {
							imageView.hideFocusFrame();
						}
					}
				} else if (progress == TakingProgress.BeginCapturing) {
					shutterSoundPlayer.start();
				}
			}
			
			@Override
			public void onCompleted() {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}
			}
			
			@Override
			public void onErrorOccurred(Exception e) {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}
				
				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Take failed", message);						
					}
				});
			}
		});
	}
	
	private void startTakingPicture() {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}
		
		camera.startTakingPicture(null, new OLYCamera.TakePictureCallback() {
			@Override
			public void onProgress(OLYCamera camera, TakingProgress progress, OLYCameraAutoFocusResult autoFocusResult) {
				if (progress == TakingProgress.EndFocusing) {
					if (!enabledFocusLock) {
						if (autoFocusResult.getResult().equals("ok") && autoFocusResult.getRect() != null) {
							RectF postFocusFrameRect = autoFocusResult.getRect();
							imageView.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);
						} else if (autoFocusResult.getResult().equals("none")) {
							imageView.hideFocusFrame();
						} else {
							imageView.hideFocusFrame();
						}
					}
				} else if (progress == TakingProgress.BeginCapturing) {
					shutterSoundPlayer.start();
				}
			}
			
			@Override
			public void onCompleted() {
				// No operation.
			}
			
			@Override
			public void onErrorOccurred(Exception e) {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}

				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Take failed", message);						
					}
				});
			}
		});
	}
	
	private void startTakingPictureWithPoint(PointF point) {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}
		
		// Display a provisional focus frame at the touched point.
		final RectF preFocusFrameRect;
		{
			float focusWidth = 0.125f;  // 0.125 is rough estimate.
			float focusHeight = 0.125f;
			float imageWidth = imageView.getIntrinsicContentSizeWidth();
			float imageHeight = imageView.getIntrinsicContentSizeHeight();
			if (imageWidth > imageHeight) {
				focusHeight *= (imageWidth / imageHeight);
			} else {
				focusHeight *= (imageHeight / imageWidth);
			}
			preFocusFrameRect = new RectF(point.x - focusWidth / 2.0f, point.y - focusHeight / 2.0f,
			                 point.x + focusWidth / 2.0f, point.y + focusHeight / 2.0f);
		}
		imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Running);
		
		// Set auto-focus point.
		try {
			camera.setAutoFocusPoint(point);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			// Lock failed.
			try {
				camera.unlockAutoFocus();
			} catch (OLYCameraKitException ee) {
			}
			enabledFocusLock = false;
			imageView.showFocusFrame(preFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Failed, 1.0);
			return;
		}
		
		camera.startTakingPicture(null, new OLYCamera.TakePictureCallback() {
			@Override
			public void onProgress(OLYCamera camera, TakingProgress progress, OLYCameraAutoFocusResult autoFocusResult) {
				if (progress == TakingProgress.EndFocusing) {
					if (!enabledFocusLock) {
						if (autoFocusResult.getResult().equals("ok") && autoFocusResult.getRect() != null) {
							RectF postFocusFrameRect = autoFocusResult.getRect();
							imageView.showFocusFrame(postFocusFrameRect, CameraLiveImageView.FocusFrameStatus.Focused);
						} else if (autoFocusResult.getResult().equals("none")) {
							imageView.hideFocusFrame();
						} else {
							imageView.hideFocusFrame();
						}
					}
				} else if (progress == TakingProgress.BeginCapturing) {
					shutterSoundPlayer.start();
				}
			}
			
			@Override
			public void onCompleted() {
				// No operation.
			}
			
			@Override
			public void onErrorOccurred(Exception e) {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}

				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Take failed", message);						
					}
				});
			}
		});
	}
	
	private void stopTakingPicture() {
		if (!camera.isTakingPicture()) {
			return;
		}
		
		camera.stopTakingPicture(new OLYCamera.TakePictureCallback() {
			@Override
			public void onProgress(OLYCamera camera, TakingProgress progress, OLYCameraAutoFocusResult autoFocusResult) {
				// No operation.
			}
			
			@Override
			public void onCompleted() {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}
			}
			
			@Override
			public void onErrorOccurred(Exception e) {
				if (!enabledFocusLock) {
					try {
						camera.clearAutoFocusPoint();
					} catch (OLYCameraKitException ee) {
					}
					imageView.hideFocusFrame();
				}

				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Take failed", message);						
					}
				});
			}
		});
	}
	
	// shutter control (movie)
	
	private void startRecordingVideo() {
		if (camera.isTakingPicture() || camera.isRecordingVideo()) {
			return;
		}
		
		HashMap<String, Object> options = new HashMap<String, Object>();
		camera.startRecordingVideo(options, new OLYCamera.CompletedCallback() {			
			@Override
			public void onCompleted() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						shutterImageView.setSelected(true);
					}
				});
			}
			
			@Override
			public void onErrorOccurred(OLYCameraKitException e) {
				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Record failed", message);						
					}
				}); 
			}
		});
	}
	
	private void stopRecordingVideo() {
		if (!camera.isRecordingVideo()) {
			return;
		}
		
		camera.stopRecordingVideo(new OLYCamera.CompletedCallback() {			
			@Override
			public void onCompleted() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						shutterImageView.setSelected(false);
					}
				});
			}
			
			@Override
			public void onErrorOccurred(OLYCameraKitException e) {
				shutterImageView.setSelected(false);
				final String message = e.getMessage();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						presentMessage("Record failed", message);						
					}
				}); 
			}
		});
	}
	

	// -------------------------------------------------------------------------
	// Camera property control
	// -------------------------------------------------------------------------
	
	@Override
	public void onUpdateStatus(OLYCamera camera, final String name) {		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (name.equals("ActualApertureValue")) {
					updateApertureValueTextView();
				} else if (name.equals("ActualShutterSpeed")) {
					updateShutterSpeedTextView();
				} else if (name.equals("ActualExposureCompensation")) {
					updateExposureCompensationTextView();
				} else if (name.equals("ActualIsoSensitivity")) {
					updateIsoSensitivityTextView();
				} else if (name.equals("RemainingRecordableImages") || name.equals("MediaBusy")) {
					updateRemainingRecordableImagesTextView();
				}
			}
		});
	}

	@Override
	public void onUpdateCameraProperty(OLYCamera camera, final String name) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (name.equals(CAMERA_PROPERTY_TAKE_MODE)) {
					updateTakemodeTextView();
				} else if (name.equals(CAMERA_PROPERTY_DRIVE_MODE)) {
					updateDrivemodeImageView();
				} else if (name.equals(CAMERA_PROPERTY_WHITE_BALANCE)) {
					updateWhiteBalanceImageView();
				} else if (name.equals(CAMERA_PROPERTY_BATTERY_LEVEL)) {
					updateBatteryLevelImageView();
				}
			}
		});
	}
	
	private void drivemodeImageViewDidTap() {
		final View view = drivemodeImageView;
		final String propertyName = CAMERA_PROPERTY_DRIVE_MODE;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateDrivemodeImageView();
					}
				});
			}
		});
	}
	
	private void updateDrivemodeImageView() {
		drivemodeImageView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_DRIVE_MODE));
		
		String drivemode;
		try {
			drivemode = camera.getCameraPropertyValue(CAMERA_PROPERTY_DRIVE_MODE);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		
		if (drivemode == null) {
			return;
		}
		
		if (drivemodeIconList.containsKey(drivemode)) {
			int resId = drivemodeIconList.get(drivemode);
			drivemodeImageView.setImageResource(resId);
		} else {
			drivemodeImageView.setImageDrawable(null);
		}
	}
	
	private void takemodeTextViewDidTap() {
		final View view = takemodeTextView;
		final String propertyName = CAMERA_PROPERTY_TAKE_MODE;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateTakemodeTextView();
						
						try {
							camera.clearAutoFocusPoint();
							camera.unlockAutoFocus();
						} catch (OLYCameraKitException e) {
							e.printStackTrace();
						}
						enabledFocusLock = false;
						imageView.hideFocusFrame();
					}
				});
			}
		});
	}
	
	private void updateTakemodeTextView() {
		takemodeTextView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_TAKE_MODE));
			
		String value = null;
		try {
			value = camera.getCameraPropertyValue(CAMERA_PROPERTY_TAKE_MODE);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
		}
		String title;
		if (value == null) {
			title = "Take";
		} else {
			title = camera.getCameraPropertyValueTitle(value);
		}
		
		takemodeTextView.setText(title);
		
		// Changing take mode may have an influence for drive mode and white balance.
		updateDrivemodeImageView();
		updateShutterSpeedTextView();
		updateApertureValueTextView();
		updateExposureCompensationTextView();
		updateIsoSensitivityTextView();
		updateWhiteBalanceImageView();
	}
	
	private void apertureValueTextViewDidTap() {
		final View view = apertureValueTextView;
		final String propertyName = CAMERA_PROPERTY_APERTURE_VALUE;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void updateApertureValueTextView() {
		apertureValueTextView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_APERTURE_VALUE));
		
		String title = camera.getCameraPropertyValueTitle(camera.getActualApertureValue());
		if (title == null) {
			title = "";
		} else {
			title = String.format("F%s", title);
		}
		apertureValueTextView.setText(title);
	}
	
	private void shutterSpeedTextViewDidTap() {
		final View view = shutterSpeedTextView;
		final String propertyName = CAMERA_PROPERTY_SHUTTER_SPEED;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void updateShutterSpeedTextView() {
		shutterSpeedTextView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_SHUTTER_SPEED));

		String title = camera.getCameraPropertyValueTitle(camera.getActualShutterSpeed());
		if (title == null) {
			title = "";
		}
		shutterSpeedTextView.setText(title);
	}
	
	private void exposureCompensationTextViewDidTap() {
		final View view = exposureCompensationTextView;
		final String propertyName = CAMERA_PROPERTY_EXPOSURE_COMPENSATION;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void updateExposureCompensationTextView() {
		exposureCompensationTextView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_EXPOSURE_COMPENSATION));
		
		String title = camera.getCameraPropertyValueTitle(camera.getActualExposureCompensation());
		if (title == null) {
			title = "";
		}
		exposureCompensationTextView.setText(title);
	}
	
	private void isoSensitivityTextViewDidTap() {
		final View view = isoSensitivityTextView;
		final String propertyName = CAMERA_PROPERTY_ISO_SENSITIVITY;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void updateIsoSensitivityTextView() {
		isoSensitivityTextView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_ISO_SENSITIVITY));
		
		String value = null;
		try {
			value = camera.getCameraPropertyValue(CAMERA_PROPERTY_ISO_SENSITIVITY);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
		}

		String title = camera.getCameraPropertyValueTitle(camera.getActualIsoSensitivity());
		if (title == null) {
			title = "";
		}
		String titlePrefix = "ISO";
		if ("<ISO/Auto>".equals(value)) {
			titlePrefix = "ISO-A";
		}
		isoSensitivityTextView.setText(String.format("%s\n%s", titlePrefix, title));
	}
	
	private void whiteBalanceImageViewDidTap() {
		final View view = whiteBalanceImageView;
		final String propertyName = CAMERA_PROPERTY_WHITE_BALANCE;
		
		final List<String> valueList;
		try {
			valueList = camera.getCameraPropertyValueList(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (valueList == null || valueList.size() == 0) return;
		
		String value;
		try {
			value = camera.getCameraPropertyValue(propertyName);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
			return;
		}
		if (value == null) return;
		view.setSelected(true);
		
		
		presentPropertyValueList(valueList, value, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				view.setSelected(false);
				
				try {
					camera.setCameraPropertyValue(propertyName, valueList.get(which));
				} catch (Exception e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateWhiteBalanceImageView();
					}
				});
			}
		});
	}
	
	private void updateWhiteBalanceImageView() {
		whiteBalanceImageView.setEnabled(camera.canSetCameraProperty(CAMERA_PROPERTY_WHITE_BALANCE));
		
		String value = null;
		try {
			value = camera.getCameraPropertyValue(CAMERA_PROPERTY_WHITE_BALANCE);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
		}
		
		if (whiteBalanceIconList.containsKey(value)) {
			int resId = whiteBalanceIconList.get(value);
			whiteBalanceImageView.setImageResource(resId);
		} else {
			whiteBalanceImageView.setImageDrawable(null);
		}
	}
	
	private void updateBatteryLevelImageView() {
		String value = null;
		try {
			value = camera.getCameraPropertyValue(CAMERA_PROPERTY_BATTERY_LEVEL);
		} catch (OLYCameraKitException e) {
			e.printStackTrace();
		}
		
		if (batteryIconList.containsKey(value)) {
			int resId = batteryIconList.get(value);
			if (resId != 0) {
				batteryLevelImageView.setImageResource(resId);
			} else {
				batteryLevelImageView.setImageDrawable(null);
			}
		} else {
			batteryLevelImageView.setImageDrawable(null);
		}
	}
	
	private void updateRemainingRecordableImagesTextView() {
		final String text;
		if (camera.isConnected() || camera.getRunMode() == OLYCamera.RunMode.Recording) {
			if (camera.isMediaBusy()) {
				text = "BUSY";
			} else {
				text = String.format(Locale.getDefault(), "%d", camera.getRemainingImageCapacity());
			}
		} else {
			text = "???";
		}
		remainingRecordableImagesTextView.setText(text);
	}
	

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------
	
	private void presentPropertyValueList(List<String> list, String initialValue, DialogInterface.OnClickListener listener) {
		Activity activity = getActivity();
		if (activity == null) return;
		
		String[] items = new String[list.size()];
		for (int ii = 0; ii < items.length; ++ii) {			
			items[ii] = camera.getCameraPropertyValueTitle(list.get(ii));
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setSingleChoiceItems(items, list.indexOf(initialValue), listener);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				drivemodeImageView.setSelected(false);
				takemodeTextView.setSelected(false);
				shutterSpeedTextView.setSelected(false);
				apertureValueTextView.setSelected(false);
				exposureCompensationTextView.setSelected(false);
				isoSensitivityTextView.setSelected(false);
				whiteBalanceImageView.setSelected(false);
			}
		});
		builder.show();
	}
	
	private void presentMessage(String title, String message) {
		Context context = getActivity();
		if (context == null) return;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(message);
		builder.show();
	}
	
	private void runOnUiThread(Runnable action) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		
		activity.runOnUiThread(action);
	}
	
	// -------------------------------------------------------------------------
	// Camera event handling
	// -------------------------------------------------------------------------

	@Override
	public void onUpdateLiveView(OLYCamera camera, byte[] data, Map<String, Object> metadata) {
		imageView.setImageData(data, metadata);
	}
	
	@Override
	public void onChangeAutoFocusResult(OLYCamera camera, OLYCameraAutoFocusResult result) {
	}

	@Override
	public void onStartRecordingVideo(OLYCamera camera) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				shutterImageView.setSelected(true);
			}
		});
	}

	@Override
	public void onStopRecordingVideo(OLYCamera camera) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				shutterImageView.setSelected(false);
			}
		});
	}
	
	@Override
	public void onReadyToReceiveCapturedImage(OLYCamera camera) {
	}

	@Override
	public void onReceiveCapturedImagePreview(OLYCamera camera, byte[] data, Map<String, Object> metadata) {
		if (camera.getActionType() == ActionType.Single) {
	        RecviewFragment fragment = new RecviewFragment();
	        fragment.setCamera(camera);
	        fragment.setImageData(data, metadata);
	        FragmentTransaction transaction = getFragmentManager().beginTransaction();
	        transaction.replace(getId(), fragment);
	       	transaction.addToBackStack(null);
	       	transaction.commit();
		}
	}
	
	@Override
	public void onFailToReceiveCapturedImagePreview(OLYCamera camera, Exception e) {
	}

	@Override
	public void onReadyToReceiveCapturedImagePreview(OLYCamera camera) {
	}

	@Override
	public void onReceiveCapturedImage(OLYCamera camera, byte[] data, Map<String, Object> metadata) {
	}
	
	@Override
	public void onFailToReceiveCapturedImage(OLYCamera camera, Exception e) {
	}

	@Override
	public void onStopDrivingZoomLens(OLYCamera camera) {
	}

}
