/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util.trie;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.Assert.*;

public class ConcurrentTrieTest {
    ConcurrentTrie<Boolean> trie;

    @Before
    public void setup() {
        trie = new ConcurrentTrie<>();
    }

    @Test
    public void testEmptyTrie() {
        assertNull(trie.get(""));
        assertNull(trie.get("a"));
        assertNull(trie.getLongestPrefixValue("a"));
        assertNull(trie.getLongestPrefixEntry("a"));
        assertEquals(0, trie.size());
        assertFalse(trie.iterator().hasNext());
        assertFalse(trie.entrySet().iterator().hasNext());
        assertTrue(trie.isEmpty());
    }

    @Test
    public void testEmptyString() {
        assertNull(trie.put("", false));
        assertFalse(trie.get(""));
        assertFalse(trie.getLongestPrefixValue("a"));
        assertFalse(trie.put("", true));
        assertTrue(trie.get(""));
        assertTrue(trie.getLongestPrefixValue(""));
        assertTrue(trie.getLongestPrefixValue("a"));
        assertEquals(entry("", true), trie.getLongestPrefixEntry(""));
        assertEquals(entry("", true), trie.getLongestPrefixEntry("a"));
        assertTrue(trie.remove(""));
        testEmptyTrie();
    }

    @Test
    public void testOneCharacterStrings() {
        assertNull(trie.put("a", true));
        assertNull(trie.put("c", true));
        assertTrue(trie.get("a"));
        assertNull(trie.get("aa"));
        assertTrue(trie.getLongestPrefixValue("a"));
        assertTrue(trie.getLongestPrefixValue("aa"));
        assertNull(trie.get("b"));
        assertNull(trie.get("bb"));
        assertNull(trie.getLongestPrefixValue("b"));
        assertNull(trie.getLongestPrefixValue("bb"));
        assertTrue(trie.get("c"));
        assertNull(trie.get("cc"));
        assertTrue(trie.getLongestPrefixValue("c"));
        assertTrue(trie.getLongestPrefixValue("cc"));
        assertNull(trie.put("", false));
        assertTrue(trie.get("a"));
        assertNull(trie.get("aa"));
        assertTrue(trie.getLongestPrefixValue("a"));
        assertTrue(trie.getLongestPrefixValue("aa"));
        assertNull(trie.get("b"));
        assertNull(trie.get("bb"));
        assertFalse(trie.getLongestPrefixValue("b"));
        assertFalse(trie.getLongestPrefixValue("bb"));
        assertTrue(trie.get("c"));
        assertNull(trie.get("cc"));
        assertTrue(trie.getLongestPrefixValue("c"));
        assertTrue(trie.getLongestPrefixValue("cc"));
        assertFalse(trie.remove(""));
        assertTrue(trie.remove("a"));
        assertTrue(trie.remove("c"));
        testEmptyTrie();
    }

