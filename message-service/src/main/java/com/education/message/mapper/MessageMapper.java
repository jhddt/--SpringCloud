package com.education.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.education.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    // TODO: 如果需要查询用户信息，应该通过服务间调用（Feign Client）获取
    // 不再直接查询其他服务的数据库
}
