package com.test.opencvtest.train;

import java.io.File;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.test.opencvtest.ImageUtil;
import com.test.opencvtest.MediaUtility;
import com.test.opencvtest.OpenCvUtility;
import com.test.opencvtest.R;

public class AddActivity extends Activity {

	private static final int CAMERA_REQUEST = 1888;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add);

		// do we need to do this in background?
		OpenCvUtility.initializeFaceDetector(OpenCvUtility
				.getFaceDetectorDataPath(this));
		OpenCvUtility.setupInternalTrainingImages(this);
		OpenCvUtility.initializeInternalTraining(OpenCvUtility
				.getRecognizerInternalTrainingDataPath(this));
	}

	public void camera(View view) {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (cameraIntent.resolveActivity(getPackageManager()) != null) {
			startActivityForResult(cameraIntent, CAMERA_REQUEST);
		}
	}

	public void done(View view) {
		EditText textView = (EditText) findViewById(R.id.name);
		ImageView imageView = (ImageView) findViewById(R.id.imgDisplay);
		if (imageView.getDrawable() == null || textView.getText().length() <= 0) {
			Toast.makeText(this, "missing image or name", Toast.LENGTH_SHORT)
					.show();
		} else {

			// store image in file
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

				String name = textView.getText().toString();
				TrainingData trainingData = new TrainingData(
						OpenCvUtility.getRecognizerTrainingDataPath());
				String file = trainingData.createTrainingData(name);
				try {
					Mat mat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
							CvType.CV_8UC1);
					org.opencv.android.Utils.bitmapToMat(bitmap, mat);

					// save original training data in
					File output = MediaUtility.getOutputMediaFile(
							MediaUtility.MEDIA_TYPE_IMAGE, OpenCvUtility.APP);
					ImageUtil.writeImage(bitmap, output.getAbsolutePath());

					int faces = OpenCvUtility.addTrainingData(
							mat.getNativeObjAddr(), file);
					if (faces > 0) {
						Intent intent = new Intent();
						intent.putExtra("path", file);
						setResult(RESULT_OK, intent);
					} else {
						setResult(RESULT_CANCELED);
					}
				} catch (Exception e) {
					Toast.makeText(this,
							"cannot save " + file + ". " + e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}
			}

			finish();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			Bitmap imageBitmap = (Bitmap) extras.get("data");

			ImageView imageView = (ImageView) findViewById(R.id.imgDisplay);
			imageView.setImageBitmap(imageBitmap);
		}
	}

}