    @Test
    public void testLongerString() {
        assertNull(trie.put("", false));
        assertNull(trie.put("abc", true));
        assertNull(trie.put("cde", true));
        assertNull(trie.put("abcdefghijklmnopqrst", false));
        assertNull(trie.put("bcdefghijklmnopqrstu", false));
        assertNull(trie.get("abcdefghijklm"));
        assertTrue(trie.getLongestPrefixValue("abcdefghijklm"));
        assertNull(trie.get("abcdefghijklmnopqrstu"));
        assertFalse(trie.getLongestPrefixValue("abcdefghijklmnopqrstu"));
        System.out.println(trie);
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyTrieIteratorThrows() {trie.iterator().next();}

    @Test(expected = NoSuchElementException.class)
    public void testEmptyTrieEntrySetIteratorThrows() {trie.entrySet().iterator().next();}

    @Test
    public void testSingleElementTrieIterator() {
        final Iterator<Entry<String, Boolean>> i = trie.iterator();
        trie.put("", true);
        assertTrue(i.hasNext());
        assertEquals(entry("", true), i.next());
        assertFalse(i.hasNext());
    }

    @Test
    public void testSingleElementTrieEntrySetIterator() {
        final Iterator<Entry<String, Boolean>> i = trie.entrySet().iterator();
        trie.put("", true);
        assertTrue(i.hasNext());
        assertEquals(entry("", true), i.next());
        assertFalse(i.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void testSingleElementTrieIteratorThrows() {
        final Iterator<Entry<String, Boolean>> i = trie.iterator();
        trie.put("", true);
        i.next();
        i.next();

    }

    @Test(expected = NoSuchElementException.class)
    public void testSingleElementTrieEntrySetIteratorThrows() {
        final Iterator<Entry<String, Boolean>> i = trie.entrySet().iterator();
        trie.put("", true);
        i.next();
        i.next();
    }

    @Test
    public void testSizeAndIterator() {
        Iterator<Entry<String, Boolean>> i = trie.entrySet().iterator();
        assertFalse(trie.iterator().hasNext());
        testEmptyTrie();
        assertFalse(trie.entrySet().iterator().hasNext());
        testEmptyTrie();
        trie.put("", false);
        assertEquals(1, trie.size());
        trie.put("a", true);
        assertEquals(2, trie.size());
        trie.put("b", true);
        assertEquals(3, trie.size());
        trie.put("aa", false);
        assertEquals(4, trie.size());
        trie.put("bb", false);
        assertEquals(5, trie.size());
        trie.put("C", true);
        assertEquals(6, trie.size());
        trie.put("c", true);
        assertEquals(7, trie.size());
        assertTrue(i.hasNext());
        assertEquals(entry("", false), i.next());
        assertTrue(i.hasNext());
        assertEquals(entry("C", true), i.next());
        assertTrue(i.hasNext());
        assertEquals(entry("a", true), i.next());
        assertTrue(i.hasNext());
        assertEquals(entry("aa", false), i.next());
        assertTrue(i.hasNext());
        assertEquals(entry("b", true), i.next());
        assertTrue(i.hasNext());
        assertEquals(entry("bb", false), i.next());
        assertTrue(i.hasNext());
        assertEquals(entry("c", true), i.next());
        assertFalse(i.hasNext());
        assertEquals(7, trie.size());
        i = trie.iterator();
        assertEquals(entry("", false), i.next());
        i.remove();
        assertEquals(entry("C", true), i.next());
        i.remove();
        assertEquals(entry("a", true), i.next());
        i.remove();
        assertEquals(entry("aa", false), i.next());
        i.remove();
        assertEquals(entry("b", true), i.next());
        i.remove();
        assertEquals(entry("bb", false), i.next());
        i.remove();
        assertEquals(entry("c", true), i.next());
        i.remove();
        assertFalse(i.hasNext());
        testEmptyTrie();
    }

    @Test(expected = IllegalStateException.class)
    public void testEmptyTrieIteratorThrowsOnRemove() {
        trie.iterator().remove();
    }

    @Test(expected = IllegalStateException.class)
    public void testSingleElementTrieIteratorThrowsOnRemove() {
        trie.put("", true);
        trie.iterator().remove();
    }

    @Test
    public void testIteratorRemoveWorksEvenAfterNoSuchElement() {
        trie.put("", true);
        final Iterator<?> i = trie.iterator();
        i.next();
        try {
            i.next();
            fail("call to next() should have thrown NuSuchElementException");
        } catch (NoSuchElementException e){}
        i.remove();
        testEmptyTrie();
    }

    @Test
    public void testEntry() {
        trie.put("a", true);
        Entry<?,?> expected = entry("a", true);
        Entry<?,?> unexpected1 = entry("a", false);
        Entry<?,?> unexpected2 = entry("A", true);
        Entry<?,?> actual = trie.iterator().next();
        assertFalse(actual.equals(null));
        assertFalse(actual.equals(""));
        assertFalse(actual.equals(unexpected1));
        assertFalse(actual.equals(unexpected2));
        assertTrue(actual.equals(expected));
        assertEquals(expected.toString(), actual.toString());
        assertEquals(expected.hashCode(), actual.hashCode());
    }

    @Test
    public void testOverwriteAndRemove() {
        trie.put("a", true);
        trie.put("b", true);
        trie.put("c", true);
        trie.put("d", true);
        trie.put("e", true);
        assertEquals(5, trie.size());
        assertEquals(null, trie.remove(""));
        assertEquals(5, trie.size());
        assertEquals(TRUE, trie.remove("a"));
        assertEquals(4, trie.size());
        trie.iterator().next().setValue(false);
        assertEquals(4, trie.size());
        assertEquals(FALSE, trie.remove("b"));
        assertEquals(3, trie.size());

        assertEquals(FALSE, trie.containsKey("abc"));
        assertEquals(FALSE, trie.containsKey(""));
        assertEquals(FALSE, trie.containsKey(null));
        assertEquals(TRUE, trie.containsKey("c"));
        assertEquals(TRUE, trie.get((Object)"c"));
        assertEquals(TRUE, trie.remove((Object)"c"));
        assertEquals(FALSE, trie.containsKey("c"));
        assertEquals(null, trie.get((Object)"c"));
        assertEquals(null, trie.remove((Object)"c"));
    }

    @Test
    public void testPrefixMap() {
        ConcurrentTrie<Boolean> prefixMap;
//        prefixMap = trie.getLongestPrefixEntry("xyzzy").getPrefixMap();
//        assertSame(trie, prefixMap);
    }
    
    private static<K,V> Entry<K, V> entry(final K k, final V v) {
        return new HashMap<K, V>() {{
            put(k, v);
        }}.entrySet().iterator().next();
    }
}
