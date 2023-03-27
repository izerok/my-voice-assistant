import lombok.Getter;

@Getter
public enum Const {


    /**
     * 文件路径相关 无默认值
     */
    SYS_PATH(System.getProperty("user.dir"), null, "系统路径", false),

    CONFIG_FILE_NAME("config.json", null, "配置文件名", false),

    TEMP_DIR("temp", null, "临时文件夹", false),
    STORE_DIR("store", null, "存储文件夹", false),

    /**
     * 声音相关文件名
     */
    TEMP_TTS_FILE_NAME("temp_tts.wav", null, "临时TTS文件名", false),
    TEMP_RECORD_FILE_NAME("temp_record.wav", null, "临时录音文件名", false),
    ZAINE_FILE_NAME("zaine.wav", null, "被唤醒时播放的音频文件名", false),
    SIKAOYIXIA_FILE_NAME("sikaoyixia.wav", null, "思考一下", false),

    /**
     * 日志相关
     */
    LOG_LEVEL("logLevel", "info", "日志级别", false),


    /**
     * 音频设备相关配置
     */
    INPUT_SAMPLE_RATE("inputSampleRate", 16000, "采样率", false),
    INPUT_SAMPLE_SIZE_IN_BITS("inputSampleSizeInBits", 16, "采样位数", false),
    INPUT_AUDIO_DEVICE_NUM("inputAudioDeviceNum", 0, "输入音频设备编号", false),
    OUTPUT_SAMPLE_RATE("outputSampleRate", 24000, "采样率", false),
    OUTPUT_SAMPLE_SIZE_IN_BITS("outputSampleSizeInBits", 16, "采样位数", false),
    OUTPUT_AUDIO_DEVICE_NUM("outputAudioDeviceNum", 0, "输入音频设备编号", false),


    /**
     * 音频阈值相关配置
     */
    RECORD_TIME("maxRecordTime", 10, "录音时长", false),
    VOLUME_THRESHOLD("volumeThreshold", 4, "音量阈值", false),
    LOW_VOLUME_DURATION("lowVolumeDuration", 2, "连续低音量持续时间", false),


    /**
     * Porcupine唤醒相关配置
     */
    SENSITIVITIES("sensitivities", 0.5f, "唤醒灵敏度", false),
    MODEL_PATH("modelPath", null, "唤醒模型路径", true),
    ACCESS_KEY("accessKey", null, "Porcupine-key", true),


    /**
     * 接口地址相关配置
     */
    NLP_URL("nlpUrl", null, "NLP接口地址", true),
    NLP_URL_HEADER("nlpUrlHeader", null, "NLP接口地址请求头", false),
    NLP_REQUEST_KEY("nlpRequestKey", null, "NLP接口请求key", true),
    NLP_RESPONSE_KEY("nlpResponseKey", null, "NLP接口返回key", true),
    NLP_REQUEST_HEADER("nlpRequestHeader", null, "NLP接口请求头", false),
    NLP_REQUEST_HEADER_VALUE("nlpRequestHeaderValue", null, "NLP接口请求头值", false),

    /**
     * 百度语音相关配置
     */
    BAIDU_APP_ID("baiduAppId", null, "百度语音AppId", true),
    BAIDU_API_KEY("baiduApiKey", null, "百度语音ApiKey", true),
    BAIDU_SECRET_KEY("baiduSecretKey", null, "百度语音SecretKey", true),

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

    public static String getStoreFilePath(Const constEnum) {
        return SYS_PATH.getKeyName() + "/" + STORE_DIR.getKeyName() + "/" + constEnum.getKeyName();
    }
}
