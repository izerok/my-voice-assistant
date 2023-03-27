import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.FileResource;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.level.Level;
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import lombok.Data;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.nio.charset.StandardCharsets;

@Data
public class LocalApi {
    //log
    public static final Log log = LogFactory.get();
    private String logLevel;


    //audio-config

    private Integer inputSampleRate;
    private Integer inputSampleSizeInBits;
    private Integer inputAudioDeviceNum;

    private Integer outputSampleRate;
    private Integer outputSampleSizeInBits;
    private Integer outputAudioDeviceNum;

    // maxRecordTime
    private Integer maxRecordTime;
    private int volumeThreshold; // 自定义音量阈值（百分比）
    private int lowVolumeDuration; // 连续低于阈值的时长（毫秒）

    private Float sensitivities;
    private String modelPath;
    //Porcupine-key
    private String accessKey;

    private String nlpUrl;
    private String nlpRequestKey;
    private String nlpResponseKey;
    private String nlpRequestHeader;
    private String nlpRequestHeaderValue;

    //音频相关配置
    private AudioFormat inputFormat;
    private AudioFormat outputFormat;
    private TargetDataLine inputLine;
    private SourceDataLine outputLine;

    private DataLine.Info inputDataLineInfo;
    private DataLine.Info outputDataLineInfo;

    /**
     * 百度语音相关配置
     */
    private String baiduAppId;
    private String baiduApiKey;
    private String baiduSecretKey;

    private RealTimeVolumeRecorder recorder;
    private AipSpeech aipSpeech;

    private static int deviceNum;

    //初始化
    public LocalApi() {
        loadConfig();
        recorder = new RealTimeVolumeRecorder(inputFormat, inputLine,
                Const.getAudioTempFilePath(Const.TEMP_RECORD_FILE_NAME),
                volumeThreshold, lowVolumeDuration, maxRecordTime);
        aipSpeech = new AipSpeech(baiduAppId, baiduApiKey, baiduSecretKey);
        log.info("等待唤醒");
    }

    public void run() {

        AudioUtil.playAudio(new FileResource(Const.ZAINE_FILE_NAME.getKeyName()).getFile().getPath());

        log.info("{}--1.开始录音", DateUtil.now());
        recorder.start();
        log.info("{}--1.录音结束", DateUtil.now());

        log.info("{}--2.开始获取ASR结果", DateUtil.now());
        String asr = getBaiDuASR(Const.getAudioTempFilePath(Const.TEMP_RECORD_FILE_NAME));
        log.info("{}--2.获取ASR结果结束{}", DateUtil.now(), asr);

        log.info("{}--3.开始获取NLP结果", DateUtil.now());
        String answer = getNLPAnswer(asr);
        log.info("{}--3.获取NLP结果结束{}", DateUtil.now(), answer);

        //5.将文本生成音频
        log.info("{}--4.开始生成音频", DateUtil.now());
        this.getBaiDuTTS(answer, Const.getAudioTempFilePath(Const.TEMP_TTS_FILE_NAME));
        log.info("{}--4.生成音频结束", DateUtil.now());

        //6.播放至声音
        log.info("{}--5.开始播放声音", DateUtil.now());
        AudioUtil.playAudio(Const.getAudioTempFilePath(Const.TEMP_TTS_FILE_NAME));
        log.info("{}--5.播放结束", DateUtil.now());

        log.info("等待唤醒");
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
            initOutputAudio();
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

    /**
     * 初始化输出音频
     */
    private void initOutputAudio() {
        outputFormat = new AudioFormat(outputSampleRate, outputSampleSizeInBits, 1, true, false);
        outputDataLineInfo = new DataLine.Info(SourceDataLine.class, outputFormat);
        outputLine = AudioUtil.getOutputAudioDevice(outputAudioDeviceNum, new DataLine.Info(SourceDataLine.class, outputFormat));
    }


    private String getNLPAnswer(String question) {
        JSONObject reqBody = new JSONObject();
        reqBody.set(nlpRequestKey, question);
        //4.调用NLP
        String result = HttpUtil.createRequest(Method.POST, nlpUrl)
                .body(reqBody.toString())
                .header(nlpRequestHeader, nlpRequestHeaderValue)
                .execute().body();
        log.info("NLP响应:{}", result);
        if (CharSequenceUtil.isBlank(result)) {
            log.info("等待唤醒");
            throw new RuntimeException("NLP返回结果为空");
        }
        String answer = JSONUtil.getByPath(new JSONObject(result), nlpResponseKey, "");
        if (CharSequenceUtil.isBlank(answer)) {
            log.info("等待唤醒");
            throw new RuntimeException("NLP返回结果为空");
        }

        return answer;
    }

    private void getBaiDuTTS(String answer, String filePath) {
        TtsResponse res = aipSpeech.synthesis(answer, "zh", 1, null);

        byte[] data = res.getData();
        FileUtil.del(filePath);
        File file = FileUtil.newFile(filePath);
        FileUtil.touch(file);
        //把二进制流写入文件
        FileUtil.writeBytes(data, file);
    }

    private String getBaiDuASR(String filePath) {
        // 识别本地文件
        org.json.JSONObject asrRes = aipSpeech.asr(filePath, "wav", inputSampleSizeInBits, null);
        log.info("识别结果:{}", asrRes);
        if (asrRes.has("result")) {
            String question = asrRes.getJSONArray("result").get(0).toString();
            log.info("识别结果:{}", question);
            return question;
        }
        throw new RuntimeException("识别结果为空");
    }

}
