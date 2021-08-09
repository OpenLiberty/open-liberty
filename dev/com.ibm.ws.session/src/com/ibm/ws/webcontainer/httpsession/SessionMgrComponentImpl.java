/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.httpsession;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.session.SessionContextRegistry;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionProperties;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.utils.EncodeCloneID;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/*
 * This is the entry point into SessionManager for CCX.WEB release
 * See WsSessionMgrComponentImpl for full WAS release
 */
public class SessionMgrComponentImpl implements SessionManager {
    
    /** Only used by WXS. Should be removed as soon as SPI is defined. */
    public static final AtomicReference<SessionMgrComponentImpl> INSTANCE = new AtomicReference<SessionMgrComponentImpl>();

    private final SessionManagerConfig serverLevelSessionManagerConfig = new SessionManagerConfig();
    private final WsLocationAdmin wsLocationAdmin;
    private final SessionStoreService sessionStoreService;
    private final Map<String, Object> mergedConfiguration;
    private final ScheduledExecutorService scheduledExecutorService;
    private boolean initialized = false;

    private static final String methodClassName = "SessionMgrComponentImpl";
    
    /**
     * <p>WXS calls this method, so the signature CANNOT be changed until we define an SPI that WXS can consume.</p>
     * <p>We should consider hiding SessionManagerConfig from WXS completely, and provide a new interface with the exact information WXS requires.</p>
     * 
     * @return the configuration of the currently active SessionManager instance
     */
    public static SessionManagerConfig getServerSessionManagerConfig() {
        SessionMgrComponentImpl service = INSTANCE.get();
        if (service == null) {
            // no session manager service is available
            return null; 
        }
        return service.getServerConfig();
    }
    
    public SessionManagerConfig getServerConfig() {
        this.initialize();
        return this.serverLevelSessionManagerConfig;
    }

    public SessionStoreService getSessionStoreService() {
        this.initialize();
        return this.sessionStoreService;
    }
    
    
    //get method to be able to retrieve ScheduledExecutorService 
    public ScheduledExecutorService getScheduledExecutorService(){
        this.initialize();
        return this.scheduledExecutorService;
    }


    private SessionStoreService getSessionStoreService(SessionStoreService storeService, Object storageRef) {
        if (storeService == null) {
            return null; // no store service is available
        }
        if (!storeService.isValid()) {
            return null; // a store service is available, but it's invalid
        }
        Map<String, Object> storeProperties = storeService.getConfiguration();
        if(storeProperties == null) {
            return null; // configuration properties are required
        }
        /*- 
         * Maintain backwards compatibility!
         * 
         * In 8.5.0.0 through 8.5.0.2, customers needed to configure session persistence like this: 
         * <httpSessionDatabase id="SessionDB" dataSourceRef="SessionDS"/>
         * <httpSession storageRef="SessionDB"/>
         * 
         * If the "storageRef" attribute of the httpSession element did not match the "id" attribute
         * of httpSessionDatabase element, then session persistence would not be enabled.
         * 
         * Both attributes were required at the time, but as of 8.5.5.0, they are not required. We need
         * to enforce the old behavior to maintain backwards compatibility.
         * 
         * See RTC defect 95213 for more information.
         */
        Object storeId = storeProperties.get("id");
        if(storageRef == null) {
            /*-
             * When storageRef attribute is not set, assume the customer is using 8.5.5+.
             * 
             * If storeId==null, then 8.5.5+ customer wants to enable session persistence.
             *                   (Risk pre-8.5.5 customer trying to disable persistence).
             * If storeId!=null, then 8.5.5+ customer may want an ID attribute for non-runtime purposes.
             *                   For example, customer scripts may generate server.xml with supposedly harmless ID attributes.
             *                   (Risk pre-8.5.5 customer trying to disable persistence).
             */
            return storeService;
        } else {
            /*- When storageRef attribute is set, assume the customer migrated from pre-8.5.5 (for backwards compatibility). */
            if(storeId==null) {
                return null; /*- pre-8.5.5 customer is trying to disable session persistence through configuration */
            } else {
                if (storageRef.equals(storeId)) {
                    return storeService; /*- pre-8.5.5 customer is trying to ENABLE session persistence */
                } else {
                    return null; /*- pre-8.5.5 customer is trying to disable session persistence through configuration */
                }
            }
        }
    }

    /**
     * Derives a unique identifier for the current server.
     * The result of this method is used to create a cloneId.
     * 
     * @return a unique identifier for the current server (in lower case)
     */
    private String getServerId() {
        UUID fullServerId = null;
        WsLocationAdmin locationService = this.wsLocationAdmin;
        if (locationService != null) {
            fullServerId = locationService.getServerId();
        }
        if (fullServerId == null) {
            fullServerId = UUID.randomUUID(); // shouldn't get here, but be careful just in case
        }
        return fullServerId.toString().toLowerCase(); // clone IDs need to be in lower case for consistency with tWAS
    }

