package com.test.opencvtest.train;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.test.opencvtest.OpenCvUtility;
import com.test.opencvtest.R;

public class FullScreenViewActivity extends Activity {
	public static final String TAG = FullScreenViewActivity.class
			.getSimpleName();

	private FullScreenImageAdapter adapter;
	private ViewPager viewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_full);

		viewPager = (ViewPager) findViewById(R.id.pager);

		Intent i = getIntent();
		int position = i.getIntExtra("position", 0);

		adapter = new FullScreenImageAdapter(FullScreenViewActivity.this,
				OpenCvUtility.getRecognizerTrainingDataPaths());

		viewPager.setAdapter(adapter);

		// displaying selected image first
		viewPager.setCurrentItem(position);
		Log.i(TAG, "position=" + position);
	}

}
