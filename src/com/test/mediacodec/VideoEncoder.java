package com.test.mediacodec;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaFormat;
import android.util.Log;

/**
 * We currently require API 16 (4.1), but we may be able to downgrade it to 4.0
 * via libstagefright.
 */
public class VideoEncoder {
	public static final String TAG = VideoEncoder.class.getSimpleName();

	// H.264 Advanced Video Coding
	private static final String MIME_TYPE = "video/avc";
	private static final int FRAME_RATE = 15; // 15fps
	// 10 seconds between I-frames
	private static final int IFRAME_INTERVAL = 10;

	private BufferInfo mBufferInfo;
	private MediaCodec mEncoder;

	private int width;
	private int height;
	private int bitRate;

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void encode(byte[] data, OutputStream outputStream) throws Exception {
		Log.i(TAG, "input size " + data.length);
		ByteBuffer[] codecInputBuffers = mEncoder.getInputBuffers();
		ByteBuffer[] codecOutputBuffers = mEncoder.getOutputBuffers();

		int inputBufferIndex = mEncoder.dequeueInputBuffer(0);
		if (inputBufferIndex >= 0) {
			ByteBuffer inputBuffer = codecInputBuffers[inputBufferIndex];
			inputBuffer.clear();
			Log.i(TAG, "capacity: " + inputBuffer.capacity() + " limit: "
					+ inputBuffer.limit());
			inputBuffer.put(data);
			mEncoder.queueInputBuffer(inputBufferIndex, 0, data.length, 0, 0);
		}

		while (true) {
			int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
			if (outputBufferIndex >= 0) {
				// write buffer data to outputstream
				ByteBuffer outputBuffer = codecOutputBuffers[outputBufferIndex];
				byte[] outputData = new byte[mBufferInfo.size];
				outputBuffer.get(outputData);
				outputStream.write(outputData, 0, outputData.length);
				Log.i(TAG, outputData.length + " bytes written");
				mEncoder.releaseOutputBuffer(outputBufferIndex, false);
			}
			else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				Log.i(TAG, "output buffers changed");
				codecOutputBuffers = mEncoder.getOutputBuffers();
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				Log.i(TAG, "output format changed");
				codecOutputBuffers = mEncoder.getOutputBuffers();
			} else {
				Log.i(TAG, "end encoding: outputBufferIndex = "
						+ outputBufferIndex);
				break;
			}
		}

	}

	public void releaseEncoder() {
		if (mEncoder != null) {
			mEncoder.stop();
			mEncoder.release();
			mEncoder = null;
		}
	}

	// this is slow in debug mode (2s), but reasonable in run mode (100ms)
	public void rgbaToYuv(byte[] yuv, byte[] rgba, int width,
			int height) {
		int frameSize = width * height;
		int chromaSize = frameSize / 4;
		int yIndex = 0;
		int uIndex = frameSize;
		int vIndex = frameSize + chromaSize;

		int R, G, B, Y, U, V;
		int index = 0;
		int rgbaIndex = 0;
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++) {
				R = rgba[rgbaIndex++] & 0xff;
				G = rgba[rgbaIndex++] & 0xff;
				B = rgba[rgbaIndex++] & 0xff;
				rgbaIndex++; // a is unused

				// well known RGB to YUV algorithm
				Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
				U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
				V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

				yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
				if (j % 2 == 0 && index % 2 == 0) {
					yuv[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
					yuv[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
				}

				index++;
			}
		}
	}

	public void prepareEncoder(int width, int height) {
		Log.i(TAG, "prepare encoder width=" + width + ", height=" + height);
		this.width = width; // 720
		this.height = height; // 576
		bitRate = 2000000;

		mBufferInfo = new BufferInfo();

		MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width,
				height);
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
				CodecCapabilities.COLOR_FormatYUV420Planar);
		format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
		format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

		mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
		mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mEncoder.start();
	}

}
