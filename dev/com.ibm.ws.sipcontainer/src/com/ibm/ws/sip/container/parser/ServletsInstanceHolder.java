/*******************************************************************************
 * Copyright (c) 2003, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package com.ibm.ws.sip.container.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.TimerListener;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.SipApplicationSessionStateListener;
import com.ibm.websphere.sip.SipSessionStateListener;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.osgi.ServletInstanceHolderInterface;

/**
 * This is a singleton service which holds all siplets instances order by application.
 * this class help implementing the feature of siplets acting as listeners should be single instance
 */
public class ServletsInstanceHolder implements ServletInstanceHolderInterface{
	 /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ServletsInstanceHolder.class);
    /* members */
    private final static ServletsInstanceHolder s_instance = new ServletsInstanceHolder();
    private SipContainer m_sipcontainer = SipContainer.getInstance();

    private final Map<String, InitMembers> sipServletThreadLocal = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> appExecutors = new ConcurrentHashMap<>();

    private ServletsInstanceHolder() {
              	// Exists only to defeat instantiation.
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "ServletsInstanceHolder", "ServletsInstanceHolder constructor");
        }
    }

    public static ServletsInstanceHolder getInstance() {
        return s_instance;
    }

    private static class InitMembers {
        public SipServlet sipServlet;
        public ServletContext sipletContext;
        public SipAppDesc appDesc;

        public InitMembers(SipAppDesc appDesc, SipServlet sipServlet, ServletContext sipletContext) {
            this.appDesc = appDesc;
            this.sipServlet = sipServlet;
            this.sipletContext = sipletContext;
        }
    }

    /**
    * Map that holds the siplets instances, order by Application name (i.e <appname,Map >).
    * each inner Map is order by <siplet-class-name,siplet-instance>
    */
    private Map<String, Map<String, Object>> m_sipAppsServlets = new HashMap<>(1);

    /**
    * add instance
    * @param appName
    * @param instance
    */
    public void addSipletInstance(String appName, String className, Object servletInstance) {
        if (c_logger.isTraceDebugEnabled()) {
           	
		c_logger.traceDebug(this,"addSipletInstance","appName["+appName+"] class["+className+"] instance["+servletInstance+"]");	
        }

        SipAppDesc sipApp = m_sipcontainer.getSipApp(appName);
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "addSipletInstance", "found sipAppDesc [" + sipApp + "]");
        }

        Map<String, Object> app = m_sipAppsServlets.get(appName);
        if (app == null) {
            app = new HashMap<>(1);
            m_sipAppsServlets.put(appName, app);
         // Setup the listeners in this stage in order at least
			// one siplet is load-on-startup and implements SipServletListener.
            sipApp.setupSipListeners();
        }

        app.put(className, servletInstance);

        if(sipApp != null) {
			if (servletInstance instanceof TimerListener) {
				sipApp.replaceTimerListener((TimerListener) servletInstance);				
	        }
	        if (servletInstance instanceof SipApplicationSessionListener){
	        	sipApp.replaceAppSessionListener((SipApplicationSessionListener) servletInstance);
	        }
	        if (servletInstance instanceof SipSessionListener){
	        	sipApp.replaceSessionListener((SipSessionListener) servletInstance);
	        }
	        if (servletInstance instanceof SipSessionStateListener){
	        	sipApp.replaceSipSessionStateListener((SipSessionStateListener) servletInstance);
	        }
	        if (servletInstance instanceof SipApplicationSessionStateListener){
	        	sipApp.replaceApplicationSessionStateListener((SipApplicationSessionStateListener) servletInstance);
	        }
	        if (servletInstance instanceof SipApplicationSessionActivationListener){
	        	sipApp.replaceApplicationSessionActivationListener((SipApplicationSessionActivationListener) servletInstance);
	        }
	        if (servletInstance instanceof SipErrorListener){
	        	sipApp.replaceErrorListener((SipErrorListener) servletInstance);
	        }        
	        if (servletInstance instanceof SipSessionAttributeListener){
	        	sipApp.replaceSessionAttributeListener((SipSessionAttributeListener) servletInstance);
	        }
	        if (servletInstance instanceof SipServletListener){
	        	sipApp.replaceSipServletListener((SipServletListener) servletInstance);
	        }
		}
		else {
			if(c_logger.isTraceDebugEnabled()){
				c_logger.error("addSipletInstance: can not add listeners");	
			}
		}
	}

    /**
    * add instance
    * @param appName
    * @param instance
    */
    public void removeSipletInstance(String appName, String className) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this,"removeSipletInstance","appName["+appName+"] class["+className+"] ");	
        }

        SipAppDesc sipApp = m_sipcontainer.getSipApp(appName);
        // Exists only to defeat instantiation.
        Map<String, Object> app = m_sipAppsServlets.get(sipApp.getApplicationName());
        Object servletInstance = null;
        if (app != null) {
            servletInstance = app.remove(className);
            if (app.isEmpty()) {
                m_sipAppsServlets.remove(appName);
            }
        }
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "removeSipletInstance", "found sipAppDesc [" + sipApp + "]");
        }

        if(servletInstance != null && sipApp != null) {
			if (servletInstance instanceof TimerListener) {
				sipApp.removeTimerListener();				
	        }
			if (servletInstance instanceof SipApplicationSessionListener) {
	        	sipApp.removeAppSessionListener(className);
	        }
			if (servletInstance instanceof SipSessionListener) {
	        	sipApp.removeSessionListener(className);
	        }
	        
	        if (servletInstance instanceof SipSessionStateListener) {
	        	sipApp.removeSipSessionStateListener(className);
	        }
	        
	        if (servletInstance instanceof SipApplicationSessionStateListener) {
	        	sipApp.removeApplicationSessionStateListener(className);
	        }
	        if (servletInstance instanceof SipApplicationSessionActivationListener) {
	        	sipApp.removeApplicationSessionActivationListener(className);
	        }
	        if (servletInstance instanceof SipErrorListener) {
	        	sipApp.removeErrorListener(className);
	        }
	        if (servletInstance instanceof SipSessionAttributeListener) {
	        	sipApp.removeSessionAttributeListener(className);
	        }
	        if (servletInstance instanceof SipServletListener) {
	        	sipApp.removeSipServletListener(className);
	        }
		}
		else
		{
			if(c_logger.isTraceDebugEnabled()){
				// this isn't error because uninitialized servlets won't be
				// included in the list of m_sipAppsServlets.
				c_logger.traceDebug(this,"removeSipletInstance","can not remove listeners for appName["+appName+"] class["+className+"] ");
			}
		}
	}
    /**
    * Cleans up SIP app data for the given application. 
    * IBM-Test-Fixes: OLGH32457
    */
    public void removeInitMember(String appName) {
        c_logger.traceDebug("removeInitMember", " removing InitMember " + appName);
	// Remove InitMember from ThreadLocal map
        sipServletThreadLocal.remove(appName);
	//  Remove servlet instances map  this is critical
        m_sipAppsServlets.remove(appName);
	// Shutdown executor for the app
        shutdownExecutor(appName);
    }
    /**
	 
    * 
    * @param appName
    * @param className
    * @return -  null if not found
    */

    public Object getSipletinstance(String appName, String className) {
        Map<String, Object> app = m_sipAppsServlets.get(appName);
        return app != null ? app.get(className) : null;
    }

   /**
   * @see ServletsInstanceHolder#saveSipletReference(String, SipServlet, ServletContext)
   */
    public void saveSipletReference(String appName, SipServlet sipServlet, ServletContext sipletContext) {
        SipAppDesc sipApp = m_sipcontainer.getSipApp(appName);
        if (sipApp == null) return;

        SipServletDesc sipDesc = sipApp.getSipServlet(sipServlet.getServletName());
        if (!sipDesc.isServletLoadOnStartup() && sipApp.getSipServletListeners().isEmpty()) return;

        InitMembers members = new InitMembers(sipApp, sipServlet, sipletContext);
appExecutors.computeIfAbsent(appName, k -> Executors.newSingleThreadExecutor())
    .submit(() -> {
        sipServletThreadLocal.put(appName, members); // simulate thread-local scoped per app
        try {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "saveSipletReference",
                        "Initializing servlet: " + sipServlet.getServletName() + " for app: " + appName);
            }

            // Save servlet instance for load-on-startup tracking
            members.appDesc.saveLoadOnStartupServlet(sipServlet);

            // Fire SIP Servlet initialized event
            EventsDispatcher.sipServletInitiated(members.appDesc, sipServlet, sipletContext, 0);

        } finally {
            
        }
    });

    }
    /**
    * @see ServletsInstanceHolder#triggerSipletInitServlet()
    */
    public void triggerSipletInitServlet(int appQueueIndex, String appName) {
        // No-op with executor model, already triggered
    }

    @Override
    public void triggerSipletInitServlet(int timestamp) {
        // implement what this method is supposed to do
    }

    public void saveOnStartupServlet(String appName) {
        // No-op with executor model, initialization already dispatched
    }

    @Override
    public void saveOnStartupServlet() {
    }

    private void shutdownExecutor(String appName) {
        ExecutorService executor = appExecutors.remove(appName);
        if (executor != null) {
            executor.shutdownNow();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "shutdownExecutor", "Shutdown executor for app: " + appName);
            }
        }
    }
}
