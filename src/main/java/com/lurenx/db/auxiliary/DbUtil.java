package com.lurenx.db.auxiliary;

import com.lurenx.db.auxiliary.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

/**
 * According class to generate or modify tables, if modified, if modify type and
 * varchar's length and all comment is effective for now.
 *
 * 通过扫描配置好的类路径扫描已配置注解的model类来新建、修改表，包括类型，长度及注释等，但暂时不包括索引的更改
 *
 * @author wujabon
 */
public class DbUtil {
    private DataSource dataSource;

    private static Connection conn;
    private static final Logger logger = LoggerFactory.getLogger(DbUtil.class);

    private static DbUtil dbUtil;
    //用于执行完sql后执行需要查库的操作
    private static CallBack callBack;

    private DbUtil(DataSource dataSource, CallBack callBack) {
        this.dataSource = dataSource;
        DbUtil.callBack = callBack;
    }

    private static class DbUtilInstance {
        static DbUtil getIns(DataSource dataSource, CallBack callBack) {
            if (dbUtil == null) {
                dbUtil = new DbUtil(dataSource, callBack);
            }
            return dbUtil;
        }
    }

    public static DbUtil getIns(DataSource dataSource, CallBack callBack) {
        return DbUtilInstance.getIns(dataSource, callBack);
    }

