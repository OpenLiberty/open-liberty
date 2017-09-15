/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.comms.server;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.comms.CommsConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class represents an array of int's. These are stored
 * in primitive int arrays which are sized at an initial size.
 * When storing more int's than this size allows, the array is
 * stashed in a List and a new int array is created.
 * 
 * @author Gareth Matthews
 */
public class IntArray {
    /** The trace */
    private static final TraceComponent tc =
                    SibTr.register(IntArray.class,
                                   CommsConstants.MSG_GROUP,
                                   CommsConstants.MSG_BUNDLE);

    /** The Vector of all the int arrays */
    private List allArrays = null;

    /** The initial array size */
    private final int arraySize;

    /** The 'working' array */
    private int[] current;

    /** The current position in the 'working' array */
    private int currentCursor = 0;

    /** The total number of elements in the array */
    private int elements = 0;

    /** The default size of the internal arrays */
    private static final int DEFAULT_SIZE = 20;

    /**
     * Constructs an int array with the specified initial size
     * of the first internal array.
     * 
     * @param initalArraySize
     */
    public IntArray(int initalArraySize) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "<init>");

        if (tc.isDebugEnabled())
            SibTr.debug(tc, "Params: initialArraySize", initalArraySize);

        this.arraySize = initalArraySize;

        current = new int[arraySize];
        // You could change the List impl here if you wanted
        allArrays = new ArrayList();

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Default constructor. Creates an int array with an initial size
     * of the first internal array being 20 elements.
     */
    public IntArray() {
        this(DEFAULT_SIZE);

        if (tc.isEntryEnabled())
            SibTr.entry(tc, "<init>");
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * Adds an int to the array.
     * 
     * @param anInt
     */
    public void add(int anInt) {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "add");

        if (tc.isDebugEnabled())
            SibTr.debug(tc, "Params: int", anInt);

        // Have we filled up the current array?
        if (currentCursor == arraySize) {
            // Stash it in the Vector
            allArrays.add(current);

            // Create a new one
            current = new int[arraySize];

            // Set the cursor at the beginning
            currentCursor = 0;
        }

        // Now add the element at the correct position
        current[currentCursor] = anInt;

        // Move the cursor along
        currentCursor++;

        // Increment the size
        elements++;

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "add");
    }

    /**
     * Gets an int from the array.
     * 
     * @param index
     * @return Returns the int
     * 
     * @throws NoSuchElementException if the element does not exist
     */
    public int get(int index) throws NoSuchElementException {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "get");

        int retValue = 0;

        // We need to work out which array this item will be in
        int arrayNumber = index / arraySize;

        // Bounds check
        if ((index + 1) > elements) {
            throw new NoSuchElementException();
        } else if (index < 0) {
            throw new NoSuchElementException();
        }
        // Is it in the current array?
        else if (allArrays.size() == arrayNumber) {
            retValue = current[index - (arrayNumber * arraySize)];
        }
        // Else get the array from the Vector and
        // return the value
        else {
            int[] tempArray = (int[]) allArrays.get(arrayNumber);
            retValue = tempArray[index - (arrayNumber * arraySize)];
        }

        if (tc.isDebugEnabled())
            SibTr.debug(tc, "rc=", retValue);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "get");

        return retValue;
    }

    /**
     * Returns the number of elements in the array
     * 
     * @return int
     */
    public int length() {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "length");
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "rc=", elements);
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "length");

        return elements;
    }

    /**
     * toString()
     * 
     * @return String
     */
    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer("[");

        for (int x = 0; x < length(); x++) {
            if (x != 0)
                ret.append(", ");
            ret.append(get(x));
        }

        ret.append("]");

        return ret.toString();
    }
}
