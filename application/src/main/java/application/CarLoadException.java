package application;

/** Ошибка чтения источника или некорректные данные в нём. */
public final class CarLoadException extends Exception {
    private static final long serialVersionUID = 1L;

    public CarLoadException(String message) {
        super(message);
    }

    public CarLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
