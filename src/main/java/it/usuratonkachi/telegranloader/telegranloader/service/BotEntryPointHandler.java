package it.usuratonkachi.telegranloader.telegranloader.service;

import it.usuratonkachi.telegranloader.telegranloader.bot.TelegranLoaderProperties;
import it.usuratonkachi.telegranloader.telegranloader.dto.Response;
import it.usuratonkachi.telegranloader.telegranloader.utils.DownloadUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.File;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotEntryPointHandler {

    private final TelegranLoaderProperties telegranLoaderProperties;

    private String telegramApiUrl = "https://api.telegram.org/%s";
    private String fileInfoEndpoint = "/bot%s/getFile?file_id=%s";
    private String downloadFileEndpoint = "/file/bot%s/%s";

    private BiFunction<String, String, String> getFileInfoUrl = (token, fileId) -> String.format(telegramApiUrl, String.format(fileInfoEndpoint, token, fileId));
    private BiFunction<String, String, String> getDownloadUrl = (token, path) -> String.format(telegramApiUrl, String.format(downloadFileEndpoint, token, path));

    public Mono<Response> downloadFlow(String fileId){
        return getFileInfoClientResponse(fileId)
                .flatMap(clientResponse -> Mono.just(clientResponse)
                        .filter(cr -> HttpStatus.OK.equals(cr.statusCode()))
                        .flatMap(cr -> cr.toEntity(new ParameterizedTypeReference<ApiResponse<File>>() {}))
                        .map(HttpEntity::getBody)
                        .flatMap(this::doDownload)
                        .switchIfEmpty(clientResponse.body(BodyExtractors.toMono(String.class)).map(e-> Response.builder().message(e).build()))
                );
    }

    private Mono<ClientResponse> getFileInfoClientResponse(String fileId){
        return WebClient.create(getFileInfoUrl.apply(telegranLoaderProperties.getToken(), fileId))
                .get()
                .exchange();
    }

    private Mono<ClientResponse> getDownloadResponse(ApiResponse<File> fileInfo){
        return WebClient.create(getDownloadUrl.apply(telegranLoaderProperties.getToken(), fileInfo.getResult().getFilePath()))
                .get()
                .exchange()
                .map(e -> e);
    }

    public Mono<Response> doDownload(ApiResponse<File> fileInfo){
        return getAsyncFileChannel(fileInfo)
                .flatMap(asynchronousFileChannel -> doDownload(fileInfo, asynchronousFileChannel)
                        .doFinally(e -> DownloadUtils.closeChannel(asynchronousFileChannel))
                )
                .onErrorResume(e -> Mono.just(Response.builder().message(e.getMessage()).build()));
    }

    private Mono<Response> doDownload(ApiResponse<File> fileInfo, AsynchronousFileChannel asynchronousFileChannel){
        return getDownloadResponse(fileInfo)
                .zipWith(Mono.just(asynchronousFileChannel))
                .flatMap(TupleUtils.function((clientResponse, asyncChannel) ->
                        Mono.just(clientResponse)
                                .filter(cr -> HttpStatus.OK.equals(cr.statusCode()))
                                .map(cr -> clientResponse.body(BodyExtractors.toDataBuffers()))
                                .flatMapMany(dataBufferFlux -> DataBufferUtils.write(dataBufferFlux, asyncChannel))
                                .collectList()
                                .map(e -> Response.builder().message("Download Finished").build())
                                .switchIfEmpty(clientResponse.toEntity(String.class).map(HttpEntity::getBody).map(e-> Response.builder().message(e).build()))
                ));
    }

    private Mono<AsynchronousFileChannel> getAsyncFileChannel(ApiResponse<File> fileInfo){
        Path p = Paths.get("/tmp", fileInfo.getResult().getFilePath());

        return Mono.just(p)
                .flatMap(destPath -> Mono.just(destPath)
                        .doOnNext(path -> path.getParent().toFile().mkdirs())
                        .doOnNext(path -> {
                            if (path.toFile().exists()) throw new RuntimeException("File already exists in " + path);
                        })
                        .map(DownloadUtils::getAsyncFileChannel)
                );
    }

}
