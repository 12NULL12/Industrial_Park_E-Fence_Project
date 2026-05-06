package com.fence.mapper;

import com.fence.entity.ParkingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ParkingRecordMapper {

    int insert(ParkingRecord record);

    int update(ParkingRecord record);

    int deleteById(Long id);

    ParkingRecord selectById(Long id);

    ParkingRecord selectByPlateNumber(@Param("plateNumber") String plateNumber);

    List<ParkingRecord> selectList(@Param("plateNumber") String plateNumber,
                                   @Param("action") String action,
                                   @Param("limit") int limit);
}