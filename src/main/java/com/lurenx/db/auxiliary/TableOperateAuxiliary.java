package com.lurenx.db.auxiliary;

import com.lurenx.db.auxiliary.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 表操作辅助类
 */
public class TableOperateAuxiliary {

	/**
	 * 根据selectLevel 来生成搜索列表，并且没有带任何where条件
	 * @param clz 需要生成class对象
	 * @param level 等级
	 * @return select 语句
	 */
	public static String getSelectByLevel(Class<?> clz, int level) {
		StringBuilder sb = new StringBuilder("select ");
		String tableName = getTableName(clz);
		if(level >= SelectLevelConst.LEVEL_NINE) {
			sb.append(" * from ").append(tableName);
			return sb.toString();
		}else {
			Field[] fields = clz.getDeclaredFields();
			boolean nonSemicolon = true;
			for(Field field : fields) {
				String name = getColumnAndNameByLevel(field, level);
				if(name != null) {
					if(nonSemicolon) {
						sb.append(" ").append(name).append(" ");
						nonSemicolon = false;
					}else {
						sb.append(",").append(name).append(" ");
					}
				}
			}
			if(nonSemicolon) {
				sb.append(" * ");
			}
		}
		sb.append(" from ").append(tableName);
		return sb.toString();
	}

	private static String getColumnAndNameByLevel(Field field, int level) {
		Annotation[] anns = field.getAnnotations();
		String name = null;
		int selectLevel = 1;
		for(Annotation ann : anns) {
			if(ann instanceof IntField) {
				name = ((IntField) ann).name();
                selectLevel = ((IntField) ann).selectLevel();
			}else if(ann instanceof LongField) {
				name = ((LongField) ann).name();
                selectLevel = ((LongField) ann).selectLevel();
            }else if(ann instanceof VarcharField) {
				name = ((VarcharField) ann).name();
                selectLevel = ((VarcharField) ann).selectLevel();
			}else if(ann instanceof TimeStampField) {
				name = ((TimeStampField) ann).name();
                selectLevel = ((TimeStampField) ann).selectLevel();
			}else if(ann instanceof DecimalField) {
				name = ((DecimalField) ann).name();
                selectLevel = ((DecimalField) ann).selectLevel();
			}else if(ann instanceof TextField) {
                TextField tAnn = (TextField) ann;
                name =  tAnn.name();
                selectLevel = tAnn.selectLevel();
			}else if(ann instanceof TinyIntField) {
				name =  ((TinyIntField) ann).name();
                selectLevel = ((TinyIntField) ann).selectLevel();
			}
		}
		if(selectLevel > level) return null;
		return name;
	}

	private static String getTableName(Class<?> clz) {
		TableInfo ann = clz.getDeclaredAnnotation(TableInfo.class);
		if(ann == null) {
			return clz.getSimpleName();
		}
		return ann.name();
	}

	/**
	 * 生成insert语句，将忽略空值字段。
	 * @param t 需要插入的对象
	 * @param <T> T类
	 * @return insert语句
	 */
	public static <T> Object[] getInsertByNotNull(T t) throws Exception {
		Class<?> clz = t.getClass();
		TableInfo ann = clz.getDeclaredAnnotation(TableInfo.class);
		if(ann == null) {
			throw new RuntimeException("no TableInfo annotation");
		}
		String tableName = ann.name();
		StringBuilder sb = new StringBuilder("insert into ").append(tableName).append(" (");
		StringBuilder values = new StringBuilder(" values (");
		Field[] fields = clz.getDeclaredFields();
        List<AuxiliaryModel> models = getAuxiliaryModels(fields);

        String fieldName;
        List<Object> args = new ArrayList<Object>();
        boolean nonSemicolon = true;
        for(AuxiliaryModel model : models) {
            fieldName = model.getFieldName();
            Object obj = clz.getMethod("get" + upperCaseFirstName(fieldName)).invoke(t);
            if(obj == null) continue;
            if(nonSemicolon) {
                sb.append(model.columnName);
                values.append("?");
                nonSemicolon = false;
            }else {
                sb.append(",").append(model.columnName);
                values.append(", ?");
            }
            args.add(obj);
        }
        if(nonSemicolon) throw new RuntimeException("no insert subject");
        sb.append(")").append(values).append(")");
		return new Object[]{sb.toString(), args};
	}

	/**
	 * 生成update操作，当字段为null时，将自动忽略。
	 * @param t 需要更新的对象
	 * @param <T> T类
	 * @return update语句
	 */
	public static <T> Object[] getUpdateByNotNull(T t) throws Exception {
        Class<?> clz = t.getClass();
        TableInfo ann = clz.getDeclaredAnnotation(TableInfo.class);
        if(ann == null) {
            throw new RuntimeException("no TableInfo annotation");
        }
        String tableName = ann.name();
        StringBuilder sb = new StringBuilder("update ").append(tableName).append(" set ");
        Field[] fields = clz.getDeclaredFields();
        List<AuxiliaryModel> models = getAuxiliaryModels(fields);

        String fieldName;
        List<Object> args = new ArrayList<Object>();
        boolean nonSemicolon = true;
        Object id = null;
        for(AuxiliaryModel model : models) {
            fieldName = model.getFieldName();
            Object obj = clz.getMethod("get" + upperCaseFirstName(fieldName)).invoke(t);
            if(obj == null) continue;
            if("id".equals(fieldName)) id = obj;
            if(nonSemicolon) {
                sb.append(model.columnName).append(" = ").append("?");
                nonSemicolon = false;
            }else {
                sb.append(",").append(model.columnName).append(" = ").append("?");
            }
            args.add(obj);
        }
        if(nonSemicolon) throw new RuntimeException("no update subject");
        sb.append(" where id = ? ");
        args.add(id);
        return new Object[]{sb.toString(), args};
	}
	
