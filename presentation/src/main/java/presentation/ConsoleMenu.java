package presentation;

import application.CarLoadException;
import application.CarService;
import application.SortDirection;
import application.port.SortAlgorithm;
import domain.Car;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Консольный адаптер. Переданные потоки остаются во владении вызывающего кода. */
public final class ConsoleMenu {
    private final CarService service;
    private final SortAlgorithm bubbleSort;
    private final SortAlgorithm insertionSort;
    private final BufferedReader input;
    private final PrintWriter output;

    public ConsoleMenu(CarService service, SortAlgorithm bubbleSort, SortAlgorithm insertionSort,
                       Reader input, PrintWriter output) {
        this.service = Objects.requireNonNull(service);
        this.bubbleSort = Objects.requireNonNull(bubbleSort);
        this.insertionSort = Objects.requireNonNull(insertionSort);
        this.input = new BufferedReader(Objects.requireNonNull(input));
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        try {
            while (true) {
                printMenu();
                String command = readLine("Выберите пункт: ").strip();
                try {
                    switch (command) {
                        case "1" -> addCar();
                        case "2" -> loadFile();
                        case "3" -> showCars();
                        case "4" -> processCars();
                        case "5" -> showCar();
                        case "6" -> removeCar();
                        case "7" -> output.println("Количество автомобилей: " + service.size());
                        case "8" -> sortCars();
                        case "0" -> {
                            output.println("Работа завершена.");
                            return;
                        }
                        default -> output.println("Неизвестная команда. Выберите пункт от 0 до 8.");
                    }
                } catch (CarLoadException | IllegalArgumentException error) {
                    output.println("Ошибка: " + error.getMessage());
                }
            }
        } catch (EOFException endOfInput) {
            output.println("Ввод завершён.");
        } catch (IOException error) {
            output.println("Ошибка чтения ввода: " + error.getMessage());
        } finally {
            output.flush();
        }
    }

    private void printMenu() {
        output.println();
        output.println("=== Автомобили ===");
        output.println("1. Добавить автомобиль вручную");
        output.println("2. Загрузить автомобили из файла");
        output.println("3. Показать все автомобили");
        output.println("4. Отобрать по мощности и отсортировать по году");
        output.println("5. Показать автомобиль по номеру");
        output.println("6. Удалить автомобиль по номеру");
        output.println("7. Показать количество автомобилей");
        output.println("8. Отсортировать по году с выбором алгоритма");
        output.println("0. Выход");
    }

    private void addCar() throws IOException {
        String model = readLine("Модель: ");
        int powerHp = readInteger("Мощность, л. с.: ");
        int year = readInteger("Год выпуска: ");
        service.add(new Car(model, powerHp, year));
        output.println("Автомобиль добавлен.");
    }

    private void loadFile() throws IOException, CarLoadException {
        String path = readLine("Путь к файлу: ").strip();
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Путь к файлу не должен быть пустым");
        }
        int count = service.loadFrom(Path.of(path));
        output.println("Загружено автомобилей: " + count);
    }

    private void showCars() {
        List<Car> cars = service.getAll();
        if (cars.isEmpty()) {
            output.println("Список автомобилей пуст.");
            return;
        }
        for (int i = 0; i < cars.size(); i++) {
            printCar(i + 1, cars.get(i));
        }
    }

    private void showCar() throws IOException {
        if (service.size() == 0) {
            output.println("Список автомобилей пуст.");
            return;
        }
        int index = readCarIndex();
        printCar(index + 1, service.get(index));
    }

    private void removeCar() throws IOException {
        if (service.size() == 0) {
            output.println("Список автомобилей пуст.");
            return;
        }
        Car removed = service.remove(readCarIndex());
        output.println("Удалён автомобиль: " + removed.model() + ".");
    }

    private int readCarIndex() throws IOException {
        int number = readInteger("Номер автомобиля: ");
        int count = service.size();
        if (number < 1 || number > count) {
            throw new IllegalArgumentException("Номер автомобиля должен быть от 1 до " + count);
        }
        return number - 1;
    }

    private void printCar(int number, Car car) {
        output.printf("%d. %s — %d л. с., %d г.%n", number, car.model(), car.powerHp(), car.year());
    }

    private void processCars() throws IOException {
        int minimumPowerHp = readInteger("Минимальная мощность, л. с.: ");
        List<String> models = service.getModelNamesByMinimumPower(minimumPowerHp);
        if (models.isEmpty()) {
            output.println("Подходящие автомобили не найдены.");
            return;
        }
        output.println("Модели по году выпуска, от старых к новым:");
        models.forEach(output::println);
    }

    private void sortCars() throws IOException {
        if (service.size() == 0) {
            output.println("Список автомобилей пуст.");
            return;
        }
        output.println("Алгоритм сортировки:");
        output.println("1. Пузырьковая сортировка");
        output.println("2. Сортировка вставками");
        SortAlgorithm algorithm = switch (readInteger("Выберите алгоритм: ")) {
            case 1 -> bubbleSort;
            case 2 -> insertionSort;
            default -> throw new IllegalArgumentException("Выберите алгоритм 1 или 2");
        };
        output.println("Направление сортировки:");
        output.println("1. По возрастанию года");
        output.println("2. По убыванию года");
        SortDirection direction = switch (readInteger("Выберите направление: ")) {
            case 1 -> SortDirection.ASCENDING;
            case 2 -> SortDirection.DESCENDING;
            default -> throw new IllegalArgumentException("Выберите направление 1 или 2");
        };
        List<Car> sorted = service.getSortedByYear(algorithm, direction);
        output.println("Автомобили по году выпуска ("
                + (direction == SortDirection.ASCENDING ? "по возрастанию" : "по убыванию") + "):");
        sorted.forEach(car -> output.printf("%s — %d л. с., %d г.%n",
                car.model(), car.powerHp(), car.year()));
    }

    private String readLine(String prompt) throws IOException {
        output.print(prompt);
        output.flush();
        String line = input.readLine();
        if (line == null) {
            throw new EOFException();
        }
        return line;
    }

    private int readInteger(String prompt) throws IOException {
        while (true) {
            String value = readLine(prompt).strip();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException error) {
                output.println("Ошибка: введите целое число.");
            }
        }
    }
}
