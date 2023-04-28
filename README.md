# ChatGpt-BotForQQ

## 介绍

an **unofficial** implement of ChatGPT in **Tencent QQ**.

🌹🌹🌹感谢[acheong08/ChatGPT](https://github.com/acheong08/ChatGPT)、[PlexPt/chatgpt-java](https://github.com/PlexPt/chatgpt-java)、[TheoKanning/openai-java](https://github.com/TheoKanning/openai-java)和[mamoe/mirai](https://github.com/mamoe/mirai.git) 🌹🌹🌹

## 原理

使用mirai登录qq并监听消息->调用openai接口将消息向gpt提问->使用mirai在qq里回复gpt的回答

## 特性
- qq登录失败时会尝试更换登陆方式进行重新登录，能一定程度上减少qq风控的影响
- 回复为引用回复，且默认情况下，在群聊需@才会回复
- 支持上下文对话。向机器人发送 “重置会话” 可以清除会话历史
- 支持使用多个apiKey。在此情况下，会优先调用使用次数最少的apiKey，达到避免同一个api请求过多造成的Http500/503问题的目的
- 不定期更新最新api

## 使用

你只需要

1.  clone本项目

2.  拥有

    -   一个OpenAI账号

    -   一个qq号

        并把它们配置在application.yml里:

```
//这是application.yml文件
#ChatGPT
#有多少apiKey就写多少，不要留空白
apiKey:
  - sk-xxxx
  - sk-xxxx
  - sk-xxxx

#qq
qq : 123456
password : xxxx
```

3.  然后启动

tips：机器人响应速度与你的网络环境挂钩。

<iframe style="width:100%;height:auto;min-width:600px;min-height:400px;" src="https://star-history.com/embed?secret=Z2hwX0tkb3JLbnBSTTNoT1pTblNYN2c0alNnaTVKdDBSVzBhN2piZw==#zqzq3/chatgpt-botforQQ&Date" frameBorder="0"></iframe>
