/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.schema;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import io.openliberty.mcp.internal.schemas.TypeUtility;
import io.openliberty.mcp.internal.schemas.TypeUtility.MapTypes;

public class TypeUtilityTest {

    HashMap<String, Integer> testMap;

    @Test
    public void testGetTypes() {
        Type testMapType = getTypeOfField("testMap");
        MapTypes mapTypes = TypeUtility.getMapTypes(testMapType);
        System.out.println(mapTypes);
        assertEquals(new MapTypes(String.class, Integer.class), mapTypes);
    }

    // -----------------------------

    interface StupidMap<V, K> extends Map<K, V> {}

    StupidMap<Integer, String> stupidMap;

    @Test
    public void testGetTypesReversed() {
        Type testMapType = getTypeOfField("stupidMap");
        MapTypes mapTypes = TypeUtility.getMapTypes(testMapType);
        System.out.println(mapTypes);
        assertEquals(new MapTypes(String.class, Integer.class), mapTypes);
    }

    // -----------------------------

    interface FixedTypeMap extends Map<Long, Type[]> {}

    FixedTypeMap fixedTypeMap;

    @Test
    public void testGetTypesFixed() {
        Type testMapType = getTypeOfField("fixedTypeMap");
        MapTypes mapTypes = TypeUtility.getMapTypes(testMapType);
        System.out.println(mapTypes);
        assertEquals(new MapTypes(Long.class, Type[].class), mapTypes);
    }

    // -----------------------------

    interface UnfixedExtendsFixedTypeMap<K, V> extends FixedTypeMap {}

    UnfixedExtendsFixedTypeMap<String, Character> unfixedExtendsFixedTypeMap;

    @Test
    public void testGetTypesUnfixedExtendsFixed() {
        Type testMapType = getTypeOfField("unfixedExtendsFixedTypeMap");
        MapTypes mapTypes = TypeUtility.getMapTypes(testMapType);
        System.out.println(mapTypes);
        assertEquals(new MapTypes(Long.class, Type[].class), mapTypes);
    }

    // -----------------------------

    @SuppressWarnings("serial")
    static class ConcreteMap<T, K, V> extends HashMap<K, V> {}

    ConcreteMap<Integer, String, Long> concreteMap;

    @Test
    public void testGetTypesConcrete() {
        Type testMapType = getTypeOfField("concreteMap");
        MapTypes mapTypes = TypeUtility.getMapTypes(testMapType);
        System.out.println(mapTypes);
        assertEquals(new MapTypes(String.class, Long.class), mapTypes);
    }

    // -----------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testNonExtendingType() {
        TypeUtility.getMapTypes(HashSet.class);
    }

    // -----------------------------

    ArrayList<String> arrayList;

    @Test
    public void testGetListType() {
        Type testType = getTypeOfField("arrayList");
        Type listType = TypeUtility.getCollectionType(testType);
        assertEquals(String.class, listType);
    }

    // -----------------------------

    interface NumberSet extends Set<Number> {};

    NumberSet numberSet;

    @Test
    public void testGetSetFixed() {
        Type testType = getTypeOfField("numberSet");
        Type listType = TypeUtility.getCollectionType(testType);
        assertEquals(Number.class, listType);
    }

    /**
     * Get the generic type of a field in this class
     *
     * @param name the field name
     * @return the generic type
     */
    private Type getTypeOfField(String name) {
        Field f;
        try {
            f = TypeUtilityTest.class.getDeclaredField(name);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
        return f.getGenericType();
    }

}