    /*
     * Service start call
     */
    public void start(SessionContextRegistry scr) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, "start");
        }
        this.initialize();
        scr.setPropertiesInSMC(serverLevelSessionManagerConfig);
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            // Prints server level config
            serverLevelSessionManagerConfig.printSessionManagerConfigForDebug(LoggingUtil.SESSION_LOGGER_CORE);
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, "start");
        }
    }
    
    public SessionMgrComponentImpl(ScheduledExecutorService scheduledExecutorService, WsLocationAdmin wsLocationAdmin, SessionStoreService sessionStoreService, Dictionary<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "<init>", "properties=" + properties);
        }
        this.wsLocationAdmin = wsLocationAdmin;
        this.scheduledExecutorService= scheduledExecutorService;
        this.mergedConfiguration = new HashMap<String, Object>();
        if (properties != null) {
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Object value = properties.get(key);
                this.mergedConfiguration.put(key, value);
            }
        }
        Object storageRef = this.mergedConfiguration.get("storageRef");
        this.sessionStoreService = this.getSessionStoreService(sessionStoreService, storageRef);
        if (this.sessionStoreService != null) {
            Map<String, Object> storeProperties = this.sessionStoreService.getConfiguration();
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "<init>", "storeProperties=" + storeProperties);
            }
            // merge store properties with core properties (allow store properties to override core properties)
            if (storeProperties != null) {
                this.mergedConfiguration.putAll(storeProperties);
            }
        }
    }

    /**
     * Delay initialization until a public method of this service is called
     */
    private void initialize() {
        if(this.initialized) {
            return;
        }
        if (LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.INFO)) {
            if (this.sessionStoreService==null) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, "initialize", "SessionMgrComponentImpl.noPersistence");
            } else {
                String modeName = "sessionPersistenceMode";
                Object modeValue = this.mergedConfiguration.get(modeName);
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, "initialize", "SessionMgrComponentImpl.persistenceMode", new Object[] { modeValue });
            }
        }
        SessionProperties.setPropertiesInSMC(this.serverLevelSessionManagerConfig, this.mergedConfiguration);
        String cloneId = SessionManagerConfig.getCloneId();
        if (cloneId == null) {
            if (this.sessionStoreService==null && SessionManagerConfig.isTurnOffCloneId()) {
                /*-
                 * In tWAS, WsSessionAffinityManager sets the CloneID to -1 when two conditions are both met:
                 * A) Running in a standalone server (com.ibm.ws.runtime.service.WLM#getMemberUID()==null)
                 * B) The HttpSessionCloneId custom property is not explicitly set
                 * 
                 * In addition, tWAS will set the CloneID to "" (the empty String) if a third condition is also met:
                 * C) The NoAdditionalSessionInfo custom property is set to "true"
                 * 
                 * In lWAS, there's no notion of a "standalone" server, because potentially any lWAS server 
                 * could require a CloneID for session affinity. As a result, our logic for using an
                 * empty Clone ID on lWAS needs to be different than our logic on tWAS.
                 * 
                 * Since most customers who specify a session store will be interested in session affinity,
                 * we'll assume that these customers are always interested in a non-empty Clone ID.
                 * 
                 * We'll also assume that customers who do not specify a session store who also explicitly
                 * set the noAdditionalInfo property to "true" would prefer an empty Clone ID.
                 * 
                 * All customers can always explicitly set the cloneId property to override these assumptions.
                 */
                cloneId = "";
            } else {
                String serverId = getServerId(); // never returns null
                if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "initialize", "serverId=" + serverId);
                }
                SessionManagerConfig.setServerId(serverId);
                cloneId = EncodeCloneID.encodeString(serverId); // never returns null
            }
            SessionManagerConfig.setCloneId(cloneId);
        }
        this.initialized=true;
    }
    
    protected boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public String getCloneID() {
        this.initialize();
        return SessionManagerConfig.getCloneId();
    }

    @Override
    public char getCloneSeparator() {
        this.initialize();
        return SessionManagerConfig.getCloneSeparator();
    }

    @Override
    public String getAffinityUrlIdentifier() {
        this.initialize();
        // rewriteId always starts with ";", and ends with "="
        String rewriteId = serverLevelSessionManagerConfig.getSessUrlRewritePrefix();
        return rewriteId.substring(1, rewriteId.length() - 1);
    }

    @Override
    public String getDefaultAffinityCookie() {
        this.initialize();
        return serverLevelSessionManagerConfig.getSessionCookieName();
    }

    /*
     * Service stop call
     */
    public void stop() {
        // nothing to do for now
    }

}