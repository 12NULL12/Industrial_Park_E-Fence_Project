package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.common.PageResult;
import com.fence.dto.vehicle.VehicleCreateRequest;
import com.fence.dto.vehicle.VehicleDetailVO;
import com.fence.dto.vehicle.VehicleLocationVO;
import com.fence.dto.vehicle.VehicleUpdateRequest;
import com.fence.entity.Vehicle;
import com.fence.mapper.VehicleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VehicleService {

    @Autowired
    private VehicleMapper vehicleMapper;

    /**
     * 创建车辆
     */
    @Transactional
    public Long createVehicle(VehicleCreateRequest request) {
        // 检查车牌号是否已存在
        Vehicle existingVehicle = vehicleMapper.selectByPlateNumber(request.getPlateNumber());
        if (existingVehicle != null) {
            throw new BusinessException("车牌号已存在");
        }

        Vehicle vehicle = new Vehicle();
        BeanUtils.copyProperties(request, vehicle);
        vehicle.setStatus("IDLE");
        vehicle.setCreateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());

        vehicleMapper.insert(vehicle);

        log.info("创建车辆成功: plateNumber={}, id={}", request.getPlateNumber(), vehicle.getId());
        return vehicle.getId();
    }

    /**
     * 更新车辆信息
     */
    @Transactional
    public void updateVehicle(Long id, VehicleUpdateRequest request) {
        log.info("收到更新车辆请求: id={}, request={}", id, request);
        
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        log.info("更新前车辆信息: id={}, plateNumber={}", vehicle.getId(), vehicle.getPlateNumber());
        
        BeanUtils.copyProperties(request, vehicle);
        
        if (request.getPlateNumber() != null) {
            vehicle.setPlateNumber(request.getPlateNumber());
        }
        
        vehicle.setId(id);
        vehicle.setUpdateTime(LocalDateTime.now());

        log.info("更新后车辆信息: id={}, plateNumber={}, vehicleType={}, brand={}, driverId={}", 
                vehicle.getId(), vehicle.getPlateNumber(), vehicle.getVehicleType(), 
                vehicle.getBrand(), vehicle.getDriverId());

        int result = vehicleMapper.update(vehicle);
        log.info("MyBatis更新结果: {} 行受影响", result);

        if (result == 0) {
            log.error("更新失败：没有记录被更新，vehicle.id={}", vehicle.getId());
            throw new BusinessException("更新失败");
        }

        log.info("更新车辆成功: id={}, plateNumber={}", id, vehicle.getPlateNumber());
    }

    /**
     * 删除车辆
     */
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 检查车辆是否正在使用中
        if ("RUNNING".equals(vehicle.getStatus())) {
            throw new BusinessException("车辆正在运行中，无法删除");
        }

        vehicleMapper.deleteById(id);

        log.info("删除车辆成功: id={}", id);
    }

    /**
     * 查询车辆详情
     */
    public VehicleDetailVO getVehicleDetail(Long id) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        return convertToVO(vehicle);
    }

    /**
     * 查询车辆列表（支持分页和条件筛选）
     */
    public List<VehicleDetailVO> queryVehicles(int limit,
                                                     String status,
                                                     String plateNumber,
                                                     String vehicleType) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("plateNumber", plateNumber);
        params.put("vehicleType", vehicleType);
        params.put("offset", 0);
        List<Vehicle> vehicles = vehicleMapper.selectList(params);
        int total = vehicleMapper.count(params);

        List<VehicleDetailVO> voList = vehicles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 根据车牌号查询车辆
     */
    public VehicleDetailVO getVehicleByPlateNumber(String plateNumber) {
        Vehicle vehicle = vehicleMapper.selectByPlateNumber(plateNumber);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        return convertToVO(vehicle);
    }

    /**
     * 更新车辆状态
     */
    @Transactional
    public void updateVehicleStatus(Long id, String status) {
        Vehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        // 验证状态转换合法性
        validateStatusTransition(vehicle.getStatus(), status);

        vehicle.setStatus(status);
        vehicle.setUpdateTime(LocalDateTime.now());
        vehicleMapper.update(vehicle);

        log.info("更新车辆状态: id={}, oldStatus={}, newStatus={}",
                id, vehicle.getStatus(), status);
    }

    /**
     * 绑定司机到车辆
     */
    @Transactional
    public void bindDriver(Long vehicleId, Long driverId, String driverName) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        vehicle.setDriverId(driverId);
        vehicle.setDriverName(driverName);
        vehicle.setUpdateTime(LocalDateTime.now());

        vehicleMapper.update(vehicle);

        log.info("绑定司机到车辆: vehicleId={}, driverId={}, driverName={}",
                vehicleId, driverId, driverName);
    }

    /**
     * 解绑司机
     */
    @Transactional
    public void unbindDriver(Long vehicleId) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        vehicle.setDriverId(null);
        vehicle.setDriverName(null);
        vehicle.setUpdateTime(LocalDateTime.now());

        vehicleMapper.update(vehicle);

        log.info("解绑司机: vehicleId={}", vehicleId);
    }

    /**
     * 绑定设备到车辆
     */
    @Transactional
    public void bindDevice(Long vehicleId, Long deviceId) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        vehicle.setDeviceId(deviceId);
        vehicle.setUpdateTime(LocalDateTime.now());

        vehicleMapper.update(vehicle);

        log.info("绑定设备到车辆: vehicleId={}, deviceId={}", vehicleId, deviceId);
    }

    /**
     * 更新车辆位置
     */
    @Transactional
    public void updateVehiclePosition(Long vehicleId, Double longitude, Double latitude) {
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        vehicle.setCurrentLongitude(longitude);
        vehicle.setCurrentLatitude(latitude);
        vehicle.setLastUpdateTime(LocalDateTime.now());
        vehicle.setUpdateTime(LocalDateTime.now());

        vehicleMapper.update(vehicle);
    }

    /**
     * 获取在线车辆数量
     */
    public int getOnlineVehicleCount() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "RUNNING");
        return vehicleMapper.count(params);
    }

    /**
     * 获取空闲车辆列表
     */
    public List<VehicleDetailVO> getIdleVehicles() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "IDLE");
        params.put("limit", 100);

        List<Vehicle> vehicles = vehicleMapper.selectList(params);
        return vehicles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    private void validateStatusTransition(String oldStatus, String newStatus) {
        // 简单的状态转换规则
        if ("DELETED".equals(oldStatus)) {
            throw new BusinessException("已删除的车辆无法更改状态");
        }

        // 可以添加更复杂的状态机逻辑
        log.debug("车辆状态转换: {} -> {}", oldStatus, newStatus);
    }

    private VehicleDetailVO convertToVO(Vehicle vehicle) {
        VehicleDetailVO vo = new VehicleDetailVO();
        BeanUtils.copyProperties(vehicle, vo);
        return vo;
    }
    /**
     * 获取所有车辆的简化位置信息
     */
    public List<VehicleLocationVO> getAllVehicleLocations() {
        // 1. 查询所有车辆（可以根据需要加 limit，比如 limit 1000）
        Map<String, Object> params = new HashMap<>();
        params.put("limit", 1000);
        List<Vehicle> vehicles = vehicleMapper.selectList(params);

        // 2. 转换为前端要求的格式
        return vehicles.stream().map(v -> {
            VehicleLocationVO vo = new VehicleLocationVO();
            vo.setId(v.getId());
            vo.setPlate(v.getPlateNumber());
            
            // 如果坐标为空，使用默认值（深圳中心）
            vo.setLng(v.getCurrentLongitude() != null ? v.getCurrentLongitude() : 113.9345);
            vo.setLat(v.getCurrentLatitude() != null ? v.getCurrentLatitude() : 22.5455);
            
            vo.setSpeed(v.getSpeed());
            vo.setStatus(v.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }
}
