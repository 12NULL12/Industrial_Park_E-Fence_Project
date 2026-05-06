package com.fence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fence.entity.Cargo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CargoMapper extends BaseMapper<Cargo> {

    List<Cargo> selectAllWithWarehouse();

    Cargo selectByIdWithWarehouse(@Param("id") Long id);

    Cargo selectByCargoCode(@Param("cargoCode") String cargoCode);
}
