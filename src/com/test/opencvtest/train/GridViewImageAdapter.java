package com.test.opencvtest.train;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.test.opencvtest.ImageUtil;
import com.test.opencvtest.R;

public class GridViewImageAdapter extends BaseAdapter {

	private static final String TAG = GridViewImageAdapter.class
			.getSimpleName();

	private Activity activity;
	private List<String> filePaths;
	private int imageWidth;
	private boolean isDeleteEnabled;

	public GridViewImageAdapter(Activity activity, List<String> filePaths,
			int imageWidth) {
		this.activity = activity;
		this.filePaths = filePaths;
		this.imageWidth = imageWidth;
	}

	@Override
	public int getCount() {
		return this.filePaths.size();
	}

	@Override
	public Object getItem(int position) {
		return this.filePaths.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Log.i(TAG, "create new view for " + position);
		LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View cell = inflater.inflate(R.layout.image_grid_cell, parent, false);
		// set parent's layout parameters TODO document this
		cell.setLayoutParams(new GridView.LayoutParams(imageWidth, imageWidth));

		ImageView imageView = (ImageView) cell.findViewById(R.id.imageView);

		// get screen dimensions
		Bitmap image = ImageUtil.decodeFile(filePaths.get(position),
				imageWidth, imageWidth);
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		// set parent's (RelativeLayout) layout parameters
		imageView.setLayoutParams(new RelativeLayout.LayoutParams(imageWidth,
				imageWidth));

		imageView.setImageBitmap(image);

		Button deleteButton = (Button) cell.findViewById(R.id.deleteButton);
		deleteButton.setVisibility(isDeleteEnabled ? View.VISIBLE
				: View.INVISIBLE);

		// image view click listener
		if (isDeleteEnabled) {
			deleteButton.setOnClickListener(new OnDeleteClickListener(position));
			Log.i(TAG, "set onclick for " + position);
		}

		return cell;
	}

	class OnDeleteClickListener implements OnClickListener {

		int position;

		// constructor
		public OnDeleteClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			Log.i(TAG, "clicked on " + position);
			String name = filePaths.remove(position);
			GridViewImageAdapter.this.notifyDataSetChanged();

			File file = new File(name);
			file.delete();
		}

	}

	public void setDeleteEnabled(boolean isDeleteEnabled) {
		this.isDeleteEnabled = isDeleteEnabled;
	}

	public void add(String path) {
		filePaths.add(path);
	}

}
