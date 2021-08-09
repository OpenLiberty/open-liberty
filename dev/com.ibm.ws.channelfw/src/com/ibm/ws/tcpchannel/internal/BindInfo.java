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
package com.ibm.ws.tcpchannel.internal;

import java.net.ServerSocket;

/**
 * Wrapper class used for the TCP early bind process, allowing the delayed
 * handling of that bind attempt.
 */
public class BindInfo {

    private Exception bindException = null;
    private ServerSocket serverSocket = null;
    private String hostname;
    private int port;
    private int reuseAddr = -1;
    private int recvBufferSize = -1;
    private int listenBacklog = 511;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BindInfo Hashcode: ").append(hashCode());
        sb.append(" hostname: ").append(hostname);
        sb.append(" port: ").append(port);
        return sb.toString();
    }

    /**
     * Set the server socket to use on the bind attempt.
     * 
     * @param socket
     */
    public void setServerSocket(ServerSocket socket) {
        this.serverSocket = socket;
    }

    /**
     * Query the configured server socket for this bind attempt.
     * 
     * @return ServerSocket
     */
    public ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    /**
     * Store the bind exception failure.
     * 
     * @param failure
     */
    public void setBindException(Exception failure) {
        this.bindException = failure;
    }

    /**
     * Access the bind exception, if one happened.
     * 
     * @return Exception, null if no error
     */
    public Exception getBindException() {
        return this.bindException;
    }

    /**
     * Set the hostname to use on the bind attempt.
     * 
     * @param name
     */
    public void setHostname(String name) {
        this.hostname = name;
    }

    /**
     * Query the hostname used on the bind attempt.
     * 
     * @return String
     */
    public String getHostname() {
        return this.hostname;
    }

    /**
     * Set the port to use on the bind attempt.
     * 
     * @param port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Query the port used on the bind attempt.
     * 
     * @return int
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Set the socket re-use flag for the bind attempt.
     * 
     * @param value
     */
    public void setReuseAddr(int value) {
        this.reuseAddr = value;
    }

    /**
     * Query the flag on the socket re-use option.
     * 
     * @return int
     */
    public int getReuseAddr() {
        return this.reuseAddr;
    }

    /**
     * Set the receive buffer size to use on the bind attempt.
     * 
     * @param size
     */
    public void setRecvBufferSize(int size) {
        this.recvBufferSize = size;
    }

    /**
     * Query the receive buffer size.
     * 
     * @return int
     */
    public int getRecvBufferSize() {
        return this.recvBufferSize;
    }

    /**
     * Set the size of the listen backlog to use.
     * 
     * @param size
     */
    public void setListenBacklog(int size) {
        this.listenBacklog = size;
    }

    /**
     * Query the size of the configured listen backlog.
     * 
     * @return int
     */
    public int getListenBacklog() {
        return this.listenBacklog;
    }

}
