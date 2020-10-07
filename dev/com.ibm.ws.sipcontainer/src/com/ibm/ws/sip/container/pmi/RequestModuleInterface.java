/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

public interface RequestModuleInterface{
    
//  Constants
	public final static int INBOUND_OTHER = 60;
	public final static int INBOUND_REGISTER = 61;
    public final static int INBOUND_INVITE = 62;
    public final static int INBOUND_ACK = 63;
    public final static int INBOUND_OPTIONS = 64;
    public final static int INBOUND_BYE = 65;
    public final static int INBOUND_CANCEL = 66;
    public final static int INBOUND_PRACK = 67;
    public final static int INBOUND_INFO = 68;
    public final static int INBOUND_SUBSCRIBE = 69;
    public final static int INBOUND_NOTIFY = 70;
    public final static int INBOUND_MESSAGE = 71;
    public final static int INBOUND_PUBLISH = 72;
    public final static int INBOUND_REFER = 73;
    public final static int INBOUND_UPDATE = 74;
    
    public final static int OUTBOUND_OTHER = 80;
    public final static int OUTBOUND_REGISTER = 81;
    public final static int OUTBOUND_INVITE = 82;
    public final static int OUTBOUND_ACK = 83;
    public final static int OUTBOUND_OPTIONS = 84;
    public final static int OUTBOUND_BYE = 85;
    public final static int OUTBOUND_CANCEL = 86;
    public final static int OUTBOUND_PRACK = 87;
    public final static int OUTBOUND_INFO = 88;
    public final static int OUTBOUND_SUBSCRIBE = 89;
    public final static int OUTBOUND_NOTIFY = 90;
    public final static int OUTBOUND_MESSAGE = 91;
    public final static int OUTBOUND_PUBLISH = 92;
    public final static int OUTBOUND_REFER = 93;
    public final static int OUTBOUND_UPDATE = 94;
    
    /**
     * Update number of Inbound Request
     * @param method The Request method
     */
    public void incrementInRequest(String method);
   
    /**
     * Update number of Outbound Request
     * @param method The Request method
     */
    public void incrementOutRequest(String method);
     
    /**
     * Update counters that were countered till now
     *
     */
    public void updateCounters();
    
    /**
     * Unregister module 
     */
    public void destroy();
}
