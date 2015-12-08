/**
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package luki.x.net;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import luki.x.base.INetEngine;
import luki.x.base.XLog;
import luki.x.task.TaskConfig;

/**
 * XNetEngine
 *
 * @author Luki
 */
public class XNetEngine implements INetEngine {
	private static final String TAG = XNetEngine.class.getSimpleName();
	private static int DEFAULT_SOCKET_TIMEOUT = 15 * 1000;
	private int RETRY_TIMES = 2;

	private TaskConfig config;

	public XNetEngine() {
		setDefaultHostnameVerifier();
	}

	private void setDefaultHostnameVerifier() {
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}

	/**
	 * POST
	 *
	 * @param url     url
	 * @param params  request params
	 * @param headers headers
	 * @return respond String
	 *
	 * @throws Exception
	 */
	public String post(String url, Map<String, String> params, Map<String, String> headers) throws Exception {
		if (TextUtils.isEmpty(url)) {
			XLog.i(TAG, "POST-URL is Null");
			return "";
		} else
			XLog.v(TAG, url);
		String result = null;
		int time = 0;
		do {
			try {
				if (headers == null)
					headers = new HashMap<>();
				if (config.requestHeaders != null)
					headers.putAll(config.requestHeaders);
				if (params == null)
					params = new HashMap<>();
				if (config.requestExtras != null)
					headers.putAll(config.requestExtras);

				result = HttpRequest.sendPost(url, DEFAULT_SOCKET_TIMEOUT, params, headers);
				time = 3;
			} catch (Exception e) {
				time++;
				XLog.w(TAG, "times:%d, %s", time, e.toString());
				if (time < RETRY_TIMES) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignored) {
					}
					continue;
				}
				throw e;
			}
		} while (time < RETRY_TIMES);
		return result;
	}

	/**
	 * GET
	 *
	 * @param url     url
	 * @param headers headers
	 * @return respond String
	 */
	public String get(String url, Map<String, String> headers) throws Exception {
		if (TextUtils.isEmpty(url)) {
			XLog.i(TAG, "GET-URL is Null");
			return "";
		} else
			XLog.v(TAG, url);
		String result = null;
		int time = 0;
		do {
			try {
				if (headers == null)
					headers = new HashMap<>();
				if (config.requestHeaders != null)
					headers.putAll(config.requestHeaders);
				if (config.requestExtras != null) {
					if (!url.contains("?")) {
						url += "?";
					}
					for (String key : config.requestExtras.keySet()) {
						url += key + "=" + config.requestExtras.get(key) + "&";
					}
					url = url.substring(0, url.length() - 1);
				}
				result = HttpRequest.sendGet(url, DEFAULT_SOCKET_TIMEOUT, headers);
				time = 3;
			} catch (Exception e) {
				time++;
				if (time < RETRY_TIMES) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ignored) {
					}
					continue;
				}
				throw e;
			}
		} while (time < RETRY_TIMES);
		return result;
	}

	public void setHttpConfig(TaskConfig config) {
		this.config = config;
		DEFAULT_SOCKET_TIMEOUT = config.timeOut;
		RETRY_TIMES = config.retryTimes;
	}
}
