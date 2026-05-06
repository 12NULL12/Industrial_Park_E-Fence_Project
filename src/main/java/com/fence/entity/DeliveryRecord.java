package com.fence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("delivery_record") // 需用户修改：确认数据库表名
public class DeliveryRecord {
    @TableId(type = IdType.AUTO)
    private Long id; // 记录ID（自增）

    private Long cargoId; // 货物ID
    private Long driverId; // 司机ID
    private Long vehicleId; // 车辆ID
    private LocalDateTime deliveryTime; // 交付时间
    private Integer status; // 状态：1成功/0失败

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}