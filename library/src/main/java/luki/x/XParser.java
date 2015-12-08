/**
 * Copyright (C) 2014 Luki(liulongke@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package luki.x;

import android.app.Activity;
import android.view.View;

import java.io.Serializable;

import luki.x.base.AsyncTask;
import luki.x.base.IDBHelper;
import luki.x.base.IParser;
import luki.x.base.ParserCallBack;
import luki.x.base.XLog;
import luki.x.db.DBEntryMap;
import luki.x.inject.content.InjectHolder;
import luki.x.inject.content.InjectParser;
import luki.x.task.TaskConfig;
import luki.x.task.TaskStatusListener;
import luki.x.util.ReflectUtils;

import static luki.x.XConfig.HOLDER_KEY;
import static luki.x.XConfig.HOLDER_POSITION;
import static luki.x.inject.view.InjectEventControl.initListener;
import static luki.x.inject.view.InjectEventControl.initView;
import static luki.x.inject.view.InjectEventControl.parseWithCDAnnotation;

/**
 * A easy tool to do something with init {@link View} or set data to View, and get a {@link IDBHelper} to operate the
 * DB, and do {@link AsyncTask} with {@link XTask}.
 * 
 * @author Luki
 * @version 1 Nov 5, 2014 7:45:20 PM
 * @since 1.0
 * @see #parseView(View)
 */
public enum XParser {
	INSTANCE;

	private static final String TAG = "XParser";

	private static final String LOG_DESTROY = "Destroy XParser";

	private static final String LOG_INIT_CONFIG = "Initialize XParser with configuration";
	private static final String WARNING_RE_INIT_CONFIG = "Try to initialize XParser which had already been initialized before. "
			+ "To re-init XParser with new configuration call XParser.destroy() at first.";
	private static final String ERROR_NOT_INIT = "XParser must be init with configuration before using";
	private static final String ERROR_INIT_CONFIG_WITH_NULL = "XParser configuration can not be initialized with null";

	private XConfig configuration;

	private final static IParser mDefaultParser = new InjectParser();

