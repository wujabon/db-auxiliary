package com.lurenx.db.auxiliary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 建表时用到
 */
public class DbCache {
	private static Map<String, TableModel> dbCache = new HashMap<String, TableModel>();
	
	public static void put(String tableName, TableModel dbModel) {
		dbCache.put(tableName, dbModel);
	}
	
	public static Iterator<Entry<String, TableModel>> iterator() {
		return dbCache.entrySet().iterator();
	}
	
	public static void clear() {
		dbCache.clear();
	}

}
