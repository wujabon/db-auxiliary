package com.lurenx.db.auxiliary;

import java.util.Map;

/**
 * 表结构数据
 */
public class TableModel {
	private String tableName;
	/**
	 * key 列名
	 */
	private Map<String, TableMetaData> datas;
	/**
	 * 表注释
	 */
	private String comment;
	
	public TableModel(){}
	
	public TableModel(String tableName) {
		this.tableName = tableName;
	}
	
	public TableModel(String tableName, Map<String, TableMetaData> datas) {
		this.tableName = tableName;
		this.datas = datas;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Map<String, TableMetaData> getDatas() {
		return datas;
	}

	public void setDatas(Map<String, TableMetaData> datas) {
		this.datas = datas;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
