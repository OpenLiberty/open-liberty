/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

public class CompositeIterableTest {

    private static final List<Integer> EMPTY = Collections.emptyList();
    private static final List<Integer> SINGLE = Arrays.asList(1);
    private static final List<Integer> DUAL = Arrays.asList(2, 3);
    private static final List<Integer> TRIPLE = Arrays.asList(4, 5, 6);

    @Test
    public void testEmptyIterable() {
        checkCompositeOf();
    }

    @Test
    public void testCompositeOfSingleEmptyIterable() {
        checkCompositeOf(EMPTY);
    }

    @Test
    public void testCompositeOfTwoEmptyIterables() {
        checkCompositeOf(EMPTY, EMPTY);
    }

    @Test
    public void testCompositeOfSingleIterable() {
        checkCompositeOf(SINGLE);
    }

    @Test
    public void testCompositeOfTwoSingleIterables() {
        checkCompositeOf(SINGLE, SINGLE);
    }

    @Test
    public void testCompositesOfTwoSinglesAndOneEmptyIterables() {
        checkCompositeOf(EMPTY, SINGLE, SINGLE);
        checkCompositeOf(SINGLE, EMPTY, SINGLE);
        checkCompositeOf(SINGLE, SINGLE, EMPTY);
    }

    @Test
    public void testMultipleComposites() {
        checkCompositeOf(EMPTY, DUAL, TRIPLE);
        checkCompositeOf(DUAL, EMPTY, TRIPLE);
        checkCompositeOf(DUAL, TRIPLE, EMPTY);
        checkCompositeOf(EMPTY, TRIPLE, DUAL);
        checkCompositeOf(TRIPLE, EMPTY, DUAL);
        checkCompositeOf(TRIPLE, DUAL, EMPTY);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveUnsupported() {
        ArrayList<Integer> removable = new ArrayList<Integer>(Arrays.asList(1));
        new CompositeIterable<Integer>(removable).iterator().remove();
    }

    private static List<Integer> flatten(List<Integer>... lists) {
        List<Integer> result = new ArrayList<Integer>();
        for (List<Integer> list : lists)
            result.addAll(list);
        return result;
    }

    private static void checkCompositeOf(List<Integer>... lists) {
        // check contents as expected
        List<Integer> expected = flatten(lists);
        CompositeIterable<Integer> actual = new CompositeIterable<Integer>(lists);
        Assert.assertEquals(expected, convertToList(actual));
        // check NoSuchElement when at end
        Iterator<Integer> iterator = actual.iterator();
        // this should cycle to the end of the iterator
        for (Integer expectedInt : expected) {
            try {
                Integer actualInt = iterator.next();
            } catch (Exception e) {
                Assert.fail("Expected next element to be " + expectedInt + " but caught " + e);
            }
        }
        // this should throw
        try {
            iterator.next();
            Assert.fail("Attempt to iterate n+1 times should have thrown NoSuchElementException");
        } catch (NoSuchElementException thisIsExpected) {
        }

        // this should also throw
        try {
            iterator.next();
            Assert.fail("Attempt to iterate n+2 times should have thrown NoSuchElementException");
        } catch (NoSuchElementException thisIsExpected) {
        }

        // lastly, check the toString() method works as expected
        Assert.assertEquals("The composite iterable's toString should combine the toString() results of its components", join("(", "+", ")", lists), actual.toString());
    }

    private static <T> String join(String start, String delim, String end, T... objects) {
        if (objects == null || objects.length == 0)
            return start + end;
        String result = start + objects[0];
        for (int i = 1; i < objects.length; result += delim + objects[i++]);
        result += end;
        return result;
    }

    /**
     * @param actual
     * @return
     */
    private static ArrayList<Integer> convertToList(Iterable<Integer> actual) {
        ArrayList<Integer> col = new ArrayList<Integer>();
        for (Integer i : actual)
            col.add(i);
        return col;
    }
}