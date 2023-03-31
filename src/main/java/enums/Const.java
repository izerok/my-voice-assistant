package enums;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.Getter;

import java.io.File;

@Getter
public enum Const {


    /**
     * 文件路径相关 无默认值
     */
    SYS_PATH(System.getProperty("user.dir"), null, "系统路径", false),

    CONFIG_FILE_NAME("config.json", null, "配置文件名", false),

    TEMP_DIR("temp", null, "临时文件夹", false),
    THINKING_DIR("thinking", null, "思考音频文件", false),

    /**
     * 声音相关文件名
     */
    TEMP_TTS_FILE_NAME("temp_tts.wav", null, "临时TTS文件名", false),
    TEMP_RECORD_FILE_NAME("temp_record.wav", null, "临时录音文件名", false),
    SIKAOYIXIA_FILE_NAME("sikaoyixia.wav", null, "思考一下", false),

    /**
     * 音频设备相关配置
     */
    INPUT_SAMPLE_RATE("inputSampleRate", 16000, "采样率", false),
    INPUT_SAMPLE_SIZE_IN_BITS("inputSampleSizeInBits", 16, "采样位数", false),
    INPUT_AUDIO_DEVICE_NUM("inputAudioDeviceNum", 0, "输入音频设备编号", false),


    /**
     * 音频阈值相关配置
     */
    MAX_RECORD_TIME("maxRecordTime", 10, "最大录音时长", false),
    RECORD_VOLUME_PERCENT("recordVolumePercent", 5, "录音的音量阈值 如果低于该阈值x秒，则停止录音 高于该阈值则开始录音", false),
    LESS_RECORD_VOLUME_TIME("lessRecordVolumeTime", 2, "未达标录音音量阈值x秒", false),


    /**
     * 百度语音相关配置
     */
    BAIDU_APP_ID("baiduAppId", null, "百度语音AppId", true),
    BAIDU_API_KEY("baiduApiKey", null, "百度语音ApiKey", true),
    BAIDU_SECRET_KEY("baiduSecretKey", null, "百度语音SecretKey", true),
    //openai相关配置
    OPENAI_KEY("openaiKey", null, "openaiKey", true),
    OPENAI_HOST("openaiHost", null, "openaiHost", true),
    ;

    private final String keyName;
    private final Object defaultValue;
    private final String description;

    private final boolean required;

    Const(String keyName, Object defaultValue, String description, boolean required) {
        this.keyName = keyName;
        this.defaultValue = defaultValue;
        this.description = description;
        this.required = required;
    }

    public static String getFilePath(Const constEnum) {
        return SYS_PATH.getKeyName() + "/" + constEnum.getKeyName();
    }


    public static String getAudioTempFilePath(Const constEnum) {
        return SYS_PATH.getKeyName() + "/" + TEMP_DIR.getKeyName() + "/" + constEnum.getKeyName();
    }


    public static String getThinkingFilePathByRandom() {
        // 获取store下的所有文件
        File[] allFile = FileUtil.ls(SYS_PATH.getKeyName() + "/" + THINKING_DIR.getKeyName());
        if (allFile.length == 0) {
            return null;
        }
        // 随机获取一个文件名
        return SYS_PATH.getKeyName() + "/" + THINKING_DIR.getKeyName() + "/" + RandomUtil.randomEle(allFile).getName();
    }
}
