package com.test.opencvtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.demo.shared.httputil.ErrorCallback;
import com.demo.shared.httputil.FileUploader;
import com.demo.shared.httputil.JsonHttpResult;
import com.demo.shared.httputil.RequestCompleteCallback;
import com.test.mediacodec.VideoEncoder;

public class MediaCodecVideoSaverActivity extends Activity implements
		CvCameraViewListener2 {

	public static final String TAG = MediaCodecVideoSaverActivity.class
			.getSimpleName();

	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mFrame;
	private int rotation;
	private int width = 0;
	private int height = 0;

	private long recordStart = 0;
	private OutputStream outputStream;
	private File outputFile;
	private VideoEncoder encoder;
	private String downloadUrl;

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

		encoder = new VideoEncoder();
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
		width = mFrame.cols();
		height = mFrame.rows();
		long afterConversion = before;
		long afterToBytes = before;
		if (recordStart > 0) {
			if (before - recordStart > 20000) {
				Log.i(TAG, "video recording finished after 20s");
				stopRecording();
			} else {
				try {
					// 7ms
					byte[] rgbaBytes = toBytes(mFrame);
					afterToBytes = System.currentTimeMillis();

					byte[] yuvBytes = new byte[(int) (rgbaBytes.length / 4 * 1.5)];
					// 2.2s
					encoder.rgbaToYuv(yuvBytes, rgbaBytes, encoder.getWidth(),
							encoder.getHeight());
					afterConversion = System.currentTimeMillis();

					// 13ms
					encoder.encode(yuvBytes, outputStream);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}

		long after = System.currentTimeMillis();
		Log.i(TAG, mFrame.cols() + " x " + mFrame.rows() + ", " + rotation * 90
				+ " toBytes time=" + (afterToBytes - before) + "ms"
				+ " conv time=" + (afterConversion - afterToBytes) + "ms"
				+ " encode time=" + (after - afterConversion) + "ms");

		return mFrame;
	}

	// 7ms
	private byte[] toBytes(Mat mat) {
		int bufferSize = mat.channels() * mat.cols() * mat.rows();
		byte[] bytes = new byte[bufferSize];
		mat.get(0, 0, bytes); // get all the pixels
		return bytes;
	}

	private void startRecording() {
		if (recordStart == 0) {
			try {
				outputFile = MediaUtility.getOutputMediaFile(
						MediaUtility.MEDIA_TYPE_VIDEO, OpenCvUtility.CAPTURE);
				outputStream = new FileOutputStream(outputFile);
				encoder.prepareEncoder(width, height);
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
			recordStart = System.currentTimeMillis();
		}
	}

	private void stopRecording() {
		if (recordStart != 0) {
			encoder.releaseEncoder();
			try {
				outputStream.close();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
			recordStart = 0;
		}
	}

	public void recordVideo(View view) {
		startRecording();
	}

	private ErrorCallback errorCallback = new ErrorCallback() {
		@Override
		public void onError(Throwable error) {
			Toast.makeText(MediaCodecVideoSaverActivity.this,
					error.getMessage(),
					Toast.LENGTH_SHORT).show();
		}
	};

	private RequestCompleteCallback<JsonHttpResult> uploadCompleteCallback = new RequestCompleteCallback<JsonHttpResult>() {
		@Override
		public void onRequestComplete(JsonHttpResult result) {
			if (result.code >= 200 && result.code <= 299
					&& result.jsonObject != null) {
				try {
					downloadUrl = result.jsonObject.getString("url");
					Toast.makeText(MediaCodecVideoSaverActivity.this,
							"Upload completed", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(MediaCodecVideoSaverActivity.this,
							e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	public void uploadVideo(View view) {
		FileUploader fileUploader = new FileUploader(uploadCompleteCallback,
				errorCallback, "http://www.find-code.com:81/upload");
		fileUploader.execute(outputFile);
	}

	public void playbackVideo(View view) {
		Intent intent = new Intent(this, MediaPlayerActivity.class);
		intent.putExtra("url", downloadUrl);
		startActivity(intent);
	}

}
