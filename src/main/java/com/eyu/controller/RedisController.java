package com.eyu.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import javax.annotation.Resource;
 
/***
 * @date: 2022/5/10 
 * @author: fenghaikuan
 * @description: TODO
 */
@RestController
public class RedisController {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @GetMapping("redis/set")
    public String setValue(@RequestParam("prompt") String prompt){
        stringRedisTemplate.opsForValue().set("prompt",prompt);
        return "set succ";
    }
    @GetMapping("redis/get")
    public String getValue(){
        String key = "prompt";
        String result = stringRedisTemplate.opsForValue().get(key);
        return result;
    }
}
