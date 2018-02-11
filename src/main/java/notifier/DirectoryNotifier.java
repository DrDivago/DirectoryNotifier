package notifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import validator.Validator;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Class observe a directory and notify the listeners when a new file is created in the directory
 *
 * @param <T>
 */

public class DirectoryNotifier<T> extends Listener<DirectoryChangedListener<T>> {

    private WatchService watchService;
    private Map<Path, Boolean> alreadyNotitfiedFiles = new HashMap<>();
    private final Logger logger = LogManager.getLogger();


    /**
     * Create a new DirectoryNotifier with a WatchService
     *
     * @throws IOException throws if error start watcher
     */
    public DirectoryNotifier() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
    }

    @SuppressWarnings("unchecked")
    private final Function<Path, Optional<T>> defaultFunction = path -> Optional.of((T) path);

    /**
     * Observe a directory passing a default function (no action)
     *
     * @param watchedPath the path to watch for new created file
     * @throws IOException throw if error in start service
     */
    public void observe(Path watchedPath) throws IOException {
        observe(watchedPath, defaultFunction);
    }

    /**
     * Observe a directory watchedPath waiting a new file is added. When a new file is added in the directory, the file
     * is transformed with the function generateInformationToNotify that created an Object of the type T
     * and notify it to the listeners
     *
     * @param watchedPath                 path to watch
     * @param generateInformationToNotify function to create an Object to be notified to the listeners
     * @throws IOException throw if error start service
     */
    public void observe(Path watchedPath, Function<Path, Optional<T>> generateInformationToNotify) throws IOException {
        Validator.validateNotNull(watchedPath);
        Validator.validateNotNull(generateInformationToNotify);

        if (!watchedPath.toFile().isDirectory())
            throw new IllegalArgumentException("You can observe only a directory");

        watchedPath.register(watchService, ENTRY_CREATE);

        logger.info("Watch service start..");
        logger.info("Watching dir: " + watchedPath);

        while (true) {
            WatchKey key;
            try {
                logger.info("Waiting for event...");
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch ( ClosedWatchServiceException e1) {
                logger.info("Closed watch service");
                return;
            }
            extractEventToNotify(watchedPath, generateInformationToNotify, key);


            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void extractEventToNotify(Path watchedPath, Function<Path, Optional<T>> generateInformationToNotify, WatchKey key) {
        //Extract the result to be notified
        key.pollEvents().forEach(event -> {

            WatchEvent.Kind<?> kind = event.kind();
            if (kind == OVERFLOW) {
                return;
            }
            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;
            Path filename = ev.context();
            Path fullPath = watchedPath.resolve(filename);

            logger.info("New file found: " + fullPath);
            Optional<T> optionalInformationToNotify = generateInformationToNotify.apply(fullPath);
            optionalInformationToNotify.ifPresent(informationToNotify -> notifyEvent(fullPath, informationToNotify));
        });
    }

    private void notifyEvent(Path fullPath, T informationToNotify) {
        try (FileChannel ch = FileChannel.open(fullPath, StandardOpenOption.WRITE);
             FileLock lock = ch.tryLock()) {

            if (lock != null && !alreadyNotitfiedFiles.containsKey(fullPath)) {
                alreadyNotitfiedFiles.put(fullPath, true);
                logger.info("Writing new file ended. Notify " + fullPath);
                fireDirectoryChangedEvent(informationToNotify);
            }
        } catch (IOException e) {
            logger.info("Exception " + e);
        }
    }

    private void fireDirectoryChangedEvent(T informationToNotify) {
        ChangedEvent<T> newFileEvent = new ChangedEvent<>(this, informationToNotify);
        notifyListeners(listener -> listener.onDirectoryChanged(newFileEvent));
    }

    /**
     * Close the watch service to the directory
     *
     * @throws IOException throw if service cannot be stopped
     */
    public void close() throws IOException {
        watchService.close();
    }
}

