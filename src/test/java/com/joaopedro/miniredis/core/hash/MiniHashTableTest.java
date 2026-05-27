package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniHashTableTest {

    @Test
    void putAndGetStoreEntry() {
        MiniHashTable table = new MiniHashTable();
        Entry entry = new Entry("bar");

        table.put("foo", entry);

        assertSame(entry, table.get("foo"));
        assertEquals("bar", table.get("foo").getValue());
    }

    @Test
    void putUpdatesExistingKeyWithoutIncreasingSize() {
        MiniHashTable table = new MiniHashTable();

        table.put("name", new Entry("first"));
        table.put("name", new Entry("second"));

        assertEquals(1, table.size());
        assertEquals("second", table.get("name").getValue());
    }

    @Test
    void removeDeletesKeyAndReturnsRemovedEntry() {
        MiniHashTable table = new MiniHashTable();
        table.put("name", new Entry("John"));

        Entry removed = table.remove("name");

        assertEquals("John", removed.getValue());
        assertNull(table.get("name"));
        assertEquals(0, table.size());
        assertNull(table.remove("missing"));
    }

    @Test
    void containsKeyReturnsWhetherKeyExists() {
        MiniHashTable table = new MiniHashTable();

        table.put("name", new Entry("John"));

        assertTrue(table.containsKey("name"));
        assertFalse(table.containsKey("missing"));
    }

    @Test
    void resizeKeepsAllEntriesAfterRehashing() {
        MiniHashTable table = new MiniHashTable();
        int initialCapacity = table.capacity();

        for (int i = 0; i < 20; i++) {
            table.put("key" + i, new Entry("value" + i));
        }

        assertTrue(table.capacity() > initialCapacity);
        assertEquals(20, table.size());

        for (int i = 0; i < 20; i++) {
            assertEquals("value" + i, table.get("key" + i).getValue());
        }
    }

    @Test
    void clearRemovesAllEntriesAndResetsCapacity() {
        MiniHashTable table = new MiniHashTable();
        int initialCapacity = table.capacity();

        for (int i = 0; i < 20; i++) {
            table.put("key" + i, new Entry("value" + i));
        }

        table.clear();

        assertEquals(0, table.size());
        assertEquals(initialCapacity, table.capacity());
        assertNull(table.get("key1"));
    }

    @Test
    void keysReturnsStoredKeys() {
        MiniHashTable table = new MiniHashTable();

        table.put("first", new Entry("1"));
        table.put("second", new Entry("2"));

        String[] keys = table.keys();

        assertEquals(2, keys.length);
        assertContains(keys, "first");
        assertContains(keys, "second");
    }

    private void assertContains(String[] values, String expected) {
        boolean found = false;

        for (int i = 0; i < values.length; i++) {
            if (expected.equals(values[i])) {
                found = true;
            }
        }

        assertTrue(found, "Expected array to contain " + expected);
    }
}