    /**
     * begin execute
     */
    public void checkClass() {
        if (dataSource == null) {
            throw new RuntimeException("Connection is null...");
        }
        try {
            conn = dataSource.getConnection();
            String basePackage = ConfigSetting.get(ConfigSetting.MODEL_PATH);
            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
            provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
            Set<BeanDefinition> classes = provider.findCandidateComponents(basePackage);
            for (BeanDefinition bean : classes) {
                Class<?> clz = Class.forName(bean.getBeanClassName());
                TableInfo tableAnn = clz.getAnnotation(TableInfo.class);
                if (tableAnn == null)
                    continue;
                analysis(clz, tableAnn, conn);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            logger.error("checkClass error", e);
        } finally {
            DDLTask.add("finished");
        }

    }

    /**
     * Analysis which is new create and which need to modify.
     *
     * @param clz      分析的class对象
     * @param tableAnn 注解
     * @param conn     数据库连接
     * @throws SQLException sql异常
     */
    private void analysis(Class<?> clz, TableInfo tableAnn, Connection conn)
            throws SQLException {
        String tableName = tableAnn.name();
        boolean exist = existTable(tableName, conn);
        DDLTask.start();
        if (!exist) {
            createTable(clz, tableName, conn, tableAnn);
        } else {
            compare(clz, tableName, conn);
        }
    }

    /****************************************************** new create table bigen *********************************************************/

    /**
     * New create table
     *
     * @param clz
     * @param tableName
     * @param conn
     * @param tableAnn
     */
    private void createTable(Class<?> clz, String tableName, Connection conn,
                             TableInfo tableAnn) {
        StringBuilder sb = new StringBuilder("CREATE TABLE `").append(tableName)
                .append("`(");
        Field[] fields = clz.getDeclaredFields();
        List<String> index = new ArrayList<String>();
        generateDDL(clz, fields, sb, index, tableName);
        sb.append(")");
        String engin = tableAnn.engin();
        if (!engin.isEmpty()) {
            sb.append("ENGINE=").append(engin);
        }
        String charSet = tableAnn.charSet();
        if (!charSet.isEmpty()) {
            sb.append(" DEFAULT CHARSET=").append(charSet);
        }
        sb.append(" COMMENT='").append(tableAnn.comment()).append("'");
        DDLTask.add(sb.toString());
        if (!index.isEmpty()) {
            DDLTask.add(index);
        }
    }

    /**
     * Generate some of new create table DDL.
     *
     * @param clz
     * @param fields
     * @param sb
     * @param index
     * @param tableName
     */
    private void generateDDL(Class<?> clz, Field[] fields, StringBuilder sb,
                             List<String> index, String tableName) {
        Annotation[] annotations;
        boolean isAdd = false;
        for (Field field : fields) {
            annotations = field.getAnnotations();
            isAdd = generateDDLByAnnotation(annotations, sb, index, tableName);
            if (isAdd)
                sb.append(",");
        }
        sb.setLength(sb.length() - 1);
    }

    /**
     * According annotation to generate DDL.
     *
     * @param annotations
     * @param sb
     * @param index
     * @param tableName
     * @return
     */
    private boolean generateDDLByAnnotation(Annotation[] annotations,
                                            StringBuilder sb, List<String> index, String tableName) {
        boolean flag = false;
        for (Annotation annotation : annotations) {
            if (annotation instanceof IntField) {
                appendIntFieldColumn(sb, (IntField) annotation, index,
                        tableName);
                flag = true;
            } else if (annotation instanceof LongField) {
                appendLongFieldColumn(sb, (LongField) annotation, index,
                        tableName);
                flag = true;
            } else if (annotation instanceof TextField) {
                appendTextFieldColumn(sb, (TextField) annotation);
                flag = true;
            } else if (annotation instanceof TinyIntField) {
                appendTinyIntFieldColumn(sb, (TinyIntField) annotation);
                flag = true;
            } else if (annotation instanceof VarcharField) {
                appendVarcharFieldColumn(sb, (VarcharField) annotation, index,
                        tableName);
                flag = true;
            } else if (annotation instanceof TimeStampField) {
                appendTimeStampColumn(sb, (TimeStampField) annotation);
                flag = true;
            } else if (annotation instanceof DecimalField) {
                appendDecimalColumn(sb, (DecimalField) annotation);
                flag = true;
            } else {
                continue;
            }
            break;
        }
        return flag;

    }

    private void appendDecimalColumn(StringBuilder sb, DecimalField annotation) {
        sb.append("`").append(annotation.name()).append("` Decimal(")
                .append(annotation.length()).append(",")
                .append(annotation.precision()).append(")");
        if (!annotation.isNull()) {
            sb.append(" NOT NULL ");
        }
        sb.append(" DEFAULT ").append(annotation.defaultValue())
                .append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /**
     * append timestamp type column
     *
     * @param sb
     * @param annotation
     */
    private void appendTimeStampColumn(StringBuilder sb,
                                       TimeStampField annotation) {
        sb.append("`").append(annotation.name()).append("` TIMESTAMP ");
        if (!annotation.isNull()) {
            sb.append(" NOT NULL ");
        }
        sb.append(" DEFAULT ").append(annotation.defaultValue())
                .append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /**
     * append varchar type column.
     *
     * @param sb
     * @param annotation
     * @param index
     * @param tableName
     */
    private void appendVarcharFieldColumn(StringBuilder sb,
                                          VarcharField annotation, List<String> index, String tableName) {
        sb.append("`").append(annotation.name()).append("` VARCHAR(")
                .append(annotation.length()).append(")");
        if (annotation.isPrimaryKey()) {
            sb.append(" PRIMARY KEY ");
        } else if (annotation.isUnique()) {
            sb.append(" UNIQUE ");
        } else if (annotation.isIndex()) {
            String name = annotation.name();
            index.add("ALTER TABLE `" + tableName + "` ADD INDEX " + tableName
                    + "_IDX_" + name + " (`" + name + "`)");
        }
        if (!annotation.isPrimaryKey()) {
            if (!annotation.isNull()) {
                sb.append(" NOT NULL ");
            }
            sb.append(" DEFAULT '").append(annotation.defaultValue())
                    .append("'");
        }
        sb.append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /**
     * append tinyint type column
     *
     * @param sb
     * @param annotation
     */
    private void appendTinyIntFieldColumn(StringBuilder sb,
                                          TinyIntField annotation) {
        sb.append("`").append(annotation.name()).append("` TINYINT(")
                .append(annotation.length()).append(")");
        if (!annotation.isNull()) {
            sb.append(" NOT NULL ");
        }
        sb.append(" DEFAULT ").append(annotation.defaultValue())
                .append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /**
     * append text type column
     *
     * @param sb
     * @param annotation
     */
    private void appendTextFieldColumn(StringBuilder sb, TextField annotation) {
        sb.append("`").append(annotation.name()).append("` TEXT ");
        if (!annotation.isNull()) {
            sb.append(" NOT NULL ");
        }
        sb.append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /**
     * append long type column.
     *
     * @param sb
     * @param annotation
     * @param index
     * @param tableName
     */
    private void appendLongFieldColumn(StringBuilder sb, LongField annotation,
                                       List<String> index, String tableName) {
        boolean autoIncrement = annotation.autoIncrement();
        boolean primaryKey = annotation.isPrimaryKey();
        boolean isNull = annotation.isNull();
        boolean unique = annotation.isUnique();
        sb.append("`").append(annotation.name()).append("` BIGINT(")
                .append(annotation.length()).append(") ");
        if (primaryKey) {
            sb.append(" PRIMARY KEY ");
        } else {
            if (!isNull) {
                sb.append(" NOT NULL ");
            }
            if (unique) {
                sb.append(" UNIQUE ");
            } else if (annotation.isIndex()) {
                String name = annotation.name();
                index.add("ALTER TABLE `" + tableName + "` ADD INDEX "
                        + tableName + "_IDX_" + name + " (`" + name + "`)");
            }
        }
        if (autoIncrement) {
            sb.append(" AUTO_INCREMENT ");
        }
        if (!primaryKey && !autoIncrement) {
            sb.append(" DEFAULT '").append(annotation.defaultValue())
                    .append("'");
        }

        sb.append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /**
     * append int type column
     *
     * @param sb
     * @param annotation
     * @param index
     * @param tableName
     */
    private void appendIntFieldColumn(StringBuilder sb, IntField annotation,
                                      List<String> index, String tableName) {
        boolean autoIncrement = annotation.autoIncrement();
        boolean primaryKey = annotation.isPrimaryKey();
        boolean isNull = annotation.isNull();
        boolean unique = annotation.isUnique();
        sb.append("`").append(annotation.name()).append("` INT(")
                .append(annotation.length()).append(") ");
        if (primaryKey) {
            sb.append(" PRIMARY KEY ");
        } else {
            if (!isNull) {
                sb.append(" NOT NULL ");
            }
            if (unique) {
                sb.append(" UNIQUE ");
            } else if (annotation.isIndex()) {
                String name = annotation.name();
                index.add("ALTER TABLE `" + tableName + "` ADD INDEX "
                        + tableName + "_IDX_" + name + " (`" + name + "`)");
            }
        }
        if (autoIncrement) {
            sb.append(" AUTO_INCREMENT ");
        }
        if (!primaryKey && !autoIncrement) {
            sb.append(" DEFAULT '").append(annotation.defaultValue())
                    .append("'");
        }

        sb.append(" COMMENT '").append(annotation.comment()).append("'");
    }

    /****************************************************** new create table end ********************************************************/

    /****************************************************** compare member and table begin ********************************************************/

    /**
     * Compare with class and table begin
     *
     * @param clz
     * @param tableName
     * @param conn
     * @throws SQLException
     */
    private void compare(Class<?> clz, String tableName, Connection conn)
            throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet primaryKeys = metaData.getPrimaryKeys(null, "%", tableName);
        Map<String, Integer> indexes = new HashMap<String, Integer>();
        while (primaryKeys.next()) {
            indexes.put(primaryKeys.getString("COLUMN_NAME"), 1);
        }

        ResultSet indexInfo = metaData.getIndexInfo(null, "%", tableName,
                false, false);
        while (indexInfo.next()) {
            String columnName = indexInfo.getString("COLUMN_NAME");
            String indexName = indexInfo.getString("INDEX_NAME");
            if (indexes.containsKey(columnName))
                continue;
            if (indexInfo.getBoolean("NON_UNIQUE")) {
                indexes.put(columnName, 3);
            } else if (indexName != null) {
                indexes.put(columnName, 2);
            }
        }

        ResultSet columns = metaData.getColumns(null, "%", tableName, "%");
        TableModel model = new TableModel(tableName);
        Map<String, TableMetaData> datas = new HashMap<String, TableMetaData>();
        model.setDatas(datas);
        String cn;
        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            int len = columns.getInt("COLUMN_SIZE");
            TableMetaData data = new TableMetaData();
            cn = columnName.toLowerCase();
            data.setColumnName(cn);
            data.setComment(columns.getString("REMARKS"));
            data.setDefaultValue(columns.getString("COLUMN_DEF"));
            data.setLength(len);
            data.setNullable(columns.getInt("NULLABLE") == 0);
            data.setType(columns.getInt("DATA_TYPE"));
            Integer indexType = indexes.get(columnName);
            data.setIndexType(indexType == null ? 0 : indexType);
            datas.put(cn, data);
        }

        ResultSet tables = metaData.getTables(null, "%", tableName,
                new String[]{"TABLE"});
        if (tables.next()) {
            //must be set parameter at jdbc url "useInformationSchema=true"
            model.setComment(tables.getString("REMARKS"));
        }
        List<String> sqls = compare(clz, tableName, conn, model);
        // DDLTask.add(sb.toString());
        logger.info("sqls  : " + sqls);
        if (!sqls.isEmpty()) {
            DDLTask.add(sqls);
        }
    }

    /**
     * compare table of comment, columns, if table is not exist, then drop it.
     *
     * @param clz
     * @param tableName
     * @param conn
     * @param model
     * @return
     */
    private List<String> compare(Class<?> clz, String tableName,
                                 Connection conn, TableModel model) {
        List<String> sqls = new ArrayList<String>();

        // table comment
        TableInfo tableAnn = clz.getAnnotation(TableInfo.class);
        String orgComment = model.getComment();
        String newComment = tableAnn.comment();
        if (commentNotSame(orgComment, newComment)) {
            sqls.add("ALTER TABLE " + tableName + " COMMENT '"
                    + tableAnn.comment() + "'");
        }

        // columns
        Map<String, TableMetaData> datas = model.getDatas();
        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields) {
            Annotation[] annotations = field.getAnnotations();
            for (Annotation ann : annotations) {
                compareColumn(tableName, ann, datas, sqls);
            }
        }

        if (!datas.isEmpty()) {
            // drop columns
            addDropSql(sqls, datas, tableName);
        }
        return sqls;
    }

    private void addDropSql(List<String> sqls, Map<String, TableMetaData> datas,
                            String tableName) {
        Iterator<String> ite = datas.keySet().iterator();
        StringBuilder sb = new StringBuilder();
        while (ite.hasNext()) {
            sb.setLength(0);
            sqls.add(sb.append("ALTER TABLE ").append(tableName)
                    .append(" DROP COLUMN `").append(ite.next()).append("`;").toString());
        }
    }

    private void compareColumn(String tableName, Annotation ann,
                               Map<String, TableMetaData> datas, List<String> sqls) {
        if (ann instanceof IntField) {
            comparatNumberType(tableName, (IntField) ann, datas, sqls,
                    Types.INTEGER);
        } else if (ann instanceof LongField) {
            comparatNumberType(tableName, (LongField) ann, datas, sqls,
                    Types.BIGINT);
        } else if (ann instanceof TextField) {
            comparatTextType(tableName, (TextField) ann, datas, sqls,
                    Types.LONGVARCHAR);
        } else if (ann instanceof TinyIntField) {
            comparatNumberType(tableName, (TinyIntField) ann, datas, sqls,
                    Types.TINYINT);
        } else if (ann instanceof VarcharField) {
            comparatVarcharField(tableName, (VarcharField) ann, datas, sqls,
                    Types.VARCHAR);
        } else if (ann instanceof TimeStampField) {
            comparatTimeStampField(tableName, (TimeStampField) ann, datas,
                    sqls, Types.TIMESTAMP);
        } else if (ann instanceof DecimalField) {
            comparatNumberType(tableName, (DecimalField) ann, datas, sqls,
                    Types.DECIMAL);
        }
    }

    private void comparatNumberType(String tableName, DecimalField ann,
                                    Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            appendAdd(sb, tableName, name, "DECIMAL", ann.length(), ann.defaultValue(), ann.comment(), ann.precision());
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), type)) {
                modifyLength(sb, tableName, name, "DECIMAL", ann.length(), ann.comment(), ann.precision());
                sqls.add(sb.toString());
            }
        }
    }

