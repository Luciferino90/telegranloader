package it.usuratonkachi.telegranloader.telegranloader.bot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegranLoaderProperties {

    private String token;
    private String username;
    private String path;

}
