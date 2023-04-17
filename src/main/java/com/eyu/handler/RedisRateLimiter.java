package com.eyu.handler;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {
    private static final String KEY_PREFIX = "rate_limiter:";

    private static final String PROMPT_KEY_PREFIX = "UniquePrompt:";
    private static final int DEFAULT_LIMIT = 10; // 每小时访问次数限制的默认值
    private static final int DEFAULT_EXPIRE_TIME = 3600; // 一个小时的秒数的默认值

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public boolean isAllowed(String key) {
        String redisKey = KEY_PREFIX + key;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 获取每小时访问次数限制和过期时间的值，如果不存在则使用默认值
        int limit = Integer.parseInt(ops.get("limit:" + key) != null ? ops.get("limit:" + key) : String.valueOf(DEFAULT_LIMIT));
        int expireTime = Integer.parseInt(ops.get("expire_time:" + key) != null ? ops.get("expire_time:" + key) : String.valueOf(DEFAULT_EXPIRE_TIME));

        Long count = ops.increment(redisKey, 1);
        if (count == 1) {
            redisTemplate.expire(redisKey, expireTime, TimeUnit.SECONDS);
        }
        return count <= limit;
    }

    public void setPrompt(String sessionId, String prompt){
        redisTemplate.opsForValue().set(PROMPT_KEY_PREFIX + sessionId, prompt);
    }

    public String getPrompt(String sessionId){
        String prompt = redisTemplate.opsForValue().get(PROMPT_KEY_PREFIX + sessionId);
        if (prompt == null || prompt.length()==0){
            prompt = redisTemplate.opsForValue().get("prompt");
        }
        return prompt;
    }
}
