/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip;

/**
 * This exception is thrown if the
 * deleteProvider
 * method of a SipStack is invoked
 * to delete a SipProvider
 * but the deletion is not allowed.
 * This may be because the SipProvider has already been deleted, or because the
 * SipProvider is in use.
 *
 * @version 1.0
 *
 */
public class UnableToDeleteProviderException extends SipException
{
    
    /**
     * Constructs a new <code> UnableToDeleteProviderException</code>
     */
    public UnableToDeleteProviderException() 
    {
        super();
    }
    
    /**
     * Constructs a new UnableToDeleteProviderException
     * with the specified detail message.
     * @param <var>msg</var> the detail message
     */
    public UnableToDeleteProviderException(String msg) 
    {
        super(msg);
    }
}
