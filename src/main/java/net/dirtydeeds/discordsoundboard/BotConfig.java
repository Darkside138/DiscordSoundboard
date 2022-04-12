package net.dirtydeeds.discordsoundboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
@Component
@Getter
@NoArgsConstructor
public class BotConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BotConfig.class);

    @Value("${leaveAfterPlayback:false}")
    boolean leaveAfterPlayback;
    @Value("${playEntranceOnJoin:true}")
    boolean playEntranceOnJoin;
    @Value("${playEntranceOnMove:true}")
    boolean playEntranceOnMove;
    @Value("${respond_to_chat_commands:true}")
    boolean respondToChatCommands;
    @Value("${leave_suffix:_leave}")
    String leaveSuffix;
    @Value("${entranceForAll:}")
    String entranceForAll;
    @Value("${bot_token:}")
    String botToken;
    @Value("${command_character:?}")
    String commandCharacter;
    @Value("${message_size_limit:1995}")
    Integer messageSizeLimit;
    @Value("${respond_to_dm:true}")
    boolean respondToDmsString;
    @Value("${allowedUsers:}")
    String allowedUsersString;
    @Value("${bannedUsers:}")
    String bannedUsersString;
    @Value("${activityString:}")
    String activityString;
    @Value("${username_to_join_channel:}")
    String botOwnerName;
    @Value("${sounds_directory:}")
    String soundFileDir;
    @Value("${maxFileSizeInBytes:10000000}")
    int maxFileSizeInBytes;
    @Value("${spring.application.version:unknown}")
    String applicationVersion;
    @Value("${controlByChannel:false}")
    boolean controlByChannel;
    @Value("${leaveOnEmptyChannel:false}")
    boolean leaveOnEmptyChannel;

    public List<String> getAllowedUsersList() {
        String allowedUsersString = getAllowedUsersString();
        if (allowedUsersString != null) {
            if (!allowedUsersString.isEmpty()) {
                String[] allowedUsersArray = allowedUsersString.trim().split(",");
                if (allowedUsersArray.length > 0) {
                    return Arrays.asList(allowedUsersArray);
                }
            }
        }
        return Collections.emptyList();
    }

    public List<String> getBannedUsersList() {
        String bannedUsersString = getBannedUsersString();
        if (bannedUsersString != null) {
            if (!bannedUsersString.isEmpty()) {
                String[] bannedUsersArray = bannedUsersString.split(",");
                if (bannedUsersArray.length > 0) {
                    return Arrays.asList(bannedUsersArray);
                }
            }
        }
        return Collections.emptyList();
    }

    public String getSoundFileDir() {
        if (soundFileDir == null || soundFileDir.isEmpty()) {
            soundFileDir = System.getProperty("user.dir") + "/sounds";
        }
        return soundFileDir;
    }
}
