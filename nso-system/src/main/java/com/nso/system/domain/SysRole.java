package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId private Long id;
    private Long tenantId;
    private String roleCode;
    private String roleName;
    private String status;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
