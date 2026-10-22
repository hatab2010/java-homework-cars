package infrastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomArrayListTest {
    @Test
    void newListIsEmpty() {
        List<String> list = new CustomArrayList<>();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.iterator().hasNext());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 10})
    void growsRepeatedlyWithoutLosingElements(int initialCapacity) {
        CustomArrayList<Integer> list = new CustomArrayList<>(initialCapacity);

        for (int i = 0; i < 1000; i++) {
            assertTrue(list.add(i));
        }

        assertEquals(1000, list.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(Integer.valueOf(i), list.get(i));
        }
    }

    @Test
    void defaultCapacityAlsoExpands() {
        CustomArrayList<Integer> list = new CustomArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        assertEquals(100, list.size());
        assertEquals(Integer.valueOf(99), list.get(99));
    }

    @Test
    void insertsAtBeginningMiddleAndEndIncludingGrowth() {
        CustomArrayList<String> list = new CustomArrayList<>(0);

        list.add(0, "B");
        list.add(0, "A");
        list.add(2, "D");
        list.add(2, "C");

        assertEquals(List.of("A", "B", "C", "D"), list);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 4})
    void removesFromBeginningMiddleAndEndPreservingRemainingOrder(int index) {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(List.of("A", "B", "C", "D", "E"));
        List<String> expected = new ArrayList<>(list);

        assertEquals(expected.remove(index), list.remove(index));

        assertEquals(expected, list);
        assertEquals(4, list.size());
    }

    @Test
    void removesLastElementAndCanBeReused() {
        CustomArrayList<String> list = new CustomArrayList<>(1);
        list.add("old");

        assertEquals("old", list.remove(0));
        assertTrue(list.isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));

        list.add("new");
        assertEquals(List.of("new"), list);
    }

    @Test
    void replacesValueWithoutChangingSize() {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(List.of("A", "B"));

        assertEquals("B", list.set(1, "C"));

        assertEquals(List.of("A", "C"), list);
        assertEquals(2, list.size());
    }

    @Test
    void supportsNullAndDuplicates() {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(Arrays.asList(null, "A", "A", null));

        assertNull(list.get(0));
        assertEquals("A", list.set(1, null));
        assertNull(list.remove(0));
        assertEquals(Arrays.asList(null, "A", null), list);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, Integer.MAX_VALUE})
    void rejectsElementAccessOnEmptyList(int index) {
        CustomArrayList<String> list = new CustomArrayList<>();

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(index));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(index, "value"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(index));
        assertTrue(list.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 2, Integer.MAX_VALUE})
    void invalidElementIndexDoesNotChangeContents(int index) {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(List.of("A", "B"));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(index));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(index, "other"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(index));
        assertEquals(List.of("A", "B"), list);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, Integer.MAX_VALUE})
    void invalidInsertionIndexDoesNotChangeContents(int index) {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(List.of("A", "B"));

        assertThrows(IndexOutOfBoundsException.class, () -> list.add(index, "other"));
        assertEquals(List.of("A", "B"), list);
    }

    @Test
    void rejectsNegativeInitialCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CustomArrayList<>(-1));
    }

    @Test
    void iteratorCanRemoveAndContinue() {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(List.of("A", "B", "C"));
        Iterator<String> iterator = list.iterator();

        assertEquals("A", iterator.next());
        iterator.remove();
        assertEquals("B", iterator.next());
        assertEquals("C", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(List.of("B", "C"), list);
    }

    @Test
    void structuralChangesInvalidateExistingIterators() {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.addAll(List.of("A", "B"));
        Iterator<String> beforeAdd = list.iterator();
        list.add("C");
        assertThrows(ConcurrentModificationException.class, beforeAdd::next);

        Iterator<String> beforeRemove = list.iterator();
        list.remove(0);
        assertThrows(ConcurrentModificationException.class, beforeRemove::next);
    }

    @Test
    void replacingElementDoesNotInvalidateIterator() {
        CustomArrayList<String> list = new CustomArrayList<>();
        list.add("A");
        Iterator<String> iterator = list.iterator();

        list.set(0, "B");

        assertEquals("B", iterator.next());
    }

    @Test
    void supportsStreamProcessingAndBulkAddThroughListInterface() {
        List<Integer> list = new CustomArrayList<>(0);
        list.addAll(List.of(4, 1, 3, 2));

        List<String> result = list.stream()
                .filter(value -> value >= 2)
                .sorted()
                .map(String::valueOf)
                .toList();

        assertEquals(List.of("2", "3", "4"), result);
        assertEquals(List.of(4, 1, 3, 2), list);
    }

    @Test
    void clearAndSubListChangesWorkWithTheBackingList() {
        List<Integer> list = new CustomArrayList<>();
        list.addAll(List.of(1, 2, 3, 4));
        list.subList(1, 3).clear();
        assertEquals(List.of(1, 4), list);

        list.clear();
        list.add(5);
        assertEquals(List.of(5), list);
    }

    @Test
    void mixedOperationsMatchStandardListBehavior() {
        List<Integer> expected = new ArrayList<>();
        List<Integer> actual = new CustomArrayList<>(0);
        Random random = new Random(2026);

        for (int step = 0; step < 2000; step++) {
            Integer value = random.nextInt(5) == 0 ? null : random.nextInt(100);
            switch (random.nextInt(4)) {
                case 0 -> assertEquals(expected.add(value), actual.add(value));
                case 1 -> {
                    int index = random.nextInt(expected.size() + 1);
                    expected.add(index, value);
                    actual.add(index, value);
                }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        assertEquals(expected.remove(index), actual.remove(index));
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        assertEquals(expected.set(index, value), actual.set(index, value));
                    }
                }
            }
            assertEquals(expected.size(), actual.size(), "Размер на шаге " + step);
            assertEquals(expected, actual, "Содержимое на шаге " + step);
        }
    }
}
