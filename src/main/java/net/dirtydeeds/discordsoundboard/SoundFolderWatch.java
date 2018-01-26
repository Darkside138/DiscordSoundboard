package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.Observable;

/**
 * Observable class used to watch changes to files in a given directory
 *
 * @author dfurrer.
 */
@Service
public class SoundFolderWatch extends Observable {

    @Async
    @SuppressWarnings("unchecked")
    public void watchDirectoryPath(String watchPath) {
        // Sanity check - Check if path is a folder
        Path path = Paths.get(watchPath);
        try {
            Boolean isFolder = (Boolean) Files.getAttribute(path,
                    "basic:isDirectory", NOFOLLOW_LINKS);
            if (!isFolder) {
                throw new IllegalArgumentException("Path: " + path + " is not a folder");
            }
        } catch (IOException e) {
            // Folder does not exists
            e.printStackTrace();
        }

        System.out.println("Watching path: " + path);

        // We obtain the file system of the Path
        FileSystem fs = path.getFileSystem ();

        // We create the new WatchService using the new try() block
        try (WatchService service = fs.newWatchService()) {

            // We register the path to the service
            // We watch for creation events
            path.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

            // Start the infinite polling loop
            WatchKey key;
            while (true) {
                key = service.take();

                // Dequeueing events
                Kind<?> kind;
                for(WatchEvent<?> watchEvent : key.pollEvents()) {
                    // Get the type of the event
                    kind = watchEvent.kind();
                    if (kind == ENTRY_CREATE || kind == ENTRY_DELETE || kind == ENTRY_MODIFY) {
                        // A new Path was created 
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        //Mark the observable object as changed.
                        this.setChanged();
                        System.out.println("New path created: " + newPath + " kind of operation: " + kind);
                        
                        notifyObservers(this);
                    }
                }

                if(!key.reset()) {
                    break; //loop
                }
            }
        } catch(IOException | InterruptedException ioe) {
            ioe.printStackTrace();
        }
    }
}
