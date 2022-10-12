/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
 * 
 * Start JFAP chain and Secure chain in-line in the context of SCR thread because in the design discussions with Alasdair,
 * it was decided that by the time Liberty profile server started, messaging has to be ready for send/receive
 * this can happen only if chains are started in the context of SCR thread
 * 
 */
@Component(
        name ="com.ibm.ws.messaging.comms.server",
        configurationPolicy = REQUIRE, 
        immediate = true, 
        property = {"type=messaging.comms.server.service", "service.vendor=IBM"})
public class CommsServerServiceFacade implements Singleton {
    private static final TraceComponent tc = Tr.register(CommsServerServiceFacade.class, JFapChannelConstants.MSG_GROUP, JFapChannelConstants.MSG_BUNDLE);
    private final static String Inbound_ConfigAlias = "wasJmsEndpoint";

    private String endpointName = null;

    private final CommsInboundChain inboundChain = new CommsInboundChain(this, false);
    private final CommsInboundChain inboundSecureChain = new CommsInboundChain(this, true);

    private int wasJmsPort;
    private String host = null;
    private int wasJmsSSLPort;
    private boolean iswasJmsEndpointEnabled = true;

    private final JsAdminService jsAdminService;
    
    private final CHFWBundle chfw;
    private final ChannelConfiguration tcpOptions;
    private final ChannelConfiguration sslOptions;
    
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
            @Reference(name = "sslOptions", target = "(id=unbound)", cardinality = OPTIONAL) // target to be overwritten by metatype
            ChannelConfiguration sslOptions,
            /* We have preserved the original behaviour of using defaultSSLOptions 
             * If we want to use ${defaultSSLVar}, use (id=unbound) here and set it in metatype */
            @Reference(name="defaultSSLOptions", target="(id=defaultSSLOptions)", cardinality=OPTIONAL)
            ChannelConfiguration defaultSSLOptions,
            @Reference(name = "sslFactoryProvider", target = "(type=SSLChannel)", cardinality = OPTIONAL)
            ChannelFactoryProvider sslFactoryProvider,
            @Reference(name = "eventEngine")
            EventEngine eventEngine,
            Map<String, Object> properties) {
        final String methodName = "<init>";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, methodName, new Object[]{jsAdminService, chfw, tcpOptions, sslOptions, defaultSSLOptions, sslFactoryProvider, eventEngine, properties});

        this.jsAdminService = jsAdminService;
        this.chfw = chfw;
        this.tcpOptions = tcpOptions;
        this.sslOptions = Optional.ofNullable(sslOptions).orElse(defaultSSLOptions);
        this.eventEngine = eventEngine;
        
        Object cid = properties.get(ComponentConstants.COMPONENT_ID);

        endpointName = (String) properties.get("id");
        if (endpointName == null)
            endpointName = Inbound_ConfigAlias + cid;

        // Allowing JFAP to accept incoming connections. 
        ServerConnectionManager.initialise(chfw.getFramework());

        //Go ahead and Register JFAPChannel with Channel Framework by providing JFAPServerInboundChannelFactory
        chfw.getFramework().registerFactory("JFAPChannel", JFAPServerInboundChannelFactory.class);

        inboundChain.init(endpointName, chfw);
        inboundSecureChain.init(endpointName + "-ssl", chfw);

        iswasJmsEndpointEnabled = parseBoolean(Inbound_ConfigAlias, "enabled", properties.get("enabled"), true);
        host = (String) properties.get("host");
        wasJmsPort = parseInteger(Inbound_ConfigAlias, "wasJmsPort", properties.get("wasJmsPort"), -1);
        
        wasJmsSSLPort = parseInteger(Inbound_ConfigAlias, "wasJmsSSLPort", properties.get("wasJmsSSLPort"), -1);
        
        if (wasJmsPort >= 0) inboundChain.enable(true);
        
        if ((wasJmsSSLPort >= 0) && (sslFactoryProvider != null)) inboundSecureChain.enable(true);
        
        if (iswasJmsEndpointEnabled) {
            factotum.updateChains();
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

        synchronized void updateChains() {
        	if (iswasJmsEndpointEnabled) {
        		if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "CommsServerServiceFacade: updating basic chain ", inboundChain);
        		try {
        			inboundChain.update();
        		} catch (Exception e) {
        			if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "Exception in updating basic chain", e);
        		}

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
        if (null != sslOptions) return sslOptions.getConfiguration();
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, "getSslOptions() returning NULL");
        return null;
    }

    public static TrmMessageFactory getTrmMessageFactory() {        
        return TrmMessageFactory.getInstance();
    }

    //obtain JsAdminService from runtime implementation (directly from runtime bundle) 
    public static JsAdminService getJsAdminService() {
        return CommonServiceFacade.getJsAdminService();
    }

    public EventEngine getEventEngine() {
        return eventEngine;
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
