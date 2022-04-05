package net.dirtydeeds.discordsoundboard.listeners;

import net.dirtydeeds.discordsoundboard.JDABot;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OnReadyListener extends ListenerAdapter {

    private final JDABot bot;

    public OnReadyListener(JDABot bot)
    {
        this.bot = bot;
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (event.getJDA().getGuildCache().isEmpty()) {
            //TODO: WARN ABOUT NOT BEING IN A GUILD
        }

        event.getJDA().getGuilds().forEach((guild) ->
                bot.getPlayerManager().setUpHandler(guild));
    }
}
