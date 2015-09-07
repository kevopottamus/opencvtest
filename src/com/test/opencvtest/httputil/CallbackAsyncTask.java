package com.test.opencvtest.httputil;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Async task that supports completion and error callbacks
 */
public abstract class CallbackAsyncTask<Params, Progress, Result> extends
		AsyncTask<Params, Progress, Result> {

	private static String TAG = CallbackAsyncTask.class.getName();

	private Throwable throwable;
	private RequestCompleteCallback<Result> requestCompleteCallback;
	private ErrorCallback errorCallback;

	public CallbackAsyncTask() {
	}

	public CallbackAsyncTask(RequestCompleteCallback<Result> doneCallback,
			ErrorCallback errorCallback) {
		this.requestCompleteCallback = doneCallback;
		this.errorCallback = errorCallback;
	}

	public boolean hasErrorCallback() {
		return errorCallback != null;
	}

	@Override
	protected Result doInBackground(Params... parms) {
		Result result = null;
		try {
			result = internalDoInBackground(parms);
		} catch (final Throwable t) {
			Log.w(TAG, t.getMessage(), t);
			this.throwable = t;
		}
		return result;
	}

	abstract protected Result internalDoInBackground(Params... parms) throws Exception;

	protected void onError(Throwable error) {
		if (errorCallback != null) {
			errorCallback.onError(throwable);
		}
	}

	@Override
	protected void onPostExecute(Result result) {
		if (throwable != null) {
			onError(throwable);
		} else if (requestCompleteCallback != null) {
			requestCompleteCallback.onRequestComplete(result);
		}
	}

	public void cancel() {
		if (!isCancelled() && getStatus() != Status.FINISHED) {
			cancel(true);
		}
	}

}
