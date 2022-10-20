/*
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jfap.inbound.channel;

import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.messaging.lifecycle.SingletonsReady.requireService;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.wsspi.kernel.service.utils.MetatypeUtils.parseBoolean;
import static com.ibm.wsspi.kernel.service.utils.MetatypeUtils.parseInteger;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.server.ServerConnectionManager;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelConfiguration;

/**
 * This is a messaging {@link Singleton}.
 * It will be required by the {@link SingletonsReady} component
 * once all configured singletons are available.
 */
@Component(
        configurationPid = "com.ibm.ws.messaging.comms.server",
        configurationPolicy = REQUIRE,
        property = {"type=messaging.comms.server.service", "service.vendor=IBM"})
public class CommsServerServiceFacade implements Singleton {
    private static final String SECURE_PORT = "wasJmsSSLPort";
    private static final String BASIC_PORT = "wasJmsPort";
    private static final String CONFIG_ALIAS = "wasJmsEndpoint";
    private static final TraceComponent tc = Tr.register(CommsServerServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private String endpointName = null;

    private final CommsInboundChain inboundChain = new CommsInboundChain(this, false);
    private final CommsInboundChain inboundSecureChain = new CommsInboundChain(this, true);

    private int basicPort;
    private int securePort;
    private String host = null;
    private boolean iswasJmsEndpointEnabled = true;

    private final JsAdminService jsAdminService;

    private final CHFWBundle chfw;
    private final ChannelConfiguration tcpOptions;
    private final SecureFacet secureFacet;
    private final EventEngine eventEngine;

    /** Lock to guard chain actions (update,stop and sslOnlyStop).. as of now as all chain actions are executed by SCR thread */
    private final SynchronizedActions factotum = new SynchronizedActions();

    @Activate
    public CommsServerServiceFacade (
            @Reference(name = "jsAdminService")
            JsAdminService jsAdminService,
            @Reference(name = "chfw")
            CHFWBundle chfw,
            @Reference(name = "tcpOptions", target = "(id=unbound)") // target to be overwritten by metatype
            ChannelConfiguration tcpOptions,
            @Reference(name = "eventEngine")
            EventEngine eventEngine,
            @Reference(name = "secureFacet")
            SecureFacet secureFacet,
            Map<String, Object> properties) {
        final String methodName = "<init>";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, methodName, new Object[]{jsAdminService, chfw, tcpOptions, eventEngine, properties});

        this.jsAdminService = jsAdminService;
        this.chfw = chfw;
        this.tcpOptions = tcpOptions;
        this.secureFacet = secureFacet;
        this.eventEngine = eventEngine;

        Object cid = properties.get(ComponentConstants.COMPONENT_ID);

        endpointName = (String) properties.get("id");
        if (endpointName == null)
            endpointName = CONFIG_ALIAS + cid;

        // Allowing JFAP to accept incoming connections.
        ServerConnectionManager.initialise(chfw.getFramework());

        //Go ahead and Register JFAPChannel with Channel Framework by providing JFAPServerInboundChannelFactory
        chfw.getFramework().registerFactory("JFAPChannel", JFAPServerInboundChannelFactory.class);

        this.inboundChain.init(endpointName, chfw);
        this.inboundSecureChain.init(endpointName + "-ssl", chfw);

        this.iswasJmsEndpointEnabled = parseBoolean(CONFIG_ALIAS, "enabled", properties.get("enabled"), true);
        this.host = (String) properties.get("host");
        this.basicPort = parseInteger(CONFIG_ALIAS, BASIC_PORT, properties.get(BASIC_PORT), -1);
        this.securePort = parseInteger(CONFIG_ALIAS, SECURE_PORT, properties.get(SECURE_PORT), -1);

        if (basicPort >= 0) inboundChain.enable(true);

        if (securePort >= 0 && secureFacet.areSecureSocketsEnabled()) inboundSecureChain.enable(true);

        if (iswasJmsEndpointEnabled) {
            factotum.updateBasicChain();
            factotum.updateSecureChain();
        } else {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "wasjmsEndpoint disabled: .. stopping chains");
            factotum.stopChains(false);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(this, tc, "Activate");
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) Tr.event(tc, "CommsServerServiceFacade deactivated, reason=" + reason);
        factotum.stopChains(true);
    }

    private final class SynchronizedActions {
        boolean deactivated;

        synchronized void updateBasicChain() {
            if (iswasJmsEndpointEnabled) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: updating basic chain ", inboundChain);
                try {
                    inboundChain.update();
                } catch (Exception e) {
                    if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in updating basic chain", e);
                }
            }
        }

        synchronized void updateSecureChain() {
            if (iswasJmsEndpointEnabled) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: updating secure chain ", inboundSecureChain);
                try {
                    inboundSecureChain.update();
                } catch (Exception e) {
                    if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in updating secure chain", e);
                }

            }
        }

        synchronized void closeViaCommsMPConnection(int mode) {
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, "CommsServerServiceFacade.SynchronizedActions closeViaCommsMPConnection", deactivated, mode);

            // We can only rely on jsAdminService until deactivation.
            if (deactivated)
                return;

            // Liberty AdminService returns the ME which is running in-process. No search filter is used.
            JsMessagingEngine local_ME = jsAdminService.getMessagingEngine(JsConstants.DEFAULT_BUS_NAME, JsConstants.DEFAULT_ME_NAME);
            if (null != local_ME) {
                JsEngineComponent _mp = local_ME.getMessageProcessor();
                if (null != _mp) { //_mp can not be NULL. But checking it.
                    _mp.stop(mode);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) Tr.exit(tc, "closeViaCommsMPConnection");
        }


        synchronized void stopChains(boolean deactivate) {
            //TODO Would it be better to stop the chains in the inverse order to startup?
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: stopping basic chain ", inboundChain);
            try {
                inboundChain.stop();
            } catch (Exception e) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in stopping basic chain", e);
            }

            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: stopping secure chain ", inboundSecureChain);
            try {
                inboundSecureChain.stop();
            } catch (Exception e) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in secure chain stopping", e);
            }

            deactivated = deactivate;
        }
    }

    void closeViaCommsMPConnections(int mode) {
        factotum.closeViaCommsMPConnection(mode);
    }

    /**
     * Access the current reference to the bytebuffer pool manager.
     *
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getBufferPoolManager() {
        return requireService(CommsServerServiceFacade.class).chfw.getBufferManager();
    }

    Map<String, Object> getTcpOptions() {
        return tcpOptions.getConfiguration();
    }

    Map<String, Object> getSslOptions() {
        if (null != secureFacet) return secureFacet.getOptions().getConfiguration();
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "getSslOptions() returning NULL");
        return null;
    }

    public static TrmMessageFactory getTrmMessageFactory() { return TrmMessageFactory.getInstance(); }
    public static JsAdminService getJsAdminService() { return CommonServiceFacade.getJsAdminService(); }
    public EventEngine getEventEngine() { return eventEngine; }
    int getBasicPort() { return basicPort; }
    int getSecurePort() { return securePort; }
    String getHost() { return host; }
}
