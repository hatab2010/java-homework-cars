package domain;

import java.time.Year;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CarTest {
    @Test
    void trimsModelWhitespace() {
        Car car = new Car("  Lada Vesta  ", 106, 2020);

        assertEquals("Lada Vesta", car.model());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void rejectsMissingModel(String model) {
        assertThrows(IllegalArgumentException.class, () -> new Car(model, 106, 2020));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositivePower(int powerHp) {
        assertThrows(IllegalArgumentException.class, () -> new Car("Lada Vesta", powerHp, 2020));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    void rejectsInvalidYear(int year) {
        assertThrows(IllegalArgumentException.class, () -> new Car("Lada Vesta", 106, year));
    }

    @Test
    void rejectsFutureYear() {
        int nextYear = Year.now().getValue() + 1;

        assertThrows(IllegalArgumentException.class, () -> new Car("Lada Vesta", 106, nextYear));
    }

    @Test
    void acceptsCurrentYearAndMinimumPower() {
        int currentYear = Year.now().getValue();

        Car car = new Car("Test", 1, currentYear);

        assertEquals(currentYear, car.year());
    }
}
