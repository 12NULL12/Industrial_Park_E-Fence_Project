package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.common.PageResult;
import com.fence.dto.driver.DriverCreateRequest;
import com.fence.dto.driver.DriverDetailVO;
import com.fence.dto.driver.DriverUpdateRequest;
import com.fence.entity.Driver;
import com.fence.entity.Vehicle;
import com.fence.mapper.DriverMapper;
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
public class DriverService {

    @Autowired
    private DriverMapper driverMapper;

    @Autowired
    private VehicleMapper vehicleMapper;

    /**
     * 创建司机
     */
    @Transactional
    public Long  createDriver(DriverCreateRequest request) {
        // 检查手机号是否已存在
        Driver existingDriver = driverMapper.selectByPhone(request.getPhone());
        if (existingDriver != null) {
            throw new BusinessException("手机号已注册");
        }

        Driver driver = new Driver();
        BeanUtils.copyProperties(request, driver);
        driver.setStatus("AVAILABLE");
        driver.setCreateTime(LocalDateTime.now());
        driver.setUpdateTime(LocalDateTime.now());

        driverMapper.insert(driver);

        log.info("创建司机成功: name={}, phone={}, id={}",
                request.getName(), request.getPhone(), driver.getId());
        return driver.getId();
    }

    /**
     * 更新司机信息
     */
    @Transactional
    public void updateDriver(DriverUpdateRequest request) {
        Driver driver = driverMapper.selectById(request.getId());
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }

        log.info("更新司机请求数据: {}", request);

        BeanUtils.copyProperties(request, driver);
        driver.setUpdateTime(LocalDateTime.now());

        log.info("更新后的司机对象: name={}, phone={}, licenseNumber={}",
                driver.getName(), driver.getPhone(), driver.getLicenseNumber());

        driverMapper.update(driver);

        log.info("更新司机信息成功: id={}", request.getId());
    }

    /**
     * 删除司机
     */
    @Transactional
    public void deleteDriver(Long id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }

        // 检查司机是否正在执行任务
        if ("ON_DUTY".equals(driver.getStatus())) {
            throw new BusinessException("司机正在执行任务，无法删除");
        }

        driverMapper.deleteById(id);

        log.info("删除司机成功: id={}", id);
    }

    /**
     * 查询司机详情
     */
    public DriverDetailVO getDriverDetail(Long id) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }
        return convertToVO(driver);
    }

    /**
     * 查询司机列表（支持分页和条件筛选）
     */
    public List<DriverDetailVO> queryDrivers(int limit,
                                                   String status,
                                                   String name,
                                                   String phone) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("name", name);
        params.put("phone", phone);
        params.put("limit", limit);
        params.put("offset", 0);
        List<Driver> drivers = driverMapper.selectList(params);
        int total = driverMapper.count(params);

        List<DriverDetailVO> voList = drivers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 绑定车辆到司机（一对一绑定）
     */
    @Transactional
    public void bindVehicle(Long driverId, Long vehicleId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }

        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }

        if (driver.getVehicleId() != null) {
            throw new BusinessException("司机已绑定车辆，请先解绑");
        }

        if (vehicle.getDriverId() != null) {
            throw new BusinessException("该车辆已被其他司机绑定");
        }

        driverMapper.bindVehicle(driverId, vehicleId, vehicle.getPlateNumber());

        vehicle.setDriverId(driverId);
        vehicle.setDriverName(driver.getName());
        vehicle.setStatus("RUNNING");
        vehicleMapper.update(vehicle);

        driverMapper.updateStatus(driverId, "ON_DUTY");

        log.info("绑定车辆到司机: driverId={}, vehicleId={}, plateNumber={}", 
                driverId, vehicleId, vehicle.getPlateNumber());
    }

    /**
     * 解绑司机与车辆
     */
    @Transactional
    public void unbindVehicle(Long driverId) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }

        if (driver.getVehicleId() == null) {
            log.warn("司机未绑定车辆，无需解绑: driverId={}", driverId);
            return;
        }

        Long vehicleId = driver.getVehicleId();
        
        driverMapper.unbindVehicle(driverId);
        driverMapper.updateStatus(driverId, "AVAILABLE");

        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle != null) {
            vehicle.setDriverId(null);
            vehicle.setDriverName(null);
            vehicle.setStatus("IDLE");
            vehicleMapper.update(vehicle);
        }

        log.info("解绑车辆: driverId={}, vehicleId={}", driverId, vehicleId);
    }

    /**
     * 更新司机在线状态
     */
    @Transactional
    public void updateDriverStatus(Long id, String status) {
        Driver driver = driverMapper.selectById(id);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }

        // 验证状态合法性
        validateStatus(status);

        driver.setStatus(status);
        driver.setUpdateTime(LocalDateTime.now());
        driverMapper.update(driver);

        log.info("更新司机状态: id={}, oldStatus={}, newStatus={}",
                id, driver.getStatus(), status);
    }

    /**
     * 查询可用司机列表（未绑定车辆且状态为AVAILABLE）
     */
    public List<DriverDetailVO> getAvailableDrivers() {
        List<Driver> drivers = driverMapper.selectAvailableDrivers();
        return drivers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据手机号查询司机
     */
    public DriverDetailVO getDriverByPhone(String phone) {
        Driver driver = driverMapper.selectByPhone(phone);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }
        return convertToVO(driver);
    }

    /**
     * 更新司机位置
     */
    @Transactional
    public void updateDriverPosition(Long driverId, Double longitude, Double latitude) {
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new BusinessException("司机不存在");
        }

        // TODO: 需要在 DriverMapper 中添加 updatePosition 方法
        // driverMapper.updatePosition(driverId, longitude, latitude);

        log.debug("更新司机位置: driverId={}, lon={}, lat={}",
                driverId, longitude, latitude);
    }

    // ==================== 私有辅助方法 ====================

    private void validateStatus(String status) {
        List<String> validStatuses = List.of(
                "AVAILABLE", "ON_DUTY", "OFF_DUTY", "LEAVE", "ONLINE", "OFFLINE"
        );
        if (!validStatuses.contains(status)) {
            throw new BusinessException("无效的司机状态");
        }
    }

    private DriverDetailVO convertToVO(Driver driver) {
        DriverDetailVO vo = new DriverDetailVO();
        BeanUtils.copyProperties(driver, vo);
        return vo;
    }
}
