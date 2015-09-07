package com.test.opencvtest;

import org.opencv.android.JavaCameraView;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

public class CustomCameraView extends JavaCameraView {

	private static final String TAG = CustomCameraView.class.getSimpleName();

	public CustomCameraView(Context context, int cameraId) {
		super(context, cameraId);
	}

	public CustomCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private int getNextCamera() {
		int next = mCameraIndex;
		if (mCameraIndex == -1) {
			next = 0;
		}

		if (next == Camera.getNumberOfCameras() - 1) {
			next = 0;
		} else {
			next++;
		}
		return next;
	}

	public void swapCamera() {
		disableView();
		mCameraIndex = getNextCamera();
		Log.i(TAG, "swapped index = " + mCameraIndex);
		enableView();
	}

}
