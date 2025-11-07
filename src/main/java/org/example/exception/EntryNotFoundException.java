package org.example.exception;

/**
 * Выбрасывается, когда ожидаемая запись (e.g., по ID) не была найдена.
 */
public class EntryNotFoundException extends DataAccessException {

    public EntryNotFoundException(String message) {
        super(message);
    }

    public EntryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}