    private void comparatTimeStampField(String tableName, TimeStampField ann,
                                        Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            appendAdd(sb, tableName, name, "TIMESTAMP", 6, ann.defaultValue(), ann.comment());
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), type)) {
                modifyLength(sb, tableName, name, "TIMESTAMP", 6, ann.comment());
                sqls.add(sb.toString());
            }
        }
    }

    private void comparatVarcharField(String tableName, VarcharField ann,
                                      Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            appendAdd(sb, tableName, name, "VARCHAR", ann.length(),
                    ann.defaultValue(), ann.comment());
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

            if (ann.isPrimaryKey()) {
                appendPK(sb, tableName, name);
                sqls.add(sb.toString());
            }
            if (ann.isUnique()) {
                appendUnique(sb, tableName, name);
                sqls.add(sb.toString());
            }
            if (ann.isIndex()) {
                appendIndex(sb, tableName, name);
                sqls.add(sb.toString());
            }
        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (ann.length() != data.getLength()
                    || commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), type)) {
                modifyLength(sb, tableName, name, "VARCHAR", ann.length(),
                        ann.comment());
                sqls.add(sb.toString());
            }
        }
    }

    private void comparatTextType(String tableName, TextField ann,
                                  Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            sb.append("ALTER TABLE ").append(tableName).append(" ADD COLUMN `")
                    .append(name).append("` TEXT ");
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), type)) {
                sb.setLength(0);
                sb.append("ALTER TABLE ").append(tableName)
                        .append(" MODIFY COLUMN `").append(name)
                        .append("` TEXT COMMENT '").append(newComment)
                        .append("'");
                sqls.add(sb.toString());
            }
        }
    }

    /**
     * Number cannot support modify length.
     *
     * @param tableName
     * @param ann
     * @param datas
     * @param sqls
     * @param type
     */
    private void comparatNumberType(String tableName, TinyIntField ann,
                                    Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            appendAdd(sb, tableName, name, "TINYINT", ann.length(),
                    ann.defaultValue(), ann.comment());
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), Types.TINYINT)) {
                modifyLength(sb, tableName, name, "TINYINT", ann.length(),
                        ann.comment());
                sqls.add(sb.toString());
            }
        }
    }

    /**
     * Number cannot support modify length.
     *
     * @param tableName
     * @param ann
     * @param datas
     * @param sqls
     * @param type
     */
    private void comparatNumberType(String tableName, LongField ann,
                                    Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            appendAdd(sb, tableName, name, "BIGINT", ann.length(),
                    ann.defaultValue(), ann.comment());
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            if (ann.autoIncrement()) {
                sb.append("AUTO_INCREMENT");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

            if (ann.isPrimaryKey()) {
                appendPK(sb, tableName, name);
                sqls.add(sb.toString());
            }
            if (ann.isUnique()) {
                appendUnique(sb, tableName, name);
                sqls.add(sb.toString());
            }
            if (ann.isIndex()) {
                appendIndex(sb, tableName, name);
                sqls.add(sb.toString());
            }
        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), type)) {
                modifyLength(sb, tableName, name, "BIGINT", ann.length(),
                        ann.comment());
                sqls.add(sb.toString());
            }
        }
    }

    /**
     * Number cannot support modify length
     *
     * @param tableName
     * @param ann
     * @param datas
     * @param sqls
     * @param type
     */
    private void comparatNumberType(String tableName, IntField ann,
                                    Map<String, TableMetaData> datas, List<String> sqls, int type) {
        String name = ann.name().toLowerCase();
        TableMetaData data = datas.remove(name);
        StringBuilder sb = new StringBuilder();
        if (data == null) {
            appendAdd(sb, tableName, name, "INT", ann.length(),
                    ann.defaultValue(), ann.comment());
            if (!ann.isNull()) {
                sb.append(" NOT NULL ");
            }
            if (ann.autoIncrement()) {
                sb.append("AUTO_INCREMENT");
            }
            sb.append(" COMMENT '").append(ann.comment()).append("'");
            sqls.add(sb.toString());

            if (ann.isPrimaryKey()) {
                appendPK(sb, tableName, name);
                sqls.add(sb.toString());
            }
            if (ann.isUnique()) {
                appendUnique(sb, tableName, name);
                sqls.add(sb.toString());
            }
            if (ann.isIndex()) {
                appendIndex(sb, tableName, name);
                sqls.add(sb.toString());
            }
        } else {
            String newComment = ann.comment();
            String orgComment = data.getComment();
            if (commentNotSame(orgComment, newComment) || typeNotTheSame(data.getType(), type)) {
                modifyLength(sb, tableName, name, "INT", ann.length(),
                        ann.comment());
                sqls.add(sb.toString());
            }
        }
    }

    private boolean typeNotTheSame(int oldType, int newType) {
        return oldType != newType;
    }

    /**
     * Judge the comment is not the same
     *
     * @param orgComment
     * @param newComment
     * @return
     */
    private boolean commentNotSame(String orgComment, String newComment) {
        return (orgComment != null && !orgComment.equals(newComment))
                || (newComment != null && !newComment.equals(orgComment));
    }

    /**
     * If column length is modified
     *
     * @param sb
     * @param tableName
     * @param columnName
     * @param type
     * @param length
     * @param comment
     */
    private void modifyLength(StringBuilder sb, String tableName,
                              String columnName, String type, int length, String comment, int... precision) {
        sb.setLength(0);
        sb.append("ALTER TABLE ").append(tableName).append(" MODIFY COLUMN `")
                .append(columnName).append("` ").append(type);
        if ("DECIMAL".equals(type)) {
            sb.append("(").append(length).append(",").append(precision != null && precision.length > 0 ? precision[0] : 0).append(")");

        } else if (!"TIMESTAMP".equals(type)) {
            sb.append("(").append(length).append(")");
        }
        sb.append(" COMMENT '").append(comment).append("'");

    }

    /**
     * Add a column ddl
     *
     * @param sb
     * @param tableName
     * @param columnName
     * @param type
     * @param length
     * @param defaultValue
     */
    private void appendAdd(StringBuilder sb, String tableName,
                           String columnName, String type, int length, Object defaultValue, String comment, int... precision) {
        sb.setLength(0);
        sb.append("ALTER TABLE ").append(tableName).append(" ADD COLUMN `")
                .append(columnName).append("` ").append(type);
        if ("DECIMAL".equals(type)) {
            sb.append("(").append(length).append(",").append(precision != null && precision.length > 0 ? precision[0] : 0).append(")");
        } else if (!"TIMESTAMP".equals(type)) {
            sb.append("(").append(length).append(")");
        }
        sb.append(" DEFAULT ");
        if (defaultValue instanceof String && !"TIMESTAMP".equals(type)) {
            sb.append("'").append(defaultValue).append("'");
        } else {
            sb.append(defaultValue);
        }
    }

    /**
     * append primary key sql syntax
     *
     * @param sb
     * @param tableName
     * @param columnName
     */
    private void appendPK(StringBuilder sb, String tableName, String columnName) {
        sb.setLength(0);
        sb.append("ALTER TABLE ").append(tableName).append(" ADD PRIMARY KEY(")
                .append(columnName).append(")");
    }

    /**
     * append unique key sql syntax
     *
     * @param sb
     * @param tableName
     * @param columnName
     */
    private void appendUnique(StringBuilder sb, String tableName,
                              String columnName) {
        sb.setLength(0);
        sb.append("ALTER TABLE ").append(tableName).append(" ADD UNIQUE ")
                .append(tableName).append("_UK_").append(columnName)
                .append("(").append(columnName).append(")");
    }

    /**
     * append index sql syntax
     *
     * @param sb
     * @param tableName
     * @param columnName
     */
    private void appendIndex(StringBuilder sb, String tableName,
                             String columnName) {
        sb.setLength(0);
        sb.append("ALTER TABLE ").append(tableName).append(" ADD INDEX ")
                .append(tableName).append("_IDX_").append(columnName)
                .append("(").append(columnName).append(")");
    }

    /****************************************************** compare member and table end ********************************************************/

    /**
     * Judge the table is exist
     *
     * @param tableName
     * @param conn
     * @return true table is exist in the db, otherwise not.
     */
    private boolean existTable(String tableName, Connection conn) {
        DatabaseMetaData metaData;
        try {
            metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, "%", tableName,
                    new String[]{"TABLE"});
            return tables.next();
        } catch (SQLException e) {
            logger.error("existTable error.", e);
        }
        return false;
    }

    /**
     * executor the ddl thread.
     *
     * @author dc
     */
    private static class DDLTask {
        static LinkedBlockingQueue<Object> sqls = new LinkedBlockingQueue<Object>(
                1000);
        static volatile boolean started = false;

        static ExecutorService es;

        //static CountDownLatch cdl = new CountDownLatch(1);

        /**
         * Add object to the queue
         *
         * @param obj must instance of String or List<String>
         */
        public static void add(Object obj) {
            try {
                sqls.put(obj);
            } catch (InterruptedException e) {
                logger.error("Method add in DDLTask error.", e);
            }
        }

        /**
         * Start the executor.
         */
        public static void start() {
            if (!started) {
                started = true;
                es = Executors.newSingleThreadScheduledExecutor();
                es.execute(new Runnable() {
                    public void run() {
                        try {
                            startDo();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }

        /**
         * Stop the executor
         */
        private static void stop() {
            if (es != null && !es.isShutdown()) {
                es.shutdown();
            }
        }

        /**
         * Login of executor
         *
         * @throws InterruptedException
         * @throws Exception
         */
        @SuppressWarnings("rawtypes")
        protected static void startDo() throws InterruptedException, Exception {
            Statement statement = null;
            while (started) {
                if (conn == null || conn.isClosed()) {
                    throw new RuntimeException("conn is null or closed.");
                }

                statement = conn.createStatement();
                try {
                    Object obj = sqls.take();
                    if ("finished".equals(obj)) {
                        //可适当调小
                        Thread.sleep(3000);
                        finished();
                        return;
                    }
                    if (obj instanceof String) {
                        logger.info("execute sql : " + obj);
                        statement.execute((String) obj);
                    } else if (obj instanceof List) {
                        for (Object sql : (List) obj) {
                            logger.info("execute sql : " + obj);
                            statement.execute((String) sql);
                        }
                    }
                } finally {
                    if (statement != null && !statement.isClosed())
                        statement.close();
                }
            }
        }

        /**
         * If finished, the close the db connection, and stop the thread.
         *
         * @throws SQLException
         */
        private static void finished() throws SQLException {
            conn.close();
            stop();
            started = false;
            logger.info("Finish execute.");
            callBack.callBack();
        }
    }
}
