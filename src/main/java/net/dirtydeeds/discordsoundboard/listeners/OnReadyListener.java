package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.JDABot;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dave Furrer
 * <p>
 * Class to run code when the bot is ready
 */
public class OnReadyListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(OnReadyListener.class);

    private final JDABot bot;

    public OnReadyListener(JDABot bot)
    {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (event.getJDA().getGuildCache().isEmpty()) {
            LOG.error("This bot is not invited to any guilds. Please see documentation: https://github.com/Darkside138/DiscordSoundboard/wiki");
        }

        event.getJDA().getGuilds().forEach((guild) ->
                bot.getPlayerManager().setUpHandler(guild));
    }
}
