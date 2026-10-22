package infrastructure;

import application.port.SortAlgorithm;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Пузырьковая сортировка с завершением при отсутствии перестановок. */
public final class BubbleSort implements SortAlgorithm {
    @Override
    public <T> void sort(List<T> items, Comparator<? super T> comparator) {
        Objects.requireNonNull(items, "Список не должен быть null");
        Objects.requireNonNull(comparator, "Компаратор не должен быть null");

        for (int end = items.size() - 1; end > 0; end--) {
            boolean swapped = false;
            for (int i = 0; i < end; i++) {
                T left = items.get(i);
                T right = items.get(i + 1);
                if (comparator.compare(left, right) > 0) {
                    items.set(i, right);
                    items.set(i + 1, left);
                    swapped = true;
                }
            }
            if (!swapped) {
                return;
            }
        }
    }
}
