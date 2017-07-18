# db-auxiliary

介绍
---

- db-auxiliary定位于在项目启动时扫描数据库表结构，根据Java里面的bean的注解更改表结构；旨在减少字段更改对运维的依赖，类似于jpa配置了hbm2ddl.auto=true；
- 与jpa建表不同的是简化了配置字段的长度与注释等，以及预留利用第三方如redis生成主键的接口。
- TableOperateAuxiliay可以简单生成一些简单的dml语句；
- db-auxiliary暂时只支持MySql数据库，并且支持的类型暂时只有7种。


