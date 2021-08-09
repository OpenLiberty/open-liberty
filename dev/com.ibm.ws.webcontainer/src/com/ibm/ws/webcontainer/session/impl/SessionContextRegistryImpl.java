/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.session.impl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;

import javax.servlet.SessionTrackingMode;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.appext.ApplicationExt;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.session.SessionContextRegistry;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.webcontainer.SessionRegistry;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

public class SessionContextRegistryImpl extends SessionContextRegistry implements SessionRegistry
{
    
    private static final String methodClassName = "SessionContextRegistryImpl";

    public SessionContextRegistryImpl(com.ibm.ws.webcontainer.httpsession.SessionManager smgr) {
        super(smgr);
    }
    
    /*
     * Create a new session context
     */
    protected synchronized IHttpSessionContext createSessionContext(
                                                                  String appName,
                                                                  boolean distributableWebApp, boolean allowDispatchRemoteInclude, 
                                                                  WebApp sc, ClassLoader appCl,
                                                                  SessionManagerConfig smc, String j2eeName,
                                                                  boolean sessionSharing, String applicationSessionAppName) throws Throwable {

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[CREATE_SESSION_CONTEXT]);
        }
        // ensure no other thread just finished creating
        IHttpSessionContext sessCtx = (IHttpSessionContext)scrSessionContexts.get(appName);
        if (sessCtx == null) {
            WebAppConfiguration webAppConfig=sc.getConfiguration();
            SessionApplicationParameters sap = new SessionApplicationParameters(appName, 
                                                                                webAppConfig.isModuleSessionTimeoutSet(),
                                                                                webAppConfig.getSessionTimeout(),
                                                                                distributableWebApp, 
                                                                                allowDispatchRemoteInclude, 
                                                                                sc, 
                                                                                appCl, 
                                                                                j2eeName, 
                                                                                webAppConfig.getSessionCookieConfig(), 
                                                                                webAppConfig.isModuleSessionTrackingModeSet(), 
                                                                                webAppConfig.getSessionTrackingMode());
            if(webAppConfig.getSessionCookieConfig()!=null) {
                webAppConfig.setHasProgrammaticCookieConfig(); // in tWAS, this happens in SessionApplicationParameters(...)
            }
            
            //if session sharing or Servlet21Compatibility (the var sessionSharing should be true) - don't create appSession
            //and calls to getIBMApplicationSession will return null
            if (!sessionSharing) {
                sap.setHasApplicationSession(true);
                sap.setApplicationSessionName(applicationSessionAppName);
            }
            sessCtx = createSessionContextObject(smc, sap);
            scrSessionContexts.put(appName, sessCtx);
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[CREATE_SESSION_CONTEXT]);
        }
        return sessCtx;
    }  

    protected IHttpSessionContext createSessionContextObject(SessionManagerConfig smc, SessionApplicationParameters sap)
    {
        return new HttpSessionContextImpl(smc, sap, this.smgr.getSessionStoreService());
    }

    public boolean isSetSessionTimeout(DeployedModule webModuleConfig)
    {
        if (getSessionTimeOut(webModuleConfig) != 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * get the session timeout
     */
    public int getSessionTimeOut(DeployedModule webModuleConfig)
    {
        int timeout = 0;
        try
        {
            timeout = webModuleConfig.getWebAppConfig().getSessionTimeout();
        }
        catch (Exception e)
        {
            // do nothing
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE))
        {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getSessionTimeOut", "" + timeout);
        }
        return timeout;
    }

    /*
     * Check if web app is marked distributable
     */
    public boolean isDistributable(DeployedModule webModuleConfig)
    {
        return webModuleConfig.getWebApp().getWebAppConfig().isDistributable();
    }

    /*
     * Check if web app can dispatch remote includes
     */
    public boolean isAllowDispatchRemoteInclude(DeployedModule webModuleConfig)
    {
        return false;
    }

    /*
     * Construct internal j2ee representation
     */
    public String getJ2EEName(DeployedModule webModuleConfig)
    {
        WebAppConfiguration webAppConfiguration = webModuleConfig.getWebAppConfig();
        WebModuleMetaData webModuleMetaData = webAppConfiguration.getMetaData();
        J2EEName j2eeName = webModuleMetaData.getJ2EEName();
        if(j2eeName==null) {
            String appName = webAppConfiguration.getApplicationName();
            String moduleName = webAppConfiguration.getModuleName();            
            return appName+"#"+moduleName;
        }
        return j2eeName.toString();
    }

    public String getAppKey(String vhost, DeployedModule webModuleConfig)
    {
        String appKey = null;
        String rootURI = webModuleConfig.getContextRoot();
        appKey = vhost + rootURI;
        return appKey;
    }

    public String getAppKey(String vhost, DeployedModule webModuleConfig, boolean useSharedSession, boolean useApplicationSession)
    {
        if(useSharedSession) {
            WebAppConfiguration webAppConfig = webModuleConfig.getWebAppConfig();
            return vhost+webAppConfig.getApplicationName();
        }
        return getAppKey(vhost, webModuleConfig); // not supported for core ... just
        // return regular appKey
    }

    @FFDCIgnore(UnableToAdaptException.class)
    public boolean getSharing(WebApp ctx)
    {
        Container war = ctx.getModuleContainer();
        if(war==null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getSharing", "ModuleContainer is null; assuming SharedSessionContext=false");
            }
            return false;
        }
        Container ear = war.getEnclosingContainer();
        if(ear==null) {
            //  the war is deployed directly (no sharing)
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getSharing", "EnclosingContainer is null; assuming SharedSessionContext=false");
            }
            return false;
        }
        ApplicationExt adapted = null;
        try {
            adapted = ear.adapt(ApplicationExt.class);
        } catch (UnableToAdaptException e) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getSharing", "Failed to adapt enclosing container; assuming SharedSessionContext=false", e);
            }
            return false;
        }
        if(adapted==null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, "getSharing", "Adapted container is null; assuming SharedSessionContext=false");
            }
            return false;
        }
        return adapted.isSharedSessionContext();
    }

