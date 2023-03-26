/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
package com.ibm.ws.sib.jfapchannel.impl;

import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.Tr.exit;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.jfapchannel.impl.CommsOutboundChain.TerminationContext.BIND;
import static com.ibm.ws.sib.jfapchannel.impl.CommsOutboundChain.TerminationContext.DEACTIVATE;
import static com.ibm.ws.sib.jfapchannel.impl.CommsOutboundChain.TerminationContext.UNBIND;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.jfapchannel.richclient.impl.JFapChannelFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

import io.openliberty.netty.internal.tls.NettyTlsProvider;


@Component(
        configurationPid = "com.ibm.ws.messaging.comms.wasJmsOutbound",
        configurationPolicy = REQUIRE,
        property = "service.vendor=IBM")
public class CommsOutboundChain implements ApplicationPrereq {
    private static final TraceComponent tc = Tr.register(CommsOutboundChain.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);
    private final static String OUTBOUND_CHAIN_CONFIG_ALIAS = "wasJmsOutbound";

    private final ChannelConfiguration tcpOptions;
    private final CommsClientServiceFacade commsClientService;
    private final String chainName;
    private final String tcpChannelName;
    private final String jfapChannelName;
    private final String sslChannelName;
    private OutboundSecureFacet secureFacet;
    
    private NettyTlsProvider nettyTlsProvider;

    /** If useSSL is set to true in the outbound connection configuration */
    private final boolean isSecureChain;
    /** If useNettyTransport is set to true in the outbound connection configuration */
    private boolean useNettyTransport = false;
    
    
    private static Map<String, CommsOutboundChain> chainList = new HashMap<String,CommsOutboundChain>();


    // TODO: Check if this can be removed in favor of using requireService
    public static CommsOutboundChain getChainDetails(String chainName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getChainDetails");

        CommsOutboundChain chain = chainList.get(chainName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getChainDetails", chain);
        return chain;
    }


    @Activate
    public CommsOutboundChain(
            @Reference(name="tcpOptions", target="(id=unbound)")
            ChannelConfiguration tcpOptions,
            @Reference(name="commsClientService")
            CommsClientServiceFacade commsClientService,
//            @Reference(name="tlsProviderService")
//            NettyTlsProvider tlsProviderService,
            /* Require SingletonsReady so that we will wait for it to ensure its availability at least until the chain is deactivated. */ 
            @Reference(name="singletonsReady")
            SingletonsReady singletonsReady,
            Map<Object, Object> properties) {

        if (isAnyTracingEnabled() && tc.isEntryEnabled())
            entry(this, tc, "<init>", tcpOptions, commsClientService, properties);

        this.tcpOptions = tcpOptions;
        this.commsClientService = commsClientService;
//        this.tlsProviderService = tlsProviderService;

        isSecureChain = MetatypeUtils.parseBoolean(OUTBOUND_CHAIN_CONFIG_ALIAS, "useSSL", properties.get("useSSL"), false);
        
        useNettyTransport = ProductInfo.getBetaEdition() ? 
				MetatypeUtils.parseBoolean(OUTBOUND_CHAIN_CONFIG_ALIAS, "useNettyTransport", properties.get("useNettyTransport"), true):
				false;

        String id = (String) properties.get("id");
        chainName = id;
        tcpChannelName = id + "_JfapTcp";
        sslChannelName = id + "_JfapSsl";
        jfapChannelName = id + "_JfapJfap";
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "activate: Using " + (useNettyTransport ? "Netty" : "Channel") + " Framework for chain ", chainName);

        if (isSecureChain) {
            // defer creation until SSL decides to show up
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsOutboundChain: Deferring secure chain startup until SSL configuration is available", chainName);
        } else {
            createBasicJFAPChain();
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "<init>");
    }

