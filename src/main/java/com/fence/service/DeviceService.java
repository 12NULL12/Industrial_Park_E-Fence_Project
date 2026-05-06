package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.common.PageResult;
import com.fence.dto.device.DeviceCreateRequest;
import com.fence.dto.device.DeviceDetailVO;
import com.fence.dto.device.DeviceUpdateRequest;
import com.fence.entity.Device;
import com.fence.mapper.DeviceMapper;
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
public class DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    /**
     * 创建设备
     */
    @Transactional
    public Long createDevice(DeviceCreateRequest request) {
        // 检查设备编号是否已存在
        Device existingDevice = deviceMapper.selectByDeviceNo(request.getDeviceNo());
        if (existingDevice != null) {
            throw new BusinessException("设备编号已存在");
        }

        Device device = new Device();
        BeanUtils.copyProperties(request, device);
        device.setStatus("OFFLINE");
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());

        deviceMapper.insert(device);

        log.info("创建设备成功: deviceNo={}, id={}", request.getDeviceNo(), device.getId());
        return device.getId();
    }

    /**
     * 更新设备信息
     */
    @Transactional
    public void updateDevice(DeviceUpdateRequest request) {
        Device device = deviceMapper.selectById(request.getId());
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        BeanUtils.copyProperties(request, device);
        device.setId(request.getId());
        device.setUpdateTime(LocalDateTime.now());

        deviceMapper.update(device);

        log.info("更新设备信息成功: id={}", request.getId());
    }

    /**
     * 删除设备
     */
    @Transactional
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        // 检查设备是否绑定车辆
        if (device.getVehicleId() != null) {
            throw new BusinessException("设备已绑定车辆，请先解绑");
        }

        deviceMapper.deleteById(id);

        log.info("删除设备成功: id={}", id);
    }

    /**
     * 查询设备详情
     */
    public DeviceDetailVO getDeviceDetail(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        return convertToVO(device);
    }

    /**
     * 查询设备列表
     */
    public List<DeviceDetailVO> queryDevices(int limit,
                                                   String status,
                                                   String deviceType,
                                                   String deviceNo,
                                                   Long vehicleId) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("deviceType", deviceType);
        params.put("deviceNo", deviceNo);
        params.put("vehicleId", vehicleId);
        params.put("limit", limit);
        params.put("offset", 0);
        List<Device> devices = deviceMapper.selectList(params);
        int total = deviceMapper.count(params);

        List<DeviceDetailVO> voList = devices.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 绑定车辆到设备
     */
    @Transactional
    public void bindVehicle(Long deviceId, Long vehicleId, String vehiclePlate) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        if (device.getVehicleId() != null) {
            throw new BusinessException("设备已绑定其他车辆");
        }

        deviceMapper.bindVehicle(deviceId, vehicleId, vehiclePlate);

        log.info("绑定车辆到设备: deviceId={}, vehicleId={}", deviceId, vehicleId);
    }

    /**
     * 解绑设备与车辆
     */
    @Transactional
    public void unbindVehicle(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        if (device.getVehicleId() == null) {
            throw new BusinessException("设备未绑定车辆");
        }

        deviceMapper.unbindVehicle(deviceId);

        log.info("解绑车辆: deviceId={}", deviceId);
    }

    /**
     * 设备上线（MQTT消息触发）
     */
    @Transactional
    public void deviceOnline(Long deviceId, Double longitude, Double latitude) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            log.warn("设备不存在: deviceId={}", deviceId);
            return;
        }

        deviceMapper.updateOnline(deviceId, longitude, latitude);

        log.info("设备上线: deviceId={}, location=({}, {})",
                deviceId, longitude, latitude);
    }

    /**
     * 设备离线
     */
    @Transactional
    public void deviceOffline(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return;
        }

        deviceMapper.updateOffline(deviceId);

        log.warn("设备离线: deviceId={}", deviceId);
    }

    /**
     * 更新设备心跳
     */
    @Transactional
    public void updateHeartbeat(Long deviceId) {
        deviceMapper.updateHeartbeat(deviceId);
    }

    /**
     * 标记设备故障
     */
    @Transactional
    public void markDeviceFault(Long deviceId, String reason) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        deviceMapper.updateFault(deviceId, reason);

        log.warn("标记设备故障: deviceId={}, reason={}", deviceId, reason);

        // TODO: 生成设备故障告警
        // alarmService.generateAlarm(...)
    }

    /**
     * 恢复设备正常
     */
    @Transactional
    public void recoverDevice(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        deviceMapper.updateStatus(deviceId, "ONLINE");

        log.info("设备恢复正常: deviceId={}", deviceId);
    }

    /**
     * 检查并处理离线设备（定时任务）
     */
    @Transactional
    public void checkOfflineDevices() {
        List<Device> offlineDevices = deviceMapper.selectOfflineDevices();

        for (Device device : offlineDevices) {
            log.warn("检测到设备长时间离线: deviceId={}, deviceNo={}",
                    device.getId(), device.getDeviceNo());

            // TODO: 生成离线告警
            // alarmService.generateAlarm(...)
        }

        if (!offlineDevices.isEmpty()) {
            log.info("检测到离线设备数量: {}", offlineDevices.size());
        }
    }

    /**
     * 根据设备编号查询设备
     */
    public DeviceDetailVO getDeviceByDeviceNo(String deviceNo) {
        Device device = deviceMapper.selectByDeviceNo(deviceNo);
        if (device == null) {
            throw new BusinessException("设备不存在");
        }
        return convertToVO(device);
    }

    // ==================== 私有辅助方法 ====================

    private DeviceDetailVO convertToVO(Device device) {
        DeviceDetailVO vo = new DeviceDetailVO();
        BeanUtils.copyProperties(device, vo);
        vo.setStatus(device.getStatus());
        return vo;
    }
}
