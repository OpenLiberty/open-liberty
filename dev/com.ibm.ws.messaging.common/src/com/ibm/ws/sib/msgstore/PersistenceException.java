/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore;

import java.sql.SQLException;

/**
 * Wrapper for exceptions thrown by the persistence layer of the message store.
 * 
 * @author drphill
 * @author pradine
 */
public class PersistenceException extends MessageStoreException
{
    private static final long serialVersionUID = -7730683559279671494L;

    /**
     * Constructor
     * 
     * @param throwable the cause of this exception
     */
    public PersistenceException(Throwable throwable)
    {
        super(throwable);

        testForSQLException(throwable);
    }

    /**
     * Constructor
     * 
     * @param string a key to the appropriate NLS string.
     */
    public PersistenceException(String string)
    {
        super(string);
    }

    /**
     * Constructor
     * 
     * @param string a key to the appropriate NLS string
     * @param throwable the cause of this exception
     */
    public PersistenceException(String string, Throwable throwable)
    {
        super(string, throwable);

        testForSQLException(throwable);
    }

    /**
     * Constructor
     * 
     * @param arg0 the key to the appropriate NLS string
     * @param args arguments to be inserted into the NLS string
     */
    public PersistenceException(String arg0, Object[] args)
    {
        super(arg0, args);
    }

    /**
     * Constructor
     * 
     * @param message the key to the appropriate NLS string
     * @param inserts arguments to be inserted into the NLS string
     * @param exception the cause of this exception
     */
    public PersistenceException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);

        testForSQLException(exception);
    }

    /**
     * Constructor
     *
     */
    public PersistenceException()
    {
        super();
    }

    /**
     * SQLExceptions use {@link java.sql.SQLException#getNextException},
     * instead of {@link java.lang.Throwable#getCause}. This method sets
     * the {@link java.lang.Throwable#initCause} on the SQLException so
     * that getCause can then be used.
     *
     */
    private void testForSQLException(Throwable throwable)
    {
        try {
            while (throwable instanceof SQLException && throwable.getCause() == null) {
                SQLException cause = ((SQLException) throwable).getNextException();
                throwable.initCause(cause);
                throwable = cause;
            }
        }
        catch (Exception e) {
            //No FFDC Code Needed.
        }
    }
}
