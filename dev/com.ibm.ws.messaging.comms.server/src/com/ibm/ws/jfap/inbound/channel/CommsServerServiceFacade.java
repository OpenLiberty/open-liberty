/*
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jfap.inbound.channel;

import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.Tr.event;
import static com.ibm.websphere.ras.Tr.register;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.messaging.lifecycle.SingletonsReady.requireService;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static com.ibm.ws.sib.utils.ras.SibTr.exit;
import static com.ibm.wsspi.kernel.service.utils.MetatypeUtils.parseBoolean;
import static com.ibm.wsspi.kernel.service.utils.MetatypeUtils.parseInteger;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.event.EventEngine;
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
    private static final TraceComponent tc = register(CommsServerServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private String endpointName = null;

    private final CommsInboundChain inboundBasicChain = new CommsInboundChain(this, false);
    private final CommsInboundChain inboundSecureChain = new CommsInboundChain(this, true);

    private int basicPort;
    private int securePort;
    private String host = null;
    private boolean iswasJmsEndpointEnabled = true;

    private final JsAdminService jsAdminService;

    private final CHFWBundle chfw;
    private final ChannelConfiguration tcpOptions;
    private final AtomicReference<InboundSecureFacet> secureFacetRef = new AtomicReference<>();
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
            Map<String, Object> properties) {
        final String methodName = "<init>";
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, methodName, new Object[]{jsAdminService, chfw, tcpOptions, eventEngine, properties});

        this.jsAdminService = jsAdminService;
        this.chfw = chfw;
        this.tcpOptions = tcpOptions;
        this.eventEngine = eventEngine;

        Object cid = properties.get(ComponentConstants.COMPONENT_ID);

        endpointName = (String) properties.get("id");
        if (endpointName == null)
            endpointName = CONFIG_ALIAS + cid;

        // Allowing JFAP to accept incoming connections.
        ServerConnectionManager.initialise(chfw.getFramework());

        //Go ahead and Register JFAPChannel with Channel Framework by providing JFAPServerInboundChannelFactory
        chfw.getFramework().registerFactory("JFAPChannel", JFAPServerInboundChannelFactory.class);

        this.inboundBasicChain.init(endpointName, chfw);
        this.inboundSecureChain.init(endpointName + "-ssl", chfw);

        this.iswasJmsEndpointEnabled = parseBoolean(CONFIG_ALIAS, "enabled", properties.get("enabled"), true);
        this.host = (String) properties.get("host");
        this.basicPort = parseInteger(CONFIG_ALIAS, BASIC_PORT, properties.get(BASIC_PORT), -1);
        this.securePort = parseInteger(CONFIG_ALIAS, SECURE_PORT, properties.get(SECURE_PORT), -1);

        if (basicPort >= 0) inboundBasicChain.enable(true);

        if (iswasJmsEndpointEnabled) {
            factotum.updateBasicChain();
        } else {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "wasjmsEndpoint disabled: .. stopping chains");
            factotum.stopBasicChain(false);
        }

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, methodName);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason) {
        if (isAnyTracingEnabled() && tc.isEventEnabled()) event(tc, "CommsServerServiceFacade deactivated, reason=" + reason);
        factotum.stopBasicChain(true);
    }

    @Reference(name = "secureFacet", cardinality = OPTIONAL, policy = DYNAMIC, policyOption = GREEDY, unbind = "unbindSecureFacet")
    void bindSecureFacet(InboundSecureFacet facet) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "bindSecureFacet", facet);
        if (securePort >= 0 && facet.areSecureSocketsEnabled()) inboundSecureChain.enable(true);
        synchronized (factotum) {
            this.secureFacetRef.set(facet);
            factotum.updateSecureChain();
        }
    }

    void unbindSecureFacet(InboundSecureFacet facet) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "unbindSecureFacet", facet);
        synchronized (factotum) {
            if (this.secureFacetRef.compareAndSet(facet, null))
                factotum.stopSecureChain(); // only stop the chain if facet was the last bound facet
        }
    }

    private final class SynchronizedActions {
        boolean deactivated;

        synchronized void updateBasicChain() {
            if (iswasJmsEndpointEnabled) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: updating basic chain ", inboundBasicChain);
                try {
                    inboundBasicChain.update();
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

            if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(tc, "closeViaCommsMPConnection");
        }

        synchronized void stopBasicChain(boolean deactivate) {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: stopping basic chain ", inboundBasicChain);
            try {
                inboundBasicChain.stop();
            } catch (Exception e) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in stopping basic chain", e);
            }

            deactivated = deactivate;
        }

        synchronized void stopSecureChain() {
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: stopping secure chain ", inboundSecureChain);
            try {
                inboundSecureChain.stop();
            } catch (Exception e) {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in secure chain stopping", e);
            }
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
        return Optional.of(secureFacetRef)
            .map(ref -> ref.get())
            .filter(f -> f.areSecureSocketsEnabled())
            .map(f -> f.getOptions())
            .map(o -> o.getConfiguration())
            .orElseGet(() -> {
                if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "getSslOptions() returning NULL");
                return null;
            });
    }

    public static TrmMessageFactory getTrmMessageFactory() { return TrmMessageFactory.getInstance(); }
    public static JsAdminService getJsAdminService() { return CommonServiceFacade.getJsAdminService(); }
    public EventEngine getEventEngine() { return eventEngine; }
    int getBasicPort() { return basicPort; }
    int getSecurePort() { return securePort; }
    String getHost() { return host; }
}
