import java.util.EventObject;

/**
 * Event to fire for a change in state
 * @param <ObjectTypeNotified> Object to be notified
 */
class ChangedEvent<ObjectTypeNotified> extends EventObject {
    private ObjectTypeNotified objectToNotify;

    /**
     * Create a new ChangeEvent with the type ObjectTypeNotified
     * @param o EventObject
     * @param objectToNotify object to notify
     */
    ChangedEvent(Object o, ObjectTypeNotified objectToNotify) {
        super(o);
        this.objectToNotify = objectToNotify;
    }

    public ObjectTypeNotified getObject() {
        return objectToNotify;
    }
}
