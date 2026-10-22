package domain;

import java.time.Year;

/** Запись каталога автомобилей. Мощность указывается в лошадиных силах. */
public record Car(String model, int powerHp, int year) {
    public Car {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Модель автомобиля не должна быть пустой");
        }
        model = model.strip();
        if (powerHp <= 0) {
            throw new IllegalArgumentException("Мощность должна быть положительной");
        }
        if (year < 1 || year > Year.now().getValue()) {
            throw new IllegalArgumentException("Год выпуска должен быть от 1 до текущего года");
        }
    }
}
