package com.lurenx.db.auxiliary.annotation;


import com.lurenx.db.auxiliary.SelectLevelConst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于创建int字段的注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntField {
    /**
     * 列名
     *
     * @return
     */
    String name();

    /**
     * 默认长度11
     *
     * @return
     */
    int length() default 11;

    /**
     * 列注释
     *
     * @return
     */
    String comment();

    /**
     * 默认值0
     *
     * @return
     */
    int defaultValue() default 0;

    /**
     * 默认非空
     *
     * @return
     */
    boolean isNull() default false;

    /**
     * 默认非自增长
     *
     * @return
     */
    boolean autoIncrement() default false;

    /**
     * 默认非索引
     *
     * @return
     */
    boolean isIndex() default false;

    /**
     * 默认非unique键
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
     * 等级1-9，1是最高优先等级
     *
     * @return
     */
    int selectLevel() default SelectLevelConst.LEVEL_ONE;
}
