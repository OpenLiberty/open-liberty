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

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory. It is the
 * top level class that provides access to the NetworkConnectionFactory classes from which
 * connections can be created.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory
 * 
 * @author Gareth Matthews
 */
public class RichClientTransportFactory implements NetworkTransportFactory
{
    /** Trace */
    private static final TraceComponent tc = SibTr.register(RichClientTransportFactory.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    /** Class name for FFDC's */
    private static final String CLASS_NAME = RichClientTransportFactory.class.getName();

    /** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/RichClientTransportFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }

    /** Local reference to the channel framework */
    private ChannelFramework channelFramework = null;

    /**
     * Constructor.
     * 
     * @param channelFramework
     */
    public RichClientTransportFactory(ChannelFramework channelFramework)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", channelFramework);
        this.channelFramework = channelFramework;
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory#getOutboundNetworkConnectionFactoryByName(java.lang.String)
     */
    @Override
    public NetworkConnectionFactory getOutboundNetworkConnectionFactoryByName(String chainName) throws FrameworkException
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOutboundNetworkConnectionFactoryByName", chainName);

        NetworkConnectionFactory connFactory = null;

        try
        {
            // Get the virtual connection factory from the channel framework using the chain name

            VirtualConnectionFactory vcFactory = CommsClientServiceFacade.getChannelFramewrok().getOutboundVCFactory(chainName);

            connFactory = new CFWNetworkConnectionFactory(vcFactory);

        } catch (com.ibm.wsspi.channelfw.exception.ChannelException e) {

            FFDCFilter.processException(e, CLASS_NAME + ".getOutboundNetworkConnectionFactoryByName",
                                        JFapChannelConstants.RICHCLIENTTRANSPORTFACT_GETCONNFACBYNAME_01,
                                        new Object[] { this, chainName });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failure to obtain OVC for Outbound chain" + chainName, e);

            throw new FrameworkException(e);
        } catch (com.ibm.wsspi.channelfw.exception.ChainException e) {

            FFDCFilter.processException(e, CLASS_NAME + ".getOutboundNetworkConnectionFactoryByName",
                                        JFapChannelConstants.RICHCLIENTTRANSPORTFACT_GETCONNFACBYNAME_01,
                                        new Object[] { this, chainName });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failure to obtain OVC for Outbound chain" + chainName, e);

            throw new FrameworkException(e);
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOutboundNetworkConnectionFactoryByName", connFactory);
        return connFactory;
    }

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory#getOutboundNetworkConnectionFactoryFromEndPoint(java.lang.Object)
     */
    @Override
    public NetworkConnectionFactory getOutboundNetworkConnectionFactoryFromEndPoint(Object endPoint)
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOutboundNetworkConnectionFactoryFromEndPoint", endPoint);

        NetworkConnectionFactory connFactory = null;
        if (endPoint instanceof CFEndPoint)
        {
            // Get the virtual connection factory from the EP and wrap it in our implementation of
            // the NetworkConnectionFactory interface
            VirtualConnectionFactory vcFactory = ((CFEndPoint) endPoint).getOutboundVCFactory();
            connFactory = new CFWNetworkConnectionFactory(vcFactory);
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOutboundNetworkConnectionFactoryFromEndPoint", connFactory);
        return connFactory;
    }

}
