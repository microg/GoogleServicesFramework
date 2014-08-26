package com.google.android.gtalkservice;

interface IHttpRequestCallback {
	void requestComplete(in byte[] bytes);
}