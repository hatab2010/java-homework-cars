package presentation;

import application.CarLoadException;
import application.CarService;
import application.port.SortAlgorithm;
import domain.Car;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuTest {
    @Test
    void addsCarAfterInvalidNumericInputAndDisplaysAllFields() {
        CarService service = emptyService();

        String output = run("1\n  Лада Веста  \nwrong\n2147483648\n106\n2020\n3\n0\n", service);

        assertEquals(List.of(new Car("Лада Веста", 106, 2020)), service.getAll());
        assertTrue(output.contains("Ошибка: введите целое число."));
        assertTrue(output.contains("Лада Веста — 106 л. с., 2020 г."));
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Vesta\n0\n2020", "Vesta\n106\n0", " \n106\n2020"})
    void invalidCarIsNotAddedAndMenuRemainsUsable(String carInput) {
        CarService service = emptyService();

        String output = run("1\n" + carInput + "\n3\n0\n", service);

        assertTrue(service.getAll().isEmpty());
        assertTrue(output.contains("Ошибка:"));
        assertTrue(output.contains("Список автомобилей пуст."));
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void loadsFromEnteredPathAndDisplaysLoadedCars() {
        Car car = new Car("Camry", 181, 2019);
        CarService service = new CarService(path -> {
            assertEquals(Path.of("folder with spaces/cars.csv"), path);
            return List.of(car);
        }, new ArrayList<>());

        String output = run("2\nfolder with spaces/cars.csv\n3\n0\n", service);

        assertEquals(List.of(car), service.getAll());
        assertTrue(output.contains("Загружено автомобилей: 1"));
        assertTrue(output.contains("Camry — 181 л. с., 2019 г."));
    }

    @Test
    void reportsFileFailureAndContinuesWithExistingCars() {
        CarService service = new CarService(path -> {
            throw new CarLoadException("Строка 2: некорректный год");
        }, new ArrayList<>());
        Car existing = new Car("Existing", 90, 2015);
        service.add(existing);

        String output = run("2\nbroken.csv\n3\n0\n", service);

        assertEquals(List.of(existing), service.getAll());
        assertTrue(output.contains("Ошибка: Строка 2: некорректный год"));
        assertTrue(output.contains("Existing — 90 л. с., 2015 г."));
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "invalid\u0000path"})
    void rejectsInvalidPathBeforeLoading(String path) {
        CarService service = new CarService(source -> {
            throw new AssertionError("Загрузчик не должен вызываться");
        }, new ArrayList<>());

        String output = run("2\n" + path + "\n0\n", service);

        assertTrue(output.contains("Ошибка:"));
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void printsFilteredModelNamesInYearOrderWithoutChangingCars() {
        CarService service = emptyService();
        List<Car> original = List.of(new Car("Newer", 200, 2022),
                new Car("Weak", 90, 2010), new Car("Older", 100, 2018));
        original.forEach(service::add);

        String output = run("4\n100\n0\n", service);

        assertTrue(output.contains("Older"));
        assertTrue(output.indexOf("Older") < output.indexOf("Newer"));
        assertFalse(output.contains("Weak"));
        assertEquals(original, service.getAll());
    }

    @Test
    void handlesNegativeThresholdAndEmptySelection() {
        String output = run("4\n-1\n4\n100\n0\n", emptyService());

        assertTrue(output.contains("Минимальная мощность не может быть отрицательной"));
        assertTrue(output.contains("Подходящие автомобили не найдены."));
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void showsCarByOneBasedNumberWithoutChangingStorage(int number) {
        CarService service = emptyService();
        List<Car> cars = List.of(new Car("First", 100, 2018),
                new Car("Middle", 120, 2019), new Car("Last", 150, 2020));
        cars.forEach(service::add);

        String output = run("5\n" + number + "\n0\n", service);

        Car selected = cars.get(number - 1);
        assertTrue(output.contains(number + ". " + selected.model() + " — "
                + selected.powerHp() + " л. с., " + selected.year() + " г."));
        assertFalse(output.contains("2. Middle —"));
        assertEquals(cars, service.getAll());
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void removesOnlySelectedDuplicateAndUsesNewNumbersAndCount() {
        CarService service = emptyService();
        Car duplicate = new Car("Vesta", 106, 2020);
        Car last = new Car("Camry", 181, 2019);
        service.add(duplicate);
        service.add(duplicate);
        service.add(last);

        String output = run("7\n6\n2\n7\n5\n2\n3\n0\n", service);

        assertEquals(List.of(duplicate, last), service.getAll());
        assertTrue(output.contains("Удалён автомобиль: Vesta."));
        assertTrue(output.contains("Количество автомобилей: 3"));
        assertTrue(output.contains("Количество автомобилей: 2"));
        assertTrue(output.contains("1. Vesta — 106 л. с., 2020 г."));
        assertTrue(output.contains("2. Camry — 181 л. с., 2019 г."));
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void removesFirstAndLastCarsUntilEmptyAndAllowsAddingAgain() {
        CarService service = emptyService();
        service.add(new Car("First", 100, 2018));
        service.add(new Car("Middle", 120, 2019));
        service.add(new Car("Last", 150, 2020));

        String output = run("6\n1\n6\n2\n6\n1\n7\n3\n"
                + "1\nNew\n110\n2021\n5\n1\n0\n", service);

        assertTrue(output.contains("Удалён автомобиль: First."));
        assertTrue(output.contains("Удалён автомобиль: Last."));
        assertTrue(output.contains("Удалён автомобиль: Middle."));
        assertTrue(output.contains("Количество автомобилей: 0"));
        assertTrue(output.contains("Список автомобилей пуст."));
        assertTrue(output.contains("1. New — 110 л. с., 2021 г."));
        assertEquals(List.of(new Car("New", 110, 2021)), service.getAll());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "3", "2147483647", "-2147483648"})
    void invalidCarNumberDoesNotChangeStorageAndMenuContinues(String number) {
        CarService service = emptyService();
        List<Car> cars = List.of(new Car("Vesta", 106, 2020), new Car("Camry", 181, 2019));
        cars.forEach(service::add);

        String output = run("5\n" + number + "\n6\n" + number + "\n7\n0\n", service);

        assertEquals(cars, service.getAll());
        assertEquals(2, output.split("Ошибка: Номер автомобиля должен быть от 1 до 2", -1).length - 1);
        assertTrue(output.contains("Количество автомобилей: 2"));
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void retriesNonNumericCarNumberBeforeViewingAndRemoving() {
        CarService service = emptyService();
        Car car = new Car("Vesta", 106, 2020);
        service.add(car);

        String output = run("5\nwrong\n2147483648\n1\n6\nwrong\n1\n7\n0\n", service);

        assertTrue(output.contains("Ошибка: введите целое число."));
        assertTrue(output.contains("1. Vesta — 106 л. с., 2020 г."));
        assertTrue(output.contains("Удалён автомобиль: Vesta."));
        assertTrue(output.contains("Количество автомобилей: 0"));
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void emptyStorageDoesNotAskForCarNumber() {
        String output = run("5\n6\n7\n0\n", emptyService());

        assertEquals(2, output.split("Список автомобилей пуст.", -1).length - 1);
        assertFalse(output.contains("Номер автомобиля: "));
        assertTrue(output.contains("Количество автомобилей: 0"));
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5\n", "6\n"})
    void endOfInputWhileChoosingCarLeavesStorageUnchanged(String input) {
        CarService service = emptyService();
        Car car = new Car("Vesta", 106, 2020);
        service.add(car);

        String output = run(input, service);

        assertTrue(output.contains("Ввод завершён."));
        assertEquals(List.of(car), service.getAll());
    }

    @Test
    void unknownCommandsDoNotTerminateMenu() {
        String output = run("abc\n99\n\n3\n0\n", emptyService());

        assertTrue(output.contains("Неизвестная команда"));
        assertTrue(output.contains("Список автомобилей пуст."));
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1\n", "1\nVesta\n", "1\nVesta\n106\n", "2\n", "4\n"})
    void endOfInputExitsWithoutAddingIncompleteCar(String input) {
        CarService service = emptyService();

        String output = run(input, service);

        assertTrue(output.contains("Ввод завершён."));
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void inputFailureIsReportedWithoutAddingIncompleteData() {
        Reader brokenInput = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("Поток недоступен");
            }

            @Override
            public void close() {
            }
        };
        StringWriter output = new StringWriter();
        CarService service = emptyService();

        new ConsoleMenu(service, new UnusedSortAlgorithm(), new UnusedSortAlgorithm(),
                brokenInput, new PrintWriter(output)).run();

        assertTrue(output.toString().contains("Ошибка чтения ввода: Поток недоступен"));
        assertTrue(service.getAll().isEmpty());
    }

    private CarService emptyService() {
        return new CarService(path -> List.of(), new ArrayList<>());
    }

    private String run(String input, CarService service) {
        StringWriter output = new StringWriter();
        new ConsoleMenu(service, new UnusedSortAlgorithm(), new UnusedSortAlgorithm(),
                new StringReader(input), new PrintWriter(output)).run();
        return output.toString();
    }

    private static final class UnusedSortAlgorithm implements SortAlgorithm {
        @Override
        public <T> void sort(List<T> items, Comparator<? super T> comparator) {
            throw new AssertionError("В этом сценарии сортировка не ожидается");
        }
    }
}
