package com.eyu.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eyu.entity.bo.ChatBO;
import com.eyu.exception.ChatException;
import com.eyu.service.InteractService;;
import com.eyu.util.BotUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;
import retrofit2.HttpException;
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


    private OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(8, 30, TimeUnit.SECONDS))
            .build();

    private List<String> apiKeys = null;

    private int counter = 0;

    public String getNextKey() {
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
    public String chat(ChatBO chatBO) throws ChatException {

        String prompt = BotUtil.getPrompt(chatBO.getSessionId(), chatBO.getPrompt());

        //向gpt提问
        String answer = null;
        try {
            answer = getAnswer(prompt);
        }catch (HttpException e){
            log.error("向gpt提问失败，提问内容：{}，原因：{}", chatBO.getPrompt(), e.getMessage(), e);
            if (500 == e.code() || 503 == e.code() || 429 == e.code()){
                log.info("尝试重新发送");
                try {
                    //可能是同时请求过多，尝试重新发送
                    Thread.sleep(3000);
                } catch (InterruptedException ex) {
                    log.error("进程休眠失败，原因：{}", ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                }
                return chat(chatBO);
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

    public String getAnswer(String prompt) throws InterruptedException {
        String content = "";
        if (client == null) {
            client = new OkHttpClient().newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .connectionPool(new ConnectionPool(8, 30, TimeUnit.SECONDS))
                    .build();
        }
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n  \"model\": \"gpt-3.5-turbo\",\n  \"messages\": "+prompt+"\n}");
        int retryCount = 0;
        boolean success = false;
        while (!success && retryCount < 3) { // 最多重试3次
            try {
                Request request = new Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .method("POST", body)
                        .addHeader("Authorization", "Bearer "+ getNextKey())
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
        content = content.trim().replaceAll("\n", "");

        return content;
    }
}