/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelNameException;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContextFactory;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

@Component(service = { CFWManager.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class CFWManager {

    /*
     * // these imports work for the channel jar included in Liberty - but that jar will not work because of needing the new thread code.
     * private static final String cfImplClass = "com.ibm.ws.channelfw.internal.ChannelFrameworkImpl";
     * private static final String wbbpMgrImplClass = "com.ibm.ws.bytebuffer.internal.WsByteBufferPoolManagerImpl";
     */
    private static CHFWBundle m_chfw = null;

    /* Singleton channelframework */
    private static ChannelFramework cfw = null;

    /* Singleton WsByteBufferPoolManager */
    private static WsByteBufferPoolManager wbbpMgr = null;

    // Reference to single instance of this class
    private static TCPConnectRequestContextFactory tcpConnectFactory = null;

    private static CFWManager singletonCfwManager = null;

    private final boolean chainCreated = false;

    public static CFWManager getInstance() {
        if (singletonCfwManager == null)
            singletonCfwManager = new CFWManager();
        return singletonCfwManager;
    }

    /**
     * DS method for setting the required channel framework service.
     *
     * @param bundle
     */
    @Reference(name = "chfwBundle")
    protected void setChfwBundle(CHFWBundle bundle) {
        System.out.println("setChfwBundle");
        m_chfw = bundle;
    }

    /**
     * This is a required static reference, this won't be called until the
     * component has been deactivated
     *
     * @param bundle
     *            CHFWBundle instance to unset
     */
    protected void unsetChfwBundle(CHFWBundle bundle) {
        System.out.println("unsetChfwBundle");
        m_chfw = null;
    }

    /**
     * Returns reference to ChannelFramework
     *
     * @return
     */
    protected CHFWBundle getChfwBundle() {
        return m_chfw;
    }

    /**
     * Activate this Endpoint
     */
    @Activate
    public void activate(Map<String, Object> properties) {
        System.out.println("activate");
        //at this point we already have m_chfw instantiated
        createTCPChannelChain("TCPChannel1", "TCPChainA", getChannelFramework());

    }

    /**
     * DS deactivate
     *
     * @param ctx
     * @param reason
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        System.out.println("deactivate");
    }

    /**
     * DS Modify
     *
     * @param config
     */
    @Modified
    protected void modified(Map<String, Object> config) {
        System.out.println("modified");
    }

    public synchronized TCPConnectRequestContextFactory getTCPConnectRequestContextFactory() {
        if (tcpConnectFactory == null) {
            try {
                tcpConnectFactory = TCPConnectRequestContextFactory.getRef();
            } catch (Exception x) {
                System.out.println("CFWManager.getTCPConnectRequestContextFactory(): " + x);
            }
        }
        return tcpConnectFactory;
    }

    public static synchronized WsByteBufferPoolManager getWsByteBufferPoolManager() {
        if (wbbpMgr == null) {
            wbbpMgr = m_chfw.getBufferManager();
        }
        return wbbpMgr;
    }

    public synchronized ChannelFramework getChannelFramework() {
        if (cfw == null) {
            cfw = m_chfw.getFramework();
        }
        return cfw;
    }

    public void createTCPChannelChain(String tcpChannelName, String chainName, ChannelFramework cfw) {

        try {
            cfw.addChannel(tcpChannelName, cfw.lookupFactory("TCPChannel"), null);

            final String[] chanList;
            chanList = new String[] { tcpChannelName };

            System.out.println("successfully add ChainData of: " + cfw.addChain(chainName, FlowType.OUTBOUND, chanList));

        } catch (InvalidChannelNameException e) {
            System.out.println("Channel name already registered!: " + e);
        } catch (InvalidChainNameException e) {
            System.out.println("Chain name already registered!: " + e);
        } catch (Exception e) {
            System.err.println("CFWManager.createChannelChain(...) exception(): " + e);
        }
    }

    public OutboundVirtualConnection createOutboundVirtualConnection() {
        return createOutboundVirtualConnection("TCPChainA");
    }

    public OutboundVirtualConnection createOutboundVirtualConnection(String chainName) {

        OutboundVirtualConnection ovc = null;
        try {
            ovc = (OutboundVirtualConnection) getChannelFramework().getOutboundVCFactory(chainName).createConnection();
        } catch (Exception e) {
            System.out.println("CFWManager..createOutboundVirtualConnection(...) exception(): " + e);
            e.printStackTrace(System.err);
        }
        return ovc;

    }

    // object needs to be of type TCPConnectRequestContext
    public TCPConnectionContext connectTCPOutbound(OutboundVirtualConnection ovc, TCPConnectRequestContext outbound) {

        TCPConnectionContext tcc = null;

        try {
            // sync connect
            ovc.connect(outbound);

            tcc = (TCPConnectionContext) ovc.getChannelAccessor();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println("CFWManager.connectOutbound(...) exception(): " + e);
        }

        return tcc;
    }

}
