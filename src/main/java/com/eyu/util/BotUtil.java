package com.eyu.util;

import com.alibaba.fastjson.JSON;
import com.eyu.config.AccountConfig;
import com.eyu.entity.model.ChatMessage;
import com.eyu.entity.model.enums.MessageRole;
import com.eyu.exception.ChatException;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * chatbot工具类
 *
 * @author zqzq3
 * @date 2023/2/1
 */
@Component
public class BotUtil {
    @Resource
    public void setAccountConfig(AccountConfig accountConfig){
        BotUtil.accountConfig = accountConfig;
    }
    private static AccountConfig accountConfig;

    private static final Map<String, List<ChatMessage>> PROMPT_MAP = new HashMap<>();

    private static final Map<String, String> userModelMap = new HashMap<>();
    private static final Map<OpenAiService, Integer> COUNT_FOR_OPEN_AI_SERVICE = new HashMap<>();
    private static CompletionRequest.CompletionRequestBuilder completionRequestBuilder;

    @PostConstruct
    public void init(){
        completionRequestBuilder = CompletionRequest.builder().model(accountConfig.getModel());
        for (OpenAiService openAiService : accountConfig.getOpenAiServiceList()){
            COUNT_FOR_OPEN_AI_SERVICE.put(openAiService, 0);
        }
    }

    public static List<String> getApiKeys(){
        return accountConfig.getApiKey();
    }

    public static List<String> getApiKeysPlus(){
        return accountConfig.getApiKeyPlus();
    }

    public static OpenAiService getOpenAiService(){
        //获取使用次数最小的openAiService 否则获取map中的第一个
        Optional<OpenAiService> openAiServiceToUse = COUNT_FOR_OPEN_AI_SERVICE.entrySet().stream()
        .min(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey);
        if (openAiServiceToUse.isPresent()){
            COUNT_FOR_OPEN_AI_SERVICE.put(openAiServiceToUse.get(), COUNT_FOR_OPEN_AI_SERVICE.get(openAiServiceToUse.get()) + 1);
            return  openAiServiceToUse.get();
        }else {
            COUNT_FOR_OPEN_AI_SERVICE.put(COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next(), COUNT_FOR_OPEN_AI_SERVICE.get(COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next()) + 1);
            return COUNT_FOR_OPEN_AI_SERVICE.keySet().iterator().next();
        }
    }
    public static CompletionRequest.CompletionRequestBuilder getCompletionRequestBuilder(){
        return completionRequestBuilder;
    }

    public static String getGpt4Prompt(String sessionId, String newPrompt, String basicPrompt) throws ChatException {
        if(StringUtils.isEmpty(basicPrompt)){
            basicPrompt = accountConfig.getBasicPrompt();
        }
        List<ChatMessage> chatMessages = new ArrayList<>();
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRole(MessageRole.SYSTEM.getName());
        systemMessage.setContent(basicPrompt);
        chatMessages.add(systemMessage);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(newPrompt);
        chatMessages.add(chatMessage);
        PROMPT_MAP.put(sessionId,chatMessages);
        String prompt = JSON.toJSONString(PROMPT_MAP.get(sessionId));

        //一个汉字大概两个token
        //预设回答的文字是提问文字数量的两倍
        if (newPrompt.length()>=100){
            throw new ChatException("问题太长了");
        }
        return prompt;
    }

    public static String getPrompt(String sessionId, String newPrompt, String basicPrompt) throws ChatException {
        // 如果提问者问题的长度超过设置的长度时 回复问题太长了 并重置他的map
        if (newPrompt.length() > accountConfig.getMaxToken()){
            PROMPT_MAP.remove(sessionId);
            throw new ChatException("问题太长了");
        }

        if (PROMPT_MAP.containsKey(sessionId)){
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(newPrompt);
            PROMPT_MAP.get(sessionId).add(chatMessage);
        } else {
            if(StringUtils.isEmpty(basicPrompt)){
                basicPrompt = accountConfig.getBasicPrompt();
            }
            List<ChatMessage> chatMessages = new ArrayList<>();
            ChatMessage systemMessage = new ChatMessage();
            systemMessage.setRole(MessageRole.SYSTEM.getName());
            systemMessage.setContent(basicPrompt);
            chatMessages.add(systemMessage);
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setContent(newPrompt);
            chatMessages.add(chatMessage);
            PROMPT_MAP.put(sessionId,chatMessages);
        }
        String prompt = JSON.toJSONString(PROMPT_MAP.get(sessionId));
        int length = PROMPT_MAP.get(sessionId).stream().filter(item -> "user".equals(item.getRole()))
                .mapToInt(item -> item.getContent().length())
                .sum();

        // 如果提问者一次会话所有问题的总长度超过设置的长度时 重置他的map 并重新生成一次会话
        if (accountConfig.getMaxToken() < (length + newPrompt.length())){
            PROMPT_MAP.remove(sessionId);
            return getPrompt(sessionId, newPrompt, basicPrompt);
        }
        return prompt;
    }

    public static void updatePrompt(String sessionId, String answer){
        if (PROMPT_MAP.containsKey(sessionId)){
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRole(MessageRole.ASSISTANT.getName());
            chatMessage.setContent(answer);
            PROMPT_MAP.get(sessionId).add(chatMessage);
        } else {
            List<ChatMessage> chatMessages = new ArrayList<>();
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setRole(MessageRole.ASSISTANT.getName());
            chatMessage.setContent(answer);
            chatMessages.add(chatMessage);
            PROMPT_MAP.put(sessionId,chatMessages);
        }
    }

    public static void resetPrompt(String sessionId){
        PROMPT_MAP.remove(sessionId);
    }

    public static void resetAll(){
        PROMPT_MAP.clear();
    }
}
