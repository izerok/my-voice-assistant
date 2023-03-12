import ai.picovoice.porcupine.Porcupine;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WakeUp {
    public static void runDemo() {
        LocalApi localApi = new LocalApi();

        Porcupine porcupine = null;
        try {
             porcupine = new Porcupine.Builder()
                    .setAccessKey(localApi.getAccessKey())
                    .setSensitivity(localApi.getSensitivities())
                    .setKeywordPath(System.getProperty("user.dir") + "/" + localApi.getModelPath())
                     .build();

            TargetDataLine micDataLine = localApi.getMicDataLine();
            try{
                micDataLine.open(localApi.getFormat());
            }catch (Exception e){
                LocalApi.log.info("打开录音设备失败");
                LocalApi.log.error(e);
            }
            micDataLine.start();

            // buffers for processing audio
            int frameLength = porcupine.getFrameLength();
            ByteBuffer captureBuffer = ByteBuffer.allocate(frameLength * 2);
            captureBuffer.order(ByteOrder.LITTLE_ENDIAN);
            short[] porcupineBuffer = new short[frameLength];

            int numBytesRead;
            while (System.in.available() == 0) {
                // read a buffer of audio
                numBytesRead = micDataLine.read(captureBuffer.array(), 0, captureBuffer.capacity());
                // don't pass to porcupine if we don't have a full buffer
                if (numBytesRead != frameLength * 2) {
                    continue;
                }
                // copy into 16-bit buffer
                captureBuffer.asShortBuffer().get(porcupineBuffer);

                // process with porcupine
                int result = porcupine.process(porcupineBuffer);
                if (result >= 0) {
                    localApi.run();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}