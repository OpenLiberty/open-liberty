/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw;

import java.net.InetAddress;

/**
 * Descriptor class used to pass around information about the local and remote
 * ends of the
 * connection.
 */
public interface ConnectionDescriptor {

    /**
     * Get the name of the host at the remote end of the connection.
     * 
     * @return remote host name
     */
    String getRemoteHostName();

    /**
     * Set the name of the host at the remote end of the connection.
     * 
     * @param s
     *            remote host name
     */
    void setRemoteHostName(String s);

    /**
     * Get the address of the host at the remote end of the connection.
     * 
     * @return remote host address
     */
    String getRemoteHostAddress();

    /**
     * Set the address of the host at the remote end of the connection.
     * 
     * @param s
     *            remote host address
     */
    void setRemoteHostAddress(String s);

    /**
     * Get the name of the host at the local end of the connection.
     * 
     * @return local host name
     */
    String getLocalHostName();

    /**
     * Set the name of the host at the local end of the connection.
     * 
     * @param s
     *            local host name
     */
    void setLocalHostName(String s);

    /**
     * Get the address of the host at the local end of the connection.
     * 
     * @return local host address
     */
    String getLocalHostAddress();

    /**
     * Set the address of the host at the local end of the connection.
     * 
     * @param s
     *            local host address
     */
    void setLocalHostAddress(String s);

    /**
     * Set the remote host name, remote host address, local host name, and
     * local host address for this connnection.
     * 
     * @param s1
     *            remote host name
     * @param s2
     *            remote host address
     * @param s3
     *            local host name
     * @param s4
     *            local host address
     */
    void setAll(String s1, String s2, String s3, String s4);

    /**
     * set the remote InetAddress and local Address (instead of remote host name,
     * remote host address, local host name, and local host address) for this
     * connnection.
     * 
     * @param remote
     *            InetAddress
     * @param local
     *            InetAddress
     */
    void setAddrs(InetAddress remote, InetAddress local);

}
