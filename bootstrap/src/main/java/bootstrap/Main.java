package bootstrap;

import application.CarService;
import infrastructure.BubbleSort;
import infrastructure.CsvCarLoader;
import infrastructure.CustomArrayList;
import infrastructure.InsertionSort;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import presentation.ConsoleMenu;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        CarService service = new CarService(new CsvCarLoader(), new CustomArrayList<>());
        ConsoleMenu menu = new ConsoleMenu(service, new BubbleSort(), new InsertionSort(),
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new PrintWriter(System.out, true, StandardCharsets.UTF_8));
        menu.run();
    }
}
