package com.fence.service;

import com.fence.common.BusinessException;
import com.fence.common.PageResult;
import com.fence.dto.fence.FenceCreateRequest;
import com.fence.dto.fence.FenceDetailVO;
import com.fence.entity.Fence;
import com.fence.entity.FenceVertex;
import com.fence.entity.FenceVehicle;
import com.fence.mapper.FenceMapper;
import com.fence.mapper.FenceVehicleMapper;
import com.fence.util.CollisionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FenceService {

    @Autowired
    private FenceMapper fenceMapper;
    @Autowired
    private FenceVehicleMapper fenceVehicleMapper; // 1. 注入 Mapper

    /**
     * 创建围栏
     */
    @Transactional
    public Long createFence(FenceCreateRequest request) {
        // 1. 创建围栏主记录
        Fence fence = new Fence();
        BeanUtils.copyProperties(request, fence);
        if (fence.getEnabled() == null) {
            fence.setEnabled(true);
        }
        if (fence.getAlarmLevel() == null) {
            fence.setAlarmLevel(2);
        }
        fence.setCreateTime(LocalDateTime.now());
        fence.setUpdateTime(LocalDateTime.now());

        fenceMapper.insert(fence);

        // 2. 保存围栏顶点坐标
        List<FenceVertex> vertices = new ArrayList<>();
        for (int i = 0; i < request.getVertices().size(); i++) {
            FenceCreateRequest.Coordinate coord = request.getVertices().get(i);
            FenceVertex vertex = new FenceVertex();
            vertex.setFenceId(fence.getId());
            vertex.setLongitude(coord.getLongitude());
            vertex.setLatitude(coord.getLatitude());
            vertex.setVertexOrder(i + 1);
            vertex.setCreateTime(LocalDateTime.now());
            vertices.add(vertex);
        }

        fenceMapper.batchInsertVertices(vertices);

        log.info("创建围栏成功: fenceName={}, id={}, vertexCount={}",
                request.getFenceName(), fence.getId(), vertices.size());

        return fence.getId();
    }

    /**
     * 更新围栏信息
     */
    @Transactional
    public void updateFence(Long id, FenceCreateRequest request) {
        Fence fence = fenceMapper.selectById(id);
        if (fence == null) {
            throw new BusinessException("围栏不存在");
        }

        // 1. 更新围栏基本信息
        fence.setFenceName(request.getFenceName());
        fence.setFenceType(request.getFenceType());
        fence.setDescription(request.getDescription());
        if (request.getAlarmLevel() != null) {
            fence.setAlarmLevel(request.getAlarmLevel());
        }
        if (request.getEnabled() != null) {
            fence.setEnabled(request.getEnabled());
        }
        fence.setUpdateTime(LocalDateTime.now());
        fenceMapper.update(fence);

        // 2. 只有当 vertices 不为空时，才更新顶点坐标
        if (request.getVertices() != null && !request.getVertices().isEmpty()) {
            if (request.getVertices().size() < 3) {
                throw new BusinessException("围栏至少需要3个顶点");
            }

            // 删除旧顶点，插入新顶点
            fenceMapper.deleteVerticesByFenceId(id);

            List<FenceVertex> vertices = new ArrayList<>();
            for (int i = 0; i < request.getVertices().size(); i++) {
                FenceCreateRequest.Coordinate coord = request.getVertices().get(i);
                FenceVertex vertex = new FenceVertex();
                vertex.setFenceId(id);
                vertex.setLongitude(coord.getLongitude());
                vertex.setLatitude(coord.getLatitude());
                vertex.setVertexOrder(i + 1);
                vertex.setCreateTime(LocalDateTime.now());
                vertices.add(vertex);
            }

            fenceMapper.batchInsertVertices(vertices);
            log.info("更新围栏顶点: fenceId={}, vertexCount={}", id, vertices.size());
        } else {
            log.info("仅更新围栏基本信息，未修改顶点: fenceId={}", id);
        }

        // 3. 如果传入了 boundVehicleIds，更新车辆绑定关系
        if (request.getBoundVehicleIds() != null) {
            // 获取当前已绑定的车辆
            List<Long> currentVehicleIds = fenceMapper.selectVehicleIdsByFenceId(id);
            
            // 计算需要新增的绑定
            List<Long> toAdd = request.getBoundVehicleIds().stream()
                    .filter(vehicleId -> !currentVehicleIds.contains(vehicleId))
                    .collect(Collectors.toList());
            
            // 计算需要删除的绑定
            List<Long> toRemove = currentVehicleIds.stream()
                    .filter(vehicleId -> !request.getBoundVehicleIds().contains(vehicleId))
                    .collect(Collectors.toList());
            
            // 删除不再绑定的车辆
            for (Long vehicleId : toRemove) {
                fenceMapper.unbindVehicle(id, vehicleId);
                log.info("解绑车辆: fenceId={}, vehicleId={}", id, vehicleId);
            }
            
            // 新增绑定的车辆
            for (Long vehicleId : toAdd) {
                FenceVehicle fenceVehicle = new FenceVehicle();
                fenceVehicle.setFenceId(id);
                fenceVehicle.setVehicleId(vehicleId);
                fenceVehicle.setBindTime(LocalDateTime.now());
                fenceVehicle.setCreateTime(LocalDateTime.now());
                fenceMapper.bindVehicle(fenceVehicle);
                log.info("绑定车辆: fenceId={}, vehicleId={}", id, vehicleId);
            }
            
            log.info("更新围栏车辆绑定: fenceId={}, 新增={}, 删除={}", id, toAdd.size(), toRemove.size());
        } else {
            log.info("未修改围栏车辆绑定: fenceId={}", id);
        }

        log.info("更新围栏成功: id={}", id);
    }

    /**
     * 删除围栏
     */
    @Transactional
    public void deleteFence(Long id) {
        Fence fence = fenceMapper.selectById(id);
        if (fence == null) {
            throw new BusinessException("围栏不存在");
        }

        // 删除关联关系
        fenceMapper.deleteBindingsByFenceId(id);

        // 删除顶点
        fenceMapper.deleteVerticesByFenceId(id);

        // 删除主记录
        fenceMapper.deleteById(id);

        log.info("删除围栏成功: id={}", id);
    }

    /**
     * 查询围栏详情
     */
    public FenceDetailVO getFenceDetail(Long id) {
        Fence fence = fenceMapper.selectById(id);
        if (fence == null) {
            throw new BusinessException("围栏不存在");
        }

        FenceDetailVO vo = convertToVO(fence);

        // 查询顶点
        List<FenceVertex> vertices = fenceMapper.selectVerticesByFenceId(id);
        List<FenceDetailVO.VertexVO> vertexVOs = vertices.stream()
                .map(v -> {
                    FenceDetailVO.VertexVO vertexVO = new FenceDetailVO.VertexVO();
                    vertexVO.setLongitude(v.getLongitude());
                    vertexVO.setLatitude(v.getLatitude());
                    vertexVO.setVertexOrder(v.getVertexOrder());
                    return vertexVO;
                })
                .collect(Collectors.toList());
        vo.setVertices(vertexVOs);

        // 查询绑定的车辆
        List<Long> vehicleIds = fenceMapper.selectVehicleIdsByFenceId(id);
        vo.setBoundVehicleIds(vehicleIds);

        return vo;
    }

    /**
     * 查询围栏列表
     */
    public List<FenceDetailVO> queryFences(int limit,
                                                 String fenceType,
                                                 Boolean enabled,
                                                 String fenceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("fenceType", fenceType);
        params.put("enabled", enabled);
        params.put("fenceName", fenceName);
        params.put("limit", limit);
        params.put("offset", 0);
        List<Fence> fences = fenceMapper.selectList(params);
        int total = fenceMapper.count(params);

        List<FenceDetailVO> voList = fences.stream()
                .map(fence -> {
                    FenceDetailVO vo = convertToVO(fence);
                    
                    List<FenceVertex> vertices = fenceMapper.selectVerticesByFenceId(fence.getId());
                    List<FenceDetailVO.VertexVO> vertexVOs = vertices.stream()
                            .map(v -> {
                                FenceDetailVO.VertexVO vertexVO = new FenceDetailVO.VertexVO();
                                vertexVO.setLongitude(v.getLongitude());
                                vertexVO.setLatitude(v.getLatitude());
                                vertexVO.setVertexOrder(v.getVertexOrder());
                                return vertexVO;
                            })
                            .collect(Collectors.toList());
                    vo.setVertices(vertexVOs);
                    
                    List<Long> vehicleIds = fenceMapper.selectVehicleIdsByFenceId(fence.getId());
                    vo.setBoundVehicleIds(vehicleIds);
                    
                    return vo;
                })
                .collect(Collectors.toList());

        return voList;
    }

    /**
     * 绑定车辆到围栏
     */
    @Transactional
    public void bindVehicleToFence(Long fenceId, Long vehicleId) {
        Fence fence = fenceMapper.selectById(fenceId);
        if (fence == null) {
            throw new BusinessException("围栏不存在");
        }

        // 检查是否已绑定
        List<Long> boundVehicles = fenceMapper.selectVehicleIdsByFenceId(fenceId);
        if (boundVehicles.contains(vehicleId)) {
            throw new BusinessException("车辆已绑定到该围栏");
        }

        FenceVehicle fenceVehicle = new FenceVehicle();
        fenceVehicle.setFenceId(fenceId);
        fenceVehicle.setVehicleId(vehicleId);
        fenceVehicle.setBindTime(LocalDateTime.now());
        fenceVehicle.setCreateTime(LocalDateTime.now());

        fenceMapper.bindVehicle(fenceVehicle);

        log.info("绑定车辆到围栏: fenceId={}, vehicleId={}", fenceId, vehicleId);
    }

    /**
     * 解绑围栏与车辆
     */
    @Transactional
    public void unbindVehicleFromFence(Long fenceId, Long vehicleId) {
        int result = fenceMapper.unbindVehicle(fenceId, vehicleId);
        if (result == 0) {
            throw new BusinessException("绑定关系不存在");
        }

        log.info("解绑车辆: fenceId={}, vehicleId={}", fenceId, vehicleId);
    }

    /**
     * 检查车辆是否在围栏内（调用临时算法）
     * TODO: 后续替换为成员A实现的CollisionService
     */
    public boolean checkVehicleInFence(Long vehicleId, Long fenceId,
                                       Double vehicleLongitude, Double vehicleLatitude) {
        // 1. 获取围栏顶点
        List<FenceVertex> vertices = fenceMapper.selectVerticesByFenceId(fenceId);
        if (vertices == null || vertices.isEmpty()) {
            log.warn("围栏没有顶点数据: fenceId={}", fenceId);
            return false;
        }

        // 2. 使用临时碰撞检测算法
        boolean inside = CollisionUtil.isPointInPolygon(
                vehicleLongitude, vehicleLatitude, vertices
        );

        log.info("车辆围栏检测结果: vehicleId={}, fenceId={}, inside={}",
                vehicleId, fenceId, inside);

        return inside;
    }

    /**
     * 检查车辆是否触发围栏告警
     */
    public Map<String, Object> checkFenceAlarm(Long vehicleId,
                                               Double longitude, Double latitude) {
        Map<String, Object> result = new HashMap<>();
        result.put("vehicleId", vehicleId);
        result.put("alarms", new ArrayList<>());

        List<Long> fenceIds = fenceMapper.selectFenceIdsByVehicleId(vehicleId);

        List<Map<String, Object>> alarms = new ArrayList<>();

        for (Long fenceId : fenceIds) {
            Fence fence = fenceMapper.selectById(fenceId);
            if (fence == null || !fence.getEnabled()) {
                continue;
            }

            boolean inside = checkVehicleInFence(vehicleId, fenceId, longitude, latitude);

            if (inside) {
                Map<String, Object> alarmInfo = new HashMap<>();
                alarmInfo.put("fenceId", fenceId);
                alarmInfo.put("fenceName", fence.getFenceName());
                alarmInfo.put("inside", true);
                alarmInfo.put("shouldAlarm", true);
                alarms.add(alarmInfo);

                log.info("检测到车辆进入禁区: vehicleId={}, fenceId={}, fenceName={}",
                        vehicleId, fenceId, fence.getFenceName());
            }
        }

        result.put("alarms", alarms);
        return result;
    }

    /**
     * 根据司机ID获取其绑定车辆的围栏列表
     */
    public List<FenceDetailVO> getFencesByDriverId(Long driverId) {
        // 1. 查询司机绑定的车辆ID
        List<Long> vehicleIds = fenceMapper.selectVehicleIdsByDriverId(driverId);
        
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            log.info("司机 {} 没有绑定任何车辆", driverId);
            return new ArrayList<>();
        }
        
        log.info("司机 {} 绑定的车辆: {}", driverId, vehicleIds);
        
        // 2. 查询这些车辆关联的围栏ID
        Set<Long> fenceIds = new HashSet<>();
        for (Long vehicleId : vehicleIds) {
            List<Long> ids = fenceMapper.selectFenceIdsByVehicleId(vehicleId);
            fenceIds.addAll(ids);
        }
        
        if (fenceIds.isEmpty()) {
            log.info("司机 {} 的车辆没有关联任何围栏", driverId);
            return new ArrayList<>();
        }
        
        log.info("司机 {} 的车辆关联的围栏ID: {}", driverId, fenceIds);
        
        // 3. 查询围栏详情
        List<FenceDetailVO> result = new ArrayList<>();
        for (Long fenceId : fenceIds) {
            try {
                FenceDetailVO vo = getFenceDetail(fenceId);
                result.add(vo);
            } catch (Exception e) {
                log.error("查询围栏详情失败: fenceId={}", fenceId, e);
            }
        }
        
        return result;
    }

    // ==================== 私有辅助方法 ====================

    private FenceDetailVO convertToVO(Fence fence) {
        FenceDetailVO vo = new FenceDetailVO();
        BeanUtils.copyProperties(fence, vo);
        return vo;
    }
    /**
     * 批量绑定车辆
     */
    @Transactional
    public void bindVehicles(Long fenceId, List<Long> vehicleIds) {
        // 1. 先删除该围栏旧的绑定关系
        fenceVehicleMapper.deleteByFenceId(fenceId);

        // 2. 批量插入新的绑定关系
        for (Long vehicleId : vehicleIds) {
            FenceVehicle fv = new FenceVehicle();
            fv.setFenceId(fenceId);
            fv.setVehicleId(vehicleId);
            fenceVehicleMapper.insert(fv);
        }
    }
}
