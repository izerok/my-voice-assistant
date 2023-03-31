import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.level.Level;
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatChoice;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import lombok.Data;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class LocalApi {
    //log
    public static final Log log = LogFactory.get();
    private String logLevel;


    //audio-config

    private Integer inputSampleRate;
    private Integer inputSampleSizeInBits;
    private Integer inputAudioDeviceNum;

    // 最大录音时长
    private Integer maxRecordTime;
    // 录音的音量阈值 如果低于该阈值x秒，则停止录音 高于该阈值则开始录音
    private int recordVolumePercent;
    // 未达标录音音量阈值x秒
    private int lessRecordVolumeTime;


    //音频相关配置
    private AudioFormat inputFormat;
    private TargetDataLine inputLine;
    private DataLine.Info inputDataLineInfo;

    /**
     * 百度语音相关配置
     */
    private String baiduAppId;
    private String baiduApiKey;
    private String baiduSecretKey;

    private RealTimeVolumeRecorder recorder;
    private AipSpeech aipSpeech;

    // 获取当前对话的上下文
    List<Message> talkHistory = new ArrayList<>();
    private OpenAiClient openAiClient;

    private List<String> openaiKey;
    private String openaiHost;

    //初始化
    public LocalApi() {
        loadConfig();
        // 初始化录音
        recorder = new RealTimeVolumeRecorder(inputFormat, inputLine,
                Const.getAudioTempFilePath(Const.TEMP_RECORD_FILE_NAME),
                maxRecordTime, recordVolumePercent, lessRecordVolumeTime);
        // 初始化百度语音
        aipSpeech = new AipSpeech(baiduAppId, baiduApiKey, baiduSecretKey);
        // 初始化openai
        openAiClient = OpenAiClient.builder()
                .apiKey(openaiKey)
                .apiHost(CharSequenceUtil.isBlank(this.openaiHost) ? "https://api.openai.com/" : this.openaiHost)
                .build();

    }

    public void run() {

        log.info("{}--1.开始录音", DateUtil.now());
        recorder.start();
        log.info("{}--1.录音结束", DateUtil.now());

        AudioUtil.playAudio(Const.getThinkingFilePathByRandom());

        log.info("{}--2.开始获取ASR结果", DateUtil.now());
        String asr = getBaiDuASR(Const.getAudioTempFilePath(Const.TEMP_RECORD_FILE_NAME));
        log.info("{}--2.获取ASR结果结束,结果{}", DateUtil.now(), asr);
        if (CharSequenceUtil.isBlank(asr)) {
            log.info("当前对话结束");
            return;
        }

        log.info("{}--3.开始获取NLP结果", DateUtil.now());
        String answer = getNLPAnswer(asr);
        log.info("{}--3.获取NLP结果结束,结果{}", DateUtil.now(), answer);
        if (CharSequenceUtil.isBlank(answer)) {
            log.info("当前对话结束");
            return;
        }

        log.info("{}--4.开始生成音频", DateUtil.now());
        boolean successFlag = this.getBaiDuTTS(answer, Const.getAudioTempFilePath(Const.TEMP_TTS_FILE_NAME));
        log.info("{}--4.生成音频结束", DateUtil.now());
        if (!successFlag) {
            log.info("当前对话结束");
            return;
        }

        log.info("{}--5.开始播放声音", DateUtil.now());
        AudioUtil.playAudio(Const.getAudioTempFilePath(Const.TEMP_TTS_FILE_NAME));
        log.info("{}--5.播放结束", DateUtil.now());
    }


    private void loadConfig() {
        try {
            File configFile = new File(Const.getFilePath(Const.CONFIG_FILE_NAME));
            boolean exist = FileUtil.exist(configFile);
            if (!exist) {
                log.error("配置文件不存在");
                System.exit(1);
                return;
            }
            JSONObject config = JSONUtil.readJSONObject(configFile, StandardCharsets.UTF_8);
            formatDataElseGetDefault(config);

            //初始化日志级别
            log.isEnabled(Level.valueOf(logLevel.toUpperCase()));
            //打印配置
            log.info("your-config:\n{}", config.toStringPretty());

            log.info("初始化完成:{}", JSONUtil.toJsonPrettyStr(this));
            //初始化音频
            initInputAudio();
        } catch (Exception e) {
            log.error("加载配置异常", e);
        }
    }

    /**
     * 格式化数据,如果没有则返回默认值
     *
     * @param data 数据
     */
    public void formatDataElseGetDefault(JSONObject data) {
        Const[] values = Const.values();
        for (Const constEnum : values) {
            Object value = null;
            if (data.containsKey(constEnum.getKeyName())) {
                value = data.get(constEnum.getKeyName());
            }
            if (constEnum.isRequired() && value == null) {
                log.error("配置文件中缺少必要的配置项:{}", constEnum.getKeyName());
                System.exit(0);
            }
            if (ObjectUtil.isNull(value)) {
                value = constEnum.getDefaultValue();
            }
            if (ObjectUtil.isNotNull(value)) {
                BeanUtil.setFieldValue(this, constEnum.getKeyName(), value);
            }
        }
    }


    /**
     * 初始化输入音频
     */
    private void initInputAudio() {
        AudioUtil.showAudioDevices();
        inputFormat = new AudioFormat(inputSampleRate, inputSampleSizeInBits, 1, true, false);
        inputDataLineInfo = new DataLine.Info(TargetDataLine.class, inputFormat);

        inputLine = AudioUtil.getInputAudioDevice(inputAudioDeviceNum, new DataLine.Info(TargetDataLine.class, inputFormat));
    }


    private String getNLPAnswer(String question) {
        // 获取当前对话的上下文
        log.info("当前对话的上下文: {}", JSONUtil.toJsonStr(talkHistory));

        // 组装问题
        Message message = Message.builder().content(question).role(Message.Role.USER).build();
        talkHistory.add(message);
        String aiAnswer = null;
        try {
            // 组装AI请求参数
            ChatCompletion completion = ChatCompletion.builder()
                    .model(ChatCompletion.Model.GPT_3_5_TURBO.getName())
                    .user("zerozzccccccc")
                    .messages(talkHistory).build();

            // 调用AI接口
            ChatCompletionResponse completions = openAiClient.chatCompletion(completion);

            // 格式化AI回复内容
            aiAnswer = contentFormat(completions);
        } catch (Exception e) {
            log.error("调用AI接口异常", e);
        }
        return aiAnswer;
    }

    private boolean getBaiDuTTS(String answer, String filePath) {
        try {
            TtsResponse res = aipSpeech.synthesis(answer, "zh", 1, null);

            byte[] data = res.getData();
            FileUtil.del(filePath);
            File file = FileUtil.newFile(filePath);
            FileUtil.touch(file);
            //把二进制流写入文件
            FileUtil.writeBytes(data, file);
        } catch (Exception e) {
            log.error("生成音频异常", e);
            return false;
        }
        return true;
    }

    private String getBaiDuASR(String filePath) {
        // 识别本地文件
        org.json.JSONObject asrRes = aipSpeech.asr(filePath, "wav", inputSampleRate, null);
        log.info("识别结果:{}", asrRes);
        if (asrRes.has("result")) {
            String question = asrRes.getJSONArray("result").get(0).toString();
            log.info("识别结果:{}", question);
            return question;
        }
        return null;
    }

    /**
     * 格式化AI回复内容
     *
     * @param completions AI回复内容
     * @return 格式化后的内容
     */
    private String contentFormat(ChatCompletionResponse completions) {
        List<Message> messages = CollStreamUtil.toList(completions.getChoices(), ChatChoice::getMessage);
        List<String> result = CollStreamUtil.toList(messages, Message::getContent);
        String join = CharSequenceUtil.join("", result);
        return CharSequenceUtil.replace(join, "\n\n", "\n");
    }
}