	/**
	 * 根据表数据生成一些格式东西
	 * @param clz
	 * @param type 0 insert 语句； 1 mybatis xml数据库字段与model类相对应；2 生成update语句
	 * @return
	 */
	public static String generateFormatStrByTable(Class<?> clz, int type) {
		Annotation[] annotations = clz.getAnnotations();
		String tableName =null;
		boolean flag = true;
		for(Annotation ann : annotations) {
			//System.out.println(ann);
			if(ann instanceof TableInfo) {
				tableName = ((TableInfo)ann).name();
				flag = false;
				break;
			}
		}
		if(flag) return "";
		Field[] fields = clz.getDeclaredFields();
        List<AuxiliaryModel> models = getAuxiliaryModels(fields);
		switch(type) {
		case 0:
			return generateInsert(tableName, models);
		case 1:
			return generateXML(clz, models);
		case 2:
			return generateUpdate(tableName, models);
		}
		return generateInsert(tableName, models);
	}

    private static List<AuxiliaryModel> getAuxiliaryModels(Field[] fields) {
        List<AuxiliaryModel> models = new ArrayList<AuxiliaryModel>();
        for(Field field : fields) {
            Annotation[] anns = field.getAnnotations();
            AuxiliaryModel model = getColumnAndName(anns, field);
            if(model == null) continue;
            models.add(model);
        }
        return models;
    }

	/**
	 * 生成mybatis的update语句
	 * @param tableName
	 * @param models
	 * @return
	 */
    private static String generateUpdate(String tableName,
			List<AuxiliaryModel> models) {
		StringBuilder sb = new StringBuilder("update ").append(tableName).append(" set ");
		int size = models.size() - 1;
		AuxiliaryModel model;
		for(int i = 0; i <= size; i ++) {
			model = models.get(i);
			sb.append(model.getColumnName()).append("=#{").append(model.getFieldName()).append("}");
			if(i != size) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * 生成mybatis的xml文件
	 * @param clz
	 * @param models
	 * @return
	 */
	private static String generateXML(Class<?> clz,
			List<AuxiliaryModel> models) {
		StringBuilder sb = new StringBuilder("<resultMap type=\"");
		sb.append(clz.getName()).append("\" id=\"");
		String simpleName = clz.getSimpleName();
		char firstName = simpleName.charAt(0);
		if(firstName <= 90 && firstName >= 65) {
			sb.append((char)(firstName + 32)).
			append(simpleName.substring(1));
		}else {
			sb.append(simpleName);
		}
		sb.append("\">\n");
		sb.append("<id column=\"id\" property=\"id\"/>\n");
		for(AuxiliaryModel model : models) {
			sb.append("<result column=\"").append(model.getColumnName()).append("\"");
			sb.append(" property=\"").append(model.getFieldName()).append("\"/>\n");
		}
		sb.append("</resultMap>");
		return sb.toString();
	}

	/**
	 * 生成mybatis的insert语句
	 * @param tableName
	 * @param models
	 * @return
	 */
	private static String generateInsert(String tableName,
			List<AuxiliaryModel> models) {
		StringBuilder sb = new StringBuilder("insert into ").append(tableName).append("(");
		StringBuilder end = new StringBuilder(") values(");
		int len = models.size();
		AuxiliaryModel model;
		for(int i = 0; i < len; i ++) {
			model = models.get(i);
			sb.append(model.getColumnName());
			end.append("#{").append(model.getFieldName()).append("}");
			if(i == len - 1) {
				break;
			}
			sb.append(",");
			end.append(",");
		}
		sb.append(end).append(")");
		return sb.toString();
	}

	/**
	 * 获取列名及对应model类的字段名
	 * @param anns
	 * @param field
	 * @return
	 */
	private static AuxiliaryModel getColumnAndName(Annotation[] anns, Field field) {
		for(Annotation ann : anns) {
			if(ann instanceof DecimalField) {
				return new AuxiliaryModel(((DecimalField)ann).name(), field.getName());
			}else if(ann instanceof IntField) {
				return new AuxiliaryModel(((IntField)ann).name(), field.getName());
			}else if (ann instanceof LongField) {
				return new AuxiliaryModel(((LongField)ann).name(), field.getName());
			}else if(ann instanceof TextField) {
				return new AuxiliaryModel(((TextField)ann).name(), field.getName());
			}else if(ann instanceof TimeStampField) {
				return new AuxiliaryModel(((TimeStampField)ann).name(), field.getName());
			}else if(ann instanceof TinyIntField) {
				return new AuxiliaryModel(((TinyIntField)ann).name(), field.getName());
			}else if(ann instanceof VarcharField) {
				return new AuxiliaryModel(((VarcharField)ann).name(), field.getName());
			}
		}
		return null;
	}

	/**
	 * 首字母大写
	 * @param name
	 * @return
	 */
    public static String upperCaseFirstName(String name) {
        char charAt = name.charAt(0);
        if (charAt >= 'a' && charAt <= 'z') {
            charAt -= 32;
            return (char) charAt + name.substring(1);
        }
        return name;
    }

	/**
	 * 辅助类
	 */
	static class AuxiliaryModel{
		private String columnName;
		private String fieldName;
		public AuxiliaryModel(String columnName, String fieldName) {
			this.columnName = columnName;
			this.fieldName = fieldName;
		}
		public String getColumnName() {
			return columnName;
		}
		public String getFieldName() {
			return fieldName;
		}
	}

}
