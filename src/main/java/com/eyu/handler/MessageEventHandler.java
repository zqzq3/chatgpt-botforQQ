package com.eyu.handler;

import com.eyu.entity.bo.ChatBO;
import com.eyu.exception.ChatException;
import com.eyu.service.InteractService;
import com.eyu.util.BotUtil;
import net.mamoe.mirai.contact.Contact;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private static final String GPT4_WORD = "高级模式";

    private static final String GPT3_WORD = "普通模式";

    private static final String HELP_WORD = "#帮助";

    private static final String SET_WORD = "#设置";

    private static final String GET_WORD = "#显示";

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
                chatBO.setSessionId(String.valueOf(event.getSender().getId()));
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
        if (GET_WORD.startsWith(prompt)) {
            String uniquePrompt = interactService.getUniquePrompt(chatBO.getSessionId());
            if (StringUtils.isEmpty(uniquePrompt)) {
                MessageChain messages = new MessageChainBuilder()
                        .append(new QuoteReply(event.getMessage()))
                        .append("你还没有设置,目前使用的是默认配置!对我说 #设置 跟上你的prompt")
                        .build();
                event.getSubject().sendMessage(messages);
                return;
            }
            //检测到帮助会话指令
            MessageChain messages = new MessageChainBuilder()
                    .append(new QuoteReply(event.getMessage()))
                    .append(uniquePrompt)
                    .build();
            event.getSubject().sendMessage(messages);
            return;
        }
        if (HELP_WORD.equals(prompt)) {
            //检测到帮助会话指令
            MessageChain messages = new MessageChainBuilder()
                    .append(new QuoteReply(event.getMessage()))
                    .append("对我说 #设置 跟上你的prompt来设置\n设置完成后,之前的对话会被移除哦 \n对我说#显示 获取你现在的prompt")
                    .build();
            event.getSubject().sendMessage(messages);
            return;
        }
        if (prompt.startsWith(SET_WORD)) {
            prompt = Arrays.stream(prompt.split("\\s+"))
                    .skip(1) // 跳过第一个单词 "#设置:"
                    .collect(Collectors.joining(" "));
            MessageChain messages;
            if (StringUtils.isEmpty(prompt) || prompt.length() > 1500){
                messages = new MessageChainBuilder()
                        .append(new QuoteReply(event.getMessage()))
                        .append("设的太长了,爬")
                        .build();
            } else {
                interactService.setUniquePrompt(chatBO.getSessionId(), prompt);
                messages = new MessageChainBuilder()
                        .append(new QuoteReply(event.getMessage()))
                        .append("耶!设置成功")
                        .build();
                BotUtil.resetPrompt(chatBO.getSessionId());
            }
            event.getSubject().sendMessage(messages);
            return;
        }
//        if (GPT4_WORD.equals(prompt)) {
//            //检测到重置会话指令
//            BotUtil.setModel(chatBO.getSessionId(), "gpt-4");
//            BotUtil.resetPrompt(chatBO.getSessionId());
//            event.getSubject().sendMessage("高级模式切换成功");
//            return;
//        }
//        if (GPT4_WORD.equals(prompt)) {
//            //检测到重置会话指令
//            BotUtil.setModel(chatBO.getSessionId(), "gpt-4");
//            BotUtil.resetPrompt(chatBO.getSessionId());
//            event.getSubject().sendMessage("高级模式切换成功");
//            return;
//        }
//
//        if (GPT3_WORD.equals(prompt)) {
//            //检测到重置会话指令
//            BotUtil.setModel(chatBO.getSessionId(), "gpt-3.5-turbo");
//            BotUtil.resetPrompt(chatBO.getSessionId());
//            event.getSubject().sendMessage("普通模式切换成功");
//            return;
//        }

        if (RESET_ALL_WORD.equals(prompt)) {
            //检测到重置会话指令
            BotUtil.resetAll();
            event.getSubject().sendMessage("重置一切成功");
            return;
        }
        if (RESET_WORD.equals(prompt)) {
            //检测到重置会话指令
            BotUtil.resetPrompt(chatBO.getSessionId());
//            BotUtil.setModel(chatBO.getSessionId(), "gpt-3.5-turbo");
            event.getSubject().sendMessage("重置会话成功");
        } else {
            CompletableFuture<String> future;
            try {
                String basicPrompt = "";
                if(prompt.contains("图片")) {
//                    BotUtil.setModel(chatBO.getSessionId(), "gpt-3.5-turbo");
                    BotUtil.resetPrompt(chatBO.getSessionId());
                    basicPrompt = "请按照以下规则给我发送图片：1.使用markdown格式；2.使用unsplash API；3.使用\" ![imgae]https://source.unsplash.com/featured/?<已翻译的英文内容> \"格式回复；4.不要使用代码块，不要描述其他内容，不要解释；5.根据我输入的内容生成对应格式；";
                }
                chatBO.setPrompt(prompt);
                future = interactService.chat(chatBO, basicPrompt);

                // 处理获取到的结果
                future.thenAccept(response -> {
                    // 处理获取到的结果
                    try {
                        Pattern pattern = Pattern.compile("!\\[.+\\]\\((.+?)\\)");
                        Matcher matcher = pattern.matcher(response);
                        if (matcher.find()) {
                            String imageUrl = matcher.group(1);
                            MessageChain messages = new MessageChainBuilder()
                                    .append(new QuoteReply(event.getMessage()))
                                    .append("你要的图片\n")
                                    .append(Image.fromId(getImageId(event.getSubject(), imageUrl)))
                                    .build();
                            event.getSubject().sendMessage(messages);
                        } else {

                            if(response.contains("😈: ")){
                                String delimiter = "😈: ";
                                int index = response.indexOf(delimiter);

                                if (index != -1) {
                                    response = response.substring(index + delimiter.length());
                                }
                            }

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
                });

                // 处理异常
                future.exceptionally(e -> {
                    // 处理异常
                    MessageChain messages = new MessageChainBuilder()
                            .append(new QuoteReply(event.getMessage()))
                            .append(e.getMessage())
                            .build();
                    event.getSubject().sendMessage(messages);
                    return null;
                });

            }catch (ChatException e){
                MessageChain messages = new MessageChainBuilder()
                        .append(new QuoteReply(event.getMessage()))
                        .append(e.getMessage())
                        .build();
                event.getSubject().sendMessage(messages);
            }
        }
    }

    public String getImageId(Contact contact, String urlLink) throws IOException {
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
        contact.uploadImage(resource);
        String result = resource.calculateResourceId();
        resource.close();
        return result;
    }
}