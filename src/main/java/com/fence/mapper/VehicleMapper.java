package com.fence.mapper;

import com.fence.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface VehicleMapper {

    int insert(Vehicle vehicle);

    int update(Vehicle vehicle);

    int deleteById(Long id);

    Vehicle selectById(Long id);

    Vehicle selectByPlateNumber(@Param("plateNumber") String plateNumber);

    List<Vehicle> selectList(Map<String, Object> params);

    int count(Map<String, Object> params);
}
