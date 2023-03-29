package com.eyu.handler;

import com.eyu.entity.bo.ChatBO;
import com.eyu.exception.ChatException;
import com.eyu.service.InteractService;
import com.eyu.util.BotUtil;
import net.mamoe.mirai.contact.MessageTooLargeException;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 事件处理
 *
 * @author zqzq3
 * @date 2023/2/1
 */
@Component
public class MessageEventHandler implements ListenerHost {
    @Resource
    private InteractService interactService;

    private static final String RESET_WORD = "重置会话";

    private static final String RESET_ALL_WORD = "RESET ALL";

    /**
     * 监听消息并把ChatGPT的回答发送到对应qq/群
     * 注：如果是在群聊则需@
     *
     * @param event 事件 ps:此处是MessageEvent 故所有的消息事件都会被监听
     */
    @EventHandler
    public void onMessage(@NotNull MessageEvent event){
        boolean flag = decide(event.getMessage().contentToString());
        if(flag){
            return;
        }
        ChatBO chatBO = new ChatBO();
        chatBO.setSessionId(String.valueOf(event.getSubject().getId()));
        if (event.getBot().getGroups().contains(event.getSubject().getId())) {
            //如果是在群聊
            if (event.getMessage().contains(new At(event.getBot().getId()))) {
                //存在@机器人的消息就向ChatGPT提问
                //去除@再提问
                String prompt = event.getMessage().contentToString().replace("@" + event.getBot().getId(), "").trim();
                response(event, chatBO, prompt);
            }
        } else {
            //不是在群聊 则直接回复
            String prompt = event.getMessage().contentToString().trim();
            response(event, chatBO, prompt);
        }
    }

    private boolean decide(String str) {
        //此处可以加屏蔽字 来屏蔽一些不想回复的信息
        return false;
    }

    private void response(@NotNull MessageEvent event, ChatBO chatBO, String prompt) {
        if (RESET_ALL_WORD.equals(prompt)) {
            //检测到重置会话指令
            BotUtil.resetAll();
            event.getSubject().sendMessage("重置一切成功");
            return;
        }
        if (RESET_WORD.equals(prompt)) {
            //检测到重置会话指令
            BotUtil.resetPrompt(chatBO.getSessionId());
            event.getSubject().sendMessage("重置会话成功");
        } else {
            String response;
            try {
                String basicPrompt = "";
                if(prompt.contains("图片")) {
                    BotUtil.resetPrompt(chatBO.getSessionId());
                    basicPrompt = "请按照以下规则给我发送图片：1.使用markdown格式；2.使用unsplash API；3.使用\" ![imgae]https://source.unsplash.com/featured/?<已翻译的英文内容> \"格式回复；4.不要使用代码块，不要描述其他内容，不要解释；5.根据我输入的内容生成对应格式；";
                }
                chatBO.setPrompt(prompt);
                response = interactService.chat(chatBO, basicPrompt);
            }catch (ChatException e){
                response = e.getMessage();
            }
            try {
                Pattern pattern = Pattern.compile("!\\[.+\\]\\((.+?)\\)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    String imageUrl = matcher.group(1);
                    MessageChain messages = new MessageChainBuilder()
                            .append(new QuoteReply(event.getMessage()))
                            .append("你要的图片")
                            .append(Image.fromId(getImageId(imageUrl)))
                            .build();
                    event.getSubject().sendMessage(messages);
                } else {
                    MessageChain messages = new MessageChainBuilder()
                            .append(new QuoteReply(event.getMessage()))
                            .append(response)
                            .build();
                    event.getSubject().sendMessage(messages);
                }
            }catch (MessageTooLargeException e){
                //信息太大，无法引用，采用直接回复
                event.getSubject().sendMessage(response);
            } catch (IOException e) {
                event.getSubject().sendMessage("图片处理失败");
            }
        }
    }

    public String getImageId(String urlLink) throws IOException {
        URL url = new URL(urlLink);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try (InputStream is = url.openStream()) {
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        }
        byte[] imageData = baos.toByteArray();
        ExternalResource resource;
        resource = ExternalResource.create(imageData);
        return resource.calculateResourceId();
    }
}