//    public SessionManagerConfig getServerSMC() {
//        return SessionMgrComponentImpl.getServerSessionManagerConfig();
//    }

    public SessionManagerConfig getSMC(DeployedModule webModuleConfig)
    {
        return this.getServerSMC();
    }

    public ClassLoader getSessionClassLoader(DeployedModule webModuleConfig)
    {
        return webModuleConfig.getClassLoader(); // returns null
    }

    /*
     * Get the SessionContext for the specified webmodule config
     * Calls initialize the first time.
     * Will create if it doesn't already exist
     */
    public IHttpSessionContext getSessionContext(DeployedModule webModuleConfig, WebApp ctx, 
                                                 String vhostName,  ArrayList sessionRelatedListeners[]) throws Throwable {  
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET_SESSION_CONTEXT]);
        }
        if (!initialized)
            initialize(); 
        
        boolean sessionSharing = getSharing(ctx);

        //gets the appropriate appKey to use as a key for the sessionContexts
        //This takes into consideration sessionSharing
        String appKey = getAppKey(vhostName, webModuleConfig, sessionSharing, false);//sessionSharing);
        String appSessionAppKey = getAppKey(vhostName, webModuleConfig, sessionSharing, true); //true is to tell us to use either the applevel or BLA level

        SessionManagerConfig smc = getSMC(webModuleConfig); 

        // cmd LIDB2842 - start block
        //  Use global sessions only if session management config has
        //  not been overridden at the EAR or WAR level.  If the session
        //  management config HAS been overridden, issue info message and
        //  give the app its own context so its sessions will not be global.
        //  This behavior was necessary because the admin application requires
        //  its own config to ensure it always runs session-in-memory.
        //
        if ((_globalSessionContext) && (smc.isUsingWebContainerSM())) {
            appKey = "GLOBAL_HTTP_SESSION_CONTEXT";
            sessionSharing = true;
        } else if (_globalSessionContext) {
            String parm[] = { appKey};
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, methodNames[GET_SESSION_CONTEXT], "SessionContextRegistry.SessionNotGlobalForWebApp", parm);
        }

        //cloning to support programmatic session cookie configuration
        //always clone!!!
        smc = smc.clone();
        //don't set here as we're still using the base config - just changing the cookie/url properties
        //smc.setUsingWebContainerSM(false);
        if (smc.isUseContextRootForSessionCookiePath()) {
            //setting the cookie path to the context path
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[GET_SESSION_CONTEXT], "Setting the cookie path to \""+ctx.getContextPath()+"\" for application - "+ appKey);
            }
            smc.setSessionCookiePath(ctx.getContextPath());
        }
        if (!ctx.getConfiguration().isModuleSessionTrackingModeSet()) {
            EnumSet<SessionTrackingMode> trackingModes = EnumSet.noneOf(SessionTrackingMode.class);
            if (smc.getEnableCookies()) {
                trackingModes.add(SessionTrackingMode.COOKIE);
            }
            if (smc.getEnableUrlRewriting()) {
                trackingModes.add(SessionTrackingMode.URL);
            }
            if (smc.useSSLId()) {
                trackingModes.add(SessionTrackingMode.SSL);
            }
            ctx.getConfiguration().setDefaultSessionTrackingMode(trackingModes);
        }

        IHttpSessionContext iSctx = (IHttpSessionContext) scrSessionContexts.get(appKey);

        if (iSctx != null) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, methodNames[GET_SESSION_CONTEXT], "SessionContextRegistry.existingContext", appKey);
            SessionContext wsCtx = (SessionContext) iSctx;
            if (sessionSharing)
                wsCtx.incrementRefCount();
        } else {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, methodNames[GET_SESSION_CONTEXT], "SessionContextRegistry.newContext", appKey);
            ClassLoader sessionClassLoader = getSessionClassLoader(webModuleConfig);
            iSctx = createSessionContext(
                                         appKey,
                                         isDistributable(webModuleConfig),
                                         isAllowDispatchRemoteInclude(webModuleConfig),
                                         ctx,
                                         sessionClassLoader,
                                         smc,
                                         getJ2EEName(webModuleConfig),
                                         sessionSharing,
                                         appSessionAppKey);
        }

        //make sure they are the same object
        ctx.getConfiguration().setSessionCookieConfig(iSctx.getWASSessionConfig().getSessionCookieConfig());
        //clone in SessionContext does not update this smc object but the _smc object in SessionContext
        //ctx.getConfiguration().setSessionManagerConfigBase(smc);
        ctx.getConfiguration().setSessionManagerConfig(iSctx.getWASSessionConfig());

        ArrayList sessionListeners = sessionRelatedListeners[0];  
        ArrayList sessionAttrListeners = sessionRelatedListeners[1];  
        ArrayList sessionIdListeners = sessionRelatedListeners[2];  // Servlet 3.1 

        String j2eeName = null;
        if ((_globalSessionContext && sessionSharing) || ((SessionContext)iSctx)._sap.getHasApplicationSession()) {
            // for global session, pass j2eename with listeners so we can stop listeners for each app
            j2eeName = getJ2EEName(webModuleConfig); 
        }
        //  add listeners to session context
        SessionContext sessCtx = (SessionContext)iSctx;
        sessCtx.addHttpSessionListener(sessionListeners, j2eeName);  
        
        // Servlet 3.1
        if (!sessionIdListeners.isEmpty()) {
           addHttpSessionIdListeners(sessionIdListeners, j2eeName, sessCtx);      
        }
        sessCtx.addHttpSessionAttributeListener(sessionAttrListeners, j2eeName);  
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET_SESSION_CONTEXT], iSctx);
        }
        return iSctx;
    }
    
    protected void addHttpSessionIdListeners(ArrayList list, String name, SessionContext sessCtx){
        // Don't do anything here since we only want to addHttpSessionIdListeners when we are in Servlet 3.1  See
        // the implementation of this method in SessionContextRegistry31Impl

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.INFO, methodClassName, "addHttpSessionIdListeners","Called addHttpSessionIdListener in a context" +
            		"other than Servlet 3.1 or later so don't do anything.");
        }
    }

}
