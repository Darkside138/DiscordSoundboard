package net.dirtydeeds.discordsoundboard;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
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
    public void watchDirectoryPath(Path path) {
        // Sanity check - Check if path is a folder
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
        try(WatchService service = fs.newWatchService()) {

            // We register the path to the service
            // We watch for creation events
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println(dir.toString());
                    dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                    return FileVisitResult.CONTINUE;
                }
            });

            // Start the infinite polling loop
            WatchKey key;
            while(true) {
                key = service.take();

                // Dequeueing events
                Kind<?> kind;
                for(WatchEvent<?> watchEvent : key.pollEvents()) {
                    // Get the type of the event
                    kind = watchEvent.kind();
                    if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        //Mark the observable object as changed.
                        this.setChanged();
                        System.out.println("New path created: " + newPath + " kind of operation: " + kind);
                        notifyObservers();
                    } else if (kind == ENTRY_DELETE) {
                        Path deletedPath = ((WatchEvent<Path>) watchEvent).context();
                        this.setChanged();
                        notifyObservers(deletedPath);
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
