package audio;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
@Slf4j
public class RecordUtil implements Runnable {


    private static final int MAX_16_BIT_SAMPLE = 32767; // 16位音频的最大值


    private final AudioFormat inputFormat;
    private final TargetDataLine inputLine;
    private final String outputFilePath;


    private final int bufferSize;
    private final ByteArrayOutputStream outputStream;


    private boolean recodingFlag;
    // 最大录音时长
    private int maxRecordTime = 10;
    // 录音的音量阈值 如果低于该阈值x秒，则停止录音 高于该阈值则开始录音
    private int recordVolumePercent = 10;
    // 未达标录音音量阈值x秒
    private int lessRecordVolumeTime = 2;


    public RecordUtil(AudioFormat audioFormat, TargetDataLine targetDataLine, String outputFilePath,
                      int maxRecordTime,
                      int recordVolumePercent,
                      int lessRecordVolumeTime) {
        this.outputFilePath = outputFilePath;
        this.inputFormat = audioFormat;
        this.inputLine = targetDataLine;


        this.maxRecordTime = maxRecordTime * 1000;
        this.recordVolumePercent = recordVolumePercent;
        this.lessRecordVolumeTime = lessRecordVolumeTime * 1000;
        log.info("最大录音时长：{}秒", this.maxRecordTime/1000);
        log.info("录音的音量阈值：{}%", this.recordVolumePercent);
        log.info("未达标录音音量阈值：{}秒", this.lessRecordVolumeTime/1000);

        bufferSize = (int) (audioFormat.getSampleRate() * audioFormat.getFrameSize() / 10);
        outputStream = new ByteArrayOutputStream();

        recodingFlag = false;
    }

    public void start() {
        try {
            Future<?> future = ThreadUtil.execAsync(this);
            future.get();
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }

    /**
     * 判断当前设备音量是否大于阈值 如果大于阈值则开始录音并将流写入到文件中
     * 当音量小于阈值时，判断是否已经开始录音 如果已经开始录音，
     * ***则判断当前时间与开始时间的差值是否大于阈值 如果大于阈值，则停止录音并将流写入到文件中
     * ***如果小于阈值，则继续录音
     * 如果没有开始录音，则继续等待
     */
    @Override
    public void run() {
        try {
            outputStream.reset();
            inputLine.open(inputFormat, bufferSize);
            inputLine.start();
            recodingFlag = false;

            byte[] buffer = new byte[bufferSize];
            // 低于阈值的开始时间
            long lessVolumePercentStartTime = 0;
            long recordStartTime = 0;


            while (true) {
                // 当前音频数据
                int bytesRead = inputLine.read(buffer, 0, buffer.length);
                // 当前音量
                int volume = calculateVolume(buffer, bytesRead);
                // 当前音量百分比
                int volumePercentage = convertVolumeToPercentage(volume);
                log.debug("当前音量百分比：{}", volumePercentage);
                // 如果当前音量大于阈值，开始录音 且没有开始录音，则开始录音
                if (volumePercentage >= recordVolumePercent && !recodingFlag) {
                    recodingFlag = true;
                    recordStartTime = System.currentTimeMillis();
                    log.info("当前音量大于阈值，开始录音。:{}", LocalDateTime.now());
                }
                // 如果已经开始录音，则将流写入到文件中
                if (recodingFlag) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                // 如果当前音量小于阈值，且已经开始录音，则判断当前时间与开始时间的差值是否大于阈值
                if (recodingFlag && volumePercentage < recordVolumePercent) {
                    // 如果开始时间为0，则将开始时间设置为当前时间
                    if (lessVolumePercentStartTime == 0) {
                        lessVolumePercentStartTime = System.currentTimeMillis();
                    }
                    // 如果当前时间与开始时间的差值大于阈值，则停止录音
                    if (DateUtil.spendMs(lessVolumePercentStartTime) >= lessRecordVolumeTime) {
                        log.info("连续{}秒音量低于阈值，自动结束录音。:{}", lessRecordVolumeTime, LocalDateTime.now());
                        recodingFlag = false;
                        break;
                    }
                }
                // 如果当前录音时间大于最大录音时间，则停止录音并将流写入到文件中
                if (recodingFlag && DateUtil.spendMs(recordStartTime) >= maxRecordTime) {
                    log.info("录音时间超过{}秒，自动结束录音。:{}", maxRecordTime, LocalDateTime.now());
                    recodingFlag = false;
                    break;
                }

            }

            inputLine.stop();
            inputLine.close();

            saveAudioToFile(outputFilePath);
            outputStream.close();
        } catch (Exception e) {

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
