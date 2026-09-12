package com.nso.business.customer.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 客户实体。
@Data
@TableName("nso_customer")
public class Customer {

    // 客户编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 客户编码
    private String customerCode;

    // 客户名称
    private String name;

    // 所属行业
    private String industry;

    // 联系人
    private String contactName;

    // 联系电话
    private String phone;

    // 客户状态
    private String status;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
