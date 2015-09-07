package com.test.opencvtest.train;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.test.opencvtest.R;

public class FullScreenImageAdapter extends PagerAdapter {
	public static final String TAG = "FullScreenImageAdapter";

	private Activity activity;
	private List<String> imagePaths;
	private LayoutInflater inflater;

	public FullScreenImageAdapter(Activity activity, List<String> imagePaths) {
		this.activity = activity;
		this.imagePaths = imagePaths;
	}

	@Override
	public int getItemPosition(Object object) {
		View view = (View) object;

		int result = POSITION_NONE;
		int index = imagePaths.indexOf(view.getTag());
		if (index >= 0) {
			result = index;
		}

		return result;
	}

	@Override
	public int getCount() {
		return this.imagePaths.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((RelativeLayout) object);
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		ImageView imgDisplay;
		Button closeButton;
		Button deleteButton;

		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewLayout = inflater.inflate(R.layout.layout_full, container,
				false);

		imgDisplay = (ImageView) viewLayout.findViewById(R.id.imgDisplay);
		closeButton = (Button) viewLayout.findViewById(R.id.buttonClose);
		deleteButton = (Button) viewLayout.findViewById(R.id.buttonDelete);

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		String imagePath = imagePaths.get(position);
		Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
		imgDisplay.setImageBitmap(bitmap);

		// close button click event
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.finish();
			}
		});

		deleteButton.setOnClickListener(new DeleteListener(
				(ViewPager) container, this, position));

		((ViewPager) container).addView(viewLayout);

		viewLayout.setTag(imagePath);

		Log.i(TAG, "instatiateItem at " + position + ": " + imagePath);

		return viewLayout;
	}

	private final class DeleteListener implements OnClickListener {
		private ViewPager pager;
		private FullScreenImageAdapter adapter;
		private int index;

		public DeleteListener(ViewPager pager, FullScreenImageAdapter adapter,
				int index) {
			this.pager = pager;
			this.adapter = adapter;
			this.index = index;
		}

		@Override
		public void onClick(View v) {
			int count = adapter.getCount();
			if (count > 1) {
				if (index == 0) {
					pager.setCurrentItem(1);
				} else {
					pager.setCurrentItem(index - 1);
				}
			} else {
				// what do we do with last one
			}

			adapter.imagePaths.remove(index);
			adapter.notifyDataSetChanged();

			// String path = imagePaths.get(index);
			// Log.i(TAG, path);
			// File file = new File(path);
			// boolean deleted = file.delete();
			// if (deleted)
			// imagePaths.remove(index);
		}
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		Log.i(TAG, "destroyItem at " + position);
		((ViewPager) container).removeView((RelativeLayout) object);
		String path = imagePaths.get(position);
		Log.i(TAG, "destroyItem path " + path);
	}

}
