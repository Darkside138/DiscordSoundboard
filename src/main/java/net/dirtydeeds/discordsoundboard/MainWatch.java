package net.dirtydeeds.discordsoundboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.*;

/**
 * MainWatch monitors the sound file directory for changes (create/modify/delete) and updates the file lis if one
 * of those events happen.
 *
 * @author dfurrer.
 */
@Service
public class MainWatch {

    private static final Logger LOG = LoggerFactory.getLogger(MainWatch.class);

    private SoundPlayer soundPlayer;

    @Async
    public void watchDirectoryPath(Path path) {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            Boolean isFolder = (Boolean) Files.getAttribute(path,
                    "basic:isDirectory", NOFOLLOW_LINKS);
            if (!isFolder) {
                throw new IllegalArgumentException("Path: " + path + " is not a folder");
            }

            LOG.info("Watching path: {} for changes. Will update sound file list when modified", path);

            WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            while (true) {
                watchKey.pollEvents().forEach(event -> soundPlayer.updateFileList());

                // Reset the watch key everytime for continuing to use it for further event polling
                boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            // Folder does not exist or we were interrupted
            e.printStackTrace();
        }
    }

    public void setSoundPlayer(SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
    }
}