package net.dirtydeeds.discordsoundboard;

import lombok.Getter;
import net.dirtydeeds.discordsoundboard.listeners.OnReadyListener;
import net.dirtydeeds.discordsoundboard.handlers.PlayerManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import javax.security.auth.login.LoginException;

@Getter
public class JDABot {

    private static final Logger LOG = LoggerFactory.getLogger(JDABot.class);

    private final PlayerManager playerManager;
    private final BotConfig botConfig;

    private JDA jda;

    public JDABot(BotConfig botConfig) {
        this.botConfig = botConfig;
        this.playerManager = new PlayerManager(this);
        this.playerManager.init();

        try {
            String botToken = botConfig.getBotToken();
            if (botToken == null) {
                LOG.error("No Discord Token found. Please confirm you have an application.properties file and you have the property bot_token filled with a valid token from https://discord.com/developers/applications");
                return;
            }

            jda = JDABuilder.createDefault(botToken)
                    .setAutoReconnect(true)
                    .addEventListeners(new OnReadyListener(this))
                    .build();
            jda.awaitReady();

            String activityString = botConfig.getActivityString();
            if (ObjectUtils.isEmpty(activityString)) {
                jda.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT,
                        "Type " + botConfig.getCommandCharacter() + "help for a list of commands."));
            } else {
                jda.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, activityString));
            }

        } catch (IllegalArgumentException e) {
            LOG.warn("The config was not populated. Please enter an email and password.");
        } catch (
                LoginException e) {
            LOG.warn("The provided bot token was incorrect. Please provide valid details.");
        } catch (InterruptedException e) {
            LOG.error("Login Interrupted.");
        }
    }
}
