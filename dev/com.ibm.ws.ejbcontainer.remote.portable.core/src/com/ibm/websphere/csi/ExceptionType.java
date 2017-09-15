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

/**
 *  <code>ExceptionType</code> defines legal values for the 
 *  exception type attribute passed to <code>TransactionControl</code>. <p>
 */

package com.ibm.websphere.csi;

public class ExceptionType {

    public static final ExceptionType NO_EXCEPTION =
                    new ExceptionType(0, "NO_EXCEPTION");

    public static final ExceptionType CHECKED_EXCEPTION =
                    new ExceptionType(1, "CHECKED_EXCEPTION");

    public static final ExceptionType UNCHECKED_EXCEPTION =
                    new ExceptionType(2, "UNCHECKED_EXCEPTION");

    /**
     * Construct new <code>ExceptionType</code> instance with
     * the given unique value. <p>
     */

    private ExceptionType(int value, String s) {

        this.value = value;
        this.name = s;
    }

    /**
     * Return unique value for this <code>ExceptionType</code>. <p>
     */

    public int getValue() {
        return value;
    }

    /**
     * Return string representation of this
     * <code>ExceptionType</code>. <p>
     */

    public String toString() {
        return name;
    }

    /**
     * Unique value for each legal <code>ExceptionType</code> for
     * fast lookups.
     */

    private int value;
    private String name;
}
