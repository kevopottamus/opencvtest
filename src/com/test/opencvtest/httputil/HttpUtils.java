package com.test.opencvtest.httputil;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;

public class HttpUtils {

	private static AndroidHttpClient client = AndroidHttpClient
			.newInstance("Android");

	private static HttpEntity getResponseEntity(String url) throws IOException {
		HttpResponse response = client.execute(new HttpGet(url));
		int code = response.getStatusLine().getStatusCode();
		if (code < 200 && code > 299) {
			throw new IOException("Error Code: " + code + "; "
					+ response.getStatusLine().getReasonPhrase());
		}
		return response.getEntity();
	}

	public static String getResponseString(String url) throws IOException {
		HttpEntity httpEntity = getResponseEntity(url);
		return EntityUtils.toString(httpEntity, "UTF-8");
	}

	public static JSONObject getResponseJson(String url) throws IOException,
			JSONException {
		String response = getResponseString(url);
		if (response == null || response.length() <= 0) {
			return null;
		} else {
			return new JSONObject(response);
		}
	}
}
