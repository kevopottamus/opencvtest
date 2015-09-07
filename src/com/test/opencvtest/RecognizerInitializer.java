package com.test.opencvtest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RecognizerInitializer extends AsyncTask<Void, Integer, Void> {
	private static final String TAG = RecognizerInitializer.class
			.getSimpleName();
	private Context context;
	private int recognizerType;

	public RecognizerInitializer(Context context, int type) {
		this.context = context;
		this.recognizerType = type;
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		Log.i(TAG, "start initialization");
		long start = System.currentTimeMillis();
		OpenCvUtility.initializeFaceDetector(OpenCvUtility
				.getFaceDetectorDataPath(context));
		OpenCvUtility.setupInternalTrainingImages(context);
		OpenCvUtility.initializeRecognizer(
				OpenCvUtility.getRecognizerInternalTrainingDataPath(context),
				OpenCvUtility.getRecognizerTrainingDataPath(),
				recognizerType);
		long end = System.currentTimeMillis();
		Log.i(TAG, "complete initialization in " + (end - start) + "ms");
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