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
import java.util.Map;
import java.util.Map.Entry;

import javax.resource.ResourceException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.ws.zos.channel.wola.WolaInterruptObjectBridge;
import com.ibm.ws.zos.channel.wola.WolaJcaBridge;
import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.WolaOtmaRequestInfo;
import com.ibm.ws.zos.channel.wola.internal.natv.WOLANativeUtils.OTMASendRcvResponseData;
import com.ibm.ws.zos.channel.wola.internal.otma.OLAIMSOTMAKeyMap;
import com.ibm.ws.zos.channel.wola.internal.otma.OTMAException;
import com.ibm.ws.zos.channel.wola.internal.otma.msg.OTMAMessageParseException;
import com.ibm.ws.zos.channel.wola.internal.otma.msg.OTMAMessageParser;
import com.ibm.wsspi.channelfw.ChannelFactory;

/**
 * WOLA Endpoint. This guy manages the WOLA CFW chain.
 *
 * This class also implements WolaJcaBridge. It provides the WOLA JCA code with
 * an entry point into the WOLA channel for invoking outbound to a service hosted
 * by a WOLA client.
 *
 */
public class WOLAEndpoint implements WolaJcaBridge {

    /**
     * Channel framework reference.
     */
    private CHFWBundle chfwBundle = null;

    /**
     * Chain builder singleton instance.
     */
    private final WOLAChainBuilder chainBuilder = new WOLAChainBuilder();

    /**
     * WOLAEndpoint is injected with a ref to localcommChannelFactoryProvider in order
     * to ensure the factory provider is registered with CFW before we try to create the chain.
     */
    private ChannelFactoryProvider localCommChannelFactoryProvider = null;

    /**
     * WOLAEndpoint is injected with a ref to wolaChannelFactoryProvider in order
     * to ensure the factory provider is registered with CFW before we try to create the chain.
     */
    private WOLAChannelFactoryProvider wolaChannelFactoryProvider = null;

    /**
     * Bridge to requestTiming-1.0
     */
    private WolaInterruptObjectBridge odiBridge = null;

    /**
     * Map to store OTMA anchors.
     */
    private OLAIMSOTMAKeyMap otmaKeyMap = null;

    /**
     * DS method for activating this component.
     *
     * Create the WOLA chain.
     *
     * @param context The OSGI component context.
     */
    protected void activate(Map<String, Object> config) {

        // Ensure the local comm and wola channel factories are installed.
        installChannelFactories(localCommChannelFactoryProvider);
        installChannelFactories(wolaChannelFactoryProvider);

        Object cid = config.get("component.id");
        String endpointId = (config.get("id") != null) ? (String) config.get("id") : "WOLAChannelEndpoint-" + cid;
        chainBuilder.init(endpointId, chfwBundle);
        chainBuilder.update(config);

        otmaKeyMap = new OLAIMSOTMAKeyMap(WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils());
    }

    /**
     * DS method for deactivating this component.
     *
     * Destroy the WOLA chain and close all OTMA connections.
     */
    protected void deactivate() {
        chainBuilder.removeChainAndChannels();
        otmaKeyMap.destroy();
    }

    /**
     * DS method for setting the channel framework bundle reference.
     *
     * @param service The CHFWBundle component reference.
     */
    protected void setChfwBundle(CHFWBundle bundle) {
        chfwBundle = bundle;
    }

    /**
     * DS method for removing the channel framework bundle reference.
     *
     * @param service The CHFWBundle component reference.
     */
    protected void unsetChfwBundle(CHFWBundle bundle) {
        chfwBundle = null;
    }

    /**
     * Set DS ref.
     */
    protected void setWolaChannelFactoryProvider(ChannelFactoryProvider wolaChannelFactoryProvider) {
        this.wolaChannelFactoryProvider = (WOLAChannelFactoryProvider) wolaChannelFactoryProvider;
    }

    /**
     * Set DS ref.
     */
    protected void setLocalCommChannelFactoryProvider(ChannelFactoryProvider localCommChannelFactoryProvider) {
        this.localCommChannelFactoryProvider = localCommChannelFactoryProvider;
    }

    /**
     * DS setter.
     */
    protected void setInterruptObjectBridge(WolaInterruptObjectBridge odiBridge) {
        this.odiBridge = odiBridge;
    }

    /**
     * DS un-setter.
     */
    protected void unsetInterruptObjectBridge(WolaInterruptObjectBridge odiBridge) {
        if (this.odiBridge == odiBridge) {
            this.odiBridge = null;
        }
    }

