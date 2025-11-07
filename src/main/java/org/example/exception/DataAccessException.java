package org.example.exception;

/**
 * Общее исключение для всех ошибок, связанных с доступом к данным.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}