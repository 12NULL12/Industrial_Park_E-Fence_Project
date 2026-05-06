package com.fence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("warehouse_admin") // 需用户修改：确认数据库表名
public class WarehouseAdmin {
    @TableId(type = IdType.AUTO)
    private Long id; // 管理员ID（自增）

    private Long warehouseId; // 关联仓库ID
    private String adminName; // 管理员姓名
    private String adminPhone; // 管理员电话
    private String role; // 角色（如"超级管理员"）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}