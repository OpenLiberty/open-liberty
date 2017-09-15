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
package com.ibm.websphere.cpi;

/**
 * CPIException is the base exception class thrown by the container
 * during its interaction with the persister via the CPI interface.
 */

public class CPIException extends java.rmi.RemoteException
{
    private static final long serialVersionUID = 7919858109913290823L;

    private int error = 0;

    /**
     * Create a new CPIException with an empty description string. <p>
     */
    public CPIException() {

    } // CPIException

    /**
     * Create a new CPIException with the associated string description.
     * 
     * @param s the String describing the exception.
     */
    public CPIException(String s) {

        super(s);

    } // CPIException

    /**
     * Create a new CPIException with the associated string
     * description and nested exception.
     * 
     * @param s the String describing the exception.
     * @param ex the Throwable nested exception object.
     */
    public CPIException(String s, Throwable ex) {
        super(s, ex);
    } // CPIException

    /**
     * Create a new CPIException with the associated string
     * description and nested exception and error code.
     * 
     * @param s the String describing the exception.
     * @param ex the Throwable nested exception object.
     * @param error the int describing the error code.
     */
    public CPIException(String s, Throwable ex, int error) {
        super(s, ex);
        this.error = error;
    } // CPIException

    /**
     * Create a new CPIException with the nested exception.
     * 
     * @param ex the Throwable nested exception object.
     */
    public CPIException(Throwable ex) {
        super("", ex); //150727
    } // CPIException

    /**
     * Create a new CPIException with the associated error code.
     * 
     * @param error the int describing the error.
     */
    public CPIException(int error) {

        this.error = error;

    } // CPIException

    /**
     * Utitility method for obtaining underlying persistence
     * error codes. JDBC specific persistence implementations
     * will map this to SQL error codes. The error code is that
     * which was passed in via a constructor.
     * 
     * @return a data store specific error code
     * 
     */
    int getErrorCode() {
        return error;
    } // getErrorCode

} // CPIException
