package infrastructure;

import application.CarLoadException;
import application.CarService;
import domain.Car;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvCarLoaderTest {
    @TempDir
    Path directory;

    private final CsvCarLoader loader = new CsvCarLoader();

    @Test
    void readsUtf8RecordsInOrderKeepingDuplicates() throws Exception {
        Path file = write("Лада Веста;106;2020\nToyota Camry;181;2019\nЛада Веста;106;2020\n");

        assertEquals(List.of(new Car("Лада Веста", 106, 2020), new Car("Toyota Camry", 181, 2019),
                new Car("Лада Веста", 106, 2020)), loader.load(file));
    }

    @Test
    void acceptsBomWhitespaceBlankLinesAndWindowsLineEndings() throws Exception {
        Path file = write("\uFEFF\r\n \t\r\n  Лада Веста  ; 106 ; 2020 \r\n\r\nToyota Camry;181;2019");

        assertEquals(List.of(new Car("Лада Веста", 106, 2020), new Car("Toyota Camry", 181, 2019)),
                loader.load(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\n \t\n", "\uFEFF"})
    void emptyFileProducesEmptyResult(String content) throws Exception {
        assertTrue(loader.load(write(content)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Vesta;106", "Vesta;106;2020;extra", "Vesta;106;", ";106;2020", "  ;106;2020",
            "Vesta;power;2020", "Vesta;106;year", "Vesta;2147483648;2020", "Vesta;106;2147483648",
            "Vesta;0;2020", "Vesta;-10;2020", "Vesta;106;0", "Vesta;106;-1", "\"Vesta\";106;2020"
    })
    void reportsPhysicalLineOfInvalidRecord(String invalidLine) throws Exception {
        Path file = write("Camry;181;2019\n\n" + invalidLine + "\n");

        CarLoadException error = assertThrows(CarLoadException.class, () -> loader.load(file));

        assertTrue(error.getMessage().startsWith("Строка 3:"), error.getMessage());
    }

    @Test
    void rejectsFutureYearUsingCarValidation() throws Exception {
        Path file = write("Vesta;106;" + (Year.now().getValue() + 1));

        CarLoadException error = assertThrows(CarLoadException.class, () -> loader.load(file));

        assertTrue(error.getMessage().startsWith("Строка 1:"));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }

    @Test
    void missingFileRetainsIoCause() {
        Path file = directory.resolve("missing.csv");

        CarLoadException error = assertThrows(CarLoadException.class, () -> loader.load(file));

        assertInstanceOf(NoSuchFileException.class, error.getCause());
        assertTrue(error.getMessage().contains(file.toString()));
    }

    @Test
    void rejectsDirectoryAsInput() {
        CarLoadException error = assertThrows(CarLoadException.class, () -> loader.load(directory));

        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    void rejectsMalformedUtf8() throws Exception {
        Path file = directory.resolve("invalid.csv");
        Files.write(file, new byte[]{(byte) 0xC3, (byte) 0x28});

        CarLoadException error = assertThrows(CarLoadException.class, () -> loader.load(file));

        assertInstanceOf(IOException.class, error.getCause());
    }

    @Test
    void invalidFileDoesNotAddEvenItsValidPrefixToService() throws Exception {
        Car existing = new Car("Existing", 90, 2015);
        CarService service = new CarService(loader);
        service.add(existing);
        Path file = write("Camry;181;2019\nVesta;invalid;2020\n");

        assertThrows(CarLoadException.class, () -> service.loadFrom(file));

        assertEquals(List.of(existing), service.getAll());
    }

    @Test
    void loadsIntoServiceAcrossApplicationBoundary() throws Exception {
        CarService service = new CarService(loader);
        Path file = write("Camry;181;2019\nVesta;106;2020\n");

        assertEquals(2, service.loadFrom(file));
        assertEquals(List.of("Camry", "Vesta"), service.getModelNamesByMinimumPower(100));
    }

    private Path write(String content) throws IOException {
        return Files.writeString(directory.resolve("cars.csv"), content, StandardCharsets.UTF_8);
    }
}
