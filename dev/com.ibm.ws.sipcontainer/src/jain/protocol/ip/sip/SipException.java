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
 * A SipException is thrown when a general JAIN SIP exception is encountered,
 * and is used when no other subclass is appropriate.
 *
 * @version 1.0
 *
 */
public class SipException extends Exception
{
    
    /**
     * Constructs a new SipException
     */
    public SipException() 
    {
        super();
    }
    
    /**
     * Constructs a new SipException with the specified
     * detail message.
     * @param <var>msg</var> the message detail of this Exception.
     */
    public SipException(String msg) 
    {
        super(msg);
    }
}
