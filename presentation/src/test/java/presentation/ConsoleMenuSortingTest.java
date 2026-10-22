package presentation;

import application.CarService;
import application.port.SortAlgorithm;
import domain.Car;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuSortingTest {
    private final CarService service = new CarService(path -> List.of(), new ArrayList<>());
    private final RecordingSortAlgorithm bubbleSort = new RecordingSortAlgorithm();
    private final RecordingSortAlgorithm insertionSort = new RecordingSortAlgorithm();

    @ParameterizedTest
    @CsvSource({"1, 1", "1, 2", "2, 1", "2, 2"})
    void selectsAlgorithmAndDirectionWithoutChangingMainOrder(int algorithm, int direction) {
        List<Car> original = List.of(new Car("Newer", 200, 2022),
                new Car("First equal", 100, 2020), new Car("Older", 90, 2018),
                new Car("Second equal", 120, 2020));
        original.forEach(service::add);

        String output = run("8\n" + algorithm + "\n" + direction + "\n0\n");

        List<String> expected = direction == 1
                ? List.of("Older —", "First equal —", "Second equal —", "Newer —")
                : List.of("Newer —", "First equal —", "Second equal —", "Older —");
        int previous = -1;
        for (String model : expected) {
            int position = output.indexOf(model);
            assertTrue(position > previous, "Порядок модели " + model);
            previous = position;
        }
        assertTrue(output.contains(direction == 1 ? "(по возрастанию):" : "(по убыванию):"));
        assertEquals(algorithm == 1 ? 1 : 0, bubbleSort.calls);
        assertEquals(algorithm == 2 ? 1 : 0, insertionSort.calls);
        assertEquals(original, service.getAll());
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void canSwitchAlgorithmAndStillUseOriginalNumbersForViewingAndRemoving() {
        Car first = new Car("Newer", 200, 2022);
        service.add(first);
        service.add(new Car("Older", 100, 2018));

        String output = run("8\n1\n1\n8\n2\n2\n5\n1\n6\n2\n3\n7\n0\n");

        assertEquals(1, bubbleSort.calls);
        assertEquals(1, insertionSort.calls);
        assertTrue(output.contains("1. Newer — 200 л. с., 2022 г."));
        assertTrue(output.contains("Удалён автомобиль: Older."));
        assertTrue(output.contains("Количество автомобилей: 1"));
        assertEquals(List.of(first), service.getAll());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 3})
    void rejectsUnknownAlgorithmAndReturnsToMainMenu(int choice) {
        Car car = new Car("Vesta", 106, 2020);
        service.add(car);

        String output = run("8\n" + choice + "\n7\n0\n");

        assertTrue(output.contains("Ошибка: Выберите алгоритм 1 или 2"));
        assertTrue(output.contains("Количество автомобилей: 1"));
        assertEquals(0, bubbleSort.calls + insertionSort.calls);
        assertEquals(List.of(car), service.getAll());
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 3})
    void rejectsUnknownDirectionBeforeRunningAlgorithm(int choice) {
        Car car = new Car("Vesta", 106, 2020);
        service.add(car);

        String output = run("8\n1\n" + choice + "\n7\n0\n");

        assertTrue(output.contains("Ошибка: Выберите направление 1 или 2"));
        assertTrue(output.contains("Количество автомобилей: 1"));
        assertEquals(0, bubbleSort.calls + insertionSort.calls);
        assertEquals(List.of(car), service.getAll());
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void retriesNonNumericChoicesAndSortsSingleCar() {
        Car car = new Car("Vesta", 106, 2020);
        service.add(car);

        String output = run("8\nwrong\n2147483648\n2\nwrong\n2\n0\n");

        assertTrue(output.contains("Ошибка: введите целое число."));
        assertTrue(output.contains("Vesta — 106 л. с., 2020 г."));
        assertEquals(0, bubbleSort.calls);
        assertEquals(1, insertionSort.calls);
        assertEquals(List.of(car), service.getAll());
        assertTrue(output.contains("Работа завершена."));
    }

    @Test
    void emptyStorageDoesNotAskForSortingOptions() {
        String output = run("8\n7\n0\n");

        assertTrue(output.contains("Список автомобилей пуст."));
        assertFalse(output.contains("Выберите алгоритм:"));
        assertFalse(output.contains("Выберите направление:"));
        assertEquals(0, bubbleSort.calls + insertionSort.calls);
        assertTrue(output.contains("Количество автомобилей: 0"));
        assertTrue(output.contains("Работа завершена."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8\n", "8\n1\n", "8\n2\n"})
    void endOfInputDuringSelectionDoesNotRunAlgorithm(String input) {
        Car car = new Car("Vesta", 106, 2020);
        service.add(car);

        String output = run(input);

        assertTrue(output.contains("Ввод завершён."));
        assertEquals(0, bubbleSort.calls + insertionSort.calls);
        assertEquals(List.of(car), service.getAll());
    }

    private String run(String input) {
        StringWriter output = new StringWriter();
        new ConsoleMenu(service, bubbleSort, insertionSort,
                new StringReader(input), new PrintWriter(output)).run();
        return output.toString();
    }

    private static final class RecordingSortAlgorithm implements SortAlgorithm {
        private int calls;

        @Override
        public <T> void sort(List<T> items, Comparator<? super T> comparator) {
            calls++;
            items.sort(comparator);
        }
    }
}
