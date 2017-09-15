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
package com.ibm.ejs.persistence;

public class EJSPersistenceException extends java.rmi.RemoteException // 126686
{
    private static final long serialVersionUID = -4057243471703509169L;

    private int error = 0;

    /**
     * Create a new EJSPersistenceException with an empty description string. <p>
     *
     */
    public EJSPersistenceException() {

    } // EJSPersistenceException

    /**
     * Create a new EJSPersistenceException with the associated string description. <p.
     *
     * @param s the <code>String</code> describing the exception <p>
     */
    public EJSPersistenceException(String s) {

        super(s);

    } // EJSPersistenceException

    /**
     * Create a new EJSPersistenceException with the associated string
     * description and nested exception. <p>
     *
     * @param s the <code>String</code> describing the exception <p>
     */

    public EJSPersistenceException(String s, Throwable ex) {

        super(s, ex);
        if (ex instanceof java.sql.SQLException) { // 126686
            error = ((java.sql.SQLException) ex).getErrorCode();
        }
    } // EJSPersistenceException

    public EJSPersistenceException(Throwable ex) {

        super("", ex); //150727

    } // EJSPersistenceException

    /**
     * Create a new EJSPersistenceException with the associated error code. <p>
     *
     * @param s the <code>String</code> describing the exception <p>
     */
    public EJSPersistenceException(int error) {

        this.error = error;

    } // EJSPersistenceException

    /**
     * Utitility method for obtaining underlying persistence
     * error codes. JDBC specific persistence implementations
     * will map this to SQL error codes. The error code is that
     * which was passed in via a constructor. <p>
     *
     * @return a data store specific error code <p>
     *
     */
    int getErrorCode() {

        return error;

    } // getErrorCode

} // EJSPersistenceException
