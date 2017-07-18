package com.zhaijuzhe.util.db.dbutil.annotation



/**
 * 该类主要用于第三方如reidis生成主键时用到
 * name为设置的主键key，value为主键开始的值
 * Created by wujabon on 2017/7/14.
 */
@Target(AnnotationTarget.CLASS)
@Retention
public annotation class PrimaryKeyInfo(val name : String, val value : Long)

/**
 * 用于建表时创建聚合索引
 * name 索引名称，columnNames 表列名
 */
@Target(AnnotationTarget.CLASS)
@Retention
public annotation class AggregateIndex(val name : String, val columnNames : Array<String>)


