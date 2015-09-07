package com.test.opencvtest;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class VideoDetectActivity extends Activity implements CvCameraViewListener2 {
	public static final String TAG = VideoDetectActivity.class.getSimpleName();

	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mFrame;
	private int rotation;

	private static final class DetectorInitializer extends
			AsyncTask<Void, Integer, Void> {

		private Context context;

		public DetectorInitializer(Context context) {
			this.context = context;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			OpenCvUtility.initializePeopleDetector();
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Toast.makeText(context, "initializing", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Toast.makeText(context, "initialized", Toast.LENGTH_SHORT).show();
		}

	}

	private void initializeDetector() {
		if (!OpenCvUtility.isPeopleDetectorInitializer()) {
			new DetectorInitializer(this).execute((Void[]) null);
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

		initializeDetector();

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
		// must use gray scale for HOG detector to work
		mFrame = inputFrame.gray();

		long start = System.currentTimeMillis();

		if (OpenCvUtility.isPeopleDetectorInitializer()) {
			OpenCvUtility.findPeople(mFrame.getNativeObjAddr());
		}

		long finish = System.currentTimeMillis();
		Log.i(TAG, mFrame.cols() + " x " + mFrame.rows() + ", " + rotation * 90
				+ " time=" + (finish - start) + "ms");
		return mFrame;
	}

}
