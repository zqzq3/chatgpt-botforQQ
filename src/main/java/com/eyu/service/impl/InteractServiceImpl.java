package com.eyu.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eyu.entity.bo.ChatBO;
import com.eyu.exception.ChatException;
import com.eyu.handler.RedisRateLimiter;
import com.eyu.service.InteractService;
import com.eyu.util.BotUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 交互服务impl
 *
 * @author zqzq3
 * @date 2022/12/10
 */
@Service
@Slf4j
public class InteractServiceImpl implements InteractService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    RedisRateLimiter rateLimiter;

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String getPrompt(String key) {
        String result = null;
        try {
            result = redisTemplate.opsForValue().get(key);
        } catch (Exception e){
            log.error("redis连接异常信息:{}", ExceptionUtils.getStackTrace(e));
        }
        return result;
    }

    public String getModel(String sessionId) {
        return "gpt-3.5-turbo";
//        String model = BotUtil.getModel(sessionId);
//        if (StringUtils.isEmpty(model)) {
//            model = "gpt-3.5-turbo";
//        }
//        return model;
    }

    private final Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 7890));

    private OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(8, 30, TimeUnit.SECONDS))
            .proxy(proxy)
            .build();

    private List<String> apiKeys = null;

    private List<String> apiKeysPlus = null;

    private int counter = 0;

    private int plusCounter = 0;

    public String getNextKey(String model) {
        if (model.contains("gpt-4")){
            if(apiKeysPlus == null){
                apiKeysPlus = BotUtil.getApiKeysPlus();
            }
            if(counter >= Integer.MAX_VALUE - 1){
                plusCounter = 0;
            }
            int index = plusCounter % apiKeysPlus.size();
            plusCounter++;
            return apiKeysPlus.get(index);
        }
        if(apiKeys == null){
            apiKeys = BotUtil.getApiKeys();
        }
        if(counter >= Integer.MAX_VALUE - 1){
            counter = 0;
        }
        int index = counter % apiKeys.size();
        counter++;
        return apiKeys.get(index);
    }

    @Override
    public String chat(ChatBO chatBO,String systemPrompt) throws ChatException {
        String model = getModel(chatBO.getSessionId());
//        if(model.contains("gpt-4")){
//            if (!rateLimiter.isAllowed(chatBO.getSessionId())) {
//                // 访问被限制
//                return "你话太密了,请找管理员解除限制";
//            }
//        }
        String basicPrompt;

        if(StringUtils.isNotBlank(systemPrompt)){
            basicPrompt = getPrompt("picturePrompt");
            if(basicPrompt == null){
                basicPrompt = systemPrompt;
            }
        } else {
            basicPrompt = rateLimiter.getPrompt(chatBO.getSessionId());
            if(basicPrompt == null || basicPrompt.length() == 0){
                basicPrompt = "请简洁回答";
            }
        }
        String prompt;
        if(model.contains("gpt-4")){
            prompt = BotUtil.getGpt4Prompt(chatBO.getSessionId(), chatBO.getPrompt(), basicPrompt);
        } else {
            prompt = BotUtil.getPrompt(chatBO.getSessionId(), chatBO.getPrompt(), basicPrompt);
        }


        //向gpt提问
        String answer = null;
        try {
            answer = getAnswer(prompt, model);
        }catch (HttpException e){
            log.error("向gpt提问失败,提问内容：{},原因：{}", chatBO.getPrompt(), e.getMessage(), e);
            if (500 == e.code() || 503 == e.code() || 429 == e.code()){
                log.info("尝试重新发送");
                try {
                    //可能是同时请求过多，尝试重新发送
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    log.error("进程休眠失败，原因：{}", ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }
                return chat(chatBO, systemPrompt);
            }
        } catch (InterruptedException e) {
            ;
        }
        if (null == answer || answer.equals("")){
            BotUtil.resetPrompt(chatBO.getSessionId());
            throw new ChatException("我无了 稍后再试下吧");
        }
        BotUtil.updatePrompt(chatBO.getSessionId(), answer);
        return answer;
    }

    public String getAnswer(String prompt, String model) throws InterruptedException {
        String content = "";
        if (client == null) {
            client = new OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(8, 30, TimeUnit.SECONDS))
                    .proxy(proxy)
                    .build();
        }
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n  \"model\": \"" + model + "\",\n  \"messages\": "+prompt+"\n}");
        int retryCount = 0;
        boolean success = false;
        while (!success && retryCount < 3) { // 最多重试3次
            try {
                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .method("POST", body)
                        .addHeader("Authorization", "Bearer "+ getNextKey(model))
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseStr = responseBody.string();
                    JSONObject jsonObject = JSONObject.parseObject(responseStr);
                    JSONArray jsonArray = jsonObject.getJSONArray("choices");
                    JSONObject result = jsonArray.getJSONObject(0);
                    content = result.getJSONObject("message").getString("content");
                }
                success = true; // 成功获取到答案，退出重试
            } catch (Exception e) {
                log.error("向gpt提问失败，提问内容：{}，原因：{}", prompt, e.getMessage(), e);
                Thread.sleep(3000);
                retryCount++;
            }
        }
//        content = content.trim().replaceAll("\n", "");

        return content;
    }

    @Override
    public void setUniquePrompt(String sessionId, String prompt){
        rateLimiter.setPrompt(sessionId, prompt);
    }

    @Override
    public String getUniquePrompt(String sessionId){
        return rateLimiter.getPrompt(sessionId);
    }

}
