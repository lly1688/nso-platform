package com.nso.business.customer.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("nso_customer")
public class Customer {
    @TableId private Long id;
    private Long tenantId;
    private String customerCode;
    private String name;
    private String industry;
    private String contactName;
    private String phone;
    private String status;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
