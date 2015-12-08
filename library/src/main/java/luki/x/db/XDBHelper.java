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
package luki.x.db;

import static luki.x.util.DBUtils.PRIMARYKEY_COLUMN;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import luki.x.base.IDBHelper;
import luki.x.base.XLog;
import luki.x.util.DBUtils;
import luki.x.util.ReflectUtils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.text.TextUtils;

/**
 * Simple DBHelper.
 * 
 * @author Luki
 * @date Aug 17, 2014 4:20:20 PM
 */
/*public*/class XDBHelper implements IDBHelper {

	private static final String ROWID_SPLIT = ",";
	private SQLiteDatabase db;
	private String dbName;
	private DBUtils dbUtils;

	XDBHelper(String dbName, Context context) {
		check(context);
		db = new SQLHelper(context.getApplicationContext(), this.dbName = dbName, null, 1).getWritableDatabase();
		dbUtils = DBUtils.getIntance(db, dbName, this);
	}

	/**
	 * check context
	 * 
	 * @param context
	 */
	private void check(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("XDBHelper context can't be null");
		}
	}

	/**
	 * Convenience method for updating or inserting rows in the database.
	 * 
	 * @param t updating or inserting data
	 * @return the number of rows affected
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> int save(T bean) {
		Class<T> clazz = (Class<T>) bean.getClass();
		Table<T> table = (Table<T>) dbUtils.checkTable(clazz);
		String tableName = table.tableName;
		long _id = getPrimaryKeyValue(clazz, table.uniqueSelection.fillIn(bean));
		String operation = null;
		try {
			ContentValues values = dbUtils.getContentValues(bean);
			if (_id > 0) { // exist and update
				operation = "UPDATE";
				values.remove(DBUtils.PRIMARYKEY_COLUMN);
				// update the relation table' data( delete all mapping data and the save the relation data's rowID
				// to the ContentValues).
				if (table.otherTypeField.size() > 0) {
					deleteRelationTableData(clazz, dbUtils.getSelection(bean), table);
					putRelationTableDataContentValues(bean, table, values);
				}

				String[] selectionArgs = new String[] { String.valueOf(_id) };
				db.update(tableName, values, PRIMARYKEY_COLUMN + "=?", selectionArgs);
			} else {// not exist and insert
				operation = "INSERT INTO";
				values.put(DBUtils.PRIMARYKEY_COLUMN, (String) null);
				// save the relation data's rowID to the ContentValues
				if (table.otherTypeField.size() > 0) {
					putRelationTableDataContentValues(bean, table, values);
				}
				_id = db.insert(tableName, null, values);
			}
			XLog.v(TAG, "operation : %s TABLE %s success. PRIMARYKEY or rowID = %s and the bean = %s ", operation, tableName, _id,
					bean.toString());
		} catch (Exception e) {
			XLog.w(TAG, "operation : %s TABLE %s  exception : %s", operation, tableName, e.toString());
			return 0;
		}
		return 1;
	}

	/**
	 * Convenience method for updating or inserting rows in the database.
	 * 
	 * @param list save data list for updating or inserting
	 * @return the number of rows affected
	 */
	public <T extends Serializable> int save(List<T> list) {
		int count = 0;
		long l = System.currentTimeMillis();
		for (T bean : list) {
			if (bean == null) {
				continue;
			}
			count += save(bean);
		}
		XLog.i(TAG, "list size is %d, success %d, cost %d", list.size(), count, System.currentTimeMillis() - l);
		return count;
	}

	/**
	 * Convenience method for inserting a row into the database.
	 * 
	 * @param t save data fro inserting
	 * @return the row ID of the newly inserted row, or -1 if an error occurred or exist
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> long insert(T t) {
		if (t == null) {
			return -1;
		}
		long rowID = -1;
		Class<T> clazz = (Class<T>) t.getClass();
		Table<T> table = (Table<T>) dbUtils.checkTable(clazz);
		String tableName = table.tableName;
		long _id = getPrimaryKeyValue(clazz, table.uniqueSelection.fillIn(t));
		String operation = null;
		try {
			if (_id > 0) { // exist, update?
//				update(t);				
				XLog.v(TAG, "operation : %s TABLE %s fail. the bean has exixts. bean = %s ", "NONE", tableName, t.toString());
				return -1;
			} else {// not exist and insert
				ContentValues values = dbUtils.getContentValues(t);
				operation = "INSERT INTO";
				values.put(PRIMARYKEY_COLUMN, (String) null);
				// save the relation data's rowID to the ContentValues
				if (table.otherTypeField.size() > 0) {
					putRelationTableDataContentValues(t, table, values);
				}
				rowID = db.insert(tableName, null, values);
				XLog.v(TAG, "operation : %s TABLE %s success. rowID = %s and the bean = %s ", operation, tableName, rowID, t.toString());
			}
		} catch (Exception e) {
			XLog.w(TAG, "operation : %s TABLE %s  exception : %s", operation, tableName, e.toString());
		}
		return rowID;
	}

	/**
	 * Convenience method for updating rows in the database.
	 * 
	 * @param t data list for updating
	 * @return the number of rows affected
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> int update(T t) {
		if (t == null) {
			return 0;
		}
		int count = 0;
		Class<T> clazz = (Class<T>) t.getClass();
		Table<T> table = (Table<T>) dbUtils.checkTable(clazz);
		String tableName = table.tableName;
		long _id = getPrimaryKeyValue(clazz, table.uniqueSelection.fillIn(t));
		String operation = null;
		try {
			if (_id > 0) { // exist and update
				ContentValues values = dbUtils.getContentValues(t);
				operation = "UPDATE";
				values.remove(PRIMARYKEY_COLUMN);
				// update the relation table' data( delete all mapping data and the save the relation data's rowID to
				// the ContentValues).
				if (table.otherTypeField.size() > 0) {
					deleteRelationTableData(clazz, dbUtils.getSelection(t), table);
					putRelationTableDataContentValues(t, table, values);
				}

				String[] selectionArgs = new String[] { String.valueOf(_id) };
				count = db.update(tableName, values, PRIMARYKEY_COLUMN + "=?", selectionArgs);
				XLog.v(TAG, "operation : %s TABLE %s success. the number of rows affected = %s and the bean = %s ", operation, tableName,
						count, t.toString());
			} else {// not exist, insert?
//				insert(t);
				return 0;
			}
		} catch (Exception e) {
			XLog.w(TAG, "operation : %s TABLE %s  exception : %s", operation, tableName, e.toString());
		}
		return count;
	}

	/**
	 * find the data with bean.
	 * 
	 * @param bean which contains field' value. And that can auto consist of selection.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> T findByBean(T bean) {
		DBSelection<T> selection = dbUtils.getSelection(bean);
		Class<T> class1 = (Class<T>) bean.getClass();
		return findBySelection(class1, selection);
	}

	/**
	 * find the data with selection.
	 * 
	 * @param clazz table and bean.
	 * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE
	 *            itself). Passing null will return all rows for the given table.
	 * @return clazz's instance
	 */
	public <T extends Serializable> T findBySelection(Class<T> clazz, DBSelection<T> selection) {
		List<T> list = selectBySelection(clazz, selection);
		return list.size() > 0 ? list.get(0) : null;
	}

	/**
	 * find the data with bean.
	 * 
	 * @param bean which contains field' value. And that can auto consist of selection.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> List<T> selectByBean(T bean) {
		DBSelection<T> selection = dbUtils.getSelection(bean);
		Class<T> class1 = (Class<T>) bean.getClass();
		return selectBySelection(class1, selection);
	}

	/**
	 * find the data with selection.
	 * 
	 * @param clazz table and bean.
	 * @param selection A filter declaring which rows to return, formatted as an SQL WHERE clause (excluding the WHERE
	 *            itself). Passing null will return all rows for the given table.
	 * @return clazz's instance
	 */
	public <T extends Serializable> List<T> selectBySelection(Class<T> clazz, DBSelection<T> selection) {
		checkClass(clazz);
		List<T> list = new ArrayList<T>();
		Table<T> table = (Table<T>) dbUtils.checkTable(clazz);
		Cursor c = null;
		try {
			if (selection == null) {
				selection = dbUtils.getSelection(clazz.newInstance());
			}
			String[] selectionArgs = selection.selectionArgs;
			if (XLog.isLogging()) {
				String sql = SQLiteQueryBuilder.buildQueryString(false, table.tableName, null, selection.selection, null, null,
						selection.orderBy, null);
				if (selectionArgs != null) {
					sql = sql.replace("?", "%s");
					String[] src = selectionArgs;
					Object[] dest = new Object[src.length];
					System.arraycopy(src, 0, dest, 0, src.length);
					XLog.v(TAG, sql, dest);
				} else {
					XLog.v(TAG, sql);
				}
			}

			c = db.query(table.tableName, null, selection.selection, selectionArgs, null, null, selection.orderBy);
			if (null != c && c.getCount() > 0) {
				while (c.moveToNext()) {
					T t = (T) dbUtils.getObject(clazz, c);
					addRelationData(c, table, t);
					list.add(t);
				}
			}
		} catch (Exception e) {
			XLog.w(TAG, e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return list;
	}

	private <T extends Serializable> void checkClass(Class<T> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("clazz must be not null.");
		}
		if (!ReflectUtils.hasParameterlessConstructor(clazz)) {
			throw new IllegalArgumentException(clazz.getName() + " must be has a parameterless constructor.");
		}
	}

	/**
	 * Convenience method for deleting rows in the database.
	 * 
	 * @param t data for deleting.
	 * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a
	 *         count pass "1" as the whereClause.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable> int delete(T t) {
		Table<T> table = (Table<T>) dbUtils.checkTable(t.getClass());
		return deleteBySelection(table.tableClass, dbUtils.getSelection(t));
	}

	/**
	 * Convenience method for deleting rows in the database.
	 * 
	 * @param clazz data for deleting.
	 * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a
	 *         count pass "1" as the whereClause.
	 */
	public <T extends Serializable> int deleteBySelection(Class<T> clazz, DBSelection<T> selection) {
		int count = 0;
		String operation = "DELETE FROM ";
		String tableName = clazz.getSimpleName();
		try {
			deleteRelationTableData(clazz, selection, null);
			count += db.delete(tableName, selection.selection, selection.selectionArgs);
			XLog.v(TAG, "operation : %s TABLE %s success.", operation, tableName);
		} catch (Exception e) {
			XLog.w(TAG, "operation : %s TABLE %s exception : %s", operation, tableName, e.toString());
		}
		return count;
	}

	/**
	 * Convenience method for deleting rows in the database.
	 * 
	 * @param list data list for deleting.
	 * @return the number of rows affected if a whereClause is passed in, 0 otherwise. To remove all rows and get a
	 *         count pass "1" as the whereClause.
	 */
	public <T extends Serializable> int delete(List<T> list) {
		int count = 0;
		if (list == null || list.isEmpty()) {
			return count;
		}
		for (T bean : list) {
			if (bean == null) continue;
			count += delete(bean);
		}
		return count;
	}

	/**
	 * add the relation data to t.
	 * 
	 * @param c
	 * @param table
	 * @param t
	 * @throws Exception
	 */
	private <T extends Serializable> void addRelationData(Cursor c, Table<T> table, T t) throws Exception {
		if (t == null) {
			return;
		}
		for (int i = 0; i < table.otherTypeField.size(); i++) {
			Field field = table.otherTypeField.get(i);
			Class<T> clazz1 = ReflectUtils.getFieldClass(field);
			if (clazz1 == null || ReflectUtils.isNormalGenericType(clazz1)) {
				continue;
			}
			int columnIndex = c.getColumnIndex(field.getName());
			if (columnIndex == -1) {
				continue;
			}
			String cv = c.getString(columnIndex);
			if (TextUtils.isEmpty(cv)) {
				continue;
			}

			DBSelection<T> dbSelection = new DBSelection<T>();
			dbSelection.selection = "ROWID=?";
			if (field.getType() == List.class || field.getType() == ArrayList.class) {
				List<T> l = new ArrayList<T>();
				String[] rowIDs = cv.split(ROWID_SPLIT);
				for (String rowID : rowIDs) {
					dbSelection.selectionArgs = new String[] { rowID };
					l.addAll(selectBySelection(clazz1, dbSelection));
				}
				field.set(t, l);
			} else {
				if (clazz1 instanceof Serializable) {
					dbSelection.selectionArgs = new String[] { cv };
					List<T> l = selectBySelection(clazz1, dbSelection);
					if (l.isEmpty()) {
						continue;
					}
					T obj = (T) l.get(0);
					if (obj == null) {
						continue;
					}

					field.set(t, obj);
				}
			}
		}
	}

	/**
	 * delete the relation data.
	 * 
	 * @param clazz parent table
	 * @param selection unique selection
	 * @param table can be null.
	 */
	private <T extends Serializable> void deleteRelationTableData(Class<T> clazz, DBSelection<T> selection, Table<T> table) {
		if (table == null) {
			table = (Table<T>) dbUtils.checkTable(clazz);
		}
		Cursor c = db.query(table.tableName, null, selection.selection, selection.selectionArgs, null, null, null);
		if (c != null && c.moveToFirst()) {
			for (int i = 0; i < table.otherTypeField.size(); i++) {
				Field field = table.otherTypeField.get(i);
				Class<T> clazz1 = ReflectUtils.getFieldClass(field);
				if (clazz1 == null || ReflectUtils.isNormalGenericType(clazz1)) {
					continue;
				}
				int columnIndex = c.getColumnIndex(field.getName());
				if (columnIndex == -1) {
					continue;
				}
				String cv = c.getString(columnIndex);
				if (TextUtils.isEmpty(cv)) {
					continue;
				}
				String[] rowIDs = cv.split(ROWID_SPLIT);
				for (String rowID : rowIDs) {
					DBSelection<T> sel = new DBSelection<T>();
					sel.selection = "ROWID=?";
					sel.selectionArgs = new String[] { rowID };
					deleteBySelection(clazz1, sel);
					XLog.v(TAG, "operation : %s TABLE %s success. And the ROWID = %s ", "DELETE", clazz1.getSimpleName(), rowID);
				}
			}
		}
		if (c != null) {
			c.close();
		}
	}

	/**
	 * set relation table data to the ContentValues.
	 * 
	 * @param bean from bean
	 * @param table can be null
	 * @param values target ContentValues
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private <T extends Serializable> void putRelationTableDataContentValues(T bean, Table<T> table, ContentValues values) throws Exception {
		if (table == null) {
			table = (Table<T>) dbUtils.checkTable(bean.getClass());
		}
		for (int i = 0; i < table.otherTypeField.size(); i++) {
			StringBuilder columnValues = new StringBuilder();
			Field field = table.otherTypeField.get(i);
			Class<T> clazz1 = ReflectUtils.getFieldClass(field);
			if (ReflectUtils.isNormalGenericType(clazz1) || !ReflectUtils.hasParameterlessConstructor(clazz1)) {
				continue;
			}
			if (clazz1 != null) {
				if (field.getType() == List.class || field.getType() == ArrayList.class) {
					List<T> l = (List<T>) field.get(bean);
					if (l == null) {
						continue;
					}
					for (T t : l) {
						if (t == null) {
							continue;
						}
						columnValues.append(ROWID_SPLIT);
						columnValues.append(insert(t));
					}
					columnValues.delete(0, ROWID_SPLIT.length());
				} else {
					T obj = (T) field.get(bean);
					if (obj == null) {
						continue;
					}
					columnValues.append(insert(obj));
				}
			}
			values.put(field.getName(), columnValues.toString());
		}
	}

	/**
	 * get Primary key.
	 * 
	 * @param clazz target table's class
	 * @param uniqueSelection unique selection
	 * @return the primary key value.
	 */
	private <T extends Serializable> long getPrimaryKeyValue(Class<T> clazz, DBSelection<T> uniqueSelection) {
		long _id = -1;
		Cursor c = null;
		try {
			if (uniqueSelection.selectionArgs.length > 0) {
				c = db.query(clazz.getSimpleName(), null, uniqueSelection.selection, uniqueSelection.selectionArgs, null, null, null);
				if (c != null && c.moveToFirst()) {
					int columnIndex = c.getColumnIndex(DBUtils.PRIMARYKEY_COLUMN);
					_id = c.getLong(columnIndex);
				}
			}
		} catch (Exception e) {
			XLog.w(TAG, e);
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return _id;
	}

	public synchronized void close() {
		if (db != null) {
			db.close();
		}
		DBEntryMap.destroy(dbName);
	}

	public boolean isOpen() {
		return db.isOpen();
	}
}
