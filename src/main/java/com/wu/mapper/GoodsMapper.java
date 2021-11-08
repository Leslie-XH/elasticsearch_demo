package com.wu.mapper;

import com.wu.domain.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

//@Repository
@Mapper
public interface GoodsMapper {

    public List<Goods> findAll();
}
