package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 工艺路线实体。
@Data
@TableName("nso_process_route")
public class ProcessRoute {

    // 工艺路线编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 路线单号
    private String routeNo;

    // 版本号
    private String versionNo;

    // 绑定文档版本编号
    private Long boundDocVersionId;

    // 发布状态
    private String status;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
