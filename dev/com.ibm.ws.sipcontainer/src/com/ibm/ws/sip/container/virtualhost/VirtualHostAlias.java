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
package com.ibm.ws.sip.container.virtualhost;

import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * @author Nitzan, Aug 8, 2005
 *
 * Represents a virtual host alias (host:port)
 */
public interface VirtualHostAlias {
    
    /**
     * Initialize object data with given host and port parameters
     * @param host
     * @param port
     */
    public void init( String host, int port);
    
    /**
     * Initialize object data with given host:port string   
     * @param hostPortStr
     */
    public void init( String hostPortStr);
    
    /**
     * @return Returns the isAnyHost.
     */
    public boolean isAnyHost();

    /**
     * @return Returns the ip.
     */
    public String getIp();
    
    /**
     * @return Returns the host name.
     */
    public String getHost();
    
    /**
     * @return Returns the port.
     */
    public int getPort();
    
    /**
     * getter for isAnyPort
     */
    public boolean isAnyPort();
	
    /**
     * 
     * @return the Regular expression if exists otherwise returns null;
     */
    public Pattern getRegExp();
    
    /**
     * 
     * @return Returns the isRegExp;
     */
    public boolean isRegExp();
    
    /**
     * Check if two virtual hosts are logically the same
     * @param vh
     * @return true if give virtual host is the same as this one
     */
    public boolean match( VirtualHostAlias vh);

    /**
     * Get inetAddress representation object
     * @return
     */
	public InetAddress getInetAddress();
	
	/**
	 * Match 2 VHAs InetAddress
	 * @param vh
	 * @return
	 */
	public boolean matchInetAddr(VirtualHostAlias vh);
	
}
