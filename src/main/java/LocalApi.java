import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.level.Level;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.common.Choice;
import com.unfbx.chatgpt.entity.completions.CompletionResponse;
import javazoom.jl.player.Player;
import lombok.Data;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class LocalApi {
    //log
    public static final Log log = LogFactory.get();

    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String TEMP_TTS_FILE_NAME = "temp_tts.wav";
    private static final String TEMP_RECORD_FILE_NAME = "temp_record.wav";
    private static final String TEMP_MP3_FILE_NAME = "temp_mp3.mp3";

    private static final String path = System.getProperty("user.dir") + '/';

    //music-config
    private static String STORE = "store/";
    private static String TEMP = "temp/";
    private static final String ZAINE_FILE = path + STORE + "zaine.mp3";
    private static final String tempRecordFilePath = path + TEMP + TEMP_RECORD_FILE_NAME;
    private static final String tempTtsFilePath = path + TEMP + TEMP_TTS_FILE_NAME;
    private static final String tempMp3FilePath = path + TEMP + TEMP_MP3_FILE_NAME;


    OpenAiClient openAiClient = null;


    //json-config
    private String paddleAsrUrl = "";
    private String paddleTtsUrl = "";

    //audio-config
    private int recordTime = 2000;
    private int unifiedSampleRate = 16000;
    private int sampleSizeInBits = 16;
    private int inputAudioDeviceNum = 0;
    private float sensitivities = 0.5f;
    private String modelPath = "";
    //Porcupine-key
    private String accessKey = "";

    //openaiKey
    private String openAiKey = "";
    //proxy
    private String proxyAddress = "";
    private Integer proxyPort = null;

    //音频相关配置
    private AudioFormat format;
    private TargetDataLine micDataLine;
    DataLine.Info dataLineInfo;


    private static int deviceNum;

    //初始化
    public LocalApi() {
        loadConfig();
        log.info("等待唤醒");
    }

    public void run() {
        //1.成功唤醒
        playAudio(ZAINE_FILE);
        //2.录音
        log.info("开始录音");
        recordAudio();
        //3.识别
        String question = getASR(tempRecordFilePath);
        log.info("识别结果:{}", question);
        //4.发送至GPT
        //播放思考一下
        //playAudio(SIKAOYIXIA_FILE);
//        String answer = getNLPContent(question);

        String answer = "";
        try {
            CompletionResponse completions = openAiClient.completions(question);
            List<Choice> collect = Arrays.stream(completions.getChoices()).collect(Collectors.toList());
            List<String> result = CollStreamUtil.toList(collect, Choice::getText);
            String join = CharSequenceUtil.join("", result);
            answer = CharSequenceUtil.replace(join, "\n\n", "\n");
        } catch (Exception e) {
            answer = "gpt响应失败";
            log.info("gpt响应失败");
            log.error(e);
        }


        log.info("gpt回答:{}", answer);
        //播放生成音频中
        //5.将文本生成音频
        log.info("开始生成音频");
        getTTS(answer);
        //6.音频转MP3
        convertAudioToMP3(tempTtsFilePath, tempMp3FilePath);
        //7.播放至声音
        log.info("开始播放声音");
        playAudio(tempMp3FilePath);
        log.info("等待唤醒");
    }

    private String getASR(String filePath) {
        //文件转base64
        File file = FileUtil.file(filePath);
        String base64 = Base64.encode(FileUtil.readBytes(file));
        JSONObject body = new JSONObject();
        body.set("audio", base64);
        body.set("audio_format", "wav");
        body.set("sample_rate", unifiedSampleRate);
        body.set("lang", "zh_cn");
        String result = HttpUtil.post(paddleAsrUrl, body.toStringPretty());
        JSONObject resultJson = new JSONObject(result);
        return resultJson.getByPath("result.transcription", String.class);

    }


    private void convertAudioToMP3(String oldFilePath, String newFilePath) {
        File source = new File(oldFilePath);
        File target = new File(newFilePath);
        try {
            //Audio Attributes
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setChannels(1);
            audio.setSamplingRate(unifiedSampleRate);

            //Encoding attributes
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            //Encode
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    /**
     * 播放音频
     *
     * @param filePath 音频文件路径
     */
    private void playAudio(String filePath) {
        try {
            BufferedInputStream buffer = new BufferedInputStream(Files.newInputStream(Paths.get(filePath)));
            Player player = new Player(buffer);
            player.play();
        } catch (Exception e) {
            log.info("音频文件:{},播放失败", filePath);
            log.error(e);
        }
    }


    private void recordAudio() {
        try {
            File file = new File(LocalApi.tempRecordFilePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            start();
            Thread.sleep(recordTime);
            stop();

        } catch (Exception e) {
            log.info("录音失败");
            log.error(e);
        }
    }

    private void getTTS(String text) {
        try {
            JSONObject body = new JSONObject();
            body.set("text", text);
            String post = HttpUtil.post(paddleTtsUrl, body.toStringPretty());
            log.debug("paddle-tts响应:{}", post);
            JSONObject result = new JSONObject(post);
            byte[] decode = Base64.decode(result.getByPath("result.audio", String.class));
            File file = new File(LocalApi.tempTtsFilePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            //把二进制流写入文件
            FileUtil.writeBytes(decode, file);
        } catch (Exception e) {
            log.info("tts失败");
            log.error(e);
        }
    }

    private void loadConfig() {
        try {
            String configPath = path + CONFIG_FILE_NAME;
            JSONObject config = new JSONObject(FileUtil.readBytes(configPath));
            String logLevel = config.getStr("logLevel");
            if (CharSequenceUtil.isBlank(logLevel)) {
                logLevel = "info";
            }
            //初始化日志级别
            log.isEnabled(Level.valueOf(logLevel.toUpperCase()));
            //打印配置
            log.info("your-config:\n{}", config.toStringPretty());

            paddleAsrUrl = config.getStr("paddleAsrUrl");
            paddleTtsUrl = config.getStr("paddleTtsUrl");
            recordTime = config.getInt("recordTime");
            unifiedSampleRate = config.getInt("unifiedSampleRate");
            sampleSizeInBits = config.getInt("sampleSizeInBits");
            inputAudioDeviceNum = config.getInt("inputAudioDeviceNum");
            modelPath = config.getStr("modelPath");
            accessKey = config.getStr("accessKey");
            sensitivities = config.getFloat("sensitivities");

            //初始化GPT
            openAiKey = config.getStr("openAiKey");
            proxyAddress = config.getStr("proxyAddress");
            proxyPort = config.getInt("proxyPort");
            if (CharSequenceUtil.isBlank(proxyAddress) || ObjectUtil.isNull(proxyPort)) {
                openAiClient = new OpenAiClient(openAiKey);
            } else {
                openAiClient = new OpenAiClient(openAiKey,
                        50, 50, 50,
                        new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort)));
            }

            //初始化音频
            initAudio();

        } catch (Exception e) {
            log.error("加载配置异常", e);
        }
    }

    private void saveConfig(JSONObject config) {
        try {
            String configPath = path + CONFIG_FILE_NAME;
            FileUtil.writeUtf8String(config.toStringPretty(), configPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAudio() {
        showAudioDevices();
        format = new AudioFormat(unifiedSampleRate, sampleSizeInBits, 1, true, false);
        dataLineInfo = new DataLine.Info(TargetDataLine.class, format);

        micDataLine = getAudioDevice(inputAudioDeviceNum, new DataLine.Info(TargetDataLine.class, format));
    }

    /**
     * 获取音频设备
     * throws LineUnavailableException
     *
     * @param deviceIndex  自定义音频设备
     * @param dataLineInfo 音频设备信息
     * @return 音频设备
     */
    private TargetDataLine getAudioDevice(int deviceIndex, DataLine.Info dataLineInfo) {
        //通过索引获取音频设备
        if (deviceIndex >= 0) {
            try {
                Mixer.Info mixerInfo = AudioSystem.getMixerInfo()[deviceIndex];
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                if (mixer.isLineSupported(dataLineInfo)) {
                    return (TargetDataLine) mixer.getLine(dataLineInfo);
                } else {
                    LocalApi.log.info("自定义音频设备不支持,索引:{}", deviceIndex);
                }
            } catch (Exception e) {
                LocalApi.log.info("自定义音频设备不存在,索引:{}", deviceIndex);
                LocalApi.log.error(e);
            }
        }

        return getDefaultCaptureDevice(dataLineInfo);
    }

    /**
     * 显示所有音频设备
     */
    private void showAudioDevices() {

        Mixer.Info[] allMixerInfo = AudioSystem.getMixerInfo();
        Line.Info captureLine = new Line.Info(TargetDataLine.class);

        for (int i = 0; i < allMixerInfo.length; i++) {

            Mixer mixer = AudioSystem.getMixer(allMixerInfo[i]);
            if (mixer.isLineSupported(captureLine)) {
                LocalApi.log.info("Device {} : {}", i, allMixerInfo[i].getName());
            }
        }
    }

    /**
     * 获取默认音频设备
     * throws LineUnavailableException
     *
     * @param dataLineInfo 音频设备信息
     * @return 音频设备
     */
    private TargetDataLine getDefaultCaptureDevice(DataLine.Info dataLineInfo) {

        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            LocalApi.log.info("当前音频设备不支持");
            System.exit(1);
        }
        try {
            return (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        } catch (Exception e) {
            LocalApi.log.info("当前音频设备不支持");
            LocalApi.log.error(e);
            System.exit(1);
            return null;
        }
    }

    private void start() {
        try {
            micDataLine.open(format);
        } catch (Exception e) {
            LocalApi.log.info("打开录音设备失败");
            LocalApi.log.error(e);
        }

        micDataLine.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                AudioInputStream ais = new AudioInputStream(micDataLine);
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(tempRecordFilePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void stop() {
        micDataLine.stop();
        micDataLine.close();
    }
}
