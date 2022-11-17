/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.impl;

import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.Tr.exit;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.sib.jfapchannel.ClientConnectionManager;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.impl.octracker.OutboundConnectionTracker;
import com.ibm.ws.sib.jfapchannel.richclient.impl.JFapChannelFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

@Component(
        name = "com.ibm.ws.messaging.comms.wasJmsOutbound",
        configurationPolicy = REQUIRE,
        property = "service.vendor=IBM")
public class CommsOutboundChain {
    private static final TraceComponent tc = Tr.register(CommsOutboundChain.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);
    private final static String OUTBOUND_CHAIN_CONFIG_ALIAS = "wasJmsOutbound";

    private final ChannelConfiguration tcpOptions;
    private final ChannelConfiguration sslOptions;
    private final CommsClientServiceFacade commsClientService;
    private final String chainName;
    private final String tcpChannelName;
    private final String jfapChannelName;
    private final String sslChannelName;

    /** If useSSL is set to true in the outbound connection configuration */
    private final boolean isSSLChain;

    @Activate
    public CommsOutboundChain(
            @Reference(name="tcpOptions", target="(id=unbound)")
            ChannelConfiguration tcpOptions,
            @Reference(name="sslOptions", target="(id=unbound)", cardinality=OPTIONAL)
            ChannelConfiguration sslOptions,
            /* We have preserved the original behaviour of using defaultSSLOptions 
             * If we want to use ${defaultSSLVar}, use (id=unbound) here and set it in metatype */
            @Reference(name="defaultSSLOptions", target="(id=defaultSSLOptions)", cardinality=OPTIONAL)
            ChannelConfiguration defaultSSLOptions,
            @Reference(name="commsClientService")
            CommsClientServiceFacade commsClientService,
            /* Require SingletonsReady so that we will wait for it to ensure its availability at least until the chain is deactivated. */ 
            @Reference(name="singletonsReady")
            SingletonsReady singletonsReady,
            Map<Object, Object> properties) {

        if (isAnyTracingEnabled() && tc.isEntryEnabled())
            entry(this, tc, "<init>", tcpOptions,/* defaultTCPOptions, */sslOptions, defaultSSLOptions, commsClientService, properties);

        //this.tcpOptions = Optional.ofNullable(tcpOptions).orElse(defaultTCPOptions);
        this.tcpOptions = tcpOptions;
        this.sslOptions = Optional.ofNullable(sslOptions).orElse(defaultSSLOptions);
        this.commsClientService = commsClientService;

        isSSLChain = MetatypeUtils.parseBoolean(OUTBOUND_CHAIN_CONFIG_ALIAS, "useSSL", properties.get("useSSL"), false);

        String id = (String) properties.get("id");
        chainName = id;
        tcpChannelName = id + "_JfapTcp";
        sslChannelName = id + "_JfapSsl";
        jfapChannelName = id + "_JfapJfap";

        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsOutboundChain: Creating " + (isSSLChain ? "Secure" : "Non-Secure") + " chain ", chainName);
        createJFAPChain();
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "<init>");
    }

    private Map<String, Object> getTcpOptions() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(this, tc, "getTcpOptions");
        Map<String, Object> tcpProps = tcpOptions.getConfiguration();
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "getTcpOptions", tcpProps);
        return tcpProps;
    }

    private synchronized void createJFAPChain() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "createJFAPChain", chainName, isSSLChain);
        try {
            ChannelFramework cfw = commsClientService.getChannelFramework();
            cfw.registerFactory("JFapChannelOutbound", JFapChannelFactory.class);

            Map<String, Object> tcpOptions = getTcpOptions();

            ChannelData tcpChannel = cfw.getChannel(tcpChannelName);

            if (tcpChannel == null) {
                String typeName = (String) tcpOptions.get("type");
                tcpChannel = cfw.addChannel(tcpChannelName, cfw.lookupFactory(typeName), new HashMap<Object, Object>(tcpOptions));
            }

            if (isSSLChain) {
                // Now we are actually trying to create an ssl channel in the chain. Which requires sslOptions and that isSSLEnabled is true.
                if (sslOptions == null) {			
                    throw new ChainException(new Throwable(nls.getFormattedMessage("missingSslOptions.ChainNotStarted", new Object[] { chainName }, null)));              
                }
                Map<String, Object> sslprops = sslOptions.getConfiguration();

                ChannelData sslChannel = cfw.getChannel(sslChannelName);
                if (sslChannel == null) {
                    sslChannel = cfw.addChannel(sslChannelName, cfw.lookupFactory("SSLChannel"), new HashMap<Object, Object>(sslprops));
                }
            }

            ChannelData jfapChannel = cfw.getChannel(jfapChannelName);
            if (jfapChannel == null)
                jfapChannel = cfw.addChannel(jfapChannelName, cfw.lookupFactory("JFapChannelOutbound"), null);

            final String[] chanList;
            chanList = isSSLChain ? new String[] { jfapChannelName, sslChannelName, tcpChannelName } : new String[] { jfapChannelName, tcpChannelName };

            ChainData cd = cfw.addChain(chainName, FlowType.OUTBOUND, chanList);
            cd.setEnabled(true);

            // The Fat test:
            // /com.ibm.ws.messaging_fat/fat/src/com/ibm/ws/messaging/fat/CommsWithSSL/CommsWithSSLTest.java
            // Checks for the presence of this string in the trace log, in order to ascertain that the chain has been enabled.
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc,
                    (isSSLChain ? "JFAP Outbound secure chain" : "JFAP Outbound chain")
                    + chainName + " successfully started ");

        } catch (ChannelException | ChainException exception) {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "JFAP Outbound chain " + chainName + " failed to start, exception ="+exception);
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "createJFAPChain");
    }

    @Deactivate
    protected void deactivate() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "deactivate");
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsOutboundChain: Destroying " + (isSSLChain ? "Secure" : "Non-Secure") + " chain ", chainName);
        terminateConnectionsAssociatedWithChain();
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this,tc, "deactivate");
    }

    /**
     * Terminate all of the connections associated with the chain, then remove the chain from the channel framework.
     * 
     * @throws Exception
     */
    private synchronized void terminateConnectionsAssociatedWithChain() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.entry(this, tc, "terminateConnectionsAssociatedWithChain", chainName);

        try {
            ChannelFramework cfw = commsClientService.getChannelFramework();
            OutboundConnectionTracker oct = ClientConnectionManager.getRef().getOutboundConnectionTracker();
            if (oct == null) {
                // if we dont have any oct that means there were no connections established

                if (cfw.getChain(chainName) != null) {// see if chain exist only then destroy
                    // we have to destroy using vcf because cfw does not allow to destroy outbound 
                    // chains directly using cfw.destroyChain(chainname)
                    // as it is valid only for inbound chains as of now
                    cfw.getOutboundVCFactory(chainName).destroy();
                }
            } else {
                oct.terminateConnectionsAssociatedWithChain(chainName);
            }
            ChainData cd = cfw.getChain(chainName);
            if (cd != null) {
                cfw.removeChain(cd);
            }

        } catch (Exception exception) {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Failure in terminating conservations and physical connections while destroying chain : " + chainName, exception);
        }

        removeChannel(tcpChannelName);
        if (isSSLChain)
            removeChannel(sslChannelName);
        removeChannel(jfapChannelName);

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
}
