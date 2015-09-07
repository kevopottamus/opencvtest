package com.test.opencvtest;

import java.io.File;

public class FileUtil {

	public static final void removeRecursive(File file) {
		if (file.isDirectory()) {
			String[] children = file.list();
			for (int i = 0; i < children.length; i++) {
				removeRecursive(new File(file, children[i]));
			}
		}
		// rename file before deletion to prevent the following error
		// open failed: EBUSY (Device or resource busy)
		File tmpFile = new File(file.getAbsolutePath()
				+ System.currentTimeMillis());
		file.renameTo(tmpFile);
		tmpFile.delete();
	}

}
