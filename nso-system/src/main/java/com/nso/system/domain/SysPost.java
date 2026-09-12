package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// 岗位对象，对应表 {@code sys_post}。
@Data
@TableName("sys_post")

public class SysPost {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 岗位编码。
    private String postCode;
    // 岗位名称。
    private String postName;
    // 排序号。
    private Integer sortNo;
    // 状态。
    private String status;
    @TableLogic
    // 删除标志（0 未删除，1 已删除）。
    private Integer deleted;
}
