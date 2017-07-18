package com.lurenx.db.auxiliary;

import com.zhaijuzhe.util.db.dbutil.annotation.PrimaryKeyInfo;

/**
 * 主键处理接口
 * Created by wujabon on 2017/7/14.
 */
public interface ProcessPrimaryKeyCallback {
    void process(PrimaryKeyInfo ann);
}
