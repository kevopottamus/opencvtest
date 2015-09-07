package com.test.opencvtest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class ImageUtil {

	public static void writeImage(Bitmap image, String name)
			throws Exception {

		FileOutputStream fos = new FileOutputStream(name);
		image.compress(CompressFormat.PNG, 90, fos);
		fos.close();

	}

	public static Bitmap decodeFile(String filePath, int width, int height) {
		try {
			File f = new File(filePath);
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			final int requiredWidth = width;
			final int requiredHeight = height;
			int scale = 1;
			while (o.outWidth / scale / 2 >= requiredWidth
					&& o.outHeight / scale / 2 >= requiredHeight) {
				scale *= 2;
			}

			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}