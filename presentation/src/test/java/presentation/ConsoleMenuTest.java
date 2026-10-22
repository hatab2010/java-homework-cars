package presentation;

import application.CarLoadException;
import application.CarService;
import domain.Car;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
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
        });

        String output = run("2\nfolder with spaces/cars.csv\n3\n0\n", service);

        assertEquals(List.of(car), service.getAll());
        assertTrue(output.contains("Загружено автомобилей: 1"));
        assertTrue(output.contains("Camry — 181 л. с., 2019 г."));
    }

    @Test
    void reportsFileFailureAndContinuesWithExistingCars() {
        CarService service = new CarService(path -> {
            throw new CarLoadException("Строка 2: некорректный год");
        });
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
        });

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

        new ConsoleMenu(service, brokenInput, new PrintWriter(output)).run();

        assertTrue(output.toString().contains("Ошибка чтения ввода: Поток недоступен"));
        assertTrue(service.getAll().isEmpty());
    }

    private CarService emptyService() {
        return new CarService(path -> List.of());
    }

    private String run(String input, CarService service) {
        StringWriter output = new StringWriter();
        new ConsoleMenu(service, new StringReader(input), new PrintWriter(output)).run();
        return output.toString();
    }
}
