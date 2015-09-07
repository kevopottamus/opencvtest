package com.test.opencvtest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import com.demo.shared.logcat.LogcatService;
import com.test.opencvtest.train.TrainActivity;

public class MainActivity extends Activity {

	public static final String TAG = "MainActivity";
	private BaseLoaderCallback mLoaderCallback = new OpenCvLoaderCallback(this);
	private RadioGroup algorithmGroup;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = new Intent(this, LogcatService.class);
		startService(intent);

		algorithmGroup = (RadioGroup) findViewById(R.id.algorithmGroup);
		algorithmGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup arg0, int arg1) {
						OpenCvUtility.setFaceRecognizerInitialized(false);
					}
				});

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	private int getSelectedAlgorithmType() {
		int id = algorithmGroup.getCheckedRadioButtonId();
		int type = 3;
		if (id == R.id.eigenAlgorithm) {
			type = 1;
		} else if (id == R.id.fisherAlgorithm) {
			type = 2;
		}
		return type;
	}

	public void recognizeInImage(View view) {
		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra("algorithm", getSelectedAlgorithmType());

		startActivity(intent);
	}

	public void recognizeInVideo(View view) {
		Intent intent = new Intent(this, VideoActivity.class);
		startActivity(intent);
	}

	public void detectInVideo(View view) {
		Intent intent = new Intent(this, VideoDetectActivity.class);
		intent.putExtra("algorithm", getSelectedAlgorithmType());

		startActivity(intent);
	}

	public void train(View view) {
		// don't assume training data changed unless add/remove training images

		Intent intent = new Intent(this, TrainActivity.class);
		startActivity(intent);
	}

	public void wipe(View view) {
		OpenCvUtility.cleanupInternalTrainingImages(this);
		OpenCvUtility.cleanupTrainingImages();
	}

	public void saveVideo(View view) {
		Intent intent = new Intent(this, MediaCodecVideoSaverActivity.class);
		startActivity(intent);
	}

	public void playVideo(View view) {
		Intent intent = new Intent(this, MediaPlayerActivity.class);
		startActivity(intent);
	}

	public void test(View view) {
		OpenCvUtility.initializeFaceDetector(OpenCvUtility
				.getFaceDetectorDataPath(this));
	}

}
