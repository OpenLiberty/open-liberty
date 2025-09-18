/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class FilterTest {
    private Filter f;

    @Before
    public void setUp() {
        f = new Filter();
    }

    @After
    public void tearDown() {
        f = null;
    }

    @Test
    public void testApplyFilterNullFilter() throws Exception
    {
        String filter = null;
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        @SuppressWarnings("unchecked")
        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals("value1", newmap.get("key1"));
        assertEquals("value2", newmap.get("key2"));
        assertEquals(2, newmap.size());

    }

    @Test
    public void testApplyFilterEmptyFilter() throws Exception
    {
        String filter = "";
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        @SuppressWarnings("unchecked")
        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals("value1", newmap.get("key1"));
        assertEquals("value2", newmap.get("key2"));
        assertEquals(2, newmap.size());
    }

    @Test
    public void testApplyFilterSimpleFilter() throws Exception
    {
        String filter = "key1";
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        @SuppressWarnings("unchecked")
        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals("value1", newmap.get("key1"));
        assertEquals(1, newmap.size());

    }

    @Test
    public void testApplyFilterSimpleFilterNoExist() throws Exception
    {
        String filter = "key";
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        @SuppressWarnings("unchecked")
        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals(0, newmap.size());

    }

    @Test
    public void testApplyFilterSimpleFilterNoExist1() throws Exception
    {
        String filter = "key11";
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        @SuppressWarnings("unchecked")
        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals(0, newmap.size());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testApplyFilterFilter1() throws Exception
    {
        String filter = "tools.key1";
        Map<Object, Object> map = new HashMap<Object, Object>();

        Map<Object, Object> map1 = new HashMap<Object, Object>();
        map1.put("key1", "Tool1value1");
        map1.put("key2", "Tool1value2");

        Map<Object, Object> map2 = new HashMap<Object, Object>();
        map2.put("key1", "Tool2value1");
        map2.put("key2", "Tool2value2");

        List<Object> list = new ArrayList<Object>();
        list.add(map1);
        list.add(map2);
        map.put("tools", list);

        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals(1, newmap.size());
        List<Object> list1 = (List<Object>) newmap.get("tools");
        assertEquals(2, list1.size());
        Map<Object, Object> newmap1 = (Map<Object, Object>) list1.get(0);
        assertEquals(1, newmap1.size());
        assertEquals("Tool1value1", newmap1.get("key1"));

        Map<Object, Object> newmap2 = (Map<Object, Object>) list1.get(1);
        assertEquals(1, newmap2.size());
        assertEquals("Tool2value1", newmap2.get("key1"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testApplyFilterFilter2() throws Exception
    {
        String filter = "tools.key1, tools.key3.key3-1";
        Map<Object, Object> map = new HashMap<Object, Object>();

        Map<Object, Object> map1 = new HashMap<Object, Object>();
        map1.put("key1", "Tool1value1");
        map1.put("key2", "Tool1value2");

        Map<Object, Object> map11 = new HashMap<Object, Object>();
        map11.put("key3-1", "Tool1key3-1value1");
        map11.put("key3-2", "Tool1key3-2value2");
        map1.put("key3", map11);

        Map<Object, Object> map2 = new HashMap<Object, Object>();
        map2.put("key1", "Tool2value1");
        map2.put("key2", "Tool2value2");

        Map<Object, Object> map21 = new HashMap<Object, Object>();
        map21.put("key3-1", "Tool2key3-1value1");
        map21.put("key3-2", "Tool2key3-2value2");
        map2.put("key3", map21);

        List<Object> list = new ArrayList<Object>();
        list.add(map1);
        list.add(map2);
        map.put("tools", list);

        Map<Object, Object> newmap = (Map<Object, Object>) f.applyFieldFilter(filter, map);
        assertEquals(1, newmap.size());
        List<Object> list1 = (List<Object>) newmap.get("tools");
        assertEquals(2, list1.size());
        Map<Object, Object> newmap1 = (Map<Object, Object>) list1.get(0);
        assertEquals(2, newmap1.size());
        assertEquals("Tool1value1", newmap1.get("key1"));

        Map<Object, Object> newmap13 = (Map<Object, Object>) newmap1.get("key3");
        assertEquals(1, newmap13.size());
        assertEquals("Tool1key3-1value1", newmap13.get("key3-1"));

        Map<Object, Object> newmap2 = (Map<Object, Object>) list1.get(1);
        assertEquals(2, newmap2.size());
        assertEquals("Tool2value1", newmap2.get("key1"));

        Map<Object, Object> newmap23 = (Map<Object, Object>) newmap2.get("key3");
        assertEquals(1, newmap23.size());
        assertEquals("Tool2key3-1value1", newmap23.get("key3-1"));

    }

    @SuppressWarnings("unchecked")
    @Test
    public void applyFilter_preserveCase() throws Exception {
        String filter = "map.Key1,list.keyONE,list.camelCase";

        Map<Object, Object> obj = new HashMap<Object, Object>();

        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("Key1", "Key1Value");
        map.put("key2", "key2Value");

        Map<Object, Object> listElement = new HashMap<Object, Object>();
        listElement.put("keyONE", "keyONEValue");
        listElement.put("camelCase", "camelCaseValue");

        List<Object> list = new ArrayList<Object>();
        list.add(listElement);

        obj.put("map", map);
        obj.put("list", list);

        Map<Object, Object> filteredMap = (Map<Object, Object>) f.applyFieldFilter(filter, obj);
        System.out.println(filteredMap);

        assertEquals("FAIL: The filetered map was not of the correct size",
                     2, filteredMap.size());

        Map<Object, Object> filteredMapMap = (Map<Object, Object>) filteredMap.get("map");
        assertEquals("FAIL: The filtered map map was not of the correct size",
                     1, filteredMapMap.size());
        assertEquals("FAIL: The filtered map map did not have the correct key/value for 'Key1'",
                     "Key1Value", filteredMapMap.get("Key1"));

        List<Object> filteredList = (List<Object>) filteredMap.get("list");
        assertEquals("FAIL: The filtered list was not of the correct size",
                     1, filteredList.size());

        Map<Object, Object> filteredListMap = (Map<Object, Object>) filteredList.get(0);
        assertEquals("FAIL: The filtered list map should have two elements",
                     2, filteredListMap.size());
        assertEquals("FAIL: The filtered list map did not have the correct key/value for 'keyONE'",
                     "keyONEValue", filteredListMap.get("keyONE"));
        assertEquals("FAIL: The filtered list map did not have the correct key/value for 'camelCase'",
                     "camelCaseValue", filteredListMap.get("camelCase"));
    }

    static class SimpleMBeanSetter {
        @SuppressWarnings("unused")
        private String noGetter;
        private String hasGetter;

        public void setNoGetter(String noGetter) {
            this.noGetter = noGetter;
        }

        public String getHasGetter() {
            return hasGetter;
        }

        public void setHasGetter(String hasGetter) {
            this.hasGetter = hasGetter;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void applyFilter_ignoresSetterWithoutGetter() throws Exception {

        SimpleMBeanSetter mbean = new SimpleMBeanSetter();
        mbean.setNoGetter("MUST NOT BE SEEN");
        mbean.setHasGetter("FindMe");

        String filter = "hasGetter";

        System.out.println(mbean);
        System.out.println(f.applyFieldFilter(filter, mbean));

        Map<Object, Object> filteredMap = (Map<Object, Object>) f.applyFieldFilter(filter, mbean);
        assertEquals("FAIL: Filtered map should only have 1 element",
                     1, filteredMap.size());
        assertEquals("FAIL: Filtered map did not contain the 'hasGetter' element",
                     "FindMe", filteredMap.get("hasGetter"));
    }

}
