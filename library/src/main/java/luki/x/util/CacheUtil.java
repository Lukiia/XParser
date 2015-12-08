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
package luki.x.util;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;

/**
 * 缓存管理类 <BR>
 * <Li>使用之前需要初始化 {@link #init(Context)}</LI><BR>
 * <Li>判断是否已经初始化 {@link #isInit()}
 * 
 * @author Luki
 */
public class CacheUtil {

	private Context mContext;
	private static CacheUtil mIntance;
	private static Object obj = new Object();

	private CacheUtil(Context context) {
		this.mContext = context;
	}

	/**
	 * 初始化
	 * 
	 * @param context
	 */
	public static void init(Context context) {
		synchronized (obj) {
			if (mIntance == null) {
				mIntance = new CacheUtil(context);
			}
		}
	}

	/**
	 * 判断是否已经初始化
	 * 
	 * @return
	 */
	public static boolean isInit() {
		if (mIntance == null) {
			return false;
		} else if (mIntance.mContext == null) {
			return false;
		} else
			return true;
	}

	public static CacheUtil getIntance() {
		if (mIntance == null) {
			throw new IllegalArgumentException("please invoke CacheUtil.init(Context) before used");
		}
		return mIntance;
	}

	/**
	 * 保存对象
	 * 
	 * @param ser
	 * @param file
	 * @throws IOException
	 */
	public boolean saveObject(Serializable ser, String file) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = mContext.openFileOutput(file, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(ser);
			oos.flush();
			return true;
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		} finally {
			try {
				oos.close();
			} catch (Exception e) {
			}
			try {
				fos.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 读取对象
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public Serializable readObject(String file) {
		if (!isExistDataCache(file))
			return null;
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = mContext.openFileInput(file);
			ois = new ObjectInputStream(fis);
			return (Serializable) ois.readObject();
		} catch (FileNotFoundException e) {
		} catch (Exception e) {
			// e.printStackTrace();
			// 反序列化失败 - 删除缓存文件
			if (e instanceof InvalidClassException || e instanceof EOFException) {
				try {
					File data = mContext.getFileStreamPath(file);
					data.delete();
				} catch (Exception e1) {
					// e1.printStackTrace();
				}
			}
		} finally {
			try {
				ois.close();
			} catch (Exception e) {
			}
			try {
				fis.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	/**
	 * 判断缓存是否存在
	 * 
	 * @param cachefile
	 * @return
	 */
	private boolean isExistDataCache(String cachefile) {
		boolean exist = false;
		File data = mContext.getFileStreamPath(cachefile);
		if (data.exists())
			exist = true;
		return exist;
	}

	/**
	 * 判断缓存是否失效
	 * 
	 * @param cachefile
	 * @return true 失效 false 缓存有效
	 */
	public boolean isCacheDataFailure(String cachefile, long cacheTime) {
		boolean failure = false;
		File data = mContext.getFileStreamPath(cachefile);
		if (data.exists() && (System.currentTimeMillis() - data.lastModified()) > cacheTime)
			failure = true;
		else if (!data.exists())
			failure = true;
		return failure;
	}
}
