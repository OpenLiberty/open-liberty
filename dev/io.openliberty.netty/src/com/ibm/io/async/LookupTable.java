/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.util.BitSet;

/**
 * A class for holding objects in a table indexed by int values
 * <p>
 * Objects can be added to the table and are then given an integer index value
 * that can be used to retrieve the objects at a later time. The index value for
 * a given object does not change, until the object is removed from the table.
 * Once an object is removed from the table, the index value can be reused for a
 * new object subsequenctly added to the LookupTable.
 * <p>
 * The capacity of the table will grow to contain all the objects that are added
 * to it. However, growing the table involves a performance overhead, so it is
 * best if the table is given an initial capacity which will not require to be
 * increased often. The capacity is increased by 1000 elements or a specifiec
 * amount each time it is increased.
 * 
 * @author MikeEdwards
 * @param <E> - object type stored in the table
 */
public class LookupTable<E> {

    /** The default initial capacity of the LookupTable */
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;
    /** Default increment to increase capacity by */
    private static final int DEFAULT_INCREMENT = 1000;

    /**
     * The elements of the LookupTable are held in theElementArray
     */
    private E[] theElementArray;

    /**
     * Holds the current capacity of the LookupTable
     */
    private int currentCapacity;

    private volatile int increment = DEFAULT_INCREMENT;

    /**
     * The count of elements in the LookupTable INVARIANT: elementCount <=
     * currentCapacity
     */
    private int elementCount;

    /**
     * This BitSet is used to flag full / empty slots in the ElementArray
     */
    private BitSet occupiedSlots;

    /**
     * Creates a LookupTable with a specified initial capacity.
     * 
     * @param initialCapacity
     *            the initial capacity of the table. 0 implies create a
     *            LookupTable with a default initial capacity.
     */
    public LookupTable(int initialCapacity) {
        this(initialCapacity, DEFAULT_INCREMENT);
    }

    /**
     * Constructor.
     * 
     * @param initialCapacity
     * @param initialIncrement
     */
    @SuppressWarnings("unchecked")
    public LookupTable(int initialCapacity, int initialIncrement) {
        int cap = initialCapacity;
        if (cap < 0) {
            throw new IllegalArgumentException("Capacity cannot be <0");
        }
        if (cap == 0) {
            cap = DEFAULT_INITIAL_CAPACITY;
        }

        this.theElementArray = (E[]) new Object[cap];
        this.currentCapacity = cap;
        this.occupiedSlots = new BitSet(cap);

        this.elementCount = 0;
        this.increment = initialIncrement;
    }

    /**
     * Finds the object in the LookupTable that is indexed by the supplied
     * value.
     * 
     * @param theIndex
     *            the index value to use for the lookup
     * @return the object indexed by the supplied index value. null if no object
     *         is indexed by the supplied value - this also applies if the index
     *         is <0 or is greater than the current capacity of the LookupTable.
     */
    public synchronized E lookupElement(int theIndex) {
        if (theIndex < 0 || theIndex > currentCapacity - 1) {
            return null;
        }
        return theElementArray[theIndex];
    }

    /**
     * Add an element to the LookupTable. Returns the index value which can be
     * used to look up the element in the LookupTable.
     * 
     * @param theElement
     *            the object to add to the LookupTable. Should not be null.
     * @return an int holding the Index value which points to the object in the
     *         LookupTable. -1 if theElement is null.
     */
    public synchronized int addElement(E theElement) {
        if (theElement == null) {
            return -1;
        }
        if (elementCount == currentCapacity) {
            // try to expand the table to handle the new value, if that fails
            // then it will throw an illegalstate exception
            expandTable();
        }
        int theIndex = occupiedSlots.nextClearBit(0);
        if (theIndex < 0 || theIndex > currentCapacity - 1) {
            throw new IllegalStateException("No space available for element");
        }
        theElementArray[theIndex] = theElement;
        elementCount++;
        occupiedSlots.set(theIndex);
        return theIndex;
    }

    /**
     * Remove the object from the LookupTable which is referenced by the
     * supplied Index value.
     * 
     * @param theIndex
     *            an int containing the index value of the element to remove
     *            from the LookupTable
     * @return the Object which was removed from the LookupTable - null if no
     *         object was removed.
     */
    public synchronized Object removeElement(int theIndex) {
        if (theIndex < 0 || theIndex > currentCapacity - 1) {
            throw new IllegalArgumentException("Index is out of range.");
        }
        Object theElement = theElementArray[theIndex];
        if (theElement != null) {
            theElementArray[theIndex] = null;
            elementCount--;
            occupiedSlots.clear(theIndex);
        }
        return theElement;
    }

    /**
     * Finds a supplied object in the LookupTable and returns its index.
     * 
     * @param element
     *            the object to find in the LookupTable. This is a SLOW
     *            operation as it scans the whole table.
     * @return the Index of the Object in the LookupTable. -1 if the object is
     *         not in the LookupTable.
     */
    public int findElement(Object element) {
        for (int i = 0; i < currentCapacity; i++) {
            if (element == theElementArray[i]) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Set the increment size for the LookupTable.
     * 
     * @param theIncrement
     *            an int specifying the new increment for the Lookup Table. Must
     *            be >0.
     * @throws IllegalArgumentException
     *             if theIncrement is <= 0
     */
    public void setIncrement(int theIncrement) {
        if (theIncrement <= 0) {
            throw new IllegalArgumentException("Increment must be >0");
        }
        this.increment = theIncrement;
    }

    /**
     * Expands the lookup table to accommodate more elements. Does this by
     * adding the expansion increment to the current capacity.
     */
    @SuppressWarnings("unchecked")
    private void expandTable() {
        int newCapacity = currentCapacity + increment;
        if (newCapacity < currentCapacity) {
            throw new IllegalStateException(
                            "Attempt to expand LookupTable beyond maximum capacity");
        }
        E[] theNewArray = (E[]) new Object[newCapacity];
        System.arraycopy(this.theElementArray, 0, theNewArray, 0, currentCapacity);
        theElementArray = theNewArray;
        currentCapacity = newCapacity;
    }

}
