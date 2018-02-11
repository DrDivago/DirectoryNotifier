package notifier;

public interface DirectoryChangedListener<T> {
    /**
     * Listener fire a onDirectoryChanged
     * @param event the information to be notified with type T
     */
    void onDirectoryChanged(ChangedEvent<T> event);
}
