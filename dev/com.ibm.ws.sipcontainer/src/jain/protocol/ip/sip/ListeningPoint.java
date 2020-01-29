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

import java.io.Serializable;

/**
 * This interface represents a unique IP network listening point,
 * and consists of host, port and transport
 *
 * @version 1.0
 *
 */
public interface ListeningPoint extends Cloneable, Serializable
{
    
    /**
     * Gets port of ListeningPoint
     * @return port of ListeningPoint
     */
    public int getPort();
    
    /**
     * Gets transport of ListeningPoint
     * @return transport of ListeningPoint
     */
    public String getTransport();
    
    /**
     * @return True is transport is a secure one
     */
    public boolean isSecure();
    
    /**
     * Indicates whether some other Object is "equal to" this ListeningPoint
     * (Note that obj must have the same Class as this ListeningPoint - this means that it
     * must be from the same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this ListeningPoint
     * @returns true if this ListeningPoint is "equal to" the obj
     * argument; false otherwise
     */
    public boolean equals(Object obj);
    
    /**
     * Creates and returns a copy of ListeningPoint
     * @returns a copy of ListeningPoint
     */
    public Object clone();
    
    /**
     * Gets host of ListeningPoint
     * @return host of ListeningPoint
     */
    public String getHost();
	
    /**
     * Gets the sent-by host name or IP address
	 * @return the sent-by host, as specified in configuration,
	 *  or the value of {@link #getHost()} if not set
	 */
	public String getSentBy();
	
	/**
     * Gets the channel name
	 * @return the channel name, as specified in configuration,
	 *  or null if not set
	 */
	public String getChannelName();
	
	/**
	 * Gets the value to be added to the Call-Id 
	 * @return the value as specified in configuration
	 */
	public String getCallIdValue();

    /**
     * TCP Transport constant
     */
    public static final String TRANSPORT_TCP = "tcp";
    
    /**
     * Default port constant
     */
    public static final int DEFAULT_PORT = 5060;
    
    /**
     * UDP Transport constant
     */
    public static final String TRANSPORT_UDP = "udp";
}
