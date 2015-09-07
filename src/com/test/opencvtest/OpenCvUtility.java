package com.test.opencvtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

public class OpenCvUtility {

	public static final String TAG = OpenCvUtility.class.getSimpleName();
	public static final String APP = "OpenCvTest";
	public static final String TRAIN_PATH = "train";
	public static final String RESOURCE_PATH = "resource";
	public static final String HAARCASCADE = "haarcascade_frontalface_default.xml";
	public static final String INTERNAL_TRAINING = "internal-training";
	public static final String CAPTURE = "capture";

	private static boolean isOpenCvInitialized = false;
	private static boolean isPeopleDetectorInitialized = false;
	private static boolean isFaceRecognizerInitialized = false;
	private static boolean isFaceDetectorInitialized = false;
	private static boolean isInternalTrainingInitialized = false;

	public static final String getVideoDataPath(Context context) {
		// TODO shall we use APP only
		File internalPath = context.getDir(APP + "/" + CAPTURE,
				Context.MODE_PRIVATE);
		return internalPath.getAbsolutePath();
	}

	public static final String getRecognizerInternalTrainingDataPath(
			Context context) {
		File internalPath = context.getDir(INTERNAL_TRAINING,
				Context.MODE_PRIVATE);
		return internalPath.getAbsolutePath();
	}

	public static final String getRecognizerTrainingDataPath() {
		// this would normally be under context.getDir()
		// we use external storage temporarily to copy into the device
		File file = new File(Environment.getExternalStorageDirectory(), APP
				+ "/" + TRAIN_PATH);

		if (!file.exists()) {
			if (file.mkdirs()) {
				Log.i(TAG, file.getAbsolutePath());
			}
		}

		return file.getAbsolutePath();
	}

	public static final List<String> getRecognizerTrainingDataPaths() {
		List<String> trainingData = new ArrayList<String>();
		File root = new File(getRecognizerTrainingDataPath());
		File[] users = root.listFiles();
		if (users != null) {
			for (File user : users) {
				if (user.isDirectory()) {
					File[] images = user.listFiles();
					if (images != null) {
						for (File image : images) {
							trainingData.add(image.getAbsolutePath());
						}
					}
				}
			}
		}
		return trainingData;
	}

	/*
	 * copy files from asset directory to application directory - only if they
	 * don't already exist
	 */
	public static final void setupInternalTrainingImages(Context context) {
		try {
			AssetManager assetManager = context.getAssets();
			String[] userIds = assetManager.list(INTERNAL_TRAINING);
			File targetRoot = new File(
					getRecognizerInternalTrainingDataPath(context));

			for (int i = 0; i < userIds.length; i++) {
				String userDir = INTERNAL_TRAINING + "/" + userIds[i];
				String[] userPhotos = assetManager.list(userDir);
				File targetUserDir = new File(targetRoot, userIds[i]);
				if (!targetUserDir.exists()) {
					targetUserDir.mkdir();
				}
				
				for (int j=0; j<userPhotos.length; j++) {
					File targetFile = new File(targetUserDir, userPhotos[j]);
					Log.i(TAG, targetFile.toString());

					if (!targetFile.exists()) {
						InputStream photoStream = assetManager.open(userDir
								+ "/" + userPhotos[j]);
						StreamUtil.copy(photoStream, new FileOutputStream(
								targetFile));
					}
				}
			}

		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	/**
	 * Remove internal training images
	 */
	public static final void cleanupInternalTrainingImages(Context context) {
		File targetRoot = new File(
				getRecognizerInternalTrainingDataPath(context));
		if (targetRoot.exists()) {
			FileUtil.removeRecursive(targetRoot);
		}
		isInternalTrainingInitialized = false;
	}

	/**
	 * Remove training data (not internal training images)
	 */
	public static final void cleanupTrainingImages() {
		File file = new File(getRecognizerTrainingDataPath());
		if (file.exists()) {
			FileUtil.removeRecursive(file);
		}
		isFaceRecognizerInitialized = false;
	}

	public static final String getFaceDetectorDataPath(Context context) {
		try {
			File resourceDir = context.getDir(RESOURCE_PATH,
					Context.MODE_PRIVATE);
			File resourceFile = new File(resourceDir, HAARCASCADE);
			if (!resourceFile.exists()) {
				// asset contents are normally compressed - we copy it to a file
				// in the app
				InputStream in = context.getAssets().open(HAARCASCADE);
				FileOutputStream out = new FileOutputStream(resourceFile);
				StreamUtil.copy(in, out);
			}
			return resourceFile.getAbsolutePath();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
	}

	public static void loadNativeLib() {
		if (!isOpenCvInitialized) {
			// Load native library after(!) OpenCV initialization
			System.loadLibrary("opencv_sample");
			Log.i(TAG, "OpenCV loaded successfully");
			isOpenCvInitialized = true;
		}
	}

	public static void initializePeopleDetector() {
		if (!isPeopleDetectorInitialized) {
			nativeInitializePeopleDetector();
			isPeopleDetectorInitialized = true;
		}
	}

	public static boolean isPeopleDetectorInitializer() {
		return isPeopleDetectorInitialized;
	}

	public static boolean isFaceRecognizerInitialized() {
		return isFaceRecognizerInitialized;
	}

	public static void setFaceRecognizerInitialized(boolean initialized) {
		isFaceRecognizerInitialized = initialized;
	}

	public static void initializeInternalTraining(String internalTrainingPath) {
		if (!isInternalTrainingInitialized) {
			nativeInitializeInternalTraining(internalTrainingPath);
			isInternalTrainingInitialized = true;
		}
	}

	public static void initializeRecognizer(String internalTrainingPath,
			String trainingPath, int type) {
		Log.i(TAG, "initialize type " + type);
		initializeInternalTraining(internalTrainingPath);
		nativeInitializeRecognizer(trainingPath, type);
		isFaceRecognizerInitialized = true;
	}

	// test code - ignore
	public static void test(Context context) {
		File resourceDir = context.getDir(RESOURCE_PATH, Context.MODE_PRIVATE);
		File resourceFile = new File(resourceDir, HAARCASCADE);

		File file = new File(Environment.getExternalStorageDirectory(), APP
				+ "/" + TRAIN_PATH + "/2/j-20140616215811.png");

		test(resourceFile.getAbsolutePath(), file.getAbsolutePath());
	}

	public static void initializeFaceDetector(String haarPath) {
		if (!isFaceDetectorInitialized) {
			Log.i(TAG, "initialize face detector");
			nativeInitializeFaceDetector(haarPath);
			isFaceDetectorInitialized = true;
		}
	}

	public static native void nativeInitializePeopleDetector();

	public static native void nativeInitializeFaceDetector(String haarPath);

	public static native void nativeInitializeInternalTraining(
			String internalTrainingPath);

	public static native void nativeInitializeRecognizer(String trainingPath,
			int type);

	public static native void nativeInitializeVideoWriter(String path,
			int width, int height);

	public static native void findPeople(long matAddrFrame);

	public static native int addTrainingData(long matAddrFrame, String output);

	public static native void findPeopleInFile(String input, String output);

	public static native void recognizePeople(long matAddrFrame);

	public static native void recognizePeopleInFile(String input, String output);

	public static native void writeVideo(long matAddrFrame);

	public static native void releaseVideo();

	public static native void test(String haarPath, String filePath);

}
