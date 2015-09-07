package com.test.opencvtest;

import java.io.File;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Test video encoding into file - will post to remote server. Might also test
 * video playback from file
 */
public class OpenCvVideoSaverActivity extends Activity implements
		CvCameraViewListener2 {

	public static final String TAG = OpenCvVideoSaverActivity.class.getSimpleName();

	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mFrame;
	private int rotation;

	private long recordStart;

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
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		File file = MediaUtility.getOutputMediaFile(MediaUtility.MEDIA_TYPE_VIDEO, OpenCvUtility.CAPTURE);
		OpenCvUtility.nativeInitializeVideoWriter(file.getAbsolutePath(),
				size.x, size.y);

		recordStart = System.currentTimeMillis();
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
		long before = System.currentTimeMillis();

		if (before - recordStart > 5000) {
			Log.i(TAG, "video recording finished after 5s");
			OpenCvUtility.releaseVideo();

		} else {
			OpenCvUtility.writeVideo(mFrame.getNativeObjAddr());
			long after = System.currentTimeMillis();
			Log.i(TAG, mFrame.cols() + " x " + mFrame.rows() + ", " + rotation
					* 90 + " time=" + (after - before) + "ms");
		}

		return mFrame;
	}

}
