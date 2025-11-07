package org.example.exception;

/**
 * Исключение, выбрасываемое при попытке вставить дублирующуюся запись (UNIQUE).
 */
public class DuplicateEntryException extends DataAccessException {

    public DuplicateEntryException(String message) {
        super(message);
    }

    public DuplicateEntryException(String message, Throwable cause) {
        super(message, cause);
    }
}