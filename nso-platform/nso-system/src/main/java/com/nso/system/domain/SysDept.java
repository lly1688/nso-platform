package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId private Long id;
    private Long tenantId;
    private Long parentId;
    private String deptName;
    private String leaderName;
    private String phone;
    private Integer sortNo;
    private String status;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
