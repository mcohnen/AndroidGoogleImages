package com.mcohnen.uberphotos;

import com.loopj.android.http.AsyncHttpClient;

public class HttpClient extends AsyncHttpClient {

	static HttpClient client;
	
	static HttpClient getInstance() {
		if (client == null) {
			client = new HttpClient();
		}
		return client;
	}

}
