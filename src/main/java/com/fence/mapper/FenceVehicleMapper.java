package com.fence.mapper;

import com.fence.entity.FenceVehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FenceVehicleMapper {

    /**
     * 插入绑定关系
     */
    void insert(FenceVehicle fenceVehicle);

    /**
     * 根据围栏ID删除所有绑定（用于重新绑定前清空）
     */
    void deleteByFenceId(@Param("fenceId") Long fenceId);

    /**
     * 查询某个围栏绑定的所有车辆ID
     */
    List<Long> selectVehicleIdsByFenceId(@Param("fenceId") Long fenceId);
}
