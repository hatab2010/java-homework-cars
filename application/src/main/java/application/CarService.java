package application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.nio.file.Path;
import domain.Car;

/** Прикладные операции добавления, просмотра и обработки автомобилей. */
public final class CarService {
    private final List<Car> cars = new ArrayList<>();
    private final CarLoader carLoader;

    public CarService(CarLoader carLoader) {
        this.carLoader = Objects.requireNonNull(carLoader, "Загрузчик не должен быть null");
    }

    /** Добавляет все прочитанные автомобили. При ошибке загрузки состояние не меняется. */
    public int loadFrom(Path path) throws CarLoadException {
        Objects.requireNonNull(path, "Путь не должен быть null");
        List<Car> loaded = List.copyOf(carLoader.load(path));
        cars.addAll(loaded);
        return loaded.size();
    }

    public void add(Car car) {
        cars.add(Objects.requireNonNull(car, "Автомобиль не должен быть null"));
    }

    /** Независимый неизменяемый снимок в порядке добавления. */
    public List<Car> getAll() {
        return List.copyOf(cars);
    }

    /** Нижняя граница мощности включается; при одинаковом годе сохраняется порядок добавления. */
    public List<Car> findByMinimumPower(int minimumPowerHp) {
        if (minimumPowerHp < 0) {
            throw new IllegalArgumentException("Минимальная мощность не может быть отрицательной");
        }
        return cars.stream()
                .filter(car -> car.powerHp() >= minimumPowerHp)
                .sorted(Comparator.comparingInt(Car::year))
                .toList();
    }

    /** Преобразование отобранных автомобилей в названия моделей без удаления повторений. */
    public List<String> getModelNamesByMinimumPower(int minimumPowerHp) {
        return findByMinimumPower(minimumPowerHp).stream()
                .map(Car::model)
                .toList();
    }
}
