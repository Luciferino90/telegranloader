package it.usuratonkachi.telegranloader.telegranloader.utils;

import lombok.experimental.UtilityClass;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

@UtilityClass
public class DownloadUtils {

    private int size = 1024;
    private static Random random = new Random();
    private static DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();

    public static AsynchronousFileChannel getAsyncFileChannel(Path toWrite){
        try {
            return AsynchronousFileChannel.open(toWrite, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Mono<Void> closeChannel(AsynchronousFileChannel asynchronousFileChannel){
        try {
            asynchronousFileChannel.close();
            return Mono.empty();
        }catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /* TODO Move to tests folder
    public static Publisher<DataBuffer> multipleChunks(int size) {
        int chunkSize = size / 10;
        return Flux.range(1, 10).map(integer -> randomBuffer(chunkSize));
    }

    private static DataBuffer randomBuffer(int size) {
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        DataBuffer buffer = dataBufferFactory.allocateBuffer(size);
        buffer.write(bytes);
        return buffer;
    }
    */

}
