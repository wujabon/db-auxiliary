package com.lurenx.db.auxiliary.annotation;


import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类型为text列
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TextField {
	/**
	 * 列名
	 * @return
	 */
	String name();

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
	 * 等级1-9，1是最高优先等级
	 *
	 * @return
	 */
	int selectLevel() default SelectLevelConst.LEVEL_ONE;
}
