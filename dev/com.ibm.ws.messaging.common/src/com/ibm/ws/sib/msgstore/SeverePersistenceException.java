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

/**
 * This exception is thrown by the Persistence Layer when it encounters
 * an error which should result in the Messaging Engine being stopped.
 */
public class SeverePersistenceException extends PersistenceException
{
    private static final long serialVersionUID = 3894291004132777117L;

    /**
     * Constructor
     * 
     * @param throwable the cause of this exception
     */
    public SeverePersistenceException(Throwable throwable)
    {
        super(throwable);
    }

    /**
     * Constructor
     * 
     * @param string a key to the appropriate NLS string.
     */
    public SeverePersistenceException(String string)
    {
        super(string);
    }

    /**
     * Constructor
     * 
     * @param string a key to the appropriate NLS string
     * @param throwable the cause of this exception
     */
    public SeverePersistenceException(String string, Throwable throwable)
    {
        super(string, throwable);
    }

    /**
     * Constructor
     * 
     * @param arg0 the key to the appropriate NLS string
     * @param args arguments to be inserted into the NLS string
     */
    public SeverePersistenceException(String arg0, Object[] args)
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
    public SeverePersistenceException(String message, Object[] inserts, Throwable exception)
    {
        super(message, inserts, exception);
    }

    /**
     * Constructor
     *
     */
    public SeverePersistenceException()
    {
        super();
    }
}
