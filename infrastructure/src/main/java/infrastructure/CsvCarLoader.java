package infrastructure;

import application.CarLoadException;
import application.port.CarLoader;
import domain.Car;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** UTF-8, без заголовка: модель;мощность;год. Кавычки и разделители внутри полей не поддерживаются. */
public final class CsvCarLoader implements CarLoader {
    @Override
    public List<Car> load(Path path) throws CarLoadException {
        Objects.requireNonNull(path, "Путь не должен быть null");
        List<Car> cars = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (!line.isBlank()) {
                    cars.add(parseLine(line, lineNumber));
                }
            }
        } catch (IOException error) {
            throw new CarLoadException("Не удалось прочитать файл: " + path, error);
        }
        return List.copyOf(cars);
    }

    private Car parseLine(String line, int lineNumber) throws CarLoadException {
        String[] fields = line.split(";", -1);
        if (fields.length != 3) {
            throw new CarLoadException("Строка " + lineNumber + ": ожидается модель;мощность;год");
        }
        try {
            if (fields[0].contains("\"")) {
                throw new IllegalArgumentException("Кавычки в модели не поддерживаются");
            }
            int powerHp = Integer.parseInt(fields[1].strip());
            int year = Integer.parseInt(fields[2].strip());
            return new Car(fields[0], powerHp, year);
        } catch (NumberFormatException error) {
            throw new CarLoadException("Строка " + lineNumber
                    + ": мощность и год должны быть целыми числами в диапазоне int", error);
        } catch (IllegalArgumentException error) {
            throw new CarLoadException("Строка " + lineNumber + ": " + error.getMessage(), error);
        }
    }
}
