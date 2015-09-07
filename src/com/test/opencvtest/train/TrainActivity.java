package com.test.opencvtest.train;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridView;

import com.test.opencvtest.OpenCvUtility;
import com.test.opencvtest.R;

public class TrainActivity extends Activity {

	private static final String TAG = TrainActivity.class.getSimpleName();

	private List<String> imagePaths;
	private GridViewImageAdapter adapter;
	private GridView gridView;
	private int columnWidth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.train);

		gridView = (GridView) findViewById(R.id.grid_view);

		// Initilizing Grid View
		initilizeGridLayout();

		// loading all image paths from SD card
		imagePaths = OpenCvUtility.getRecognizerTrainingDataPaths();

		// Gridview adapter
		adapter = new GridViewImageAdapter(TrainActivity.this, imagePaths,
				columnWidth);

		// setting grid view adapter
		gridView.setAdapter(adapter);
	}

	private void initilizeGridLayout() {
		Resources r = getResources();
		float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				AppConstant.GRID_PADDING, r.getDisplayMetrics());

		columnWidth = (int) ((Utils.getScreenWidth(this) - ((AppConstant.NUM_OF_COLUMNS + 1) * padding)) / AppConstant.NUM_OF_COLUMNS);

		gridView.setNumColumns(AppConstant.NUM_OF_COLUMNS);
		gridView.setColumnWidth(columnWidth);
		gridView.setStretchMode(GridView.NO_STRETCH);
		gridView.setPadding((int) padding, (int) padding, (int) padding,
				(int) padding);
		gridView.setHorizontalSpacing((int) padding);
		gridView.setVerticalSpacing((int) padding);
	}

	public void add(View view) {
		adapter.setDeleteEnabled(false);

		Log.i(TAG, "starting AddActivity");
		Intent intent = new Intent(this, AddActivity.class);
		startActivityForResult(intent, 0);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "activity result = " + resultCode);
		if (resultCode == RESULT_OK) {
			String path = data.getStringExtra("path");
			adapter.add(path);
			adapter.notifyDataSetChanged();

			// need to re-initialize recognizer
			OpenCvUtility.setFaceRecognizerInitialized(false);
		}
	}

	public void delete(View view) {
		Log.i(TAG, "delete button clicked");

		adapter.setDeleteEnabled(true);
		adapter.notifyDataSetChanged();

		// need to re-initialize recognizer
		OpenCvUtility.setFaceRecognizerInitialized(false);
	}

	public void back(View view) {
		finish();
	}

}
