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
 * This Exception is thrown when an attempt is made to
 * create a SipProvider with a ListeningPoint which
 * is not owned by the SipStack or is being used byb another SipProvider
 *
 * @version 1.0
 *
 */
public final class ListeningPointUnavailableException extends SipException
{
    
    /**
     * Constructs a new ListeningPointUnavailableException
     */
    public ListeningPointUnavailableException() 
    {
        super();
    }
    
    /**
     * Constructs a new ListeningPointUnavailableException with
     * the specified detail message.
     * @param <var>msg</var> the detail message
     */
    public ListeningPointUnavailableException(String msg) 
    {
        super(msg);
    }
}
