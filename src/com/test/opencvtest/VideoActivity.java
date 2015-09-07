package com.test.opencvtest;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class VideoActivity extends Activity implements CvCameraViewListener2 {
	public static final String TAG = VideoActivity.class.getSimpleName();

	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mFrame;
	private int rotation;
	private int algorithm;

	private void initializeRecognizer(int type) {
		if (!OpenCvUtility.isFaceRecognizerInitialized()) {
			new RecognizerInitializer(this, type).execute((Void[]) null);
		}
	}

	public void swapCamera(View view) {
		if (mOpenCvCameraView instanceof CustomCameraView) {
			CustomCameraView customView = (CustomCameraView) mOpenCvCameraView;
			customView.swapCamera();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video);

		Intent intent = getIntent();
		algorithm = intent.getIntExtra("algorithm", 2);

		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		rotation = display.getRotation();

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.enableView();

		// initialize on resume, not needed in onCreate
		initializeRecognizer(algorithm);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		Log.i(TAG, "camera view started");
	}

	@Override
	public void onCameraViewStopped() {
		Log.i(TAG, "camera view stopped");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
		}

		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		rotation = display.getRotation();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mFrame = inputFrame.rgba();
		long start = System.currentTimeMillis();
		// mFrame = inputFrame.gray();
		// return mFrame;

		// Log.i(TAG, mFrame.cols() + " x " + mFrame.rows() + ", " + rotation *
		// 90);
		// if (rotation == 0) { // t, then flip around x to rotate -90
		// Mat mTranspose = mFrame.t();
		// Core.flip(mFrame.t(), mTranspose, 0);
		// Imgproc.resize(mTranspose, mTranspose, mFrame.size());
		// return mTranspose;
		// } else if (rotation == 1) {
		// // everything is ok
		// } else if (rotation == 2) { // t, then flip around y to rotate 90
		// Mat mTranspose = mFrame.t();
		// Core.flip(mFrame.t(), mTranspose, 1);
		// Imgproc.resize(mTranspose, mTranspose, mFrame.size());
		// return mTranspose;
		// } else { // flip around x
		// Core.flip(mFrame, mFrame, 0);
		// return mFrame;
		// }

		if (OpenCvUtility.isFaceRecognizerInitialized()) {
			OpenCvUtility.recognizePeople(mFrame.getNativeObjAddr());
		} else {
			Log.w(TAG, "face recognizer not initialized");
		}

		long finish = System.currentTimeMillis();
		Log.i(TAG, mFrame.cols() + " x " + mFrame.rows() + ", " + rotation * 90
				+ " time=" + (finish - start) + "ms");
		return mFrame;
	}

}
