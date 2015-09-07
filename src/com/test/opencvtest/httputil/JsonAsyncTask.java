package com.test.opencvtest.httputil;

import org.json.JSONObject;


public class JsonAsyncTask extends
		CallbackAsyncTask<String, Void, JsonHttpResult> {

	public JsonAsyncTask(RequestCompleteCallback<JsonHttpResult> doneCallback,
			ErrorCallback errorCallback) {
		super(doneCallback, errorCallback);
	}

	@Override
	protected JsonHttpResult internalDoInBackground(String... params)
			throws Exception {
		JsonHttpResult result = new JsonHttpResult();
		JSONObject jsonObject = HttpUtils.getResponseJson(params[0]);
		result.jsonObject = jsonObject;
		result.code = 200;
		return result;
	}

}
