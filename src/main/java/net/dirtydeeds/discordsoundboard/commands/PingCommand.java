package net.dirtydeeds.discordsoundboard.commands;

/**
 * @author Dave Furrer
 * <p>
 * Command to check the bots latency
 */
public class PingCommand extends Command {

    public PingCommand() {
        this.name = "Ping";
        this.help = "Checks the bots latency";
    }

    @Override
    protected void execute(CommandEvent event) {
//        event.replyByPrivateMessage("Websocket: " + event.getJda.getGatewayPing() + "ms");
    }
}
