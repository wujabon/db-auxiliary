package com.lurenx.db.auxiliary.annotation;

import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于创建Decimal字段的注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DecimalField {
	/**
	 * 列名
	 * @return
	 */
	String name();
	/**
	 * 默认长度10
	 * @return
	 */
	int length() default 10;

	/**
	 * 默认精度2
	 * @return
	 */
	int precision() default 2;

	/**
	 * 列注释
	 * @return
	 */
	String comment();
	/**
	 * 默认非空
	 * @return
	 */
	boolean isNull() default false;
	/**
	 * 默认值0
	 * @return
	 */
	double defaultValue() default 0;

	/**
	 * 等级1-9，1是最高优先等级
	 @return
	 */
	int selectLevel() default SelectLevelConst.LEVEL_ONE;

}
