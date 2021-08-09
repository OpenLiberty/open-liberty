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

public class CSIActivityRequiredException
                extends CSIException
{
    private static final long serialVersionUID = -6336387163843485291L;

    /**
     * Create a new CSIActivityRequiredException with an empty
     * description string. <p>
     */

    public CSIActivityRequiredException() {

    }

    /**
     * Create a new CSIActivityRequiredException with the
     * associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */

    public CSIActivityRequiredException(String s) {

        super(s);
    }

    /**
     * Create a new CSIActivityRequiredException with the associated
     * string description and nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */

    public CSIActivityRequiredException(String s, Throwable ex) {

        super(s, ex);
    }

    /**
     * Create a new CSIActivityRequiredException with an empty
     * description string and a minor code. <p>
     * 
     * @param minorCode the <code>int</code> minor code
     */

    public CSIActivityRequiredException(int minorCode) {
        super(minorCode);

    }

    /**
     * Create a new CSIActivityRequiredException with the
     * associated string description and a minor code. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param minorCode the <code>int</code> minor code
     */

    public CSIActivityRequiredException(String s, int minorCode) {

        super(s, minorCode);
    }

    /**
     * Create a new CSIActivityRequiredException with the associated
     * string description and nested exception and a minor code. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     * 
     * @param minorCode the <code>int</code> minor code
     */

    public CSIActivityRequiredException(String s, Throwable ex, int minorCode) {

        super(s, ex, minorCode);
    }

} // CSIActivityRequiredException
