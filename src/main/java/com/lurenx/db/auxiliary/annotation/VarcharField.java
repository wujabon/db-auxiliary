package com.lurenx.db.auxiliary.annotation;


import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VarcharField {
	String name();
	int length();
	String comment();
	/**
	 * default value is empty string.
	 * @return
	 */
	String defaultValue() default "";
	/**
	 * default is not null.
	 * @return
	 */
	boolean isNull() default false;
	/**
	 * default is false.
	 * @return
	 */
	boolean isIndex() default false;
	/**
	 * default is false.
	 * @return
	 */
	boolean isUnique() default false;
	/**
	 * default is false.
	 * @return
	 */
	boolean isPrimaryKey() default false;
	/**
	 * 1-9
	 * select 时用到
	 * @return
	 */
	int selectLevel() default SelectLevelConst.LEVEL_ONE;
}
