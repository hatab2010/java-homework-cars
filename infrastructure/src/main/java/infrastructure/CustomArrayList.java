package infrastructure;

import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;

/** Динамический массив с индексами от нуля. Допускает null и повторяющиеся элементы. */
public final class CustomArrayList<T> extends AbstractList<T> implements RandomAccess {
    private static final int DEFAULT_CAPACITY = 10;

    private Object[] elements;
    private int size;

    public CustomArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public CustomArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Начальная вместимость не может быть отрицательной");
        }
        elements = new Object[initialCapacity];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        Objects.checkIndex(index, size);
        return (T) elements[index];
    }

    @Override
    public T set(int index, T element) {
        T previous = get(index);
        elements[index] = element;
        return previous;
    }

    @Override
    public boolean add(T element) {
        add(size, element);
        return true;
    }

    @Override
    public void add(int index, T element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Индекс: " + index + ", размер: " + size);
        }
        if (size == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Достигнут предельный размер массива");
        }
        ensureCapacity(size + 1);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = element;
        size++;
        modCount++;
    }

    @Override
    public T remove(int index) {
        T removed = get(index);
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(elements, index + 1, elements, index, moved);
        }
        elements[--size] = null;
        modCount++;
        return removed;
    }

    private void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity <= elements.length) {
            return;
        }
        long grownCapacity = elements.length + Math.max(1L, elements.length / 2L);
        int newCapacity = (int) Math.min(Integer.MAX_VALUE, Math.max(minimumCapacity, grownCapacity));
        Object[] expanded = new Object[newCapacity];
        System.arraycopy(elements, 0, expanded, 0, size);
        elements = expanded;
    }
}
