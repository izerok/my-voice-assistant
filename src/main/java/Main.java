import api.OutsideApi;
import audio.AudioUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import config.LocalConfig;
import enums.Const;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
    public static void main(String[] args) {
        LocalConfig localConfig = new LocalConfig();

        while (true) {
            try {
                log.info("{}--1.开始录音", DateUtil.now());
                localConfig.getRecorder().start();
                log.info("{}--1.录音结束", DateUtil.now());

                AudioUtil.playAudio(Const.getThinkingFilePathByRandom());

                log.info("{}--2.开始获取ASR结果", DateUtil.now());
                String asr = OutsideApi.getBaiDuASR(localConfig.getAipSpeech(), Const.getAudioTempFilePath(Const.TEMP_RECORD_FILE_NAME), localConfig.getInputSampleRate());
                log.info("{}--2.获取ASR结果结束,结果{}", DateUtil.now(), asr);
                if (CharSequenceUtil.isBlank(asr)) {
                    log.info("当前对话结束");
                    continue;
                }

                log.info("{}--3.开始获取NLP结果", DateUtil.now());
                String answer = OutsideApi.getNLPAnswer(localConfig.getOpenAiClient(), localConfig.getTalkHistory(), asr);
                log.info("{}--3.获取NLP结果结束,结果{}", DateUtil.now(), answer);
                if (CharSequenceUtil.isBlank(answer)) {
                    log.info("当前对话结束");
                    continue;
                }

                log.info("{}--4.开始生成音频", DateUtil.now());
                boolean successFlag = OutsideApi.getBaiDuTTS(localConfig.getAipSpeech(), answer, Const.getAudioTempFilePath(Const.TEMP_TTS_FILE_NAME));
                log.info("{}--4.生成音频结束", DateUtil.now());
                if (!successFlag) {
                    log.info("当前对话结束");
                    continue;
                }

                log.info("{}--5.开始播放声音", DateUtil.now());
                AudioUtil.playAudio(Const.getAudioTempFilePath(Const.TEMP_TTS_FILE_NAME));
                log.info("{}--5.播放结束", DateUtil.now());
            } catch (Exception e) {
                log.error("异常", e);
            }
        }
    }
}
