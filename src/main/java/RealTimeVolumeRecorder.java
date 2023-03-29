import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RealTimeVolumeRecorder implements Runnable {

    // 自定义音量阈值（百分比）
    private final int volumeThreshold;
    // 连续低于阈值的时长（毫秒）
    private final int lowVolumeDuration;
    private static final int MAX_16_BIT_SAMPLE = 32767; // 16位音频的最大值
    private final int maxRecordTime;


    private final AudioFormat inputFormat;
    private final TargetDataLine inputLine;
    private final String outputFilePath;


    private final int bufferSize;
    private final ByteArrayOutputStream outputStream;
    private long lowVolumeStartTime;

    public RealTimeVolumeRecorder(AudioFormat audioFormat, TargetDataLine targetDataLine, String outputFilePath, int volumeThreshold, int lowVolumeDuration, int maxRecordTime) {
        this.outputFilePath = outputFilePath;
        this.inputFormat = audioFormat;
        this.inputLine = targetDataLine;


        this.maxRecordTime = maxRecordTime * 1000;

        this.volumeThreshold = volumeThreshold;
        this.lowVolumeDuration = lowVolumeDuration * 1000;

        bufferSize = (int) (audioFormat.getSampleRate() * audioFormat.getFrameSize() / 10);
        outputStream = new ByteArrayOutputStream();
        lowVolumeStartTime = 0;

    }

    public void start() {
        try {
            Future<?> future = ThreadUtil.execAsync(this);
            future.get();
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }


    @Override
    public void run() {
        try {
            outputStream.reset();
            inputLine.open(inputFormat, bufferSize);
            inputLine.start();

            byte[] buffer = new byte[bufferSize];
            lowVolumeStartTime = 0;
            while (true) {

                // 获取录音开始时间
                long startTime = System.currentTimeMillis();


                int bytesRead = inputLine.read(buffer, 0, buffer.length);
                int volume = calculateVolume(buffer, bytesRead);
                int volumePercentage = convertVolumeToPercentage(volume);
                LocalApi.log.info("{}--当前音量:{}", DateUtil.now(), volumePercentage);
                outputStream.write(buffer, 0, bytesRead);
                if (volumePercentage >= volumeThreshold) {
                    // 当前音量大于阈值，重置开始时间
                    lowVolumeStartTime = 0;
                }
                // 当前音量低于阈值，开始计时 如果是第一次低于阈值，记录开始时间
                if (lowVolumeStartTime == 0) {
                    LocalApi.log.info("当前音量低于阈值，开始计时。:{}", LocalDateTime.now());
                    lowVolumeStartTime = System.currentTimeMillis();
                }
                // 如果当前时间 - 开始时间 >= 连续低于阈值的时长，自动结束录音
                if (DateUtil.spendMs(lowVolumeStartTime) >= lowVolumeDuration) {
                    LocalApi.log.info("连续{}秒音量低于阈值，自动结束录音。:{}", lowVolumeDuration, LocalDateTime.now());
                    break;
                }


                if (DateUtil.spendMs(startTime) >= maxRecordTime) {
                    LocalApi.log.info("录音时间超过最大录音时间{}秒，自动结束录音。", maxRecordTime);
                    break;
                }

            }

            inputLine.stop();
            inputLine.close();

            saveAudioToFile(outputFilePath);
            outputStream.close();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    // 计算音量大小
    private int calculateVolume(byte[] buffer, int bytesRead) {
        int sum = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            int sample = (buffer[i + 1] << 8) | (buffer[i] & 0xff);
            sum += Math.abs(sample);
        }
        return (sum / (bytesRead / 2));
    }

    // 将音量转换为百分比
    private int convertVolumeToPercentage(int volume) {
        return (int) (((double) volume / MAX_16_BIT_SAMPLE) * 100);
    }

    // 将音频保存到文件
    private void saveAudioToFile(String filePath) throws IOException {
        // 先删除历史文件
        FileUtil.del(filePath);
        File file = FileUtil.newFile(filePath);
        FileUtil.touch(file);

        byte[] audioData = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = new AudioInputStream(inputStream, inputFormat, audioData.length / inputFormat.getFrameSize());
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File(filePath));
    }

}
