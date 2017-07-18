package com.lurenx.db.auxiliary;

/**
 * 表元数据
 */
public class TableMetaData {
	/**
	 * 列名
	 */
	private String columnName;
	/**
	 * 数据类型，参见java.sql.Types
	 */
	private int type;
	/**
	 * true 可空，false 不可空
	 */
	private boolean nullable;
	/**
	 * 注释
	 */
	private String comment;
	/**
	 * 长度
	 */
	private int length;
	/**
	 * 默认值
	 */
	private String defaultValue;
	/**
	 * 0-normal,
	 * 1-primaryKey,
	 * 2-unique,
	 * 3-normal index.
	 */
	private int indexType;

	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public boolean isNullable() {
		return nullable;
	}
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public String getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	public int getIndexType() {
		return indexType;
	}
	public void setIndexType(int indexType) {
		this.indexType = indexType;
	}
	
}
