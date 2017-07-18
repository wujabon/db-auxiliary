package com.lurenx.db.auxiliary.annotation;


import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeStampField {
	String name();
	String comment();
	boolean isNull() default false;
	/**
	 * default is <p>CURRENT_TIMESTAMP<p>
	 * @return
	 */
	String defaultValue() default "CURRENT_TIMESTAMP";
	/**
	 * 1-9
	 * select 时用到
	 * @return
	 */
	int selectLevel() default SelectLevelConst.LEVEL_ONE;

}
