> this is a voice assistant app

# GPT_Voice_Assistant_Java

## 1.Description

本项目是基于GPT-NLP的语音助手，使用了百度的语音识别和语音合成，使用了Picovoice的唤醒词，使用了OpenAI的GPT模型

## 2.Function

通过唤醒词唤醒后，进行语音对话，对话结束后，再次唤醒，进行下一次对话

## 3.Features

1. 可在PC（笔记本、台式机）实现语音助手功能
2. 运行在树莓派（Raspberry Pi）增加麦克风+喇叭可实现完整语音助手功能

## 4.Project structure

1. 基于百度平台的语音识别和语音合成
2. 基于Picovoice的唤醒词
3. 基于OpenAI的GPT模型
4. 基于javax.sound.sampled 自适应录音 连续X秒音量低于阈值，自动结束录音
5. 基于Java11

## 5.Config.json-Settings

| key                    | description         | default value                  | required |
|------------------------|---------------------|--------------------------------|----------|
| logLevel               | 日志级别                | info                           | false    |
| inputSampleRate        | 输入采样率               | 16000                          | false    |
| inputSampleSizeInBits  | 输入采样位数              | 16                             | false    |
| inputAudioDeviceNum    | 输入音频设备编号            | 5                              | false    |
| outputSampleRate       | 输出采样率               | 16000                          | false    |
| outputSampleSizeInBits | 输出采样位数              | 16                             | false    |
| outputAudioDeviceNum   | 输出音频设备编号            | 5                              | false    |
| maxRecordTime          | 最大录音时间              | 10                             | false    |
| volumeThreshold        | 音量阈值(百分比)           | 4                              | false    |
| lowVolumeDuration      | 低音量持续时间(秒)          | 2                              | false    |
| sensitivities          | 唤醒词敏感度              | 0.9                            | false    |
| modelPath              | 唤醒词模型路径             | ppn/hey-zero_en_mac_v2_1_0.ppn | false    |
| accessKey              | Porcupine的accessKey | ""                             | true     |
| nlpUrl                 | GPT-NLP服务地址         | ""                             | true     |
| nlpUrlHeader           | GPT-NLP服务请求头        | ""                             | true     |
| nlpRequestHeaderValue  | GPT-NLP服务请求头中的value | ""                             | true     |
| nlpUrlBody             | GPT-NLP服务请求体        | ""                             | true     |
| nlpResponseKey         | GPT-NLP服务响应体中的key   | ""                             | true     |
| baiduAppId             | 百度语音识别的AppId        | ""                             | true     |
| baiduApiKey            | 百度语音识别的ApiKey       | ""                             | true     |
| baiduSecretKey         | 百度语音识别的SecretKey    | ""                             | true     |

```json
{
  "modelPath": "ppn/hey-zero_en_mac_v2_1_0.ppn",
  "accessKey": "",
  "sensitivities": 0.9,
  "nlpUrl": "http://xxx:xxx/api/v1/chat",
  "nlpRequestKey": "question",
  "nlpResponseKey": "data.answer",
  "nlpRequestHeader": "Authorization",
  "nlpRequestHeaderValue": "xxxx",
  "baiduAppId": "xxxx",
  "baiduApiKey": "xxxx",
  "baiduSecretKey": "xxxx"
}

```

## 6.How to run

```command
java run WakeUp.main or build and run jar
```

# 7.Question

> 如何生成自己的唤醒词？

见 https://console.picovoice.ai/

> 如何注册百度语音识别和语音合成？

见 https://ai.baidu.com/tech/speech/