    @Reference(name = "secureFacet", cardinality = OPTIONAL, policy = DYNAMIC, policyOption = GREEDY, unbind = "unbindSecureFacet")
    void bindSecureFacet(OutboundSecureFacet newFacet) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "bindSecureFacet", newFacet);
        if (isSecureChain) {
            terminateConnectionsAssociatedWithChain(BIND, null, newFacet);
            createSecureJFAPChain(newFacet);
        } else {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "Ignoring SecureFacet bind because useSSL was false");
        }
    }

    void unbindSecureFacet(OutboundSecureFacet oldFacet) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "unbindSecureFacet", oldFacet);
        if (isSecureChain) {
            terminateConnectionsAssociatedWithChain(UNBIND, oldFacet, null);
        } else {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "Ignoring SecureFacet unbind because useSSL was false");
        }
    }

    public Map<String, Object> getTcpOptions() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(this, tc, "getTcpOptions");
        Map<String, Object> tcpProps = tcpOptions.getConfiguration();
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "getTcpOptions", tcpProps);
        return tcpProps;
    }
    
    @Reference(name = "nettyTlsProvider", cardinality = OPTIONAL, policyOption = GREEDY, unbind = "unbindTlsProviderService")
    void bindNettyTlsProviderService(NettyTlsProvider tlsProvider) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(this, tc, "bindTlsProviderService", tlsProvider);
        this.nettyTlsProvider = tlsProvider;
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "bindTlsProviderService");
    }
    
    void unbindTlsProviderService(NettyTlsProvider oldService) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "unbindTlsProviderService", oldService);
        // TODO: Figure out if there's something to be done here
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "unbindTlsProviderService");
    }
    
    public NettyTlsProvider getNettyTlsProvider() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(this, tc, "getNettyTlsProvider");
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "getNettyTlsProvider", nettyTlsProvider);
        return nettyTlsProvider;
    }
    
    private synchronized void createBasicJFAPChain() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "createBasicJFAPChain", chainName);
        if(!useNetty()) {
        	// Use Channel Framework for transport
        	try {
                ChannelFramework cfw = commsClientService.getChannelFramework();
                cfw.registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

                Map<String, Object> tcpOptions = getTcpOptions();

                ChannelData tcpChannel = cfw.getChannel(tcpChannelName);

                if (tcpChannel == null) {
                    String typeName = (String) tcpOptions.get("type");
                    tcpChannel = cfw.addChannel(tcpChannelName, cfw.lookupFactory(typeName), new HashMap<Object, Object>(tcpOptions));
                }

                ChannelData jfapChannel = cfw.getChannel(jfapChannelName);
                if (jfapChannel == null)
                    jfapChannel = cfw.addChannel(jfapChannelName, cfw.lookupFactory("JFapChannelOutbound"), null);

                final String[] chanList = { jfapChannelName, tcpChannelName };

                ChainData cd = cfw.addChain(chainName, FlowType.OUTBOUND, chanList);
                cd.setEnabled(true);

                // The Fat test:
                // /com.ibm.ws.messaging_fat/fat/src/com/ibm/ws/messaging/fat/CommsWithSSL/CommsWithSSLTest.java
                // Checks for the presence of this string in the trace log, in order to ascertain that the chain has been enabled.
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "JFAP Outbound chain"+ chainName + " successfully started ");
                chainList.put(chainName, this);
            } catch (ChannelException | ChainException exception) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "JFAP Outbound chain " + chainName + " failed to start, exception ="+exception);
            }
        }else {
        	// Use Netty Framework for transport
            chainList.put(chainName, this);
        }
        
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "createBasicJFAPChain");
    }

    private synchronized void createSecureJFAPChain(OutboundSecureFacet secureFacet) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "createSecureJFAPChain", chainName);
        if(!useNetty()) {
        	// Use Channel Framework for transport
        	try {
                ChannelFramework cfw = commsClientService.getChannelFramework();
                cfw.registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

                Map<String, Object> tcpOptions = getTcpOptions();

                ChannelData tcpChannel = cfw.getChannel(tcpChannelName);

                if (tcpChannel == null) {
                    String typeName = (String) tcpOptions.get("type");
                    tcpChannel = cfw.addChannel(tcpChannelName, cfw.lookupFactory(typeName), new HashMap<Object, Object>(tcpOptions));
                }

                if (null == secureFacet) throw new ChainException(new Throwable(nls.getFormattedMessage("missingSslOptions.ChainNotStarted", new Object[] { chainName }, "Chain not started " + chainName)));

                ChannelData sslChannel = cfw.getChannel(sslChannelName);
                if (sslChannel == null) {
                    sslChannel = cfw.addChannel(sslChannelName, cfw.lookupFactory("SSLChannel"), secureFacet.copyConfig());
                }

                ChannelData jfapChannel = cfw.getChannel(jfapChannelName);
                if (jfapChannel == null) {
                    jfapChannel = cfw.addChannel(jfapChannelName, cfw.lookupFactory("JFapChannelOutbound"), null);
                }

                final String[] chanList = { jfapChannelName, sslChannelName, tcpChannelName };

                ChainData cd = cfw.addChain(chainName, FlowType.OUTBOUND, chanList);
                cd.setEnabled(true);

                // The Fat test:
                // /com.ibm.ws.messaging_fat/fat/src/com/ibm/ws/messaging/fat/CommsWithSSL/CommsWithSSLTest.java
                // Checks for the presence of this string in the trace log, in order to ascertain that the chain has been enabled.
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "JFAP Outbound secure chain" + chainName + " successfully started ");
                chainList.put(chainName, this);
            } catch (ChannelException | ChainException exception) {
                FFDCFilter.processException(exception, "CommsOutboundChain.createSecureJFAPChain for chain " + this.chainName, jfapChannelName, this);
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "JFAP Outbound secure chain " + chainName + " failed to start, exception ="+exception);
            }
        }else {
        	// Use Netty Framework for transport
        	// TODO: Verify Dynamic updates with SSL on Netty
            chainList.put(chainName, this);
        }
        
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "createSecureJFAPChain");
    }

    @Deactivate
    protected void deactivate() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "deactivate");
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsOutboundChain: Destroying " + (isSecureChain ? "Secure" : "Non-Secure") + " chain ", chainName);
        terminateConnectionsAssociatedWithChain(DEACTIVATE, null, null);
        // TODO: Check about having this in both deactivate and terminate connections
        chainList.put(chainName, this);
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this,tc, "deactivate");
    }

    enum TerminationContext { BIND, UNBIND, DEACTIVATE }

    /**
     * Terminate all of the connections associated with the chain, then remove the chain from the channel framework.
     */
    private synchronized void terminateConnectionsAssociatedWithChain(final TerminationContext caller, final OutboundSecureFacet oldFacet, final OutboundSecureFacet newFacet) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(this, tc, "terminateConnectionsAssociatedWithChain", chainName, caller, oldFacet, newFacet, secureFacet);

        // check preconditions
        assert DEACTIVATE == caller || isSecureChain;
        assert UNBIND == caller || oldFacet == null;
        assert BIND == caller || newFacet == null;
        
        if (isSecureChain) {
            // For the secure chain this method can be called from the bind, unbind, *and* deactivate methods.
            // Ensure the work only happens once per secure facet.
            final boolean alreadyTerminated;
            if (caller == UNBIND) {
                if (oldFacet == secureFacet) {
                    // do the unbind and the cleanup
                    secureFacet = null;
                    alreadyTerminated = false;
                } else {
                    // cleaned up from earlier bind/deactivate call
                    alreadyTerminated = true;
                }
            } else {
                // clean up only if old facet was not null
                alreadyTerminated = null == secureFacet;
                // always set the new facet (which is null for DEACTIVATE)
                secureFacet = newFacet;
            }
            if (alreadyTerminated) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Already terminated chain - returning early");
                return;
            }
        }
        
        if(useNetty()) {
        	try {
                // try to close existing connections
                OutboundConnectionTracker oct = ClientConnectionManager.getRef().getOutboundConnectionTracker();
                if (null != oct) oct.terminateConnectionsAssociatedWithChain(chainName);
            } catch (Exception exception) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Failure in terminating conversations and physical connections while destroying chain : " + chainName, exception);
            }
        }else {
        	try {
                ChannelFramework cfw = commsClientService.getChannelFramework();
                // try to close existing connections
                OutboundConnectionTracker oct = ClientConnectionManager.getRef().getOutboundConnectionTracker();
                if (null != oct) oct.terminateConnectionsAssociatedWithChain(chainName);
                // if the chain exists destroy it (via the VCF because CFW does not support destroying outbound chains directly)
                if (null != cfw.getChain(chainName)) cfw.getOutboundVCFactory(chainName).destroy();

                ChainData cd = cfw.getChain(chainName);
                if (null != cd) cfw.removeChain(cd);

            } catch (Exception exception) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Failure in terminating conversations and physical connections while destroying chain : " + chainName, exception);
            }
            removeChannel(tcpChannelName);
            if (isSecureChain) removeChannel(sslChannelName);
            removeChannel(jfapChannelName);
        }
        
        
        chainList.remove(chainName);

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "terminateConnectionsAssociatedWithChain");
    }

    /**
     * Remove a channel from the channel framework.
     * @param channelName of the channel to be removed.
     */
    private void removeChannel(String channelName) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeChannel", channelName);

        ChannelFramework cfw = commsClientService.getChannelFramework();
        try {
            if (cfw.getChannel(channelName) != null)
                cfw.removeChannel(channelName);
        } catch (ChannelException | ChainException exception) {
            // Neither of these exceptions are permanent failures. 
            // They usually indicate that we're the looser of a race with the channel framework to
            // tear down the chain. For example, the SSL feature was removed.
            if (isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Error removing channel:" + channelName, exception);
        }

        if (isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeChannel");
    }


    @Override
    public String getApplicationPrereqID() {
        return chainName;
    }
    
    
    /**
	 * Query if Netty has been enabled for this endpoint
	 * @return true if Netty should be used for this endpoint
	 */
	public boolean useNetty() {
	    return useNettyTransport;
	}
	
	/**
	 * Query if useSSL has been enabled for this endpoint
	 * @return true if useSSL is enabled and SSL should be used for this endpoint
	 */
	public boolean isSecureChain() {
	    return isSecureChain;
	}
	
	/**
	 * Get the SSL Options for this Chain
	 * @return the OutboundSecureFacet wrapper of SSL Options for the chain
	 */
	public OutboundSecureFacet getSecureFacet() {
	    return secureFacet;
	}

    
}
