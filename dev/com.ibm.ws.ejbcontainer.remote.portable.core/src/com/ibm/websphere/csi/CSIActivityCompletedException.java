/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

public class CSIActivityCompletedException
                extends CSIException
{
    private static final long serialVersionUID = -8936050037724995978L;

    /**
     * Create a new CSIActivityCompletedException with an empty
     * description string. <p>
     */

    public CSIActivityCompletedException() {

    }

    /**
     * Create a new CSIActivityCompletedException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */

    public CSIActivityCompletedException(String s) {

        super(s);
    }

    /**
     * Create a new CSIActivityCompletedException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */

    public CSIActivityCompletedException(String s, Throwable ex) {

        super(s, ex);
    }

    /**
     * Create a new CSIActivityCompletedException with an empty
     * description string and a minor code. <p>
     * 
     * @param minorCode the <code>int</code> minor code
     */

    public CSIActivityCompletedException(int minorCode) {
        super(minorCode);

    }

    /**
     * Create a new CSIActivityCompletedException with the
     * associated string description and a minor code. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param minorCode the <code>int</code> minor code
     */

    public CSIActivityCompletedException(String s, int minorCode) {

        super(s, minorCode);
    }

    /**
     * Create a new CSIActivityCompletedException with the associated
     * string description and nested exception and a minor code. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     * 
     * @param minorCode the <code>int</code> minor code
     */

    public CSIActivityCompletedException(String s, Throwable ex, int minorCode) {

        super(s, ex, minorCode);
    }

} // CSIActivityCompletedException
