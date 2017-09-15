/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

public class OneTimeUseArrayListTest {
    private ExecutorService es;
    private final List<String> checkList = new ArrayList<String>();

    private class MyRunnable implements Runnable {
        private final String name;

        MyRunnable(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            synchronized (checkList) {
                checkList.add(name);
            }
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof MyRunnable) && ((MyRunnable) o).name == name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Before
    public void before() {
        es = Executors.newSingleThreadExecutor();
        checkList.clear();
    }

    private OneTimeUseArrayList createNewOneTimeList() {
        OneTimeUseArrayList list = new OneTimeUseArrayList();
        assertTrue("Failed to add an item to the list prior to iterating", list.add(es, new MyRunnable("a")));
        assertTrue("Failed to add an item to the list prior to iterating", list.add(es, new MyRunnable("b")));
        assertTrue("Failed to add an item to the list prior to iterating", list.add(es, new MyRunnable("c")));
        assertEquals("Unexpected list size", 3, list.size());

        return list;
    }

    private void checkAddingToList(OneTimeUseArrayList list) {

        // ensure all futures are executed
        for (int i = 0; i < list.size(); i++) {
            try {
                assertNull("Unexpected non-null value returned from future.get()", list.get(i).get());
            } catch (Exception e) {
                e.printStackTrace();
                fail("Unexpected exception thrown when getting future: " + e);
            }
        }
        assertEquals("Unexpected value from runnable found in list", "a", checkList.get(0));
        assertEquals("Unexpected value from runnable found in list", "b", checkList.get(1));
        assertEquals("Unexpected value from runnable found in list", "c", checkList.get(2));

        assertFalse("List returned true, indicating that we could add to the list after iterating", list.add(es, new MyRunnable("d")));
        assertEquals("Unexpected list size (maybe we really did add an item after iterating?)", 3, list.size());
        assertEquals("Unexpected check list size (indicates that we executed a runnable after iterating over the list)", 3, checkList.size());

    }

    @Test
    public void addAfterIterate() {

        OneTimeUseArrayList list = createNewOneTimeList();

        Iterator<Future<?>> iter = list.iterator();
        for (int i = 0; i < 3; i++) {
            assertTrue("Iterator should have three entries but has " + i, iter.hasNext());
            iter.next();
        }

        checkAddingToList(list);
    }

    @Test
    public void addAfterListIterate() {
        OneTimeUseArrayList list = createNewOneTimeList();

        ListIterator<Future<?>> iter = list.listIterator();

        assertTrue("ListIterator should have three entries but has none", iter.hasNext());
        assertTrue("ListIterator should have three entries but has one", iter.hasNext());
        assertTrue("ListIterator should have three entries but has two", iter.hasNext());

        checkAddingToList(list);
    }

    @Test
    public void addAfterListIterateWithIndex() {
        OneTimeUseArrayList list = createNewOneTimeList();

        ListIterator<Future<?>> iter = list.listIterator(1);

        assertTrue("ListIterator should have two entries but has none", iter.hasNext());
        assertTrue("ListIterator should have two entries but has one", iter.hasNext());

        checkAddingToList(list);
    }

}
