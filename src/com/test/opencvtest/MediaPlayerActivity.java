package com.test.opencvtest;

import java.io.File;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Use Android MediaPlayer to play back video
 */
public class MediaPlayerActivity extends Activity implements
		SurfaceHolder.Callback, OnPreparedListener {

	public static final String TAG = MediaPlayerActivity.class.getSimpleName();

	private MediaPlayer mediaPlayer;
	private SurfaceHolder surfaceHolder;
	private SurfaceView playerSurfaceView;
	private CommandSender sender;

	private void initializePlayer() {
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDisplay(surfaceHolder);

			Bundle extras = getIntent().getExtras();
			String url = extras == null ? null : extras.getString("url");
			if (url == null) {
				File mediaStorageDir = new File(
						Environment
								.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
						OpenCvUtility.CAPTURE);
				File file = new File(mediaStorageDir, "tree.mp4");
				// File file = new
				// File("/storage/sdcard0/Pictures/capture/VID_20140925_225025.mp4");
				// File file = new
				// File("/storage/sdcard0/Pictures/capture/tree.mp4");
				url = file.getAbsolutePath();
			}

			// this works with libstreaming-example1 application
			// sometimes network isn't very stable when usb is plugged in - try
			// unplug usb
			// url = "rtsp://192.168.1.5:1234";

			// not working preprae failed, neither works with spydroid app
			// url =
			// "http://192.168.1.5:8080/spydroid.sdp?id=0&h264=500-20-320-240&flash=off&camera=0";
			// url = "rtsp://jchen-02:8086";
			url = "rtsp://www.find-code.com:1935/live/test.stream";
			// url = "rtsp://jchen-02:1935/live/testStream";

			Log.i(TAG, url);
			mediaPlayer.setDataSource(url);

			mediaPlayer.prepare();
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void releasePlayer() {
		mediaPlayer.release();
		mediaPlayer = null;
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		player.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		initializePlayer();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		releasePlayer();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sender = new CommandSender();
		sender.start();
		sender.sendCommand("START");

		setContentView(R.layout.mediaplayer);
		playerSurfaceView = (SurfaceView) findViewById(R.id.playersurface);

		surfaceHolder = playerSurfaceView.getHolder();
		surfaceHolder.addCallback(this);
	}
}