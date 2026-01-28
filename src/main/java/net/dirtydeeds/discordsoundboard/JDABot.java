package net.dirtydeeds.discordsoundboard;

import lombok.Getter;
import net.dirtydeeds.discordsoundboard.listeners.OnReadyListener;
import net.dirtydeeds.discordsoundboard.handlers.PlayerManager;
import net.dirtydeeds.discordsoundboard.service.PlaybackService;
import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

@Getter
public class JDABot {

    private static final Logger LOG = LoggerFactory.getLogger(JDABot.class);

    private final PlayerManager playerManager;
    private final BotConfig botConfig;
    private final PlaybackService playbackService;

    private JDA jda;

    public JDABot(BotConfig botConfig, PlaybackService playbackService) {
        this.botConfig = botConfig;
        this.playbackService = playbackService;
        this.playerManager = new PlayerManager(this, playbackService);
        this.playerManager.init();

        try {
            String botToken = botConfig.getBotToken();
            if (botToken == null) {
                LOG.error("No Discord Token found. Please confirm you have an application.properties file and you have the property bot_token filled with a valid token from https://discord.com/developers/applications");
                return;
            }
            LOG.debug("Bot Token: {}", botToken);
            // Configure DAVE (Discord Audio Video Encryption) protocol for JDA 6.3.0+
            AudioModuleConfig audioConfig = new AudioModuleConfig()
                    .withDaveSessionFactory(new JDaveSessionFactory());

            jda = JDABuilder.createDefault(botToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES)
                    .disableCache(CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                    .enableCache(CacheFlag.ACTIVITY)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setAutoReconnect(true)
                    .setAudioModuleConfig(audioConfig)
                    .addEventListeners(new OnReadyListener(this))
                    .build();
            jda.awaitReady();

            String activityString = botConfig.getActivityString();
            if (ObjectUtils.isEmpty(activityString)) {
                jda.getPresence().setActivity(Activity.of(Activity.ActivityType.PLAYING,
                        "Type " + botConfig.getCommandCharacter() + "help for a list of commands."));
            } else {
                jda.getPresence().setActivity(Activity.of(Activity.ActivityType.PLAYING, activityString));
            }

        } catch (IllegalArgumentException e) {
            LOG.warn("The config was not populated. Please enter an email and password.");
        } catch (InterruptedException e) {
            LOG.error("Login Interrupted.");
        }
    }
}
