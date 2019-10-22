package com.ibm.aries.buildtasks.semantic.versioning.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class VersionedEntityMapTest {

    private Set<String> debugSet(String... e) {
        Set<String> s = new HashSet<String>();
        for (String z : e) {
            s.add(z);
        }
        return s;
    }

    @Test
    public void testMultiNullLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");

        map1.put(e1, debugSet("a", "b"));

        VersionedEntity key = new VersionedEntity("Key1", null);
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a", "b");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b", expected, results);
    }

    @Test
    public void testMultiLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");

        map1.put(e1, debugSet("a", "b"));

        VersionedEntity key = new VersionedEntity("Key1", "1.0.0");
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a", "b");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b", expected, results);
    }

    @Test
    public void testMultiMixedLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", null);

        map1.put(e1, debugSet("a", "b"));
        map1.put(e2, debugSet("c", "d"));

        VersionedEntity key = new VersionedEntity("Key1", "1.0.0");
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a", "b", "c", "d");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b c d", expected, results);

        VersionedEntity key2 = new VersionedEntity("Key1", null);
        results = map1.get(key2);
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b c d", expected, results);

    }

    @Test
    public void testSingleNullLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");

        map1.put(e1, debugSet("a"));

        VersionedEntity key = new VersionedEntity("Key1", null);
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a", expected, results);
    }

    @Test
    public void testSingleLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");

        map1.put(e1, debugSet("a"));

        VersionedEntity key = new VersionedEntity("Key1", "1.0.0");
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a", expected, results);
    }

    @Test
    public void testSingleLookupOfNull() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", null);

        map1.put(e1, debugSet("a"));

        VersionedEntity key = new VersionedEntity("Key1", "1.0.0");
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a", expected, results);

        VersionedEntity key2 = new VersionedEntity("Key1", null);
        Set<String> results2 = map1.get(key2);

        assertNotNull("null lookup should have given back a non null result", results2);
        assertEquals("null lookup should have given back expected results of a", expected, results2);

    }

    @Test
    public void testMultiMatchNullLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));

        VersionedEntity key = new VersionedEntity("Key1", null);
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a", "b", "c");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b c", expected, results);
    }

    @Test
    public void testMultiMatchLookup() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));

        VersionedEntity key = new VersionedEntity("Key1", "1.0.0");
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a", expected, results);
    }

    @Test
    public void testMultiMatchLookupWithNulls() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");
        VersionedEntity e3 = new VersionedEntity("Key1", null);

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));
        map1.put(e3, debugSet("d"));

        VersionedEntity key = new VersionedEntity("Key1", null);
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a", "b", "c", "d");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b c d", expected, results);
    }

    @Test
    public void testNullLookupWithExtraDataPresent() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");
        VersionedEntity e3 = new VersionedEntity("Key2", "1.0.0");

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));
        map1.put(e3, debugSet("d"));

        VersionedEntity key = new VersionedEntity("Key1", null);
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("a", "b", "c");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of a b c", expected, results);
    }

    @Test
    public void testLookupWithExtraDataPresent() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");
        VersionedEntity e3 = new VersionedEntity("Key2", "1.0.0");

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));
        map1.put(e3, debugSet("d")); //

        VersionedEntity key = new VersionedEntity("Key1", "2.0.0");
        Set<String> results = map1.get(key);

        Set<String> expected = debugSet("b", "c");
        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back expected results of b c", expected, results);
    }

    @Test
    public void testNullLookupWithNoMatches() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));

        VersionedEntity key = new VersionedEntity("Key2", null);
        Set<String> results = map1.get(key);

        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back an empty set", 0, results.size());
    }

    @Test
    public void testLookupWithNoMatches() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        map1.put(e1, debugSet("a"));
        map1.put(e2, debugSet("b", "c"));

        VersionedEntity key = new VersionedEntity("Key2", "1.0.0");
        Set<String> results = map1.get(key);

        assertNotNull("null lookup should have given back a non null result", results);
        assertEquals("null lookup should have given back an empty set", 0, results.size());
    }

    @Test
    public void testMerge() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        VersionedEntity mp = new VersionedEntity("a", "1.0.0");
        Set<String> empty = debugSet();
        map1.put(mp, empty);
        assertTrue("Add of empty set did not result in map containing key", map1.get(mp).isEmpty());

        Set<String> content = debugSet("a");
        map1.merge(mp, "a");
        assertTrue("Merge into empty set did not result in set holding new object", map1.get(mp).size() == 1);
        assertEquals("Merge into empty set did not result in set holding new object", content, map1.get(mp));

        Set<String> content2 = debugSet("b");
        VersionedEntity mp2 = new VersionedEntity("b", "1.0.0");
        map1.merge(mp2, "b");
        assertTrue("Merge into empty set did not result in set holding new object", map1.get(mp2).size() == 1);
        assertEquals("Merge into empty set did not result in set holding new object", content2, map1.get(mp2));

        Set<String> combined = debugSet("b", "c");
        map1.merge(mp2, "c");
        assertTrue("Merge into empty set did not result in set holding new object", map1.get(mp2).size() == 2);
        assertEquals("Merge into empty set did not result in set holding new object", combined, map1.get(mp2));

        Set<String> withNull = debugSet("b", "c", "d");
        VersionedEntity mpNull = new VersionedEntity("b", null);
        map1.merge(mpNull, "d");
        assertTrue("Merge into empty set did not result in set holding new object", map1.get(mp2).size() == 3);
        assertEquals("Merge into empty set did not result in set holding new object", withNull, map1.get(mpNull));
    }

    public void testSize() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        assertEquals("New map should have size 0", 0, map1.size());

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        map1.put(e1, debugSet("a"));

        assertEquals("Map should have size 1", 1, map1.size());

        map1.put(e2, debugSet("b", "c"));

        assertEquals("Map should have size 2", 2, map1.size());

        VersionedEntity key = new VersionedEntity("Key2", null);
        map1.put(key, debugSet("a-null"));

        assertEquals("Map should have size 3", 3, map1.size());

        //merge to an existing key should not increase the map size.  
        map1.merge(e1, "a2");

        assertEquals("Map should have size 3", 3, map1.size());

        //merging to a null key should not increase the map size.        
        map1.merge(key, "a-null-extra");

        assertEquals("Map should have size 3", 3, map1.size());

        VersionedEntity fish = new VersionedEntity("Fish", "1.0.0");
        map1.merge(fish, "newvalue");

        //merging an unknown key should increase the size of the map.
        assertEquals("Map should have size 4", 4, map1.size());
    }

    public void testKeySet() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        assertEquals("New map keyset should have size 0", 0, map1.keySet().size());

        HashSet<VersionedEntity> expectedKeys = new HashSet<VersionedEntity>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        expectedKeys.add(e1);
        map1.put(e1, debugSet("a"));

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        expectedKeys.add(e2);
        map1.put(e2, debugSet("b", "c"));

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        VersionedEntity key = new VersionedEntity("Key2", null);
        expectedKeys.add(key);
        map1.put(key, debugSet("a-null"));

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        //merge to an existing key should not increase the map size.  
        map1.merge(e1, "a2");

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        //merging to a null key should not increase the map size.        
        map1.merge(key, "a-null-extra");

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        VersionedEntity fish = new VersionedEntity("Fish", "1.0.0");
        expectedKeys.add(fish);
        map1.merge(fish, "newvalue");

        //merging an unknown key should increase the size of the map.
        assertEquals("keyset not as expected", expectedKeys, map1.keySet());
    }

    public void testEntrySet() {
        VersionedEntityMap<VersionedEntity, String> map1 = new VersionedEntityMap<VersionedEntity, String>();

        assertEquals("New map keyset should have size 0", 0, map1.keySet().size());

        HashSet<VersionedEntity> expectedKeys = new HashSet<VersionedEntity>();

        VersionedEntity e1 = new VersionedEntity("Key1", "1.0.0");
        VersionedEntity e2 = new VersionedEntity("Key1", "2.0.0");

        expectedKeys.add(e1);
        map1.put(e1, debugSet("a"));

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        expectedKeys.add(e2);
        map1.put(e2, debugSet("b", "c"));

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        VersionedEntity key = new VersionedEntity("Key2", null);
        expectedKeys.add(key);
        map1.put(key, debugSet("a-null"));

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        //merge to an existing key should not increase the map size.  
        map1.merge(e1, "a2");

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        //merging to a null key should not increase the map size.        
        map1.merge(key, "a-null-extra");

        assertEquals("keyset not as expected", expectedKeys, map1.keySet());

        VersionedEntity fish = new VersionedEntity("Fish", "1.0.0");
        expectedKeys.add(fish);
        map1.merge(fish, "newvalue");

        //merging an unknown key should increase the size of the map.
        assertEquals("keyset not as expected", expectedKeys, map1.keySet());
    }
}
