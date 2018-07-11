package com.example.server.service;

import com.example.server.domain.Message;

/**
 * 服务接口类
 */
public interface IService {
    Message echoMsg(int msg);
}
