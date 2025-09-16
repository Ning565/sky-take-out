package com.star.mapper;

import com.star.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiConversationMapper {
    // 新增对话记录
    int insert(AiConversation conversation);

    // 根据ID查询
    AiConversation selectById(@Param("id") Long id);

    // 根据sessionId查询全部对话
    List<AiConversation> selectBySessionId(@Param("sessionId") String sessionId);

    // 查询用户所有对话
    List<AiConversation> selectByUserId(@Param("userId") Long userId);

    // 更新AI回复、上下文等
    int update(AiConversation conversation);

    // 删除对话记录
    int deleteById(@Param("id") Long id);
} 