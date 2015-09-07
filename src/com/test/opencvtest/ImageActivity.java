package com.test.opencvtest;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.test.opencvtest.train.Utils;

public class ImageActivity extends Activity {
	
	public static final String TAG = ImageActivity.class.getSimpleName();
	public static final int ACTIVITY_SELECT_IMAGE = 1234;
    
    private String pictureFile;
	private int algorithm;

	private void initializeRecognizer(int type) {
		if (!OpenCvUtility.isFaceRecognizerInitialized()) {
			new RecognizerInitializer(this, type).execute((Void[]) null);
		}
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.image);

		Intent intent = getIntent();
		algorithm = intent.getIntExtra("algorithm", 2);

        Button captureButton = (Button) findViewById(R.id.loadButton);
        captureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
        case ACTIVITY_SELECT_IMAGE:
            if (resultCode == RESULT_OK) {
                Uri selectedImage = imageReturnedIntent.getData();
                String[] filePathColumn = { Media.DATA };

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                pictureFile = cursor.getString(columnIndex);
                cursor.close();

                ImageView image = (ImageView) findViewById(R.id.image);
				File output = MediaUtility.getOutputMediaFile(
						MediaUtility.MEDIA_TYPE_IMAGE, OpenCvUtility.APP);

				if (OpenCvUtility.isFaceRecognizerInitialized()) {
					OpenCvUtility.recognizePeopleInFile(pictureFile,
							output.getAbsolutePath());

					Point dimension = Utils.getScreenSize(this);
					image.setImageBitmap(ImageUtil.decodeFile(
							output.getAbsolutePath(), dimension.x, dimension.y));
				} else {
					Toast.makeText(this, "recognizer not initialized",
							Toast.LENGTH_SHORT).show();
				}
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();

		// initialize on resume,
		initializeRecognizer(algorithm);
    }

}
