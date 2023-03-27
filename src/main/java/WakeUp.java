import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import javazoom.jl.decoder.JavaLayerException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WakeUp {
    public static void main(String[] args) {
        // 运行示例程序
        runDemo();
    }

    public static void runDemo() {
        LocalApi localApi = new LocalApi();
        Porcupine porcupine = null;

        try {
            // 初始化Porcupine对象
            porcupine = initPorcupine(localApi);

            // 获取麦克风输入
            TargetDataLine micDataLine = localApi.getInputLine();

            // 开始监听麦克风输入
            startListening(localApi, porcupine, micDataLine);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (porcupine != null) {
                porcupine.delete();
            }
        }
    }

    private static Porcupine initPorcupine(LocalApi localApi) throws PorcupineException {
        // 创建并配置Porcupine对象
        return new Porcupine.Builder()
                .setAccessKey(localApi.getAccessKey())
                .setSensitivity(localApi.getSensitivities())
                .setKeywordPath(Const.SYS_PATH.getKeyName() + "/" + localApi.getModelPath())
                .build();
    }

    private static void startListening(LocalApi localApi, Porcupine porcupine, TargetDataLine micDataLine) throws IOException, PorcupineException, LineUnavailableException, UnsupportedAudioFileException, JavaLayerException {
        int numBytesRead;

        // 当没有其他输入时持续监听
        while (System.in.available() == 0) {
            // 打开并启动麦克风输入线
            micDataLine.open(localApi.getInputFormat());
            micDataLine.start();

            // 创建音频处理缓冲区
            int frameLength = porcupine.getFrameLength();
            ByteBuffer captureBuffer = ByteBuffer.allocate(frameLength * 2);
            captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
            short[] porcupineBuffer = new short[frameLength];

            // 读取音频缓冲区
            numBytesRead = micDataLine.read(captureBuffer.array(), 0, captureBuffer.capacity());

            // 如果缓冲区未满，不传递给Porcupine处理
            if (numBytesRead != frameLength * 2) {
                continue;
            }

            // 将数据复制到16位缓冲区
            captureBuffer.asShortBuffer().get(porcupineBuffer);

            // 使用Porcupine处理音频
            int result = porcupine.process(porcupineBuffer);

            // 如果结果大于等于0，执行本地API
            if (result >= 0) {
                localApi.run();
            }
        }
    }
}