package com.test.opencvtest.train;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;

public class TrainingData {

	private static final String NAME_TXT = "name.txt";
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat(
			"yyyyMMddHHmmss", Locale.getDefault());

	private Properties names = new Properties(); // index to name
	private int maxIndex = 0;
	private String trainingRoot;

	private void readNames(File file) {
		if (file.exists()) {
			try {
				FileReader reader = new FileReader(file);
				names.load(reader);
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void writeNames(File file) {
		try {
			FileWriter writer = new FileWriter(file);
			names.store(writer, null);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private File getNameFile() {
		return new File(trainingRoot, NAME_TXT);
	}

	public TrainingData(String trainingRoot) {
		this.trainingRoot = trainingRoot;

		File root = new File(trainingRoot);
		readNames(getNameFile());

		// determine max index
		for (Enumeration<Object> key = names.keys(); key.hasMoreElements();) {
			String indexValue = (String) key.nextElement();
			int index = Integer.parseInt(indexValue);
			if (index > maxIndex) {
				maxIndex = index;
			}
		}
		// list all directories, just in case properties file doesn't exist
		File[] indexes = root.listFiles();
		if (indexes != null) {
			for (File index : indexes) {
				if (index.isDirectory()) {
					String indexValue = index.getName();
					getOrAddByIndex(indexValue);
				}
			}
		}
	}

	public String createTrainingData(String name) {
		String index = getOrAddName(name);
		File directory = new File(trainingRoot + "/" + index);
		if (!directory.exists()) {
			directory.mkdirs();
			writeNames(getNameFile());
		}

		StringBuilder builder = new StringBuilder(trainingRoot);
		builder.append("/").append(index).append("/").append(name).append("-")
				.append(FORMAT.format(new Date())).append(".png");
		return builder.toString();
	}

	/**
	 * Get name associated with index. If index doesn't exist, create index ->
	 * index entry and update max index.
	 */
	public String getOrAddByIndex(String index) {
		String name = names.getProperty(index);
		if (name == null) {
			names.put(index, index);
			int indexValue = Integer.valueOf(index);
			if (indexValue > maxIndex) {
				maxIndex = indexValue;
			}
			return index;
		} else {
			return name;
		}
	}

	private String getIndex(String name) {
		for (Entry<Object, Object> entry : names.entrySet()) {
			if (name.equals(entry.getValue())) {
				return (String) entry.getKey();
			}
		}
		return null;
	}

	/**
	 * Get index associated with name. If name doesn't exist, create index ->
	 * name entry and update max index
	 */
	public String getOrAddName(String name) {
		String index = getIndex(name);
		if (index == null) {
			maxIndex++;
			String indexString = String.valueOf(maxIndex);
			names.put(indexString, name);
			return indexString;
		} else {
			return index;
		}
	}
	
}
