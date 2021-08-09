/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * <p>
 * This service registers and re-registers the SessionManager service 
 * in response to session configuration changes and session-database 
 * configuration changes.
 * </p>
 * <p>
 * Since this service is immediate, it's activated as soon as the
 * Servlet feature is added, and deactivated when it's removed. No
 * other services should depend on this coordinator service; they should
 * depend on the SessionManager service.
 * </p>
 */
public class SessionMgrCoordinator {

    private static final String CLASS_NAME = SessionMgrCoordinator.class.getSimpleName();
    
    private volatile ComponentContext context;
    private volatile ServiceRegistration<SessionManager> smgrRegistration = null;
    private volatile SessionMgrComponentImpl smgr = null;

    private ConfigurationAdmin configAdmin = null;
    private WsLocationAdmin wsLocationAdmin = null;
    private ServiceReference<ApplicationRecycleCoordinator> appRecycleService= null;
    private SessionStoreService sessionStoreService = null;
    private ScheduledExecutorService scheduledExecutorService = null;
    
    /**
     * Since the services this service consumes are dynamic,
     * activate will be the first method called by DS. This
     * method needs to register a default SessionManager service,
     * but it should also be careful NOT to restart applications
     * (unless applications are already running somehow) 
     * 
     * @param context the context used to register the SessionManager service
     * @throws Throwable 
     */
    protected void activate(ComponentContext context) throws Throwable {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "activate", context);
        }
        this.context = context;
        if(sessionStoreService == null && this.foundSessionStoreServiceConfig()) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "activate", "Will not register default SessionManager service because a SesionStoreService will be available soon");
            }
        } else {
            this.registerSessionManager();            
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "activate");
        }
    }
    
    /**
     * <p>
     * Changes to the session configuration in the httpSession element
     * in server.xml require an application restart because certain properties 
     * have an effect during application start/stop.
     * </p>
     * <p>
     * This method is called whenever a change is made to the session
     * configuration. A configuration change should trigger exactly one 
     * application restart if the SessionManager service has been 
     * registered AND initialized, but no application restarts if the 
     * SessionManager is not initialized (a restart is not needed because 
     * no one is using the registered/unregistered SessionManager service).
     * </p>
     * 
     * @param context the context used to register the SessionManager service
     */
    protected void modified(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "modified", context);
        }
        this.context = context;
        this.registerSessionManager();
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "modified");
        }
    }

    /**
     * Since this service is immediate and dynamic, it will not be
     * deactivated unless the Servlet feature is removed. When this happens,
     * we assume that an external feature stops applications appropariately.
     * The deactivate should NOT stop applications via the appRecycleService
     * because it will NOT be able to issue a corresponding start operation
     * on the appRecycleService.
     * 
     * @param context the context used to register the SessionManager service
     */
    protected void deactivate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "deactivate", context);
        }
        this.unregisterSessionManager(); // we won't restart applications in this case, so assume someone else stops them
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "deactivate");
        }
    }

    private synchronized void unregisterSessionManager() {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "unregisterSessionManager");
        }
        if (this.smgrRegistration != null) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "unregisterSessionManager", "Unregistering current SessionManager");
            }
            this.smgrRegistration.unregister();
            this.smgrRegistration = null;
            SessionMgrComponentImpl.INSTANCE.compareAndSet(this.smgr, null);
            this.smgr = null;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "unregisterSessionManager", "No SessionManager is currently registered; no need to unregister");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "unregisterSessionManager");
        }
    }
    
    /**
     * <p>
     * Unregisters the SessionManager service (if one is registered),
     * and registers a new SessionManager service based on the current
     * configuration / SessionStoreService.
     * </p>
     * <p>
     * We must stop applications before unregistering the SessionManager
     * service because applications may require the SessionManager during stop
     * (due to listeners, etc).
     * </p>
     * <p>
     * Note that the start operation on the appRecycleService will only
     * be called if the stop operation was previously called. That's important,
     * because we must be careful to always call start if we ever call stop.
     * </p>
     */
    private synchronized void registerSessionManager() {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "registerSessionManager");
        }
        
        // If we haven't activated yet, don't try to register
        if ( this.context ==  null ) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "registerSessionManager");
            }
            return;
        }
        
        /*- Step 1: Stop applications using old SessionManager (if SessionManager has initialized) */
        if(this.smgr != null && this.smgr.isInitialized()) {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "registerSessionManager", "Stopping applications because the SessionManager has been initialized");
            }
            try {
                ApplicationRecycleCoordinator arc = context.getBundleContext().getService(appRecycleService);
                if ( arc != null ) {
                    arc.recycleApplications(null);
                }
            } catch(Throwable thrown) {
               FFDCFilter.processException(thrown, getClass().getName(), "153", this);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "registerSessionManager", "Skipping application restart because the SessionManager is not initialized");
            }
        }
        /*- Step 2: Register new SessionManager (to allow dynamic update in WebContainer) 
         * 
         * Note that we must register a new service BEFORE unregistering the old service
         * so that anyone consuming the SessionManager can use a dynamic update (if they want to).
         */
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "registerSessionManager", "Registering a new SessionManager service");
        }
        Dictionary<String, Object> properties = this.context.getProperties();
        SessionMgrComponentImpl newSmgr = new SessionMgrComponentImpl(this.scheduledExecutorService, this.wsLocationAdmin, this.sessionStoreService, properties);
        ServiceRegistration<SessionManager> newSmgrRegistration = this.context.getBundleContext().registerService(SessionManager.class, newSmgr, properties);
        /*- Step 3: Unregister old SessionManager */
        this.unregisterSessionManager();
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "registerSessionManager", "Caching new new SessionManager registration");
        }
        this.smgrRegistration = newSmgrRegistration;
        this.smgr = newSmgr;
        SessionMgrComponentImpl.INSTANCE.set(this.smgr);
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "registerSessionManager");
        }
    }
    
    /**
     * In the case where the session.db bundle starts at around the same time that the first
     * application is starting, the coordinator needs to wait for the forthcoming SessionStoreService
     * before registering the first SessionManager instance. (This timing window will happen if, 
     * for example, the restConnector feature is enabled in addition to deferServletLoad="false").
     * We use configAdmin to foresee whether a SessionStoreService will be coming shortly.
     * 
     * @return true if config for a SessionStoreService is found by ConfigurationAdmin
     * @throws Throwable 
     */
    private boolean foundSessionStoreServiceConfig() throws Throwable {
        boolean found = false;
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "foundDatabaseConfig");
        }
        String databaseConfigFilter = FilterUtils.createPropertyFilter(Constants.SERVICE_PID, "com.ibm.ws.session.db");
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "foundSessionStoreServiceConfig", "Database configuration filter: " + databaseConfigFilter);
        }
        
        String sessionCacheConfigFilter = FilterUtils.createPropertyFilter(Constants.SERVICE_PID, "com.ibm.ws.session.cache");
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "foundSessionStoreServiceConfig", "Session Cache configuration filter: " + sessionCacheConfigFilter);
        }
        try {
            Configuration[] sessionCacheConfigurations = configAdmin.listConfigurations(sessionCacheConfigFilter);
            if(sessionCacheConfigurations != null) {
                for(Configuration configuration : sessionCacheConfigurations) {
                    if(configuration == null) {
                        continue;
                    }
                    found = true;
                    if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "foundSessionCacheConfig", "Found matching session cache configuration at " + configuration.getBundleLocation() + ": " + configuration.getProperties());
                    }
                }
            }
            
            Configuration[] databaseConfigurations = configAdmin.listConfigurations(databaseConfigFilter);
            if(databaseConfigurations != null) {
                for(Configuration configuration : databaseConfigurations) {
                    if(configuration == null) {
                        continue;
                    }
                    found = true;
                    if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                        LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, CLASS_NAME, "foundDatabaseConfig", "Found matching database configuration at " + configuration.getBundleLocation() + ": " + configuration.getProperties());
                    }
                }
            }
        } catch (Throwable thrown) {
            FFDCFilter.processException(thrown, getClass().getName(), "88", this);
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "foundDatabaseConfig", found);
        }
        return found;
    }
    
    /**
     * @param configurationAdmin the service used to check the server configuration
     */
    protected void setConfigAdmin(ConfigurationAdmin configurationAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "setConfigurationAdmin", configurationAdmin);
        }
        this.configAdmin = configurationAdmin;
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "setConfigurationAdmin");
        }
    }

    /**
     * @param configurationAdmin the service used to check the server configuration
     */
    protected void unsetConfigAdmin(ConfigurationAdmin configurationAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "unsetConfigurationAdmin", configurationAdmin);
        }
        if(this.configAdmin==configurationAdmin) {
            this.configAdmin=null;
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "unsetConfigurationAdmin");
        }
    }
    
    /**
     * Since the location service is only used during SessionManager initialization,
     * we don't need to re-register the SessionManager service or restart applications
     * if there's a change to the location service.
     * 
     * @param wsLocationAdmin the service used to determine the default session clone ID
     */
    protected void setLocationService(WsLocationAdmin wsLocationAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "setLocationService", wsLocationAdmin);
        }
        this.wsLocationAdmin = wsLocationAdmin;
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "setLocationService");
        }
    }

    /**
     * Since the location service is only used during SessionManager initialization,
     * we don't need to re-register the SessionManager service or restart applications
     * if there's a change to the location service.
     * 
     * @param wsLocationAdmin the previous service used to determine the default session clone ID
     */
    protected void unsetLocationService(WsLocationAdmin wsLocationAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "unsetLocationService", wsLocationAdmin);
        }
        if(this.wsLocationAdmin==wsLocationAdmin) {
            this.wsLocationAdmin=null;
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "unsetLocationService");
        }
    }
    
    /**
     * @param appRecycleService the service used to restart applications in response to session config changes
     */
    protected void setAppRecycleService(ServiceReference<ApplicationRecycleCoordinator> appRecycleService) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "setAppRecycleService", appRecycleService);
        }
        this.appRecycleService = appRecycleService;
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "setAppRecycleService");
        }
    }

    /**
     * @param appRecycleService the previous service used to restart applications in response to session config changes
     */
    protected void unsetAppRecycleService(ServiceReference<ApplicationRecycleCoordinator> appRecycleService) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "unsetAppRecycleService", appRecycleService);
        }
        if(this.appRecycleService==appRecycleService) {
            this.appRecycleService=null;
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "unsetAppRecycleService");
        }
    }

    /**
     * <p>
     * Changes to the session database configuration in the httpSessionDatabase element
     * in server.xml require an application restart because certain properties have an effect
     * during application start/stop.
     * </p>
     * <p>
     * This method is called whenever a new DatabaseStoreService is
     * registered. A DatabaseStoreService is registered the first time
     * the sessionDatabase feature is enabled, and also when a change
     * has been made to the session database configuration. When a
     * configuration change is made, first the unset operation is called,
     * and then the set operation is called. We must register a new
     * SessionManager service during both calls to ensure that a SessionManager
     * service is always available.
     * </p>
     * <p>
     * A configuration change should trigger exactly one
     * application restart if the SessionManager service has been
     * registered AND initialized, but no application restarts if the
     * SessionManager is not initialized (a restart is not needed because
     * no one is using the registered/unregistered SessionManager service).
     * </p>
     * <p>
     * In the case where both the httpSession element is changed AND
     * the httpSessionDatabase element is changed during the same configuration
     * event, only one application restart should occur. This behavior will
     * occur despite the fact that the SessionManager service will be re-registered
     * three times: unsetSessionStoreService, setSessionStoreService, and modified.
     * This behavior is achieved by relying on the appRecycleService to wait for
     * the end of the configuration event before restarting the apps.
     * </p>
     * 
     * @param sessionStoreService the service used to persist session
     *            information beyond the local server's memory
     */
    protected void setSessionStoreService(SessionStoreService sessionStoreService) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "setSessionStoreService", sessionStoreService);
        }
        this.sessionStoreService = sessionStoreService;
        this.registerSessionManager();
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "setSessionStoreService");
        }
    }

    /**
     * @see #setSessionStoreService(SessionStoreService)
     * @param sessionStoreService the service previously used to persist session
     *            information beyond the local server's memory
     */
    protected void unsetSessionStoreService(SessionStoreService sessionStoreService) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "unsetSessionStoreService", sessionStoreService);
        }
        if(this.sessionStoreService==sessionStoreService) {
            this.sessionStoreService=null;
            this.registerSessionManager();
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "unsetSessionStoreService");
        }
    }
    
    
    //Registers a new session manager service (in this case ScheduledExecutorService) with
    //updated configuration
    
    //Tracing is also added for debugging purposes
    
    protected void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "setScheduledExecutorService", scheduledExecutorService);
        }
        this.scheduledExecutorService = scheduledExecutorService;
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "setScheduledExecutorService");
        }
    }
    
    protected void unsetScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(CLASS_NAME, "unsetScheduledExecutorService", scheduledExecutorService);
        }
        if(this.scheduledExecutorService==scheduledExecutorService) {
            this.scheduledExecutorService=null;
        }
        if (TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(CLASS_NAME, "unsetScheduledExecutorService");
        }
    }
}
