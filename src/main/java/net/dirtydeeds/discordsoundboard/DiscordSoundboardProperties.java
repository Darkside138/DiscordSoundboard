package net.dirtydeeds.discordsoundboard;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties()
//@PropertySource("classpath:application.properties")
@Validated
public class DiscordSoundboardProperties {

    @NotNull
    private String botToken;
    private String usernameToJoinChannel;
    private String commandCharacter = "?";
    private boolean respondToChatCommands = true;
    private boolean respondToDm = true;
    private String leaveSuffix = "_leave";
    @Min(0)
    @Max(1994)
    private int messageSizeLimit = 1994;
    private String soundsDirectory;
    // TODO test if comma separated strings get parsed automatically... not sure if some config is needed to do it
    private List<String> allowedUserIds = Collections.emptyList();
    private List<String> bannedUserIds = Collections.emptyList();
    private boolean leaveAfterPlayback = false;

    @Min(0)
    private int rateLimitRestrictDuration;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getUsernameToJoinChannel() {
        return usernameToJoinChannel;
    }

    public void setUsernameToJoinChannel(String usernameToJoinChannel) {
        this.usernameToJoinChannel = usernameToJoinChannel;
    }

    public String getCommandCharacter() {
        return commandCharacter;
    }

    public void setCommandCharacter(String commandCharacter) {
        this.commandCharacter = commandCharacter;
    }

    public boolean isRespondToChatCommands() {
        return respondToChatCommands;
    }

    public void setRespondToChatCommands(boolean respondToChatCommands) {
        this.respondToChatCommands = respondToChatCommands;
    }

    public boolean isRespondToDm() {
        return respondToDm;
    }

    public void setRespondToDm(boolean respondToDm) {
        this.respondToDm = respondToDm;
    }

    public String getLeaveSuffix() {
        return leaveSuffix;
    }

    public void setLeaveSuffix(String leaveSuffix) {
        this.leaveSuffix = leaveSuffix;
    }

    public int getMessageSizeLimit() {
        return messageSizeLimit;
    }

    public void setMessageSizeLimit(int messageSizeLimit) {
        this.messageSizeLimit = messageSizeLimit;
    }

    public String getSoundsDirectory() {
        return soundsDirectory;
    }

    public void setSoundsDirectory(String soundsDirectory) {
        this.soundsDirectory = soundsDirectory;
    }

    public boolean isLeaveAfterPlayback() {
        return leaveAfterPlayback;
    }

    public void setLeaveAfterPlayback(boolean leaveAfterPlayback) {
        this.leaveAfterPlayback = leaveAfterPlayback;
    }

    public List<String> getAllowedUserIds() {
        return allowedUserIds;
    }

    public void setAllowedUserIds(List<String> allowedUserIds) {
        this.allowedUserIds = allowedUserIds;
    }

    public List<String> getBannedUserIds() {
        return bannedUserIds;
    }

    public void setBannedUserIds(List<String> bannedUserIds) {
        this.bannedUserIds = bannedUserIds;
    }

    public int getRateLimitRestrictDuration() {
        return rateLimitRestrictDuration;
    }

    public void setRateLimitRestrictDuration(int rateLimitRestrictDuration) {
        this.rateLimitRestrictDuration = rateLimitRestrictDuration;
    }
}
