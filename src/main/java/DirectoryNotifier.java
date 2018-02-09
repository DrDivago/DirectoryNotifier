import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Class observe a directory and notify the listeners when a new file is created in the directory
 * @param <ObjectNotified>
 */

public class DirectoryNotifier<ObjectNotified> extends Listener<DirectoryChangedListener<ObjectNotified>>  {

    private WatchService watchService;
    private Map<Path, Boolean> alreadyNotitfiedFiles = new HashMap<>();


    /**
     * Create a new DirectoryNotifier with a WatchService
     * @throws IOException throws if error start watcher
     */
    public DirectoryNotifier() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
    }

    @SuppressWarnings("unchecked")
    private final Function<Path, Optional<ObjectNotified>> defaultFunction = (path)-> Optional.of((ObjectNotified) path);

    /**
     * Observe a directory passing a default function (no action)
     * @param watchedPath the path to watch for new created file
     * @throws IOException throw if error in start service
     */
    public void observe(Path watchedPath) throws IOException {
        observe(watchedPath, defaultFunction);
    }

    /**
     * Observe a directory watchedPath waiting a new file is added. When a new file is added in the directory, the file
     * is transformed with the function generateInformationToNotify that created an Object of the type ObjectNotified
     * and notify it to the listeners
     * @param watchedPath path to watch
     * @param generateInformationToNotify function to create an Object to be notified to the listeners
     * @throws IOException throw if error start service
     */
    public void observe(Path watchedPath, Function<Path, Optional<ObjectNotified>> generateInformationToNotify) throws IOException {
        //Validator.validateNotNull(watchedPath);
        //Validator.validateNotNull(generateInformationToNotify);
        if (Files.isDirectory(watchedPath)) {
            watchedPath.register(watchService, ENTRY_MODIFY);

            //LOGGER.info("Watch service start..");
            //LOGGER.info("Watching dir: " + watchedPath);

            for (; ; ) {
                WatchKey key;
                try {
                    //LOGGER.info("Waiting for event...");
                    key = watchService.take();
                } catch (InterruptedException e) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path fullPath = watchedPath.resolve(filename);

                    //LOGGER.info("New file found: " + fullPath);

                    //Extract the result to be notified
                    Optional<ObjectNotified> optionalInformationToNotify = generateInformationToNotify.apply(fullPath);

                    optionalInformationToNotify.ifPresent(informationToNotify -> {


                        try(FileChannel ch = FileChannel.open(fullPath, StandardOpenOption.WRITE);
                            FileLock lock = ch.tryLock()){

                            if(lock != null) {
                                if (!alreadyNotitfiedFiles.containsKey(fullPath)) {
                                    alreadyNotitfiedFiles.put(fullPath, true);
                                    ch.close();
                                    //LOGGER.info("Writing new file ended. Notify " + fullPath);
                                    fireDirectoryChangedEvent(informationToNotify);
                                }
                            }
                        } catch (IOException e) {
                            //Process is still copying the file
                            //LOGGER.info("Waiting for " + fullPath + " completely wrote on disk..");
                        }
                    });
                }
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }
    }

    private void fireDirectoryChangedEvent(ObjectNotified informationToNotify) {
        ChangedEvent<ObjectNotified> newFileEvent = new ChangedEvent<>(this, informationToNotify);
        notifyListeners( listener -> listener.onDirectoryChanged(newFileEvent));
    }

    /**
     * Close the watch service to the directory
     * @throws IOException throw if service cannot be stopped
     */
    public void close() throws IOException {
        watchService.close();
    }
}

