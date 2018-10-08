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
package com.ibm.ws.el30.fat.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.el.ELProcessor;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.el30.fat.beans.EL30MapCollectionObjectBean;

/**
 * This servlet tests all the lists, sets and maps EL 3.0 operations on collection objects.
 */
@WebServlet("/EL30CollectionObjectOperationsServlet")
public class EL30CollectionObjectOperationsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final List<Integer> list;
    private final Map<Integer, String> map;
    private final Map<Integer, EL30MapCollectionObjectBean> nestedMap;
    private final Set<String> set;

    /**
     * Constructor
     */
    public EL30CollectionObjectOperationsServlet() {
        super();

        // List to be used as a collection object
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
        set = new HashSet<String>() {
            {
                add("1");
                add("4");
                add("3");
                add("5");
                add("2");
            }
        };

        // Map to be used as a collection object
        map = new HashMap<Integer, String>() {
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ELProcessor elp = new ELProcessor();
        PrintWriter pw = response.getWriter();

        // Obtain the stream by getting a collection view of the map
        Collection<String> collectionMap = map.values();
        Collection<EL30MapCollectionObjectBean> collectionNestedMap = nestedMap.values();

        // Define the list, map, set and printer objects within the ELProcessor object
        elp.defineBean("list", list);
        elp.defineBean("set", set);
        elp.defineBean("map", collectionMap);
        elp.defineBean("nestedMap", collectionNestedMap);
        elp.defineBean("printer", pw);

        if (request.getParameterMap().containsKey("testListCollectionOperations")) {
            evaluateListOperations(pw, request, elp);
        } else if (request.getParameterMap().containsKey("testSetCollectionOperations")) {
            evaluateSetOperations(pw, request, elp);
        } else {
            evaluateMapOperations(pw, request, elp);
        }

    }

    /**
     * Evaluate all list collection object operations
     *
     * @param pw
     * @param request
     * @param elp
     */
    private void evaluateListOperations(PrintWriter pw, HttpServletRequest request, ELProcessor elp) {
        pw.println("Original List: " + list.toString());

        if (request.getParameter("testListCollectionOperations").equals("filter")) {
            // Get all the elements greater or  equal than 3
            Object obj = evaluateOperations(elp, pw, "list.stream().filter(e -> e >= 3).toList()");
            if (obj != null)
                pw.println("Filter: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("map")) {
            // Add 2 to each element of the list
            Object obj = evaluateOperations(elp, pw, "list.stream().map(e -> e + 2).toList()");
            if (obj != null)
                pw.println("Map: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("flatMap")) {
            // Flat the list
            Object obj = evaluateOperations(elp, pw, "[[1, 4], [3], [2], [5, 3, 1]].stream().flatMap(e -> e.stream()).toList()");
            if (obj != null)
                pw.println("FlatMap: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("distinct")) {
            // Get distinct values from the list
            Object obj = evaluateOperations(elp, pw, "list.stream().distinct().toList()");
            if (obj != null)
                pw.println("Distinct: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("sorted")) {
            // Sort the list in decreasing order
            Object obj = evaluateOperations(elp, pw, "list.stream().sorted((i, j) -> j-i).toList()");
            if (obj != null)
                pw.println("Sorted in Decreasing: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("forEach")) {
            // To print a list of integers
            pw.print("ForEach: ");
            evaluateOperations(elp, pw, "list.stream().forEach(e -> printer.print(e))");
        } else if (request.getParameter("testListCollectionOperations").equals("peek")) {
            // To print the a list of integer before and after a filter. It filters even values.
            pw.print("Debug Peek: ");
            Object obj = evaluateOperations(elp, pw, "list.stream().peek(e -> printer.print(e)).filter(e -> e%2 == 0).peek(e -> printer.print(e)).toList()");
            if (obj != null)
                pw.println(" Peek: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("iterator")) {
            // This method returns an iterator for the source stream, suitable for use in Java codes
            Object obj = evaluateOperations(elp, pw, "list.stream().iterator()");
            if (obj != null) {
                Iterator<Integer> iter = (Iterator<Integer>) obj;
                pw.print("Iterator: ");
                while (iter.hasNext()) {
                    pw.print(iter.next() + " ");
                }
            }
        } else if (request.getParameter("testListCollectionOperations").equals("limit")) {
            // Get the first 3 elements in the list
            Object obj = evaluateOperations(elp, pw, "list.stream().limit(3).toList()");
            if (obj != null)
                pw.println("Limit: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("substream")) {
            // Get a sub list out of the entire list
            Object obj = evaluateOperations(elp, pw, "list.stream().substream(2, 4).toList()");
            if (obj != null)
                pw.println("Substream: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("toArray")) {
            // Get an array containing the elements of the source stream.
            Object obj = evaluateOperations(elp, pw, "list.stream().toArray()");
            if (obj != null)
                pw.println("ToArray: " + Arrays.toString((Object[]) obj));
        } else if (request.getParameter("testListCollectionOperations").equals("toList")) {
            // Get a list containing the elements of the source stream
            Object obj = evaluateOperations(elp, pw, "list.stream().toList()");
            if (obj != null)
                pw.println("ToList: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("reduce")) {
            // Find the largest number in the list
            Object obj = evaluateOperations(elp, pw, "list.stream().reduce((p, q) -> p > q ? p:q).get()");
            if (obj != null)
                pw.println("Reduce: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("max")) {
            // Find the maximum number in the list
            Object obj = evaluateOperations(elp, pw, "list.stream().max().get()");
            if (obj != null)
                pw.println("Max: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("min")) {
            // Find the minimum number in the list
            Object obj = evaluateOperations(elp, pw, "list.stream().min().get()");
            if (obj != null)
                pw.println("Min: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("average")) {
            // Find the average value of the list
            Object obj = evaluateOperations(elp, pw, "list.stream().average().get()");
            if (obj != null)
                pw.println("Average: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("sum")) {
            // Find the sum of the list
            Object obj = evaluateOperations(elp, pw, "list.stream().sum()");
            if (obj != null)
                pw.println("Sum: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("count")) {
            // Find the amount of element in the list
            Object obj = evaluateOperations(elp, pw, "list.stream().count()");
            if (obj != null)
                pw.println("Count: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("anyMatch")) {
            // Check if the list contains any element smaller than 2
            Object obj = evaluateOperations(elp, pw, "list.stream().anyMatch(e -> e < 2).orElse(false)");
            if (obj != null)
                pw.println("AnyMatch: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("allMatch")) {
            // Check if all the elements in the list are smaller than 4
            Object obj = evaluateOperations(elp, pw, "list.stream().allMatch(e -> e < 4).orElse(false)");
            if (obj != null)
                pw.println("AllMatch: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("noneMatch")) {
            // Check if the none of the elements are greater than 5
            Object obj = evaluateOperations(elp, pw, "list.stream().noneMatch(e -> e > 5).orElse(false)");
            if (obj != null)
                pw.println("NoneMatch: " + obj);
        } else if (request.getParameter("testListCollectionOperations").equals("findFirst")) {
            // Get the first element of the list
            Object obj = evaluateOperations(elp, pw, "list.stream().findFirst().get()");
            if (obj != null)
                pw.println("FindFirst: " + obj);
        } else {
            pw.println("Invalid Parameter");
        }
    }

    /**
     * Evaluate all set collection object operations
     *
     * @param pw
     * @param request
     * @param elp
     */
    private void evaluateSetOperations(PrintWriter pw, HttpServletRequest request, ELProcessor elp) {
        //pw.println("Original Set: " + set.toString());

        if (request.getParameter("testSetCollectionOperations").equals("filter")) {
            // Get all the elements greater or  equal than 3
            Object obj = evaluateOperations(elp, pw, "set.stream().filter(e -> e >= 3).toList()");
            if (obj != null)
                pw.println("Filter: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("map")) {
            // Add 2 to each element of the set
            Object obj = evaluateOperations(elp, pw, "set.stream().map(e -> e + 2).toList()");
            if (obj != null)
                pw.println("Map: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("flatMap")) {
            // Flat the set
            Object obj = evaluateOperations(elp, pw, "{{1, 4}, {3}, {2}, {5, 3, 1}}.stream().flatMap(e -> e.stream()).toList()");
            if (obj != null)
                pw.println("FlatMap: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("distinct")) {
            // Get distinct values from the set
            Object obj = evaluateOperations(elp, pw, "set.stream().distinct().toList()");
            if (obj != null)
                pw.println("Distinct: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("sorted")) {
            // Sort the set in decreasing order
            Object obj = evaluateOperations(elp, pw, "set.stream().sorted((i, j) -> j-i).toList()");
            if (obj != null)
                pw.println("Sorted in Decreasing: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("forEach")) {
            // To print a list of integers
            pw.print("ForEach: ");
            evaluateOperations(elp, pw, "set.stream().forEach(e -> printer.print(e))");
        } else if (request.getParameter("testSetCollectionOperations").equals("peek")) {
            // To print the a list of integer before and after a filter. It filters even values.
            pw.print("Debug Peek: ");
            Object obj = evaluateOperations(elp, pw, "set.stream().peek(e -> printer.print(e)).filter(e -> e%2 == 0).peek(e -> printer.print(e)).toList()");
            if (obj != null)
                pw.println(" Peek: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("iterator")) {
            // This method returns an iterator for the source stream, suitable for use in Java codes
            Object obj = evaluateOperations(elp, pw, "set.stream().iterator()");
            if (obj != null) {
                Iterator<Integer> iter = (Iterator<Integer>) obj;
                pw.print("Iterator: ");
                while (iter.hasNext()) {
                    pw.print(iter.next() + " ");
                }
            }
        } else if (request.getParameter("testSetCollectionOperations").equals("limit")) {
            // Get the first 3 elements in the (ascending sort) set
            Object obj = evaluateOperations(elp, pw, "set.stream().sorted((i, j) -> i-j).limit(3).toList()");
            if (obj != null)
                pw.println("Limit: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("substream")) {
            // Get a sub set out of the entire (ascending sort) set
            Object obj = evaluateOperations(elp, pw, "set.stream().sorted((i, j) -> i-j).substream(2, 4).toList()");
            if (obj != null)
                pw.println("Substream: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("toArray")) {
            // Get an array containing the elements of the source stream.
            Object obj = evaluateOperations(elp, pw, "set.stream().toArray()");
            if (obj != null)
                pw.println("ToArray: " + Arrays.toString((Object[]) obj));
        } else if (request.getParameter("testSetCollectionOperations").equals("toList")) {
            // Get a list containing the elements of the source stream
            Object obj = evaluateOperations(elp, pw, "set.stream().toList()");
            if (obj != null)
                pw.println("ToList: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("reduce")) {
            // Find the largest number in the set
            Object obj = evaluateOperations(elp, pw, "set.stream().reduce((p, q) -> p > q ? p:q).get()");
            if (obj != null)
                pw.println("Reduce: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("max")) {
            // Find the maximum number in the set
            Object obj = evaluateOperations(elp, pw, "set.stream().max().get()");
            if (obj != null)
                pw.println("Max: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("min")) {
            // Find the minimum number in the set
            Object obj = evaluateOperations(elp, pw, "set.stream().min().get()");
            if (obj != null)
                pw.println("Min: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("average")) {
            // Find the average value of the set
            Object obj = evaluateOperations(elp, pw, "set.stream().average().get()");
            if (obj != null)
                pw.println("Average: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("sum")) {
            // Find the sum of the set
            Object obj = evaluateOperations(elp, pw, "set.stream().sum()");
            if (obj != null)
                pw.println("Sum: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("count")) {
            // Find the amount of element in the set
            Object obj = evaluateOperations(elp, pw, "set.stream().count()");
            if (obj != null)
                pw.println("Count: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("anyMatch")) {
            // Check if the set contains any element smaller than 2
            Object obj = evaluateOperations(elp, pw, "set.stream().anyMatch(e -> e < 2).orElse(false)");
            if (obj != null)
                pw.println("AnyMatch: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("allMatch")) {
            // Check if all the elements in the set are smaller than 4
            Object obj = evaluateOperations(elp, pw, "set.stream().allMatch(e -> e < 4).orElse(false)");
            if (obj != null)
                pw.println("AllMatch: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("noneMatch")) {
            // Check if the none of the elements are greater than 5
            Object obj = evaluateOperations(elp, pw, "set.stream().noneMatch(e -> e > 5).orElse(false)");
            if (obj != null)
                pw.println("NoneMatch: " + obj);
        } else if (request.getParameter("testSetCollectionOperations").equals("findFirst")) {
            // Get the first element of the (ascending sort) set
            Object obj = evaluateOperations(elp, pw, "set.stream().sorted((i, j) -> i-j).findFirst().get()");
            if (obj != null)
                pw.println("FindFirst: " + obj);
        } else {
            pw.println("Invalid Parameter");
        }
    }

    /**
     * Evaluate all map collection object operations
     *
     * @param pw
     * @param request
     * @param elp
     */
    private void evaluateMapOperations(PrintWriter pw, HttpServletRequest request, ELProcessor elp) {
        pw.println("Original Map: " + map.toString());

        if (request.getParameter("testMapCollectionOperations").equals("filter")) {
            // Get all the elements greater or  equal than 3
            Object obj = evaluateOperations(elp, pw, "map.stream().filter(e -> e >= 3).toList()");
            if (obj != null)
                pw.println("Filter: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("map")) {
            // Add 2 to each element of the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().map(e -> e + 2).toList()");
            if (obj != null)
                pw.println("Map: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("flatMap")) {
            // Flat the map collection view
            Object obj = evaluateOperations(elp, pw, "nestedMap.stream().flatMap(e -> e.map.stream()).toList()");
            if (obj != null)
                pw.println("FlatMap: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("distinct")) {
            // Get distinct values from the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().distinct().toList()");
            if (obj != null)
                pw.println("Distinct: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("sorted")) {
            // Sort the map collection view in decreasing order
            Object obj = evaluateOperations(elp, pw, "map.stream().sorted((i, j) -> j-i).toList()");
            if (obj != null)
                pw.println("Sorted in Decreasing: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("forEach")) {
            // To print a list of integers
            pw.print("ForEach: ");
            evaluateOperations(elp, pw, "map.stream().forEach(e -> printer.print(e))");
        } else if (request.getParameter("testMapCollectionOperations").equals("peek")) {
            // To print the a list of integer before and after a filter. It filters even values.
            pw.print("Debug Peek: ");
            Object obj = evaluateOperations(elp, pw, "map.stream().peek(e -> printer.print(e)).filter(e -> e%2 == 0).peek(e -> printer.print(e)).toList()");
            if (obj != null)
                pw.println(" Peek: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("iterator")) {
            // This method returns an iterator for the source stream, suitable for use in Java codes
            Object obj = evaluateOperations(elp, pw, "map.stream().iterator()");
            if (obj != null) {
                Iterator<Integer> iter = (Iterator<Integer>) obj;
                pw.print("Iterator: ");
                while (iter.hasNext()) {
                    pw.print(iter.next() + " ");
                }
            }
        } else if (request.getParameter("testMapCollectionOperations").equals("limit")) {
            // Get the first 3 elements in the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().limit(3).toList()");
            if (obj != null)
                pw.println("Limit: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("substream")) {
            // Get a sub map out of the entire map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().substream(2, 4).toList()");
            if (obj != null)
                pw.println("Substream: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("toArray")) {
            // Get an array containing the elements of the source stream.
            Object obj = evaluateOperations(elp, pw, "map.stream().toArray()");
            if (obj != null)
                pw.println("ToArray: " + Arrays.toString((Object[]) obj));
        } else if (request.getParameter("testMapCollectionOperations").equals("toList")) {
            // Get a list containing the elements of the source stream
            Object obj = evaluateOperations(elp, pw, "map.stream().toList()");
            if (obj != null)
                pw.println("ToList: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("reduce")) {
            // Find the largest number in the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().reduce((p, q) -> p > q ? p:q).get()");
            if (obj != null)
                pw.println("Reduce: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("max")) {
            // Find the maximum number in the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().max().get()");
            if (obj != null)
                pw.println("Max: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("min")) {
            // Find the minimum number in the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().min().get()");
            if (obj != null)
                pw.println("Min: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("average")) {
            // Find the average value of the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().average().get()");
            if (obj != null)
                pw.println("Average: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("sum")) {
            // Find the sum of the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().sum()");
            if (obj != null)
                pw.println("Sum: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("count")) {
            // Find the amount of element in the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().count()");
            if (obj != null)
                pw.println("Count: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("anyMatch")) {
            // Check if the map collection view contains any element smaller than 2
            Object obj = evaluateOperations(elp, pw, "map.stream().anyMatch(e -> e < 2).orElse(false)");
            if (obj != null)
                pw.println("AnyMatch: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("allMatch")) {
            // Check if all the elements in the map collection view are smaller than 4
            Object obj = evaluateOperations(elp, pw, "map.stream().allMatch(e -> e < 4).orElse(false)");
            if (obj != null)
                pw.println("AllMatch: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("noneMatch")) {
            // Check if the none of the elements are greater than 5
            Object obj = evaluateOperations(elp, pw, "map.stream().noneMatch(e -> e > 5).orElse(false)");
            if (obj != null)
                pw.println("NoneMatch: " + obj);
        } else if (request.getParameter("testMapCollectionOperations").equals("findFirst")) {
            // Get the first element of the map collection view
            Object obj = evaluateOperations(elp, pw, "map.stream().findFirst().get()");
            if (obj != null)
                pw.println("FindFirst: " + obj);
        } else {
            pw.println("Invalid Parameter");
        }
    }

    /**
     * Helper method to evaluate operations on collection objects
     *
     * @param elp The ELProcessor
     * @param pw PrintWriter
     * @param expression Expression to be evaluated.
     * @return the result of the evaluated expression
     */
    private Object evaluateOperations(ELProcessor elp, PrintWriter pw, String expression) {
        Object obj = null;
        try {
            obj = elp.eval(expression);
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("Test Failed. An exception was thrown: " + e.toString());
        }
        return obj;
    }
}
