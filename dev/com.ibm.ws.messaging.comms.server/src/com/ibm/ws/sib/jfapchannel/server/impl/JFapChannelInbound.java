/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

// NOTE: D181601 is not changed flagged as it modifies every line of trace and FFDC.

package com.ibm.ws.sib.jfapchannel.server.impl;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Inbound JFAP Channel. Participates as a channel in the channel framework.
 * 
 * @author prestona
 */
public class JFapChannelInbound implements InboundChannel {
    private static final TraceComponent tc = SibTr.register(JFapChannelInbound.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);

    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.server.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapChannelInbound.java, SIB.comms, WASX.SIB, aa1225.01 1.15");
    }

    // The discriminator for this channel.
    private final Discriminator discriminator;

    private final ChannelFactoryData channelFactoryData; // D196678.10.1

    private ChannelData chfwConfig = null;

    /**
     * Creates a new inbound channel.
     * 
     * @param cc
     */
    public JFapChannelInbound(ChannelFactoryData factoryData, ChannelData cc) // F177053, D196678.10.1
    {
        update(cc);
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { factoryData, cc }); // D196678.10.1
        discriminator = new JFapDiscriminator(this);
        channelFactoryData = factoryData; // D196678.10.1
        chfwConfig = cc;
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Returns the discriminator for this channel.
     */
    public Discriminator getDiscriminator() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDiscriminator");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDiscriminator", discriminator);
        return discriminator;
    }

    // end F177053

    /**
     * Returns an acceptable device side interface for this channel. This will
     * always be the TCP Channel context.
     */
    // begin F177053
    public Class getDeviceInterface() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeviceInterface");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDeviceInterface");
        return TCPConnectionContext.class;
    }

    // end F177053

    /**
     * Receives notification of a channel configuration change.
     */
    public void update(ChannelData cc) // F177053
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "update", cc); // F177053
        // TODO: do we respond to this in any way?		
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "update"); // F177053
        this.chfwConfig = cc;
    }

    // begin F177053
    public void start() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "start");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "start");
    }

    // end F177053

    // begin F177053
    public void stop(long millisec) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "stop", "" + millisec);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "stop");
    }

    // end F177053

    // begin F177053
    public void init() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "init");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "init");
    }

    // end F177053

    // begin F177053
    public void destroy() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "destroy");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "destroy");
    }

    // end F177053

    /** {@inheritDoc} */
    @Override
    public DiscriminationProcess getDiscriminationProcess() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getDiscriminatoryType() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setDiscriminationProcess(DiscriminationProcess dp) {
    // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getApplicationInterface() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return this.chfwConfig.getName();
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {

        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getConnectionLink", vc);
        ConnectionLink retValue = new JFapInboundConnLink(vc, channelFactoryData, chfwConfig);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getConnectionLink", retValue);
        return retValue;
    }
    /** {@inheritDoc} */

}
