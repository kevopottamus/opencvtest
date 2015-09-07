package com.test.opencvtest.httputil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
	private static final int MAX_BUFFER_SIZE = 1 * 1024 * 1024;

	public static void copyWithProgress(InputStream inputStream,
			OutputStream outputStream, ProgressIndicator indicator)
			throws IOException {

		int total = 0;
		int bytesAvailable = inputStream.available();
		int bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);
		byte[] buffer = new byte[bufferSize];

		int bytesRead = inputStream.read(buffer, 0, bufferSize);
		while (bytesRead > 0) {
			outputStream.write(buffer, 0, bytesRead);
			total += bytesRead;

			if (indicator != null) {
				indicator.showProgress(total);
			}

			bytesAvailable = inputStream.available();
			bufferSize = Math.min(bytesAvailable, MAX_BUFFER_SIZE);
			bytesRead = inputStream.read(buffer, 0, bufferSize);
		}

	}

}
