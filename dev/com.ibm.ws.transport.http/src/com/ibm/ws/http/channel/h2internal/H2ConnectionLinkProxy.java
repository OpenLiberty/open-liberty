/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
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
package com.ibm.ws.http.channel.h2internal;

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 *
 */
public class H2ConnectionLinkProxy implements ConnectionLink {

    H2HttpInboundLinkWrap link = null;

    public H2ConnectionLinkProxy(H2HttpInboundLinkWrap x) {
        link = x;
    }

    @Override
    public ConnectionLink getDeviceLink() {
        return this;
    }

    @Override
    public Object getChannelAccessor() {
        return link.getConnectionContext();

    }

    @Override
    public void close(VirtualConnection vc, Exception e) {
        link.closeDeviceLink(e);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#ready(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public void ready(VirtualConnection vc) {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(java.lang.Exception)
     */
    @Override
    public void destroy(Exception e) {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getVirtualConnection()
     */
    @Override
    public VirtualConnection getVirtualConnection() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.channelfw.ConnectionLink#setApplicationCallback(com.ibm.wsspi.channelfw.ConnectionReadyCallback)
     */
    @Override
    public void setApplicationCallback(ConnectionReadyCallback next) {

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getApplicationCallback()
     */
    @Override
    public ConnectionReadyCallback getApplicationCallback() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.channelfw.ConnectionLink#setDeviceLink(com.ibm.wsspi.channelfw.ConnectionLink)
     */
    @Override
    public void setDeviceLink(ConnectionLink next) {

    }

}