    /**
     * Despite the fact WOLAEndpoint has mandatory dependencies on WOLAChannelFactoryProvider and
     * LocalCommChannelFactoryProvider -- thereby guaranteeing that the factory providers are activated
     * prior to activating WOLAEndpoint -- there's still NO guarantee that the factory providers will
     * have been injected into CHFWBundle (and thus have their channel factories registered with CFW)
     * by the time WOLAEndpoint.activate runs. If they haven't, then WOLAEndpoint will fail to start
     * the chain, due to the null factory(s). To get around that problem, this method forces the
     * registration of the channel factories given by the provider. This method is called from
     * activate().
     */
    private void installChannelFactories(ChannelFactoryProvider provider) {
        for (Entry<String, Class<? extends ChannelFactory>> entry : provider.getTypes().entrySet()) {
            chfwBundle.getFramework().registerFactory(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     *
     * Bridge from WOLA JCA to go outbound to the client (regiserName) hosting the
     * given service.
     *
     * Note: I put this "bridge" code here to ensure that the WOLA component is
     * active before the bridge is made available to JCA callers.
     */
    @Override
    public byte[] jcaInvoke(String registerName,
                            String serviceName,
                            byte[] appData,
                            WolaJcaRequestInfo wolaJcaRequestInfo) throws IOException, ResourceException {

        if (wolaChannelFactoryProvider.isAttachedToWolaGroupSharedMemoryArea()) {
            byte[] outputByteArray;
            boolean incrementResult = false;
            try {
                incrementResult = wolaChannelFactoryProvider.incrementRequestCount();
                if (incrementResult == true) {
                    outputByteArray = new WolaOutboundRequest().setWolaOutboundConnMgr(wolaChannelFactoryProvider.getOutboundConnMgr()).setWolaInterruptObjectBridge(odiBridge).invoke(registerName,
                                                                                                                                                                                       serviceName,
                                                                                                                                                                                       appData,
                                                                                                                                                                                       wolaJcaRequestInfo);
                } else {
                    throw new IOException("Cannot get a WOLA connection for a client with registration name " + registerName + " and service name " + serviceName
                                          + " because the WOLA channel is stopping.");
                }
            } finally {
                if (incrementResult == true) {
                    wolaChannelFactoryProvider.decrementRequestCount();
                }
            }
            return outputByteArray;
        } else {
            throw new IOException("Cannot get a WOLA connection for a client with registration name " + registerName + " and service name " + serviceName
                                  + " because the server is not attached to shared memory. This can happen when a server is stopping.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     * @throws OTMAMessageParseException
     * @throws OTMAException
     */
    @Override
    public byte[] otmaInvoke(byte[] requestData, WolaOtmaRequestInfo requestParms) throws IOException, OTMAMessageParseException, OTMAException {

        byte[] anchor = otmaKeyMap.getOTMAAnchorKey(new OLAIMSOTMAKeyMap.Key(requestParms.getOTMAGroupID(), requestParms.getOTMAServerName(), requestParms.getOTMAClientName()));
        // if (anchor ==  null) ?

        // Parse the request bytes into segment list and segment data.
        OTMAMessageParser parser = new OTMAMessageParser();
        parser.parseOTMARequestMessage(requestData, requestParms.getOTMAReqLLZZ() == 1);
        int headerSegments[] = parser.getRequestHeaderSegments();

        if (requestParms.getOTMAMaxSegments() >= headerSegments[0]) {

            // Initiate OTMA send/receive.
            OTMASendRcvResponseData response = WOLAChannelFactoryProvider.getInstance().getWOLANativeUtils().otmaSendRcv(anchor,
                                                                                                                         headerSegments,
                                                                                                                         parser.getRequestMessageData(),
                                                                                                                         Integer.parseInt(requestParms.getOTMASyncLevel()),
                                                                                                                         requestParms.getOTMAMaxSegments(),
                                                                                                                         requestParms.getOTMAMaxRecvSize());

            // Convert OTMA response
            parser.parseOTMAResponseMessage(response.segmentList, response.segmentData, requestParms.getOTMARespLLZZ() == 1);

        } else {
            Object[] fillins = new Object[2];
            fillins[0] = Integer.toString(headerSegments[0]);
            fillins[1] = Integer.toString(requestParms.getOTMAMaxSegments());
            TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
            throw new OTMAMessageParseException(nls.getFormattedMessage("OTMA_MAX_REQ_SEGMENTS_EXCEEDED",
                                                                        fillins, "CWWKB0510E: OTMA request segment count of " + fillins[0]
                                                                                 + " exceeds the maximum allowed number of segments " + fillins[1] + " . "));

        }
        return parser.getResponseMessage();

    }
}
