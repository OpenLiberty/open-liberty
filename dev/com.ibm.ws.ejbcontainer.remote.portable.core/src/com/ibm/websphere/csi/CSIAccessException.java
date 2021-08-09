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

/**
 *  The base exception type for all exceptions thrown by the
 *  container-server interface. <p>
 */

package com.ibm.websphere.csi;

public class CSIAccessException
                extends CSIException
{
    private static final long serialVersionUID = 4785451820797479654L;

    /**
     * Create a new CSIAccessException with an empty
     * description string. <p>
     */
    public CSIAccessException() {

    } // CSIAccessException

    /**
     * Create a new CSIAccessException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public CSIAccessException(String s) {

        super(s);

    } // CSIAccessException

    /**
     * Create a new CSIAccessException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */
    public CSIAccessException(String s, Throwable ex) {

        super(s, ex);

    } // CSIAccessException

    /**
     * Create a new CSIAccessException with a
     * minor code. <p>
     */
    public CSIAccessException(int minorCode) {
        super(minorCode);

    } // CSIAccessException

    /**
     * Create a new CSIAccessException with the
     * associated string description and minor code. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * @param minorCode the <code>int</code> describing the minor code <p>
     */
    public CSIAccessException(String s, int minorCode) {

        super(s, minorCode);

    } // CSIAccessException

    /**
     * Create a new CSIAccessException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * @param ex the nested <code>Throwable</code>
     * @param minorCode the <code>int</code> describing the minor code <p>
     */
    public CSIAccessException(String s, Throwable ex, int minorCode) {

        super(s, ex, minorCode);

    } // CSIAccessException

} // CSIAccessException
