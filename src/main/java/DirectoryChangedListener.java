public interface DirectoryChangedListener<ObjectTypeNotified> {
    /**
     * Listener fire a onDirectoryChanged
     * @param event the information to be notified with type ObjectTypeNotified
     */
    void onDirectoryChanged(ChangedEvent<ObjectTypeNotified> event);
}
