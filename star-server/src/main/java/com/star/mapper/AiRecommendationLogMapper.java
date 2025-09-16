package com.star.mapper;

import com.star.entity.AiRecommendationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiRecommendationLogMapper {
    // 新增推荐记录
    int insert(AiRecommendationLog log);

    // 根据ID查询
    AiRecommendationLog selectById(@Param("id") Long id);

    // 根据sessionId查询推荐记录
    List<AiRecommendationLog> selectBySessionId(@Param("sessionId") String sessionId);

    // 查询用户所有推荐记录
    List<AiRecommendationLog> selectByUserId(@Param("userId") Long userId);

    // 更新推荐记录
    int update(AiRecommendationLog log);

    // 删除推荐记录
    int deleteById(@Param("id") Long id);
} 