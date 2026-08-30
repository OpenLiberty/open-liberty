/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.io.IOException;

import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.InboundProtocolLink;

/**
 * The CFW ConnectionLink class for the WOLAChannel. The downstream channel
 * is LocalComm, so the downstream (aka "device-side") ConnectionLink for this
 * guy is LocalCommConnLink.
 *
 * LocalCommConnLink.getApplicationCallback() <----------> WolaConnLink.getDeviceLink()
 * LocalCommServiceContext <------------------------------------------+ getDeviceLink().getChannelAccesor()
 *
 */
public class WolaConnLink extends InboundProtocolLink implements ConnectionLink {

    /**
     * This guy manages outbound requests (server-outbound-to-client) over this connection.
     */
    private final WolaOutboundRequestService wolaOutboundRequestService = new WolaOutboundRequestService(this);

    /**
     * {@inheritDoc}
     *
     * Called by downstream device-side connlink when a new connection is ready.
     */
    @Override
    public void ready(VirtualConnection vc) {

        this.vc = vc;

        // Read the first message (possibly synchronously).
        getDeviceLinkChannelAccessor().read(new WOLAMessageReader(this));
    }

    /**
     * @return The device-side (LocalCommChannel) channel accessor (LocalCommServiceContext).
     *
     * @throws RuntimeException(IOException) if the device link is null
     */
    public LocalCommServiceContext getDeviceLinkChannelAccessor() {
        return (LocalCommServiceContext) getDeviceLink().getChannelAccessor();
    }

    /**
     * @return The WolaOutboundRequestService for this connection.
     */
    protected WolaOutboundRequestService getOutboundRequestService() {
        return wolaOutboundRequestService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getChannelAccessor() {
        // This should never be called since this is the top of the chain.
        throw new IllegalStateException("Not implemented. This channel is at the top of the chain.");
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(Exception)
     */
    @Override
    public void destroy(Exception e) {
        // Post any outbound requests waiting for a response
        wolaOutboundRequestService.postAll(e);
        super.destroy(e);
    }

    /**
     * Destroy is called by the downstream channel (localcomm) in response to
     * a close (initiated by the upstream channel (WOLA)) or by a destroy initiated
     * by the downstream channel. In other words, this method gets called whenever
     * the connection is going away.
     */
    @Override
    protected void destroy() {
        super.destroy();

        // Post any outbound requests waiting for a response
        wolaOutboundRequestService.postAll(null);
    }

    /**
     * Due to the asynchronous nature of the WOLA Channel and LocalComm Channel code,
     * it's possible for getDeviceLink() to be called after destroy() (e.g. from close()
     * or from getDeviceLinkChannelAccessor()). destroy() nulls out the device link so
     * these callers will suffer NPEs. There's no easy way to protect against this without
     * adding synchronization and whatnot, and since we're dealing with a destroyed
     * connection anyway it's probably not worth it to be so careful. Instead, just
     * substitute a more meaningful RuntimeException/IOException indicating that the
     * connection has been destroyed, instead of a NPE which always looks like a defect.
     *
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getDeviceLink()
     *
     * @throws RuntimeException(IOException) if the device link is null
     */
    @Override
    public ConnectionLink getDeviceLink() {
        ConnectionLink deviceLink = super.getDeviceLink();
        if (deviceLink == null) {
            throw new RuntimeException(new IOException("Device-side ConnectionLink is null. The connection has been destroyed"));
        }
        return deviceLink;
    }
}