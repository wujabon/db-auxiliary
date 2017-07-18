package com.lurenx.db.auxiliary.annotation;


import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * tinyint 列
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TinyIntField {
    /**
     * 列名
     *
     * @return
     */
    String name();

    /**
     * 列默认长度 4
     *
     * @return
     */
    int length() default 4;

    String comment();

    /**
     * 非空
     *
     * @return
     */
    boolean isNull() default false;

    /**
     * 默认值为0
     *
     * @return
     */
    int defaultValue() default 0;

    /**
     * 等级1-9，1是最高优先等级
     *
     * @return
     */
    int selectLevel() default SelectLevelConst.LEVEL_ONE;
}
