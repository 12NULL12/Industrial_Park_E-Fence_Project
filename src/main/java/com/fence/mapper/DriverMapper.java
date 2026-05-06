package com.fence.mapper;

import com.fence.entity.Driver;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface DriverMapper {

    int insert(Driver driver);

    int update(Driver driver);

    int deleteById(Long id);

    Driver selectById(Long id);

    Driver selectByPhone(@Param("phone") String phone);

    List<Driver> selectList(Map<String, Object> params);

    int count(Map<String, Object> params);

    int bindVehicle(@Param("driverId") Long driverId,
                    @Param("vehicleId") Long vehicleId,
                    @Param("vehiclePlate") String vehiclePlate);

    int unbindVehicle(@Param("driverId") Long driverId);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    List<Driver> selectAvailableDrivers();
}
