/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELProcessor;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.el30.fat.beans.EL30MapCollectionObjectBean;
import com.ibm.ws.el30.fat.beans.EL30PrinterBean;

import componenttest.app.FATServlet;

/**
 * This servlet tests all the lists, sets and maps EL 3.0 operations on collection objects.
 */
@WebServlet("/EL30CollectionObjectOperationsServlet")
public class EL30CollectionObjectOperationsServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    private final List<Integer> list;
    private final Map<Integer, String> map;
    private final Map<Integer, EL30MapCollectionObjectBean> nestedMap;
    private final Set<String> set;
    private ELProcessor elp;
    private EL30PrinterBean printerBean;

    /**
     * Constructor
     */
    public EL30CollectionObjectOperationsServlet() {
        super();

        // List to be used as a collection object
        // Use ArrayList to preserve insertion order to make tests consistent across JDKs.
        list = new ArrayList<Integer>() {
            {
                add(1);
                add(4);
                add(3);
                add(2);
                add(5);
                add(3);
                add(1);
            }
        };

        // Set to be used as a collection object
        // Use LinkedHashSet to preserve insertion order to make tests consistent across JDKs.
        set = new LinkedHashSet<String>() {
            {
                add("1");
                add("4");
                add("3");
                add("5");
                add("2");
            }
        };

        // Map to be used as a collection object
        // Use LinkedHashMap to preserve insertion order to make  tests consistent across JDKs.
        map = new LinkedHashMap<Integer, String>() {
            {
                put(1, "1");
                put(2, "4");
                put(3, "3");
                put(4, "2");
                put(5, "5");
                put(6, "3");
                put(7, "1");
            }
        };

        final EL30MapCollectionObjectBean[] cObjectOp = new EL30MapCollectionObjectBean[] { new EL30MapCollectionObjectBean(),
                                                                                            new EL30MapCollectionObjectBean(),
                                                                                            new EL30MapCollectionObjectBean() };
        cObjectOp[0].setMap(map);
        cObjectOp[1].setMap(map);
        cObjectOp[2].setMap(map);

        // Nested map to be used as a collection object with the flatMap operation
        nestedMap = new HashMap<Integer, EL30MapCollectionObjectBean>() {
            {
                put(1, cObjectOp[0]);
                put(2, cObjectOp[1]);
                put(3, cObjectOp[2]);
            }
        };

        elp = new ELProcessor();

        // Obtain the stream by getting a collection view of the map
        Collection<String> collectionMap = map.values();
        Collection<EL30MapCollectionObjectBean> collectionNestedMap = nestedMap.values();

        // Define the list, map, set and printer objects within the ELProcessor object
        printerBean = new EL30PrinterBean();
        elp.defineBean("list", list);
        elp.defineBean("set", set);
        elp.defineBean("map", collectionMap);
        elp.defineBean("nestedMap", collectionNestedMap);
        elp.defineBean("printer", printerBean);
    }

    @Test
    public void testListCollectionFilterOperation() throws Exception {
        testCollectionFilterOperation("list.stream().filter(e -> e >= 3).toList()", "[4, 3, 5, 3]");
    }

    @Test
    public void testMapCollectionFilterOperation() throws Exception {
        testCollectionFilterOperation("map.stream().filter(e -> e >= 3).toList()", "[4, 3, 5, 3]");
    }

    @Test
    public void testSetCollectionFilterOperation() throws Exception {
        testCollectionFilterOperation("set.stream().filter(e -> e >= 3).toList()", "[4, 3, 5]");
    }

    private void testCollectionFilterOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get all the elements greater or  equal than 3
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Filter: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionMapOperation() throws Exception {
        testCollectionMapOperation("list.stream().map(e -> e + 2).toList()", "[3, 6, 5, 4, 7, 5, 3]");
    }

    @Test
    public void testMapCollectionMapOperation() throws Exception {
        testCollectionMapOperation("map.stream().map(e -> e + 2).toList()", "[3, 6, 5, 4, 7, 5, 3]");
    }

    @Test
    public void testSetCollectionMapOperation() throws Exception {
        testCollectionMapOperation("set.stream().map(e -> e + 2).toList()", "[3, 6, 5, 7, 4]");
    }

    private void testCollectionMapOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Add 2 to each element of the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Map: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionFlapMapOperation() throws Exception {
        testCollectionFlatMapOperation("[[1, 4], [3], [2], [5, 3, 1]].stream().flatMap(e -> e.stream()).toList()", "[1, 4, 3, 2, 5, 3, 1]");
    }

    @Test
    public void testMapCollectionFlapMapOperation() throws Exception {
        testCollectionFlatMapOperation("nestedMap.stream().flatMap(e -> e.map.stream()).toList()",
                                       "[1, 4, 3, 2, 5, 3, 1, 1, 4, 3, 2, 5, 3, 1, 1, 4, 3, 2, 5, 3, 1]");
    }

    @Test
    public void testSetCollectionFlatMapOperation() throws Exception {
        testCollectionFlatMapOperation("{{1, 4}, {3}, {2}, {5, 3, 1}}.stream().flatMap(e -> e.stream()).toList()", "[2, 3, 1, 4, 1, 3, 5]");
    }

    private void testCollectionFlatMapOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Flat the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("FlatMap: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionDistinctOperation() throws Exception {
        testCollectionDistinctOperation("list.stream().distinct().toList()", "[1, 4, 3, 2, 5]");
    }

    @Test
    public void testMapCollectionDistinctOpertation() throws Exception {
        testCollectionDistinctOperation("map.stream().distinct().toList()", "[1, 4, 3, 2, 5]");
    }

    @Test
    public void testSetCollectionDistinctOperation() throws Exception {
        testCollectionDistinctOperation("set.stream().distinct().toList()", "[1, 4, 3, 5, 2]");
    }

    private void testCollectionDistinctOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get distinct values from the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Distinct: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionSortedOperation() throws Exception {
        testCollectionSortedOperation("list.stream().sorted((i, j) -> j-i).toList()", "[5, 4, 3, 3, 2, 1, 1]");
    }

    @Test
    public void testMapCollectionSortedOperation() throws Exception {
        testCollectionSortedOperation("map.stream().sorted((i, j) -> j-i).toList()", "[5, 4, 3, 3, 2, 1, 1]");
    }

    @Test
    public void testSetCollectionSortedOperation() throws Exception {
        testCollectionSortedOperation("set.stream().sorted((i, j) -> j-i).toList()", "[5, 4, 3, 2, 1]");
    }

    private void testCollectionSortedOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Sort the list in decreasing order
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Sorted in decreasing order: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionForEachOperation() throws Exception {
        testCollectionForEachOperation("list.stream().forEach(e -> printer.appendToValue(e))", "1432531");
    }

    @Test
    public void testMapCollectionForEachOperation() throws Exception {
        testCollectionForEachOperation("map.stream().forEach(e -> printer.appendToValue(e))", "1432531");
    }

    @Test
    public void testSetCollectionForEachOperation() throws Exception {
        testCollectionForEachOperation("set.stream().forEach(e -> printer.appendToValue(e))", "14352");
    }

    private void testCollectionForEachOperation(String expression, String expectedResult) throws Exception {
        String result;

        // To print a list of integers, clear the bean to ensure no left over information from previous test.
        printerBean.clearValue();
        evaluateOperations(elp, expression);
        result = printerBean.getValue();

        System.out.println("ForEach: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionPeekOperation() throws Exception {
        testCollectionPeekOperation("list.stream().peek(e -> printer.appendToValue(e)).filter(e -> e%2 == 0).peek(e -> printer.appendToValue(e)).toList()", "[4, 2]");
    }

    @Test
    public void testMapCollectionPeekOperation() throws Exception {
        testCollectionPeekOperation("map.stream().peek(e -> printer.appendToValue(e)).filter(e -> e%2 == 0).peek(e -> printer.appendToValue(e)).toList()", "[4, 2]");
    }

    @Test
    public void testSetCollectionPeekOperation() throws Exception {
        testCollectionPeekOperation("set.stream().peek(e -> printer.appendToValue(e)).filter(e -> e%2 == 0).peek(e -> printer.appendToValue(e)).toList()", "[4, 2]");
    }

    private void testCollectionPeekOperation(String expression, String expectedResult) throws Exception {
        String result;

        // To print the a list of integer before and after a filter. It filters even values.
        printerBean.clearValue();
        Object obj = evaluateOperations(elp, expression);
        System.out.println("Debug Peek: " + printerBean.getValue());
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Peek: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionIterationOperation() throws Exception {
        testCollectionIterationOperation("list.stream().iterator()", "1 4 3 2 5 3 1");
    }

    @Test
    public void testMapCollectionIterationOperation() throws Exception {
        testCollectionIterationOperation("map.stream().iterator()", "1 4 3 2 5 3 1");
    }

    @Test
    public void testSetCollectionIterationOpration() throws Exception {
        testCollectionIterationOperation("set.stream().iterator()", "1 4 3 5 2");
    }

    private void testCollectionIterationOperation(String expression, String expectedResult) throws Exception {
        String result = "";

        // This method returns an iterator for the source stream, suitable for use in Java codes
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);

        Iterator<Integer> iter = (Iterator<Integer>) obj;

        while (iter.hasNext()) {
            result += iter.next() + " ";
        }

        System.out.println("Iteration: " + result.trim());
        // Remove the trailing space added in the above iteration for a good comparison to expectedResult.
        assertEquals("Expected: " + expectedResult + " but was: " + result.trim(), expectedResult, result.trim());
    }

    @Test
    public void testListCollectionLimitOperation() throws Exception {
        testCollectionLimitOperation("list.stream().limit(3).toList()", "[1, 4, 3]");
    }

    @Test
    public void testMapCollectionLimitOperation() throws Exception {
        testCollectionLimitOperation("map.stream().limit(3).toList()", "[1, 4, 3]");
    }

    @Test
    public void testSetCollectionLimitOperation() throws Exception {
        testCollectionLimitOperation("set.stream().limit(3).toList()", "[1, 4, 3]");
    }

    private void testCollectionLimitOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get the first 3 elements in the collection
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Limit: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionSubStreamOperation() throws Exception {
        testCollectionSubStreamOperation("list.stream().substream(2, 4).toList()", "[3, 2]");
    }

    @Test
    public void testMapCollectionSubStreamOperation() throws Exception {
        testCollectionSubStreamOperation("map.stream().substream(2, 4).toList()", "[3, 2]");
    }

    @Test
    public void testSetCollectionSubStreamOperation() throws Exception {
        testCollectionSubStreamOperation("set.stream().substream(2, 4).toList()", "[3, 5]");
    }

    private void testCollectionSubStreamOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get a sub list out of the entire list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("SubStream: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionArrayOperation() throws Exception {
        testCollectionArrayOperation("list.stream().toArray()", "[1, 4, 3, 2, 5, 3, 1]");
    }

    @Test
    public void testMapCollectionArrayOperation() throws Exception {
        testCollectionArrayOperation("map.stream().toArray()", "[1, 4, 3, 2, 5, 3, 1]");
    }

    @Test
    public void testSetCollectionArrayOperation() throws Exception {
        testCollectionArrayOperation("set.stream().toArray()", "[1, 4, 3, 5, 2]");
    }

    private void testCollectionArrayOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get an array containing the elements of the source stream.
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = Arrays.toString((Object[]) obj);

        System.out.println("ToArray: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionToListOperation() throws Exception {
        testCollectionToListOperation("list.stream().toList()", "[1, 4, 3, 2, 5, 3, 1]");
    }

    @Test
    public void testMapCollectionToListOperation() throws Exception {
        testCollectionToListOperation("map.stream().toList()", "[1, 4, 3, 2, 5, 3, 1]");
    }

    @Test
    public void testSetCollectionToListOperation() throws Exception {
        testCollectionToListOperation("set.stream().toList()", "[1, 4, 3, 5, 2]");
    }

    private void testCollectionToListOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get a list containing the elements of the source stream
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("ToList: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    @Test
    public void testListCollectionReduceOperation() throws Exception {
        testCollectionReduceOperation("list.stream().reduce((p, q) -> p > q ? p:q).get()", "5");
    }

    @Test
    public void testMapCollectionReduceOperation() throws Exception {
        testCollectionReduceOperation("map.stream().reduce((p, q) -> p > q ? p:q).get()", "5");
    }

    @Test
    public void testSetCollectionReduceOperation() throws Exception {
        testCollectionReduceOperation("set.stream().reduce((p, q) -> p > q ? p:q).get()", "5");
    }

    private void testCollectionReduceOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Find the largest number in the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Reduce: " + result);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testListCollectionMaxOperation() throws Exception {
        testCollectionMaxOperation("list.stream().max().get()", "5");
    }

    @Test
    public void testMapCollectionMaxOperation() throws Exception {
        testCollectionMaxOperation("map.stream().max().get()", "5");
    }

    @Test
    public void testSetCollectionMaxOperation() throws Exception {
        testCollectionMaxOperation("set.stream().max().get()", "5");
    }

    private void testCollectionMaxOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Find the maximum number in the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Max: " + result);
        assertEquals(expectedResult, result);

    }

    @Test
    public void testListCollectionMinOperation() throws Exception {
        testCollectionMinOperation("list.stream().min().get()", "1");
    }

    @Test
    public void testMapCollectionMinOperation() throws Exception {
        testCollectionMinOperation("map.stream().min().get()", "1");
    }

    @Test
    public void testSetCollectionMinOperation() throws Exception {
        testCollectionMinOperation("set.stream().min().get()", "1");
    }

    private void testCollectionMinOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Find the minimum number in the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Min: " + result);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testListCollectionAverageOperation() throws Exception {
        testCollectionAverageOperation("list.stream().average().get()", "2.7142857142857144");
    }

    @Test
    public void testMapCollectionAverageOperation() throws Exception {
        testCollectionAverageOperation("map.stream().average().get()", "2.7142857142857144");
    }

    @Test
    public void testSetCollectionAverageOperation() throws Exception {
        testCollectionAverageOperation("set.stream().average().get()", "3.0");
    }

    private void testCollectionAverageOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Find the average value of the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Average: " + result);
        assertEquals(expectedResult, result);
    }

    @Test
    public void testListCollectionSumOperation() throws Exception {
        testCollectionSumOperation("list.stream().sum()", "19");
    }

    @Test
    public void testMapCollectionSumOperation() throws Exception {
        testCollectionSumOperation("map.stream().sum()", "19");
    }

    @Test
    public void testSetCollectionSumOperation() throws Exception {
        testCollectionSumOperation("set.stream().sum()", "15");
    }

    private void testCollectionSumOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Find the sum of the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Sum : " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionCountOperation() throws Exception {
        testCollectionCountOperation("list.stream().count()", "7");
    }

    @Test
    public void testMapCollectionCountOperation() throws Exception {
        testCollectionCountOperation("map.stream().count()", "7");
    }

    @Test
    public void testSetCollectionCountOperation() throws Exception {
        testCollectionCountOperation("set.stream().count()", "5");
    }

    private void testCollectionCountOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Find the amount of element in the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("Count: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionAnyMatchOperation() throws Exception {
        testCollectionAnyMatchOperation("list.stream().anyMatch(e -> e < 2).orElse(false)", "true");
    }

    @Test
    public void testMapCollectionAnyMatchOperation() throws Exception {
        testCollectionAnyMatchOperation("map.stream().anyMatch(e -> e < 2).orElse(false)", "true");
    }

    @Test
    public void testSetCollectionAnyMatchOperation() throws Exception {
        testCollectionAnyMatchOperation("set.stream().anyMatch(e -> e < 2).orElse(false)", "true");
    }

    private void testCollectionAnyMatchOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Check if the list contains any element smaller than 2
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("AnyMatch: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionAllMatchOperation() throws Exception {
        testCollectionAllMatchOperation("list.stream().allMatch(e -> e < 4).orElse(false)", "false");
    }

    @Test
    public void testMapCollectionAllMatchOperation() throws Exception {
        testCollectionAllMatchOperation("map.stream().allMatch(e -> e < 4).orElse(false)", "false");
    }

    @Test
    public void testSetCollectionAllMatchOperation() throws Exception {
        testCollectionAllMatchOperation("set.stream().allMatch(e -> e < 4).orElse(false)", "false");
    }

    private void testCollectionAllMatchOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Check if all the elements in the list are smaller than 4
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("AllMatch: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionNoneMatchOperation() throws Exception {
        testCollectionNoneMatchOperation("list.stream().noneMatch(e -> e > 5).orElse(false)", "true");
    }

    @Test
    public void testMapCollectionNoneMatchOperation() throws Exception {
        testCollectionNoneMatchOperation("map.stream().noneMatch(e -> e > 5).orElse(false)", "true");
    }

    @Test
    public void testSetCollectionNoneMatchOperation() throws Exception {
        testCollectionNoneMatchOperation("set.stream().noneMatch(e -> e > 5).orElse(false)", "true");
    }

    private void testCollectionNoneMatchOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Check if the none of the elements are greater than 5
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("NoneMatch: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    @Test
    public void testListCollectionFindFirstOperation() throws Exception {
        testCollectionFindFirstOperation("list.stream().findFirst().get()", "1");
    }

    @Test
    public void testMapCollectionFindFirstOperation() throws Exception {
        testCollectionFindFirstOperation("map.stream().findFirst().get()", "1");
    }

    @Test
    public void testSetCollectionFindFirstOperation() throws Exception {
        testCollectionFindFirstOperation("set.stream().findFirst().get()", "1");
    }

    private void testCollectionFindFirstOperation(String expression, String expectedResult) throws Exception {
        String result;

        // Get the first element of the list
        Object obj = evaluateOperations(elp, expression);
        assertNotNull(obj);
        result = obj.toString();

        System.out.println("FindFirst: " + result);
        assertEquals("Expected: " + expectedResult + " but was: " + result, expectedResult, result);

    }

    /**
     * Helper method to evaluate operations on collection objects
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated.
     * @return the result of the evaluated expression
     */
    private Object evaluateOperations(ELProcessor elp, String expression) {

        return elp.eval(expression);

    }
}