	/**
	 * Initializes XParser instance with configuration.<br />
	 * If configurations was set before ( {@link #isInitialized()} == true) then this method does nothing.<br />
	 * To force initialization with new configuration you should {@linkplain #destroy() destroy XParser} at first.
	 * 
	 * @param configuration {@linkplain XConfig XParser configuration}
	 * @throws IllegalArgumentException if <b>configuration</b> parameter is null
	 */
	public synchronized void init(XConfig configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
		}
		if (this.configuration == null) {
			XLog.d(TAG, LOG_INIT_CONFIG);
			this.configuration = configuration;
		} else {
			XLog.w(TAG, WARNING_RE_INIT_CONFIG);
		}
	}

	/**
	 * Returns <b>true</b> - if XParser {@linkplain #init(XConfig) is initialized with
	 * configuration}; <b>false</b> - otherwise
	 */
	public boolean isInitialized() {
		return configuration != null;
	}

	/**
	 * Traversal the value, format, click, longClick from the InjectHolder.<BR>
	 * see more <BR>
	 * {@link #parseView(View)}
	 * 
	 * @param target The target is an Activity who contains the click or longClick method.
	 * @param callBack callBack
	 */
	public void parse(Activity target, ParserCallBack callBack) {
		parse(target, null, callBack);
	}

	/**
	 * Traversal the value, format, click, longClick from the InjectHolder.<BR>
	 * see more <BR>
	 * {@link #parseView(View)}
	 * 
	 * @param target The target is an Activity who contains the click or longClick method.
	 * @param data dataSource
	 * @param callBack callBack
	 */
	public void parse(Activity target, Object data, ParserCallBack callBack) {
		View view = target.getWindow().getDecorView();
		parse(target, data, view, callBack);
	}

	/**
	 * Traversal the value and format and click and longClick from the InjectHolder.<BR>
	 * see more <BR>
	 * {@link #parseView(View)}
	 * 
	 * @param target The target is an object who contains the click or longClick method. If you doesn't need click or
	 *            longClick method, it can be null.
	 * @param data dataSource
	 * @param view Which contains contentDescription.
	 * @param callBack callBack
	 */
	public void parse(Object target, Object data, View view, ParserCallBack callBack) {
		checkConfiguration();
		if (view == null || data == null) {
			return;
		}
		InjectHolder holder;
		holder = (InjectHolder) view.getTag(HOLDER_KEY);
		if (holder == null) {
			holder = parseView(view);
			view.setTag(HOLDER_KEY, holder);
		}
		Integer position = (Integer) view.getTag(HOLDER_POSITION);
		if (position == null) position = -1;
		holder.position = position;

		mDefaultParser.onParse(target, data, holder, callBack);
		if (configuration.userParser != null) {
			configuration.userParser.onParse(target, data, holder, callBack);
		}
	}

	/**
	 * Parse the view contentDescription for each child, and add the each child to InjectHolder.
	 * 
	 * @param view  views who contains contentDescription collection.
	 * @return views who contains contentDescription collection.
	 */
	public InjectHolder parseView(View view) {
		checkConfiguration();
		InjectHolder injectHolder;
		if (configuration.userParser != null) {
			injectHolder = configuration.userParser.onParseView(view);
			if (injectHolder != null) {
				return injectHolder;
			}
		}
		return mDefaultParser.onParseView(view);
	}

	/**
	 * initial all views and it's listener method in the activity. see more {@link #inject(Object, View)}
	 * 
	 * @param activity activity
	 */
	public void inject(Activity activity) {
		inject(activity, activity.getWindow().getDecorView());
	}

	/**
	 * initial all views and it's listener method in the view. And the listener method should be in the target.
	 * All about the annotation should write in the target <BR>
	 * e.g.<BR>
	 * public class Sample {<BR>
	 * <BR>
	 * &nbsp;　&nbsp;＠ViewInject(value = R.id.test, longClick = "longClick")<BR>
	 * &nbsp;　&nbsp;View v;<BR>
	 * &nbsp;　&nbsp;＠ViewInject(longClick = "longClick")<BR>
	 * &nbsp;　&nbsp;View test;<BR>
	 * &nbsp;　&nbsp;View view;<BR>
	 * <BR>
	 * &nbsp;　&nbsp;public Sample(View view) {<BR>
	 * &nbsp;　 　&nbsp;this.view = view;<BR>
	 * &nbsp;　 　&nbsp;ViewInjectUtil.initViewInject(this, view);<BR>
	 * &nbsp;　&nbsp;}<BR>
	 * <BR>
	 * &nbsp;　&nbsp;＠ViewListener(ids = {R.id.test}, type=ListenerType.CLICK)<BR>
	 * &nbsp;　&nbsp;public void click(View v){<BR>
	 * &nbsp;　 　&nbsp;Toast.makeText(view.getContext(), "click", Toast.LENGTH_SHORT).show();<BR>
	 * &nbsp;　&nbsp;}<BR>
	 * <BR>
	 * &nbsp;　&nbsp;public boolean longClick(View v){<BR>
	 * &nbsp;　 　&nbsp;Toast.makeText(view.getContext(), "longClick", Toast.LENGTH_SHORT).show();<BR>
	 * &nbsp;　 　&nbsp;return true;<BR>
	 * &nbsp;　&nbsp;}<BR>
	 * <BR>
	 * }<BR>
	 * 
	 * @param target target
	 * @param view view
	 */
	public void inject(Object target, View view) {
		initView(target, view);
		initListener(target, view);
		parseWithCDAnnotation(target, view);
	}

	/**
	 * Get a XTask instance.
	 * 
	 * @param callBack callBack
	 * @return XTask
	 */
	public <T extends Serializable> XTask<T> getXTask(TaskStatusListener callBack) {
		checkConfiguration();
		TaskConfig config = null;
		if (!XTask.isInit()) {
			config = new TaskConfig();
			config.cacheInDB = configuration.cacheInDB;
			config.errorType = configuration.errorType;
			config.requestExtras = configuration.requestExtras;
			config.requestHeaders = configuration.requestHeaders;
			config.timeOut = configuration.timeout;
			config.retryTimes = configuration.times;
			config.dataParser = configuration.dataParser;
			try {
				ReflectUtils.setFieldValue(config, "isDefault", true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new XTask<>(callBack, config);
	}

	/**
	 * Get a XTask instance with taskConfig.
	 * 
	 * @param callBack callBack
	 * @return XTask
	 */
	public <T extends Serializable> XTask<T> getXTask(TaskStatusListener callBack, TaskConfig config) {
		checkConfiguration();
		try {
			ReflectUtils.setFieldValue(config, "isDefault", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new XTask<>(callBack, config);
	}

	/**
	 * returns a IDBHelper object who can convenient and unified to manage the data.
	 * 
	 * @return IDBHelper
	 * @see IDBHelper
	 */
	public IDBHelper getDBHelper() {
		return getDBHelper(null);
	}

	/**
	 * When the database does not exist, it will automatically generate a database according to the database name.
	 * And returns a IDBHelper object who can convenient and unified to manage the data.
	 * 
	 * @param dbName dbName
	 * @return IDBHelper
	 * @see IDBHelper
	 */
	public IDBHelper getDBHelper(String dbName) {
		checkConfiguration();
		return DBEntryMap.getDBHelper(XConfig.sContext, dbName);
	}

	/**
	 * Checks if XParser's configuration was initialized
	 * 
	 * @throws IllegalStateException if configuration wasn't initialized
	 */
	private void checkConfiguration() {
		if (configuration == null) {
			throw new IllegalStateException(ERROR_NOT_INIT);
		}
	}

	/**
	 * {@linkplain #inject(Activity) Stops XParser} and clears current configuration. <br />
	 * You can {@linkplain #init(XConfig) init} XParser with new configuration after calling this
	 * method.
	 */
	public void destroy() {
		XLog.d(TAG, LOG_DESTROY);
		configuration = null;
	}
}
