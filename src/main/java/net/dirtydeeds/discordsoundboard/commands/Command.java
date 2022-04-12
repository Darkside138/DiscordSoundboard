package net.dirtydeeds.discordsoundboard.commands;

import lombok.Getter;

@Getter
public abstract class Command {
    protected String name = "null";
    protected String help = "no help available";

    protected abstract void execute(CommandEvent event);

    public final void run(CommandEvent event) {
        execute(event);
    }
}
