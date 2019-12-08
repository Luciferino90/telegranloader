package it.usuratonkachi.telegranloader.telegranloader;

import it.usuratonkachi.telegranloader.telegranloader.bot.TelegranLoaderProperties;
import it.usuratonkachi.telegranloader.telegranloader.bot.TelegranLoaderWebhook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
@EnableConfigurationProperties(value = TelegranLoaderProperties.class)
public class TelegranloaderApplication {

	public static void main(String[] args) {
		ApiContextInitializer.init();
		SpringApplication.run(TelegranloaderApplication.class, args);
	}

}
