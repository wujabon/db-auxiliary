package com.lurenx.db.auxiliary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * 配置文件设置
 */
public class ConfigSetting {
	private static Properties prop = new Properties();
	public static final String MODEL_PATH = "model.path";
	private static final Logger logger = LoggerFactory.getLogger(ConfigSetting.class);
	private static final String FILE_NAME = "auxiliary.properties";
	
	static {
		try {
			String filePath = System.getProperty("user.dir") + FILE_NAME;
			File file = new File(filePath);
			if(!file.exists()) {
				filePath = ConfigSetting.class.getClassLoader().getResource(FILE_NAME).getFile();
			}
			InputStream in = new BufferedInputStream(new FileInputStream(filePath));
			prop.load(ConfigSetting.class.getClassLoader().getResourceAsStream(FILE_NAME));
		} catch (IOException e) {
			logger.error("Read config file error.", e);
		}
	}
	
	public static String get(String key) {
		return prop.getProperty(key);
	}

}
