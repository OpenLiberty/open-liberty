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

package com.ibm.ws.sib.admin;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

public final class ExtendedBoolean implements java.lang.Comparable {

    private static final TraceComponent tc = SibTr.register(ExtendedBoolean.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    private final static String STRING_FALSE = "false";
    public final static ExtendedBoolean FALSE = new ExtendedBoolean(STRING_FALSE, 0, 0);

    private final static String STRING_TRUE = "true";
    public final static ExtendedBoolean TRUE = new ExtendedBoolean(STRING_TRUE, 1, 1);

    private final static String STRING_NONE = "none";
    public final static ExtendedBoolean NONE = new ExtendedBoolean(STRING_NONE, 2, 2);

    private final static ExtendedBoolean[] set = { FALSE, TRUE, NONE };

    private final static ExtendedBoolean[] indexSet = { FALSE, TRUE, NONE };

    private final static String[] nameSet = { STRING_FALSE, STRING_TRUE, STRING_NONE };

    // Maximum index 
    public final static int MAX_INDEX = NONE.getIndex();

    private final String name;
    private final int value;
    private final int index;

    /**
     * Private constructor which ensures the "constants" defined here are the total set
     * 
     * @param aName
     * @param aValue
     * @param aIndex
     */
    private ExtendedBoolean(String aName, int aValue, int aIndex) {
        name = aName;
        value = aValue;
        index = aIndex;
    }

    /**
     * Returns the corresponding ExtendedBoolean for a given integer.
     * <p>
     * This method should NOT be called by any code outside SIB. It is
     * only public so that it can be accessed by sub-packages.
     * 
     * @param aValue the integer for which an ExtendedBoolean is required.
     * @return The corresponding ExtendeBoolean
     */
    public final static ExtendedBoolean getExtendedBoolean(int aValue) {
        if (tc.isDebugEnabled())
            SibTr.info(tc, "Value = " + aValue);
        return set[aValue];
    }

    /**
     * Returns the integer representation of this ExtendedBoolean. T
     * <p>
     * This method should NOT be called by any code outside SIB. It is
     * only public so that it can be accessed by sub-packages.
     * 
     * @return The int representation of the ExtendedBoolean instance.
     */
    public final int toInt() {
        return value;
    }

    /**
     * Returns the name of this ExtendedBoolean.
     * 
     * @return The name of the ExtendedBoolean instance.
     */
    @Override
    public final String toString() {
        return name;
    }

    /**
     * Compare this ExtendedBoolean with another.
     * <p>
     * The method implements java.util.Comaparable.compareTo and therefore
     * has the same semantics.
     * 
     * @param other The Reliability this is to be compared with.
     * @return An int indicating the relative values as follows:
     *         <br> >0: this > other (i.e. more Reliable).
     *         <br> 0: this == other
     *         <br> <0: this < other (i.e. less Reliable).
     */
    public final int compareTo(Object other) {
        if (tc.isDebugEnabled())
            SibTr.info(
                       tc,
                       "this: " + this + ", other: " + other.toString() + ", result: " + (this.value - ((ExtendedBoolean) other).value));

        return (this.value - ((ExtendedBoolean) other).value);
    }

    /****************************************************************************/
    /* Internal use only Public methods */
    /****************************************************************************/

    /**
     * Returns the corresponding ExtendedBoolean for a given name.
     * <p>
     * This method should NOT be called by any code outside SIB. It is
     * only public so that it can be accessed by sub-packages.
     * 
     * @param name The toString value of an ExtendedBoolean constant.
     * @return The corresponding ExtendedBoolean
     */
    public final static ExtendedBoolean getExtendedBooleanByName(String name) throws NullPointerException, IllegalArgumentException {
        if (tc.isDebugEnabled())
            SibTr.info(tc, "Name = " + name);

        if (name == null) {
            throw new NullPointerException();
        }

        /* Look for the name in the nameSet, and return the corresponding */
        /* Reliability from the indexSet. */
        for (int i = 0; i <= MAX_INDEX + 1; i++) {
            if (name.equals(nameSet[i])) {
                return indexSet[i];
            }
        }

        /* If the name didn't match, throw IllegalArgumentException */
        throw new IllegalArgumentException();
    }

    /**
     * Returns the corresponding ExtendedBoolean for a given index.
     * <p>
     * This method should NOT be called by any code outside SIB. It is
     * only public so that it can be accessed by sub-packages.
     * 
     * @param index The index for which an ExtendedBoolean is required.
     * @return The corresponding ExtendedBoolean
     */
    public final static ExtendedBoolean getExtendedBooleanByIndex(int index) {
        if (tc.isDebugEnabled())
            SibTr.info(tc, "Index = " + index);
        return indexSet[index + 1];
    }

    /**
     * Returns the index of the Reliability.
     * <p>
     * This method should NOT be called by any code outside SIB. It is
     * only public so that it can be accessed by sub-packages.
     * 
     * @return The index of the ExtendedBoolean instance.
     */
    public final int getIndex() {
        return index;
    }

}
