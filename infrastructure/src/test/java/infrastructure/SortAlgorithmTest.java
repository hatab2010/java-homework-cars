package infrastructure;

import application.port.SortAlgorithm;
import domain.Car;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SortAlgorithmTest {
    @ParameterizedTest
    @MethodSource("algorithms")
    void sortsBoundaryCasesInBothDirections(SortAlgorithm algorithm) {
        List<List<Integer>> cases = List.of(
                List.of(),
                List.of(7),
                List.of(1, 2, 3, 4),
                List.of(4, 3, 2, 1),
                List.of(2, 2, 2),
                List.of(3, -1, 3, 0, -1),
                List.of(Integer.MAX_VALUE, 0, Integer.MIN_VALUE));
        List<Comparator<Integer>> directions = List.of(Comparator.naturalOrder(), Comparator.reverseOrder());

        for (List<Integer> source : cases) {
            for (Comparator<Integer> comparator : directions) {
                List<Integer> actual = customList(source);
                List<Integer> expected = new ArrayList<>(source);
                expected.sort(comparator);

                algorithm.sort(actual, comparator);

                assertEquals(expected, actual, "Исходные данные: " + source);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void sortsStringsInStandardListUsingCallerComparator(SortAlgorithm algorithm) {
        List<String> items = new ArrayList<>(List.of("bbb", "cc", "a", "bb", "a"));
        Comparator<String> comparator = Comparator.comparingInt(String::length)
                .thenComparing(Comparator.naturalOrder());

        algorithm.sort(items, comparator);

        assertEquals(List.of("a", "a", "bb", "cc", "bbb"), items);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void sortsCarsWithoutComparableAndPreservesEqualYearOrder(SortAlgorithm algorithm) {
        Car newer = new Car("Newer", 150, 2022);
        Car firstEqual = new Car("First equal", 100, 2020);
        Car older = new Car("Older", 90, 2018);
        Car secondEqual = new Car("Second equal", 120, 2020);
        List<Car> items = customList(List.of(newer, firstEqual, older, secondEqual));
        Comparator<Car> byYear = Comparator.comparingInt(Car::year);

        algorithm.sort(items, byYear);
        assertEquals(List.of(older, firstEqual, secondEqual, newer), items);

        algorithm.sort(items, byYear.reversed());
        assertEquals(List.of(newer, firstEqual, secondEqual, older), items);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void acceptsComparatorOfSupertype(SortAlgorithm algorithm) {
        List<Integer> items = customList(List.of(3, 1, 2));
        Comparator<Number> comparator = Comparator.comparingDouble(Number::doubleValue);

        algorithm.sort(items, comparator);

        assertEquals(List.of(1, 2, 3), items);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void randomInputsMatchStandardSortForBothListImplementations(SortAlgorithm algorithm) {
        Random random = new Random(2026);
        List<Comparator<Integer>> directions = List.of(Comparator.naturalOrder(), Comparator.reverseOrder());
        for (int sample = 0; sample < 40; sample++) {
            List<Integer> source = new ArrayList<>();
            int size = random.nextInt(100);
            for (int i = 0; i < size; i++) {
                source.add(random.nextInt(21) - 10);
            }
            for (Comparator<Integer> comparator : directions) {
                List<Integer> expected = new ArrayList<>(source);
                expected.sort(comparator);
                List<Integer> custom = customList(source);
                List<Integer> standard = new ArrayList<>(source);

                algorithm.sort(custom, comparator);
                algorithm.sort(standard, comparator);

                assertEquals(expected, custom, "Собственная коллекция, пример " + sample);
                assertEquals(expected, standard, "Стандартная коллекция, пример " + sample);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void comparatorCanDefineOrderForNullElements(SortAlgorithm algorithm) {
        List<Integer> items = customList(Arrays.asList(3, null, 1, null, 2));

        algorithm.sort(items, Comparator.nullsFirst(Comparator.naturalOrder()));

        assertEquals(Arrays.asList(null, null, 1, 2, 3), items);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    void rejectsMissingArgumentsBeforeChangingList(SortAlgorithm algorithm) {
        List<Integer> items = customList(List.of(2, 1));

        assertThrows(NullPointerException.class,
                () -> algorithm.sort(null, Comparator.<Integer>naturalOrder()));
        assertThrows(NullPointerException.class, () -> algorithm.sort(items, null));
        assertThrows(NullPointerException.class, () -> algorithm.sort(new CustomArrayList<Integer>(), null));
        assertEquals(List.of(2, 1), items);
    }

    private static Stream<SortAlgorithm> algorithms() {
        return Stream.of(new BubbleSort(), new InsertionSort());
    }

    private static <T> List<T> customList(List<T> source) {
        List<T> items = new CustomArrayList<>(0);
        items.addAll(source);
        return items;
    }
}
