package application;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import domain.Car;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CarServiceTest {
    @Test
    void emptyCatalogProducesEmptyResults() {
        CarService service = new CarService(path -> List.of());

        assertTrue(service.getAll().isEmpty());
        assertTrue(service.findByMinimumPower(100).isEmpty());
        assertTrue(service.getModelNamesByMinimumPower(100).isEmpty());
    }

    @Test
    void preservesInsertionOrderAndDuplicates() {
        CarService service = new CarService(path -> List.of());
        Car car = new Car("Lada Vesta", 106, 2020);
        service.add(car);
        service.add(car);

        assertEquals(List.of(car, car), service.getAll());
    }

    @Test
    void rejectsNullWithoutChangingCatalog() {
        CarService service = new CarService(path -> List.of());

        assertThrows(NullPointerException.class, () -> service.add(null));
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void returnsImmutableSnapshotIndependentOfLaterAdditions() {
        CarService service = new CarService(path -> List.of());
        Car first = new Car("Lada Vesta", 106, 2020);
        Car second = new Car("Toyota Camry", 181, 2019);
        service.add(first);
        List<Car> snapshot = service.getAll();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(second));
        service.add(second);

        assertEquals(List.of(first), snapshot);
        assertEquals(List.of(first, second), service.getAll());
    }

    @Test
    void filtersAtInclusivePowerBoundaryAndSortsByYearWithoutMutatingCatalog() {
        CarService service = new CarService(path -> List.of());
        Car newer = new Car("New", 200, 2022);
        Car excluded = new Car("Weak", 99, 2010);
        Car boundary = new Car("Boundary", 100, 2018);
        Car sameYear = new Car("Same year", 150, 2018);
        List<Car> original = List.of(newer, excluded, boundary, sameYear);
        original.forEach(service::add);

        assertEquals(List.of(boundary, sameYear, newer), service.findByMinimumPower(100));
        assertEquals(original, service.getAll());
        assertTrue(service.findByMinimumPower(201).isEmpty());
    }

    @Test
    void mapsFilteredAndSortedCarsToModelNamesKeepingDuplicates() {
        CarService service = new CarService(path -> List.of());
        service.add(new Car("Camry", 200, 2022));
        service.add(new Car("Excluded", 90, 2010));
        service.add(new Car("Vesta", 106, 2020));
        service.add(new Car("Camry", 181, 2019));

        assertEquals(List.of("Camry", "Vesta", "Camry"), service.getModelNamesByMinimumPower(100));
    }

    @Test
    void zeroThresholdIncludesEveryCar() {
        CarService service = new CarService(path -> List.of());
        Car car = new Car("Test", 1, 2020);
        service.add(car);

        assertEquals(List.of(car), service.findByMinimumPower(0));
    }

    @Test
    void rejectsNegativeThresholdEvenForEmptyCatalog() {
        CarService service = new CarService(path -> List.of());

        assertThrows(IllegalArgumentException.class, () -> service.findByMinimumPower(-1));
        assertThrows(IllegalArgumentException.class, () -> service.getModelNamesByMinimumPower(-1));
    }

    @Test
    void processedResultsCannotBeModified() {
        CarService service = new CarService(path -> List.of());
        service.add(new Car("Vesta", 106, 2020));

        assertThrows(UnsupportedOperationException.class, () -> service.findByMinimumPower(0).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> service.getModelNamesByMinimumPower(0).add("Other"));
        assertEquals(1, service.getAll().size());
    }

    @Test
    void loadsFromRequestedPathAndAppendsCarsKeepingOrderAndDuplicates() throws CarLoadException {
        Path file = Path.of("cars.csv");
        Car existing = new Car("Vesta", 106, 2020);
        Car loaded = new Car("Camry", 181, 2019);
        CarService service = new CarService(path -> {
            assertEquals(file, path);
            return List.of(loaded, existing);
        });
        service.add(existing);

        int count = service.loadFrom(file);

        assertEquals(2, count);
        assertEquals(List.of(existing, loaded, existing), service.getAll());
    }

    @Test
    void emptySourceDoesNotClearExistingCars() throws CarLoadException {
        CarService service = new CarService(path -> List.of());
        Car existing = new Car("Vesta", 106, 2020);
        service.add(existing);

        assertEquals(0, service.loadFrom(Path.of("empty.csv")));
        assertEquals(List.of(existing), service.getAll());
    }

    @Test
    void propagatesLoadFailureWithoutChangingCars() {
        CarLoadException failure = new CarLoadException("Не удалось загрузить данные");
        CarService service = new CarService(path -> {
            throw failure;
        });
        Car existing = new Car("Vesta", 106, 2020);
        service.add(existing);

        CarLoadException actual = assertThrows(CarLoadException.class,
                () -> service.loadFrom(Path.of("broken.csv")));

        assertSame(failure, actual);
        assertEquals(List.of(existing), service.getAll());
    }

    @Test
    void rejectsMissingLoader() {
        assertThrows(NullPointerException.class, () -> new CarService(null));
    }

    @Test
    void rejectsMissingPathBeforeCallingLoader() {
        CarService service = new CarService(path -> fail("Загрузчик не должен вызываться"));

        assertThrows(NullPointerException.class, () -> service.loadFrom(null));
    }
}
