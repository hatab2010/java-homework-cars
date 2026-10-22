package application;

import java.util.Comparator;
import java.util.List;

public interface SortAlgorithm {
    /**
     * Сортирует переданный список по компаратору, сохраняя порядок равных элементов.
     * Список должен поддерживать замену элементов через set; размер не меняется.
     */
    <T> void sort(List<T> items, Comparator<? super T> comparator);
}
