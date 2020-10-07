/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip;

/**
 * This Exception is thrown when a user attempts to reference
 * a transaction that does not exist
 *
 * @version 1.0
 *
 */
public class TransactionDoesNotExistException extends SipException
{
    
    /**
     * Constructs a new TransactionDoesNotExistException
     */
    public TransactionDoesNotExistException() 
    {
        super();
    }
    
    /**
     * Constructs a new TransactionDoesNotExistException with
     * the specified detail message.
     * @param <var>msg</var> the detail message
     */
    public TransactionDoesNotExistException(String msg) 
    {
        super(msg);
    }
}
