/*******************************************************************************
 * Copyright (c) 2011, 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jfap.inbound.channel;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.comms.server.AcceptListenerFactoryImpl;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.server.ServerConnectionManager;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelConfiguration;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

/**
 * 
 * Start JFAP chain and Secure chain in-line in the context of SCR thread because in the design discussions with Alasdair,
 * it was decided that by the time Liberty profile server started, messaging has to be ready for send/receive
 * this can happen only if chains are started in the context of SCR thread
 * 
 */
public class CommsServerServiceFacade {
    private static final TraceComponent tc = Tr.register(CommsServerServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private final static String Inbound_ConfigAlias = "wasJmsEndpoint";

    private String endpointName = null;

    private final CommsInboundChain inboundChain = new CommsInboundChain(this, false);
    private final CommsInboundChain inboundSecureChain = new CommsInboundChain(this, true);

    private int wasJmsPort;
    private String host = null;
    private int wasJmsSSLPort;
    private boolean iswasJmsEndpointEnabled = true;

    private static CHFWBundle _chfw_bunlde;
    private static final AtomicServiceReference<CHFWBundle> _chfwRef = new AtomicServiceReference<CHFWBundle>("chfwBundle");

    private static final AtomicServiceReference<ChannelConfiguration> _tcpOptionsRef = new AtomicServiceReference<ChannelConfiguration>("tcpOptions");

    /** Optional, dynamic reference to an SSL channel factory provider: could be used to start/stop SSL chains */
    private static final AtomicServiceReference<ChannelFactoryProvider> _sslFactoryProviderRef = new AtomicServiceReference<ChannelFactoryProvider>("sslSupport");
    private static final AtomicServiceReference<ChannelConfiguration> _sslOptionsRef = new AtomicServiceReference<ChannelConfiguration>("sslOptions");

    private static final AtomicServiceReference<ChannelConfiguration> _commsClientServiceRef = new AtomicServiceReference<ChannelConfiguration>("commsClientService");
    private static final AtomicServiceReference<CommonServiceFacade> _commonServiceFacadeRef = new AtomicServiceReference<CommonServiceFacade>("commonServiceFacade");

    private static final AtomicServiceReference<EventEngine> _eventSvcRef = new AtomicServiceReference<EventEngine>("eventService");

    private volatile Map<String, Object> Config = null;

    /** Lock to guard chain actions (update,stop and sslOnlyStop).. as of now as all chain actions are executed by SCR thread */
    private final Object actionLock = new Object();

    public void activate(Map<String, Object> properties, ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "Activate ", properties);

        Config = properties;
        Object cid = Config.get(ComponentConstants.COMPONENT_ID);

        endpointName = (String) properties.get("id");
        if (endpointName == null)
            endpointName = Inbound_ConfigAlias + cid;

        _chfwRef.activate(context);
        _tcpOptionsRef.activate(context);
        _sslOptionsRef.activate(context);
        _sslFactoryProviderRef.activate(context);

        _commonServiceFacadeRef.activate(context);
        _commsClientServiceRef.activate(context);
        _eventSvcRef.activate(context);

        _chfw_bunlde = getCHFWBundle();

        // Allowing JFAP to accept incoming connections. 
        ServerConnectionManager.initialise(new AcceptListenerFactoryImpl());

        //Go ahead and Register JFAPChannel with Channel Framework by providing JFAPServerInboundChannelFactory
        _chfw_bunlde.getFramework().registerFactory("JFAPChannel", JFAPServerInboundChannelFactory.class);

        inboundChain.init(endpointName, getCHFWBundle());
        inboundSecureChain.init(endpointName + "-ssl", getCHFWBundle());

        modified(context, properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "Activate");
    }

    protected void deactivate(ComponentContext ctx, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "CommsServerServiceFacade deactivated, reason=" + reason);
        //First make Config NULL
        Config = null;

        performAction(stopBasicChainAction);
        performAction(stopSSLChainAction);

        _chfwRef.deactivate(ctx);
        _tcpOptionsRef.deactivate(ctx);
        _sslOptionsRef.deactivate(ctx);
        _sslFactoryProviderRef.deactivate(ctx);

