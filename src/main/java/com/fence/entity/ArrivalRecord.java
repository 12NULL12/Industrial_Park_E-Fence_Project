package com.fence.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("arrival_record") // 需用户修改：确认数据库表名
public class ArrivalRecord {
    @TableId(type = IdType.AUTO)
    private Long id; // 记录ID（自增）

    private Long cargoId; // 货物ID
    private Long warehouseId; // 到达仓库ID
    private LocalDateTime arrivalTime; // 到达时间
    private String remark; // 备注（如"提前30分钟到达"）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}