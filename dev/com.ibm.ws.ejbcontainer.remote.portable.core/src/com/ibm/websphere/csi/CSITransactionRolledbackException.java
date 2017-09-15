/*******************************************************************************
 * Copyright (c) 1998, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

public class CSITransactionRolledbackException
                extends CSIException
{
    private static final long serialVersionUID = 6095094648647166480L;

    /**
     * Create a new CSITransactionRolledbackException with an empty
     * description string. <p>
     */
    public CSITransactionRolledbackException() {

    } // CSITransactionRolledbackException

    /**
     * Create a new CSITransactionRolledbackException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public CSITransactionRolledbackException(String s) {

        super(s);

    } // CSITransactionRolledbackException

    /**
     * Create a new CSITransactionRolledbackException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */
    public CSITransactionRolledbackException(String s, Throwable ex) {

        super(s, ex);

    } // CSITransactionRolledbackException

    /**
     * Create a new CSITransactionRolledbackException with an empty
     * description string and a minor code. <p>
     * 
     * @param minorCode the <code>int</code> minor code
     */
    public CSITransactionRolledbackException(int minorCode) {
        super(minorCode);

    } // CSITransactionRolledbackException

    /**
     * Create a new CSITransactionRolledbackException with the
     * associated string description and a minor code. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     * @param minorCode the <code>int</code> minor code
     */
    public CSITransactionRolledbackException(String s, int minorCode) {

        super(s, minorCode);

    } // CSITransactionRolledbackException

    /**
     * Create a new CSITransactionRolledbackException with the associated
     * string description and nested exception and a minor code. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     * @param minorCode the <code>int</code> minor code
     */
    public CSITransactionRolledbackException(String s, Throwable ex, int minorCode) {

        super(s, ex, minorCode);

    } // CSITransactionRolledbackException

} // CSITransactionRolledbackException
