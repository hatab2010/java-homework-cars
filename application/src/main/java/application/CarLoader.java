package application;

import domain.Car;
import java.nio.file.Path;
import java.util.List;

/** Источник автомобилей. Возвращает весь результат либо сообщает об ошибке. */
public interface CarLoader {
    List<Car> load(Path path) throws CarLoadException;
}
