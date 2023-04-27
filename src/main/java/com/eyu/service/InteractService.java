package com.eyu.service;

import com.eyu.entity.bo.ChatBO;
import com.eyu.exception.ChatException;
import java.util.concurrent.CompletableFuture;

/**
 * 交互服务
 *
 * @author zqzq3
 * @date 2022/12/10
 */
public interface InteractService {
    /**
     * 聊天
     *
     * @param chatBO 聊天BO
     * @return {@link String}
     * @throws ChatException 聊天异常
     */
    CompletableFuture<String> chat(ChatBO chatBO, String systemPrompt) throws ChatException;

    void setUniquePrompt(String sessionId, String prompt);

    String getUniquePrompt(String sessionId);
}
