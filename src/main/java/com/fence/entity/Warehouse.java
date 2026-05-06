// Warehouse.java（仓库实体，对应warehouse表）
package com.fence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class Warehouse {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String warehouseCode; // 仓库编号
    private String warehouseName; // 仓库名称
    private String address; // 地址
    private BigDecimal longitude; // 经度
    private BigDecimal latitude; // 纬度
    private Long adminId;
    private String managerName; // 负责人姓名
    private Integer capacity; // 仓库容量
    private Integer currentStock; // 当前库存
    @TableField(fill = FieldFill.INSERT)
    private String managerPhone; // 联系电话
    private Integer status; // 状态：1正常/0维护中（需用户定义枚举）
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}