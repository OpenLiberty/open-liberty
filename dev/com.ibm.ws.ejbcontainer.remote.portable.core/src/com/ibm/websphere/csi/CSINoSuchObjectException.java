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

public class CSINoSuchObjectException
                extends CSIException
{
    private static final long serialVersionUID = 1804172477963855625L;

    /**
     * Create a new CSINoSuchObjectException with an empty
     * description string. <p>
     */
    public CSINoSuchObjectException() {

    } // CSINoSuchObjectException

    /**
     * Create a new CSINoSuchObjectException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public CSINoSuchObjectException(String s) {

        super(s);

    } // CSINoSuchObjectException

    /**
     * Create a new CSINoSuchObjectException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */
    public CSINoSuchObjectException(String s, Throwable ex) {

        super(s, ex);

    } // CSINoSuchObjectException

    /**
     * Create a new CSINoSuchObjectException with an empty
     * description string and a minor code. <p>
     */
    public CSINoSuchObjectException(int minorCode) {
        super(minorCode);
    } // CSINoSuchObjectException

    /**
     * Create a new CSINoSuchObjectException with the
     * associated string description and minor code. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     * @param minorCode the <code>int</code> describing the minor code <p>
     */
    public CSINoSuchObjectException(String s, int minorCode) {

        super(s, minorCode);

    } // CSINoSuchObjectException

    /**
     * Create a new CSINoSuchObjectException with the associated
     * string description and nested exception and minor code. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     * @param minorCode the <code>int</code> describing the minor code <p>
     */
    public CSINoSuchObjectException(String s, Throwable ex, int minorCode) {

        super(s, ex, minorCode);

    } // CSINoSuchObjectException

} // CSINoSuchObjectException
