package com.fence.mapper;

import com.fence.entity.Alarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface AlarmMapper {

    int insert(Alarm alarm);

    int update(Alarm alarm);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("handleMethod") String handleMethod,
                     @Param("handleRemark") String handleRemark,
                     @Param("handlerId") Long handlerId,
                     @Param("handlerName") String handlerName);

    Alarm selectById(Long id);


    List<Alarm> selectList(Map<String, Object> params);

    int countByStatus(@Param("status") String status);

    Map<String, Object> statisticsByType();

    Map<String, Object> statisticsByLevel();
    Alarm selectLatestByVehicleAndType(@Param("vehicleId") Long vehicleId,
                                       @Param("alarmType") String alarmType);
}
