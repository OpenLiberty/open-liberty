/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.framework.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory. It
 * basically wrappers the com.ibm.websphere.channelfw.VirtualConnectionFactory code in the
 * underlying TCP channel.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory
 * @see com.ibm.websphere.channelfw.VirtualConnectionFactory
 * 
 * @author Gareth Matthews
 */
public class CFWNetworkConnectionFactory implements NetworkConnectionFactory
{
    /** Trace */
    private static final TraceComponent tc = SibTr.register(CFWNetworkConnectionFactory.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    /** Class name for FFDC's */
    private static final String CLASS_NAME = CFWNetworkConnectionFactory.class.getName();

    /** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/CFWNetworkConnectionFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }

    /** The virtual connection factory this object wraps */
    private VirtualConnectionFactory vcFactory = null;

    /**
     * Constructor.
     * 
     * @param vcFactory
     */
    public CFWNetworkConnectionFactory(VirtualConnectionFactory vcFactory)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", vcFactory);
        this.vcFactory = vcFactory;
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory#createConnection(java.lang.Object)
     */
    @Override
    public NetworkConnection createConnection(Object endpoint) throws FrameworkException
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnection", endpoint);

        NetworkConnection conn = null;
        try
        {
            // Create the connection from the VC Factory. As we are connecting using an EP, we must
            // cast to the right VCF
            // Romil liberty changes 
            VirtualConnectionFactory wsVcf = vcFactory;
            OutboundVirtualConnection vc = (OutboundVirtualConnection) wsVcf.createConnection();
            conn = new CFWNetworkConnection(vc);
        } catch (ChannelFrameworkException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".createConnection",
                                        JFapChannelConstants.CFWNETWORKCONNECTIONFACT_CREATECONN_01,
                                        new Object[] { this, endpoint });

            if (tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to create connection", e);

            throw new FrameworkException(e);
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "createConnection", conn);
        return conn;
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory#createConnection()
     */
    @Override
    public NetworkConnection createConnection() throws FrameworkException
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "createConnection");

        NetworkConnection conn = null;
        try
        {
            // Create the connection from the VC Factory
            OutboundVirtualConnection vc = (OutboundVirtualConnection) vcFactory.createConnection();
            conn = new CFWNetworkConnection(vc);
        } catch (ChannelFrameworkException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".createConnection",
                                        JFapChannelConstants.CFWNETWORKCONNECTIONFACT_CREATECONN_02,
                                        new Object[] { this });

            if (tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to create connection", e);

            throw new FrameworkException(e);
        }

        return conn;
    }

/*
 * (non-Javadoc)
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory#getOutboundVirtualConFactory()
 */
    @Override
    public VirtualConnectionFactory getOutboundVirtualConFactory() {
        return vcFactory;
    }

}
