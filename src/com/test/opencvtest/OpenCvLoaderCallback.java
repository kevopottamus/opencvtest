package com.test.opencvtest;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import android.content.Context;

public class OpenCvLoaderCallback extends BaseLoaderCallback {

	public OpenCvLoaderCallback(Context appContext) {
		super(appContext);
	}

	// called on UI thread
	@Override
	public void onManagerConnected(int status) {
		switch (status) {
		case LoaderCallbackInterface.SUCCESS:
			OpenCvUtility.loadNativeLib();
			break;

		default:
			super.onManagerConnected(status);
			break;
		}
	}

}
