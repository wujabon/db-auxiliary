package com.lurenx.db.auxiliary.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表信息注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TableInfo {
	/**
	 * 表名
	 * @return
	 */
	String name();
	/**
	 * 表注释
	 * @return
	 */
	String comment();
	/**
	 * 默认innoDB引擎
	 * @return
	 */
	String engin() default "InnoDB";
	/**
	 * 默认utf8的编码
	 * @return
	 */
	String charSet() default "utf8";
}
