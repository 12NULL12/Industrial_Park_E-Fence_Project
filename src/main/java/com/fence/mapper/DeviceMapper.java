package com.fence.mapper;

import com.fence.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface DeviceMapper {

    int insert(Device device);

    int update(Device device);

    int deleteById(Long id);

    Device selectById(Long id);

    Device selectByDeviceNo(@Param("deviceNo") String deviceNo);

    List<Device> selectList(Map<String, Object> params);

    int count(Map<String, Object> params);

    // 状态更新
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateOnline(@Param("id") Long id,
                     @Param("longitude") Double longitude,
                     @Param("latitude") Double latitude);

    int updateOffline(@Param("id") Long id);

    int updateHeartbeat(@Param("id") Long id);

    int updateFault(@Param("id") Long id, @Param("remark") String remark);

    // 绑定车辆
    int bindVehicle(@Param("deviceId") Long deviceId,
                    @Param("vehicleId") Long vehicleId,
                    @Param("vehiclePlate") String vehiclePlate);

    int unbindVehicle(@Param("deviceId") Long deviceId);

    // 查询异常设备
    List<Device> selectOfflineDevices();

    List<Device> selectFaultDevices();
}
