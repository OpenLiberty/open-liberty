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

public class CSIInvalidTransactionException
                extends CSIException
{
    private static final long serialVersionUID = -4846800366819804437L;

    /**
     * Create a new CSIInvalidTransactionException with an empty
     * description string. <p>
     */
    public CSIInvalidTransactionException() {

    } // CSIInvalidTransactionException

    /**
     * Create a new CSIInvalidTransactionException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public CSIInvalidTransactionException(String s) {

        super(s);

    } // CSIInvalidTransactionException

    /**
     * Create a new CSIInvalidTransactionException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */
    public CSIInvalidTransactionException(String s, Throwable ex) {

        super(s, ex);

    } // CSIInvalidTransactionException

    /**
     * Create a new CSIInvalidTransactionException with an empty
     * description string and a minor code. <p>
     * 
     * @param minorCode the <code>int</code> describing the minor code <p>
     */
    public CSIInvalidTransactionException(int minorCode) {
        super(minorCode);

    } // CSIInvalidTransactionException

    /**
     * Create a new CSIInvalidTransactionException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param minorCode the <code>int</code> describing the minor code <p>
     */
    public CSIInvalidTransactionException(String s, int minorCode) {

        super(s, minorCode);

    } // CSIInvalidTransactionException

    /**
     * Create a new CSIInvalidTransactionException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     * 
     * @param minorCode the <code>int</code> minor code
     */
    public CSIInvalidTransactionException(String s, Throwable ex, int minorCode) {

        super(s, ex, minorCode);

    } // CSIInvalidTransactionException

} // CSIInvalidTransactionException
