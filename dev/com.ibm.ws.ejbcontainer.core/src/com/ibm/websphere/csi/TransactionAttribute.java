/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import com.ibm.ws.ejbcontainer.InternalConstants;

/**
 * <code>TransactionAttribute</code> defines legal values for the
 * transaction attribute passed to <code>TransactionControl</code>. <p>
 */
public class TransactionAttribute {

    public static final TransactionAttribute TX_NOT_SUPPORTED =
                    new TransactionAttribute(InternalConstants.TX_NOT_SUPPORTED, "TX_NOT_SUPPORTED");

    public static final TransactionAttribute TX_BEAN_MANAGED =
                    new TransactionAttribute(InternalConstants.TX_BEAN_MANAGED, "TX_BEAN_MANAGED");

    public static final TransactionAttribute TX_REQUIRED =
                    new TransactionAttribute(InternalConstants.TX_REQUIRED, "TX_REQUIRED");

    public static final TransactionAttribute TX_SUPPORTS =
                    new TransactionAttribute(InternalConstants.TX_SUPPORTS, "TX_SUPPORTS");

    public static final TransactionAttribute TX_REQUIRES_NEW =
                    new TransactionAttribute(InternalConstants.TX_REQUIRES_NEW, "TX_REQUIRES_NEW");

    public static final TransactionAttribute TX_MANDATORY =
                    new TransactionAttribute(InternalConstants.TX_MANDATORY, "TX_MANDATORY");

    public static final TransactionAttribute TX_NEVER =
                    new TransactionAttribute(InternalConstants.TX_NEVER, "TX_NEVER");

    /**
     * Construct new <code>TransactionAttribute</code> instance with
     * the given unique value. <p>
     */

    private TransactionAttribute(int value, String s) {

        this.value = value;
        this.name = s;
    }

    /**
     * Return unique value for this <code>TransactionAttribute</code>. <p>
     */

    public int getValue() {
        return value;
    }

    /**
     * Return string representation of this
     * <code>TransactionAttribute</code>. <p>
     */

    public String toString() {
        return name;
    }

    /**
     * Return number of possible <code>TransactionAttribute</code>. <p>
     */

    public static int getNumAttrs() {
        return numAttrs;
    }

    /**
     * Unique value for each legal <code>TransactionAttribute</code> for
     * fast lookups.
     */

    private int value;
    private String name;
    private static final int numAttrs = 7;

}
