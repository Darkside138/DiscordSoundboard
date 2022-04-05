package net.dirtydeeds.discordsoundboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author dfurrer.
 *
 * This is the MainController for the SpringBoot application
 */
@SpringBootApplication
@EnableAsync
public class MainController {

    public MainController() {
    }

    public static void main(String[] args) {
        SpringApplication.run(MainController.class, args);
    }

    @Bean
    @SuppressWarnings("unused")
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("discord-soundboard")
                .pathsToMatch("/soundsApi/**")
                .build();
    }
}
