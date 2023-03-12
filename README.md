> this is a voice assistant app

# GPT_Voice_Assistant_Java

## 1.Description

本项目是基于GPT的语音助手，使用了PaddleSpeech的语音识别和语音合成，使用了Picovoice的唤醒词，使用了OpenAI的GPT模型，使用了PyAudio录音，使用了PyAudioPlay播放音频，使用了PaddleSpeech进行语音特征

## 2.Function

通过唤醒词唤醒后，进行语音对话，对话结束后，再次唤醒，进行下一次对话

## 3.Features

1. 可在PC（笔记本、台式机）实现语音助手功能
2. 运行在树莓派（Raspberry Pi）增加麦克风+喇叭可实现完整语音助手功能

## 4.Project structure

1. 基于PaddleSpeech的语音识别和语音合成
2. 基于Picovoice的唤醒词
3. 基于OpenAI的GPT模型
4. 基于PyAudio录音
5. 基于PyAudioPlay播放音频
6. 基于Java11

## 5.Config.json-Settings

| key                 | description                |
|---------------------|----------------------------|
| paddleAsrUrl        | paddle asr service url     |
| paddleTtsUrl        | paddle tts service url     |
| recordTime          | record time                |
| unifiedSampleRate   | unified sample rate        |
| sampleSizeInBits    | sample size in bits        |
| inputAudioDeviceNum | input audio device num     |
| modelPath           | model path                 |
| accessKey           | porcupine key              |
| openAiKey           | open ai key                |
| proxyAddress        | proxy address (allow null) |
| proxyPort           | proxy port (allow null)    |
| sensitivities       | sensitivities              |
| logLevel            | log level                  |

```json

{
  "paddleAsrUrl": "http://xxx:xxxx/paddlespeech/asr",
  "paddleTtsUrl": "http://xxx:xxxx/paddlespeech/tts",
  "recordTime": 2000,
  "unifiedSampleRate": 16000,
  "sampleSizeInBits": 16,
  "inputAudioDeviceNum": 5,
  "modelPath": "ppn/hey-zero_en_mac_v2_1_0.ppn",
  "accessKey": "",
  "openAiKey": "",
  "proxyAddress": "127.0.0.1",
  "proxyPort": 7890,
  "sensitivities": 0.9,
  "logLevel": "info"
}
```

## 6.How to run

```command
java run Voice.main or build and run jar
```

# 7.Question

> 如何生成自己的唤醒词？

见 https://console.picovoice.ai/

> 如何搭建paddlespeech 服务

见 https://github.com/PaddlePaddle/PaddleSpeech