package config;

import audio.AudioUtil;
import audio.RecordUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baidu.aip.speech.AipSpeech;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.Message;
import enums.Const;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class LocalConfig {
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

    private RecordUtil recorder;
    private AipSpeech aipSpeech;

    // 获取当前对话的上下文
    List<Message> talkHistory = new ArrayList<>();
    private OpenAiClient openAiClient;

    private List<String> openaiKey;
    private String openaiHost;
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

            //打印配置
            log.info("your-config:\n{}", config.toStringPretty());

            log.info("初始化完成:{}", JSONUtil.toJsonPrettyStr(this));
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

    public LocalConfig() {
        loadConfig();
        initInputAudio();
        // 初始化录音
        recorder = new RecordUtil(inputFormat, inputLine,
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

    /**
     * 初始化输入音频
     */
    private void initInputAudio() {
        AudioUtil.showAudioDevices();
        inputFormat = new AudioFormat(inputSampleRate, inputSampleSizeInBits, 1, true, false);
        inputDataLineInfo = new DataLine.Info(TargetDataLine.class, inputFormat);

        inputLine = AudioUtil.getInputAudioDevice(inputAudioDeviceNum, new DataLine.Info(TargetDataLine.class, inputFormat));
    }
}