        _commonServiceFacadeRef.deactivate(ctx);
        _commsClientServiceRef.deactivate(ctx);
        _eventSvcRef.deactivate(ctx);

    }

    protected void modified(ComponentContext context,
                            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "modified", properties);

        iswasJmsEndpointEnabled = MetatypeUtils.parseBoolean(Inbound_ConfigAlias, "enabled",
                                                             properties.get("enabled"),
                                                             true);

        host = (String) Config.get("host");

        wasJmsPort = MetatypeUtils.parseInteger(Inbound_ConfigAlias, "wasJmsPort",
                                                properties.get("wasJmsPort"),
                                                -1);

        wasJmsSSLPort = MetatypeUtils.parseInteger(Inbound_ConfigAlias, "wasJmsSSLPort",
                                                   properties.get("wasJmsSSLPort"),
                                                   -1);

        Config = properties;

        if (wasJmsPort >= 0)
            inboundChain.enable(true);

        if ((wasJmsSSLPort >= 0) && (_sslFactoryProviderRef.getService() != null))
            inboundSecureChain.enable(true);

        if (iswasJmsEndpointEnabled) {
            performAction(updateBasicChainAction);
            performAction(updateSSLChainAction);
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "wasjmsEndpoint disabled: .. stopping chains");

            performAction(stopBasicChainAction);
            performAction(stopSSLChainAction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "modified");
    }

    // Four Runnable actions (i.e stopBasicChain,stopSecureChain,updateBasicChain and updateSecureChain). However all these run() are called in-line ( not from other thread) in SCR action thread
    //in-line means in the context of DS functions (i.e activate,deactivate,modify, set/unset )

    private void performAction(Runnable action) {
        // As we are running in SCR action thread.. just calling action.run()
        // depending on the need, in future this may be modified to use a executor service. 
        action.run();
    }

    private final Runnable stopBasicChainAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "CommsServerServiceFacade: stoppin basic chain ", inboundChain);

                //Catch any unchecked/uncaught exceptions so that it would not harm the code flow
                try {
                    inboundChain.stop();
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Exception in stopping Basic chain", e);
                }
            }
        }
    };

    private final Runnable stopSSLChainAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "CommsServerServiceFacade: stopping secure chain ", inboundSecureChain);

                //Catch any unchecked/uncaught exceptions so that it would not harm the code flow
                try {
                    inboundSecureChain.stop();
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Exception in secure chain stopping", e);
                }
            }
        }
    };

    private final Runnable updateSSLChainAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                //only do in case if Activate is called.
                if ((Config != null) && iswasJmsEndpointEnabled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "CommsServerServiceFacade: updating secure chain ", inboundSecureChain);

                    //Catch any unchecked/uncaught exceptions so that it would not harm the code flow
                    try {
                        inboundSecureChain.update();
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Exception in updating secure  chain", e);
                    }

                }
            }
        }
    };

    private final Runnable updateBasicChainAction = new Runnable() {
        @Override
        @Trivial
        public void run() {
            synchronized (actionLock) {
                //only do in case if Activate is called.
                if ((Config != null) && iswasJmsEndpointEnabled) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "CommsServerServiceFacade: updating basic chain ", inboundChain);

                    //Catch any unchecked/uncaught exceptions so that it would not harm the code flow
                    try {
                        inboundChain.update();
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Exception in updating badic  chain", e);
                    }
                }
            }
        }
    };

    void closeViaCommsMPConnections(int mode) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "CommsServerServiceFacade closeViaCommsMPConnections", mode);

        JsAdminService admnService = getJsAdminService();
        if (null != admnService) {
            // Liberty AdminService returns the ME which is running in-process. No search filter is used.
            JsMessagingEngine local_ME = admnService.getMessagingEngine(JsConstants.DEFAULT_BUS_NAME, JsConstants.DEFAULT_ME_NAME);
            if (null != local_ME) {
                JsEngineComponent _mp = local_ME.getMessageProcessor();
                if (null != _mp) { //_mp can not be NULL. But checking it.
                    _mp.stop(mode);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "closeViaCommsMPConnections");

    }

    protected void setCommsClientService(ServiceReference<ChannelConfiguration> service) {
        _commsClientServiceRef.setReference(service);
    }

    protected void unsetCommsClientService(ServiceReference<ChannelConfiguration> service) {
        _commsClientServiceRef.unsetReference(service);
    }

    protected void setChfwBundle(ServiceReference<CHFWBundle> ref) {
        _chfwRef.setReference(ref);
    }

    protected void unsetChfwBundle(ServiceReference<CHFWBundle> ref) {
        _chfwRef.unsetReference(ref);
    }

    private CHFWBundle getCHFWBundle() {
        return _chfwRef.getService();
    }

    @Trivial
    protected void setSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "enable ssl support " + ref.getProperty("type"), this);
        }
        _sslFactoryProviderRef.setReference(ref);

        if ((Config != null) && (wasJmsSSLPort >= 0))
            inboundSecureChain.enable(true);

        performAction(updateSSLChainAction);

    }

    @Trivial
    public void unsetSslSupport(ServiceReference<ChannelFactoryProvider> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "disable ssl support " + ref.getProperty("type"), this);
        }

        if (this._sslFactoryProviderRef.unsetReference(ref)) {
            inboundSecureChain.enable(false);
        }
        //CFW disables chain once after coming to know of there is no SSL provider for the given configuration.
        //So. we will need not do any thing here.
    }

    /**
     * Access the current reference to the bytebuffer pool manager.
     * 
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getBufferPoolManager() {
        return _chfw_bunlde.getBufferManager();
    }

    @Trivial
    protected void setTcpOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set tcp options " + service.getProperty("id"), this);
        }
        _tcpOptionsRef.setReference(service);

        //TODO: I don't think so, from here updateAction has to be called..Activate() method wud do it
        //Once tcpOptions are bound .. it is going to static.. only possibility is update to get called
    }

    @Trivial
    protected void updatedTcpOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(this, tc, "update tcp options " + service.getProperty("id"), this);

        performAction(updateBasicChainAction);
        performAction(updateSSLChainAction);
    }

    //TCP is must for JFAP Comms server.. so this scenario would not arise
    protected void unsetTcpOptions(ServiceReference<ChannelConfiguration> service) {}

    Map<String, Object> getTcpOptions() {
        ChannelConfiguration chanCnfgService = _tcpOptionsRef.getService();
        return chanCnfgService.getConfiguration();
    }

    @Trivial
    protected void setSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "set ssl options " + service.getProperty("id"));
        }
        _sslOptionsRef.setReference(service);

        performAction(updateSSLChainAction);

    }

    @Trivial
    protected void updatedSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "update ssl options " + service.getProperty("id"));
        }

        performAction(updateSSLChainAction);

    }

    @Trivial
    protected void unsetSslOptions(ServiceReference<ChannelConfiguration> service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "unset ssl options " + service.getProperty("id"));
        }

        if (this._sslOptionsRef.unsetReference(service)) {
            performAction(stopSSLChainAction);
        }

    }

    Map<String, Object> getSslOptions() {
        ChannelConfiguration chanCnfgService = _sslOptionsRef.getService();
        if (null != chanCnfgService)
            return chanCnfgService.getConfiguration();
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "getSslOptions() returning NULL as _sslOptionsRef.getHighestRankedService() returned NUll _sslOptionsRef: ", _sslOptionsRef);
            return null;
        }
    }

    protected void setCommonServiceFacade(ServiceReference<CommonServiceFacade> ref) {
        _commonServiceFacadeRef.setReference(ref);
    }

    protected void unsetCommonServiceFacade(ServiceReference<CommonServiceFacade> ref) {
        _commonServiceFacadeRef.unsetReference(ref);
    }

    protected void setEventService(ServiceReference<EventEngine> ref) {
        _eventSvcRef.setReference(ref);
    }

    protected void unsetEventService(ServiceReference<EventEngine> ref) {
        _eventSvcRef.unsetReference(ref);
    }

    //obtain TrmMessageFactory from MFP implementation ( via common bundle) 
    public static TrmMessageFactory getTrmMessageFactory() {
        return _commonServiceFacadeRef.getService().getTrmMessageFactory();
    }

    //obtain JsAdminService from runtime implementation (directly from runtime bundle) 
    public static JsAdminService getJsAdminService() {
        return _commonServiceFacadeRef.getService().getJsAdminService();
    }

    public static EventEngine getEventEngine() {
        return _eventSvcRef.getService();
    }

    int getConfigured_wasJmsPort() {
        return wasJmsPort;
    }

    int getConfigured_wasJmsSSLPort() {
        return wasJmsSSLPort;
    }

    String getConfigured_Host() {
        return host;
    }
}
