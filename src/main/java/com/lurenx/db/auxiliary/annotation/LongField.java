package com.lurenx.db.auxiliary.annotation;


import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于创建bigint的字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LongField {
    /**
     * 列名
     *
     * @return
     */
    String name();

    /**
     * 长度默认16
     *
     * @return
     */
    int length() default 16;

    /**
     * 列注释
     *
     * @return
     */
    String comment();

    /**
     * 默认非自增
     *
     * @return
     */
    boolean autoIncrement() default false;

    /**
     * 默认非空
     *
     * @return
     */
    boolean isIndex() default false;

    /**
     * 默认非唯一键
     *
     * @return
     */
    boolean isUnique() default false;

    /**
     * 默认非主键
     *
     * @return
     */
    boolean isPrimaryKey() default false;

    /**
     * 默认非空
     *
     * @return
     */
    boolean isNull() default false;

    /**
     * 默认值0
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
