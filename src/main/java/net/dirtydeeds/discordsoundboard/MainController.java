package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.beans.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * @author dfurrer.
 *
 * This is the MainController for the SpringBoot application
 */
@SpringBootApplication
@EnableAsync
public class MainController implements RepositoryRestConfigurer {

    public MainController() {
    }

    public static void main(String[] args) {
        SpringApplication.run(MainController.class, args);
    }

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(SoundFile.class);
        config.exposeIdsFor(User.class);
    }
}
