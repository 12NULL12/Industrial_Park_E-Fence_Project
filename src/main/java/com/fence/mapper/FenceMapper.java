package com.fence.mapper;

import com.fence.entity.Fence;
import com.fence.entity.FenceVertex;
import com.fence.entity.FenceVehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface FenceMapper {

    // Fence 主表操作
    int insert(Fence fence);
    int update(Fence fence);
    int deleteById(Long id);
    Fence selectById(Long id);
    List<Fence> selectList(Map<String, Object> params);
    int count(Map<String, Object> params);

    // FenceVertex 顶点操作
    int insertVertex(FenceVertex vertex);
    int batchInsertVertices(@Param("vertices") List<FenceVertex> vertices);
    List<FenceVertex> selectVerticesByFenceId(@Param("fenceId") Long fenceId);
    int deleteVerticesByFenceId(@Param("fenceId") Long fenceId);

    // FenceVehicle 关联操作
    int bindVehicle(FenceVehicle fenceVehicle);
    int unbindVehicle(@Param("fenceId") Long fenceId, @Param("vehicleId") Long vehicleId);
    List<Long> selectVehicleIdsByFenceId(@Param("fenceId") Long fenceId);
    List<Long> selectFenceIdsByVehicleId(@Param("vehicleId") Long vehicleId);
    int deleteBindingsByFenceId(@Param("fenceId") Long fenceId);
    
    // 根据司机ID查询其绑定的车辆ID列表
    List<Long> selectVehicleIdsByDriverId(@Param("driverId") Long driverId);
}
