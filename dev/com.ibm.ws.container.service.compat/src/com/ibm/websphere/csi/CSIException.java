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

import java.io.PrintWriter;

/**
 * The base exception type for all exceptions thrown by the
 * container-server interface. <p>
 */
public class CSIException
                extends java.rmi.RemoteException
{
    private static final long serialVersionUID = -5642270396942813316L;

    //0 is chosen as it is the default minor code for CORBA exceptions
    public static final int NO_MINOR_CODE = 0;
    private int minorCode = NO_MINOR_CODE;

    /**
     * Create a new CSIException with an empty description string. <p>
     */
    public CSIException() {

    } // CSIException

    /**
     * Create a new CSIException with the associated minor code. <p.
     * 
     * @param minorCode the <code>int</code> for the minor code <p>
     */
    public CSIException(int minorCode) {

        this.minorCode = minorCode;

    } // CSIException

    /**
     * Create a new CSIException with the associated string description. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     */
    public CSIException(String s) {

        super(s);

    } // CSIException

    /**
     * Create a new CSIException with the associated string description and minor code. <p.
     * 
     * @param s the <code>String</code> describing the exception <p>
     * @param minorCode the <code>int</code> for the minor code <p>
     */
    public CSIException(String s, int minorCode) {

        super(s);
        this.minorCode = minorCode;

    } // CSIException

    /**
     * Create a new CSIException with the associated string description and
     * nested exception. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     */
    public CSIException(String s, Throwable ex) {

        super(s, ex);

    } // CSIException

    /**
     * Create a new CSIException with the associated string description and
     * nested exception and minor code. <p>
     * 
     * @param s the <code>String</code> describing the exception <p>
     * 
     * @param ex the nested <code>Throwable</code>
     * 
     * @param minorCode the <code>int</code> minor code
     */
    public CSIException(String s, Throwable ex, int minorCode) {

        super(s, ex);
        this.minorCode = minorCode;

    } // CSIException

    /**
     * Print backtrace for this exception and any nested exception as well.
     */
    @Override
    public void printStackTrace(PrintWriter s) {

        super.printStackTrace(s);

    } // printStackTrace

    /**
     * Get the minor code for this exception
     * 
     * @return an int describing the minorCode.
     *         If none was set, NO_MINOR_CODE is returned.
     */
    public int getMinorCode() {
        return minorCode;
    }

} // CSIException
