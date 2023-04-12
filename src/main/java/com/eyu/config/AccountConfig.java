package com.eyu.config;

import com.eyu.handler.MessageEventHandler;
import com.theokanning.openai.OpenAiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 帐户配置
 *
 * @author zqzq3
 * @date 2023/02/13
 */
@Slf4j
@Data
@Component
@ConfigurationProperties("account")
public class AccountConfig {
    @Resource
    ProxyConfig proxyConfig;
    private Long qq;
    private String password;
    private Bot qqBot;
    @Resource
    private MessageEventHandler messageEventHandler;

    private List<String> apiKey;
    private List<String> apiKeyPlus;
    private List<OpenAiService> openAiServiceList;
    private String basicPrompt;
    private Integer maxToken;
    private Double temperature;
    private String model;

    @PostConstruct
    public void init() {
        //配置代理
        if (null != proxyConfig.getHost() && !"".equals(proxyConfig.getHost())) {
            System.setProperty("http.proxyHost", proxyConfig.getHost());
            System.setProperty("https.proxyHost", proxyConfig.getHost());
        }
        if (null != proxyConfig.getPort() && !"".equals(proxyConfig.getPort())) {
            System.setProperty("http.proxyPort", proxyConfig.getPort());
            System.setProperty("https.proxyPort", proxyConfig.getPort());
        }
        //ChatGPT
        model = "gpt-3.5-turbo";
        maxToken = 1024;
        temperature = 0.8;
        basicPrompt = "简洁回答";
        openAiServiceList = new ArrayList<>();
        for (String apiKey : apiKey){
            apiKey = apiKey.trim();
            if (!"".equals(apiKey)){
                openAiServiceList.add(new OpenAiService(apiKey, Duration.ofSeconds(1000)));
                log.info("apiKey为 {} 的账号初始化成功", apiKey);
            }
        }
        FixProtocolVersion.fix();
        //qq
        //登录
        BotConfiguration.MiraiProtocol[] protocolArray = BotConfiguration.MiraiProtocol.values();
        BotConfiguration.MiraiProtocol protocol = protocolArray[4];
//        int loginCounts = 1;
//        for (BotConfiguration.MiraiProtocol protocol : miraiProtocols) {
//            try {
//                log.warn("正在尝试第 {} 次， 使用 {} 的方式进行登录", loginCounts++, protocol);
//                qqBot = BotFactory.INSTANCE.newBot(qq, password.trim(), new BotConfiguration(){{setProtocol(protocol);}});
//                qqBot.login();
//                log.info("成功登录账号为 {} 的qq, 登陆方式为 {}",qq, protocol);
//                //订阅监听事件
//                qqBot.getEventChannel().registerListenerHost(this.messageEventHandler);
//                break;
//            }catch (Exception e){
//                log.error("登陆失败，qq账号为 {}, 登陆方式为 {} ，原因：{}", qq, protocol, e.getMessage());
//                if (loginCounts > 3){
//                    log.error("经过多种登录方式仍然登陆失败，可能是密码错误或者受风控影响，请尝试修改密码、绑定手机号等方式提高qq安全系数或者待会再试试");
//                    System.exit(-1);
//                }
//            }
//        }
        int loginCounts = 1;
        for (int i = 0; i < 3; i++) {
            try {
                log.warn("正在尝试第 {} 次， 使用 {} 的方式进行登录", loginCounts++, protocol);
                qqBot = BotFactory.INSTANCE.newBot(qq, password.trim(), new BotConfiguration(){{setProtocol(protocol);}});
                qqBot.login();
                log.info("成功登录账号为 {} 的qq, 登陆方式为 {}",qq, protocol);
                //订阅监听事件
                qqBot.getEventChannel().registerListenerHost(this.messageEventHandler);
                break;
            }catch (Exception e){
                log.error("登陆失败，qq账号为 {}, 登陆方式为 {} ，原因：{}", qq, protocol, e.getMessage());
                if (loginCounts > 3){
                    log.error("经过多种登录方式仍然登陆失败，可能是密码错误或者受风控影响，请尝试修改密码、绑定手机号等方式提高qq安全系数或者待会再试试");
                    System.exit(-1);
                }
            }
        }
    }
}
