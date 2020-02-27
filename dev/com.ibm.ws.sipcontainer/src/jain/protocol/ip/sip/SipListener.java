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
 * This interface defines the methods required by all JAIN SIP
 * applications to receive and process SipEvents that
 * are emitted by an object implementing the
 * SipProvider interface.
 * It must be noted that any object that implements the:
 *<UL>
 *<LI>SipListener Interface is referred to as SipListenerImpl.
 *<LI>SipProvider Interface is referred to as SipProviderImpl.
 *</UL>
 *
 * @see SipProvider
 * @see SipEvent
 *
 * @version 1.0
 *
 *
 */
public interface SipListener extends java.util.EventListener
{
    
    /**
     * Processes a Response received on one of the SipListener's ListeningPoints.
     * @param <var>responseReceivedEvent</var> SipEvent received because Response was received
     */
    public void processResponse(SipEvent responseReceivedEvent);
    
    /**
     * Processes the time out of a transaction specified by
     * the transactionId.
     * @param <var>transactionTimeOutEvent</var> SipEvent received because transaction timed out
     */
    public void processTimeOut(SipEvent transactionTimeOutEvent);
    
    /**
     * Processes a Request received on one of the SipListener's ListeningPoints.
     * @param <var>requestReceivedEvent</var> SipEvent received because Request was received
     */
    public void processRequest(SipEvent requestReceivedEvent);
}
