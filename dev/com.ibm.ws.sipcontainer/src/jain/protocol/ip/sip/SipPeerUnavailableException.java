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
 * The SipPeerUnavailableException indicates that the JAIN SIP Peer class (a particular
 * implementation of JAIN SIP) could not be located in the classpath.
 *
 * @version 1.0
 *
 */
public class SipPeerUnavailableException extends SipException
{
    
    /**
     * Constructs a new JAIN SIP Peer Unavailable Exception.
     */
    public SipPeerUnavailableException() 
    {
        super();
    }
    
    /**
     * Constructs a new JAIN SIP Peer Unavailable Exception with
     * the specified message detail.
     * @param <var>msg</var> the message detail of this Exception.
     */
    public SipPeerUnavailableException(String msg) 
    {
        super(msg);
    }
}
