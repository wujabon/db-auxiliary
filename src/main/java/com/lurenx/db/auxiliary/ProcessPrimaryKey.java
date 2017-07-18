package com.lurenx.db.auxiliary;

import com.zhaijuzhe.util.db.dbutil.annotation.PrimaryKeyInfo;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 处理主键，用于使用第三方缓存工具生成主键类
 * Created by wujabon on 2017/7/14.
 */
public class ProcessPrimaryKey {
    private static ProcessPrimaryKey ppk;
    /**
     * 扫描后回调处理
     */
    private ProcessPrimaryKeyCallback callback;

    private ProcessPrimaryKey(ProcessPrimaryKeyCallback callback){
        this.callback = callback;
    }
    private static class ProcessPrimaryKeyIns{
        static ProcessPrimaryKey getIns(ProcessPrimaryKeyCallback callback) {
            if(ppk == null) {
                ppk = new ProcessPrimaryKey(callback);
            }
            return ppk;
        }

    }

    public static ProcessPrimaryKey getIns(ProcessPrimaryKeyCallback callback) {
        return ProcessPrimaryKeyIns.getIns(callback);
    }

    /**
     *
     * @throws Exception
     */
    public void initKey() throws Exception {
        String basePackage = ConfigSetting.get(ConfigSetting.MODEL_PATH);
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        Set<BeanDefinition> classes = provider.findCandidateComponents(basePackage);
        PrimaryKeyInfo ann;
        for (BeanDefinition bean : classes) {
            Class<?> clz = Class.forName(bean.getBeanClassName());
            ann = clz.getAnnotation(PrimaryKeyInfo.class);
            if(ann == null) continue;
            callback.process(ann);

        }
    }
}
