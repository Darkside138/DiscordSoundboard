package net.dirtydeeds.discordsoundboard.commands;

import com.sun.management.OperatingSystemMXBean;
import net.dirtydeeds.discordsoundboard.BotConfig;
import net.dirtydeeds.discordsoundboard.SoundPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Dave Furrer
 * <p>
 * Command to get information about the bot and the system it's running on
 */
public class InfoCommand extends Command {
    private static final Logger LOG = LoggerFactory.getLogger(InfoCommand.class);

    private final SoundPlayer soundPlayer;
    private final BotConfig botConfig;

    private final static DecimalFormat df2 = new DecimalFormat("#.##");

    public InfoCommand(SoundPlayer soundPlayer, BotConfig botConfig) {
        this.soundPlayer = soundPlayer;
        this.botConfig = botConfig;
        this.name = "info";
        this.help = "Returns info about the bot";
    }

    @Override
    protected void execute(CommandEvent event) {
        LOG.info("Responding to info request by {}", event.getRequestingUser());

        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        long prevUpTime = runtimeMXBean.getUptime();
        long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
        double cpuUsage;
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }

        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;

        cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));

        List<MemoryPoolMXBean> memoryPools = new ArrayList<>(ManagementFactory.getMemoryPoolMXBeans());
        long usedHeapMemoryAfterLastGC = 0;
        for (MemoryPoolMXBean memoryPool : memoryPools) {
            if (memoryPool.getType().equals(MemoryType.HEAP)) {
                MemoryUsage poolCollectionMemoryUsage = memoryPool.getCollectionUsage();
                usedHeapMemoryAfterLastGC += poolCollectionMemoryUsage.getUsed();
            }
        }

        Package thisPackage = getClass().getPackage();
        String version = null;
        if (thisPackage != null) {
            version = getClass().getPackage().getImplementationVersion();
        }
        if (version == null) {
            version = "DEVELOPMENT";
        }

        long uptimeDays = TimeUnit.DAYS.convert(upTime, TimeUnit.MILLISECONDS);
        long uptimeHours = TimeUnit.HOURS.convert(upTime, TimeUnit.MILLISECONDS) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(upTime));
        long uptimeMinutes = TimeUnit.MINUTES.convert(upTime, TimeUnit.MILLISECONDS) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(upTime));
        long upTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(upTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(upTime));

        event.replyByPrivateMessage("DiscordSoundboard info: ```" +
                "CPU: " + df2.format(cpuUsage) + "%" +
                "\nMemory: " + humanReadableByteCount(usedHeapMemoryAfterLastGC) +
                "\nUptime: Days: " + uptimeDays + " Hours: " + uptimeHours + " Minutes: " + uptimeMinutes + " Seconds: " + upTimeSeconds +
                "\nVersion: " + version +
                "\nSoundFiles: " + soundPlayer.getAvailableSoundFiles().size() +
                "\nCommand Prefix: " + botConfig.getCommandCharacter() +
                "\nSound File Path: " + botConfig.getSoundFileDir() +
                "\nSoundboard Version: " + botConfig.getApplicationVersion() +
                "\nWeb UI URL: localhost:" + soundPlayer.getApplicationContext().getWebServer().getPort() +
                "\nSwagger URL: localhost:" + soundPlayer.getApplicationContext().getWebServer().getPort() + "/swagger-ui/index.html" +
                "```");
    }

    private static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("kMGTPE").charAt(exp - 1) + ("");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}