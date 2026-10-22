package infrastructure;

import application.port.SortAlgorithm;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Сортировка вставками со сдвигом элементов отсортированной части списка. */
public final class InsertionSort implements SortAlgorithm {
    @Override
    public <T> void sort(List<T> items, Comparator<? super T> comparator) {
        Objects.requireNonNull(items, "Список не должен быть null");
        Objects.requireNonNull(comparator, "Компаратор не должен быть null");

        for (int i = 1; i < items.size(); i++) {
            T value = items.get(i);
            int j = i - 1;
            while (j >= 0 && comparator.compare(items.get(j), value) > 0) {
                items.set(j + 1, items.get(j));
                j--;
            }
            items.set(j + 1, value);
        }
    }
}
