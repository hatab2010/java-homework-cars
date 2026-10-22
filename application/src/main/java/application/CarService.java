package application;

import application.port.CarLoader;
import application.port.SortAlgorithm;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.nio.file.Path;
import domain.Car;

/** Прикладные операции добавления, просмотра и обработки автомобилей. */
public final class CarService {
    private final List<Car> cars;
    private final CarLoader carLoader;

    /** Переданная изменяемая коллекция становится хранилищем сервиса без копирования. */
    public CarService(CarLoader carLoader, List<Car> cars) {
        this.carLoader = Objects.requireNonNull(carLoader, "Загрузчик не должен быть null");
        this.cars = Objects.requireNonNull(cars, "Коллекция автомобилей не должна быть null");
        cars.forEach(car -> Objects.requireNonNull(car, "Автомобиль не должен быть null"));
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

    /** Индекс начинается с нуля и соответствует порядку добавления. */
    public Car get(int index) {
        return cars.get(index);
    }

    /** Удаляет по индексу с нуля и возвращает удалённый автомобиль. */
    public Car remove(int index) {
        return cars.remove(index);
    }

    public int size() {
        return cars.size();
    }

    /** Независимый неизменяемый снимок в порядке добавления. */
    public List<Car> getAll() {
        return List.copyOf(cars);
    }

    /** Сортирует копию данных, сохраняя номера автомобилей в основном списке. */
    public List<Car> getSortedByYear(SortAlgorithm algorithm, SortDirection direction) {
        Objects.requireNonNull(algorithm, "Алгоритм сортировки не должен быть null");
        Objects.requireNonNull(direction, "Направление сортировки не должно быть null");
        Comparator<Car> comparator = Comparator.comparingInt(Car::year);
        if (direction == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }
        List<Car> sorted = new ArrayList<>(cars);
        algorithm.sort(sorted, comparator);
        return List.copyOf(sorted);
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
