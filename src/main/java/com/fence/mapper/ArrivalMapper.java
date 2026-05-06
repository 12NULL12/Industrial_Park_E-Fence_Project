package com.fence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fence.entity.ArrivalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArrivalMapper extends BaseMapper<ArrivalRecord> {

    List<ArrivalRecord> selectByCargoId(@Param("cargoId") Long cargoId);

    List<ArrivalRecord> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    List<ArrivalRecord> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    ArrivalRecord selectLatestByCargoId(@Param("cargoId") Long cargoId);
}
