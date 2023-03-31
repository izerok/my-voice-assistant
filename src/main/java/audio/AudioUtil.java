package audio;

import cn.hutool.core.io.FileUtil;
import javazoom.jl.player.Player;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;

/**
 * 音频工具类
 */
@Slf4j
public class AudioUtil {
    public static void playAudio(String filePath) {
        try {
            Player player = new Player(FileUtil.getInputStream(filePath));
            player.play();
        } catch (Exception e) {
            log.info("播放音频失败");
            log.error("播放音频失败", e);
        }

    }

    /**
     * 显示所有音频设备
     */
    public static void showAudioDevices() {

        Mixer.Info[] allMixerInfo = AudioSystem.getMixerInfo();
        Line.Info captureLine = new Line.Info(TargetDataLine.class);
        Line.Info audioLine = new Line.Info(SourceDataLine.class);

        for (int i = 0; i < allMixerInfo.length; i++) {

            Mixer mixer = AudioSystem.getMixer(allMixerInfo[i]);
            if (mixer.isLineSupported(captureLine)) {
                log.info("录音设备 {} : {}", i, allMixerInfo[i].getName());
            }
            if (mixer.isLineSupported(audioLine)) {
                log.info("扬声器设备 {} : {}", i, allMixerInfo[i].getName());
            }
        }
    }

    /**
     * 获取默认录音设备
     * throws LineUnavailableException
     *
     * @param dataLineInfo 音频设备信息
     * @return 音频设备
     */
    public static TargetDataLine getDefaultInputAudioDevice(DataLine.Info dataLineInfo) {

        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            log.info("当前音频设备不支持");
            System.exit(1);
        }
        try {
            return (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        } catch (Exception e) {
            log.info("当前音频设备不支持");
            log.error("当前音频设备不支持", e);
            System.exit(1);
            return null;
        }
    }

    /**
     * 获取输出音频设备
     *
     * @param deviceIndex  索引
     * @param dataLineInfo 音频设备信息
     * @return 音频设备
     */
    public static SourceDataLine getOutputAudioDevice(int deviceIndex, DataLine.Info dataLineInfo) {
        //通过索引获取音频设备
        if (deviceIndex >= 0) {
            try {
                Mixer.Info mixerInfo = AudioSystem.getMixerInfo()[deviceIndex];
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                if (mixer.isLineSupported(dataLineInfo)) {
                    return (SourceDataLine) mixer.getLine(dataLineInfo);
                } else {
                    log.info("自定义音频输出设备不支持,索引:{}", deviceIndex);
                }
            } catch (Exception e) {
                log.info("自定义音频输出设备获取失败,索引:{}", deviceIndex);
            }
        }
        return getDefaultOutputAudioDevice(dataLineInfo);
    }

    public static SourceDataLine getDefaultOutputAudioDevice(DataLine.Info dataLineInfo) {
        if (!AudioSystem.isLineSupported(dataLineInfo)) {
            log.info("当前默认输出音频设备不支持");
            System.exit(1);
        }
        try {
            return (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        } catch (Exception e) {
            log.info("当前默认输出音频设备不支持");
            log.error("当前默认输出音频设备不支持", e);
            System.exit(1);
            return null;
        }
    }

    /**
     * 获取输入音频设备
     * throws LineUnavailableException
     *
     * @param deviceIndex  自定义音频设备
     * @param dataLineInfo 音频设备信息
     * @return 音频设备
     */
    public static TargetDataLine getInputAudioDevice(int deviceIndex, DataLine.Info dataLineInfo) {
        //通过索引获取音频设备
        if (deviceIndex >= 0) {
            try {
                Mixer.Info mixerInfo = AudioSystem.getMixerInfo()[deviceIndex];
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                if (mixer.isLineSupported(dataLineInfo)) {
                    return (TargetDataLine) mixer.getLine(dataLineInfo);
                } else {
                    log.info("自定义音频输入设备不支持,索引:{}", deviceIndex);
                }
            } catch (Exception e) {
                log.info("自定义音频输入设备不存在,索引:{}", deviceIndex);
                log.error("自定义音频输入设备不存在", e);
            }
        }

        return getDefaultInputAudioDevice(dataLineInfo);
    }


}
