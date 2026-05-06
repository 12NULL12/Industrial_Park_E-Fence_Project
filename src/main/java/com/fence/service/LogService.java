package com.fence.service;

import com.fence.common.PageResult;
import com.fence.dto.log.LogDetailVO;
import com.fence.dto.log.LogQueryRequest;
import com.fence.entity.OperationLog;
import com.fence.mapper.LogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LogService {

    @Autowired
    private LogMapper logMapper;

    /**
     * 记录操作日志
     */
    @Transactional
    public void recordLog(OperationLog operationLog) {
        logMapper.insert(operationLog);
    }


    /**
     * 记录调度日志
     */
    public void recordDispatchLog(Long operatorId, String operatorName,
                                  String instructionType, String content,
                                  Long vehicleId, boolean success) {
        OperationLog log = new OperationLog();
        log.setLogType("DISPATCH");
        log.setModule("DISPATCH");
        log.setOperation("下发指令: " + instructionType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRequestMethod("POST");
        log.setRequestUrl("/api/dispatch/create");
        log.setRequestParams("{\"instructionType\":\"" + instructionType +
                "\",\"content\":\"" + content +
                "\",\"vehicleId\":" + vehicleId + "}");
        log.setResponseStatus(success ? 200 : 500);

        logMapper.insert(log);
    }
    /**
     * 查询日志详情
     */
    public LogDetailVO getLogDetail(Long id) {
        OperationLog operationLog = logMapper.selectById(id);
        if (operationLog == null) {
            return null;
        }
        return convertToVO(operationLog);
    }

    /**
     * 清理旧日志（定时任务）
     */
    @Transactional
    public int cleanOldLogs(int days) {
        int count = logMapper.deleteOldLogs(days);
        log.info("清理旧日志: 删除{}天前日志，共{}条", days, count);
        return count;
    }

    /**
     * 统计日志数量
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("loginCount", logMapper.countByType("LOGIN"));
        stats.put("operationCount", logMapper.countByType("OPERATION"));
        stats.put("dispatchCount", logMapper.countByType("DISPATCH"));
        stats.put("systemCount", logMapper.countByType("SYSTEM"));
        return stats;
    }
    /**
     * 根据日志类型查询全量数据（适配前端分页）
     */
    public List<LogDetailVO> queryLogsByType(String logType, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("logType", logType);
        params.put("limit", limit);

        List<OperationLog> logs = logMapper.selectList(params);
        return logs.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    private LogDetailVO convertToVO(OperationLog operationLog) {
        LogDetailVO vo = new LogDetailVO();
        BeanUtils.copyProperties(operationLog, vo);
        
        if ("LOGIN".equals(operationLog.getLogType())) {
            vo.setUsername(operationLog.getOperatorName());
            vo.setLoginStatus(operationLog.getResponseStatus() == 200 ? "success" : "fail");
            vo.setLocation("深圳市南山区");
        } else if ("DISPATCH".equals(operationLog.getLogType())) {
            try {
                String params = operationLog.getRequestParams();
                if (params != null && params.contains("vehicleId")) {
                    int start = params.indexOf("vehicleId\":") + 11;
                    int end = params.indexOf(",", start);
                    if (end == -1) end = params.indexOf("}", start);
                    String vehicleId = params.substring(start, end).trim();
                    vo.setVehiclePlate("京A" + vehicleId);
                }
                if (params != null && params.contains("instructionType")) {
                    int start = params.indexOf("instructionType\":\"") + 18;
                    int end = params.indexOf("\"", start);
                    vo.setInstructionType(params.substring(start, end));
                }
            } catch (Exception e) {
                log.warn("解析调度日志参数失败", e);
            }
        }
        
        return vo;
    }
}
