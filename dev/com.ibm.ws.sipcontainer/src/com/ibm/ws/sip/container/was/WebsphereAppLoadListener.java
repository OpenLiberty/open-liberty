/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServlet;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.ServletsInstanceHolder;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.webcontainer.osgi.metadata.WebModuleMetaDataImpl;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInitializationCollaborator;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Listen to WebApp initialization events sent by Websphere
 * 										when it loads new web applications
 * 
 * @author Nitzan
 * 
 * 
 * A declarative services component.
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
configurationPid = "com.ibm.ws.sip.container.was.WebsphereAppLoadListener",
service = com.ibm.wsspi.webcontainer.collaborator.WebAppInitializationCollaborator.class,
property = {"service.vendor=IBM"} )
public class WebsphereAppLoadListener implements WebAppInitializationCollaborator
{
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger =
                    Log.get(WebsphereAppLoadListener.class);

    @Override
    public void started(Container moduleContainer) {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug( "started");
    	}
    	try {
    		WebAppConfiguration webConfig = (WebAppConfiguration) moduleContainer.adapt(WebAppConfig.class);
			String name = webConfig.getModuleName();
			SipAppDescManager appDescMangar  = SipAppDescManager.getInstance();
			
			SipAppDesc appDesc = appDescMangar.getSipAppDesc(name);
			if(appDesc == null) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("SipModuleStateListener Failed finding SipAppDesc for "+name);
				}
				return;
			}
			
			String vhost = webConfig.getVirtualHostName();
			
			appDesc.setVirtualHost(vhost, webConfig.getVirtualHostList());
			List<SipServletDesc> siplets = appDesc.getSipServlets();
			
			for (SipServletDesc sipDesc : siplets) {
				// if the application contains load on startup servlets we
				// need to initialize the application now.
				// we can't simply use the Web Container initialization
				// because of additional SIP Container properties that can
				// only be set after the initialization
				if (sipDesc.isServletLoadOnStartup()) {

					
					try {
						
						appDescMangar.initSipAppIfNeeded(name);
					} catch (ServletException e) {
						if (c_logger.isErrorEnabled()) {
							c_logger.error("error.init.loadonstartup.app",null, e.getLocalizedMessage());
						}

					} catch (Throwable e) {
						if (c_logger.isErrorEnabled()) {
							c_logger.error("error.init.loadonstartup.app",null, e.getLocalizedMessage());
						}
					}
				
					break;
				}
			}
			List<SipServlet> sipServletsList =appDesc.getLoadOnStartupServlets();
			 synchronized (sipServletsList) {
			Iterator<SipServlet> iterator = sipServletsList.iterator();
			while(iterator.hasNext()){
				SipServlet sipServlet = (SipServlet) iterator.next();
				EventsDispatcher.sipServletInitiated(appDesc, sipServlet , webConfig.getWebApp(), -1);
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(null, "module started", "Triggering event servlet initialized, servlet name " +sipServlet.getServletName());
				}
			}
			 }

		} catch (UnableToAdaptException e) {

        	if (c_logger.isTraceDebugEnabled()) {
        		c_logger.traceDebug(null, "started","not SipAppDesc found not a sip application: " +  moduleContainer.getName(), e);
            }
        }finally {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(null, "moduleStarted");
			}
		}
    }

    @Override
    public void starting(Container moduleContainer) {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug( null, "starting", moduleContainer.getName());
    	}
    }

   

    @Override
    public void stopped(Container moduleContainer) {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug( "stopped");
    	}
    }

    @Override
    public void stopping(Container moduleContainer) {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(null, "stopping", moduleContainer.getName());
        }
        try {

            WebAppConfiguration webConfig = (WebAppConfiguration) moduleContainer.adapt(WebAppConfig.class);
            String moduleName = webConfig.getModuleName();
            WebApp webApp = webConfig.getWebApp();
            SipAppDesc appDesc = SipAppDescManager.getInstance().getSipAppDesc(webApp);

            if (null != appDesc && appDesc.wasInitialized()) {

                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "stopping", "SIP Application: module=" + moduleName + ", app=" + appDesc.getAppName());
                }
                PropertiesStore store = PropertiesStore.getInstance();
                boolean invalidate = store.getProperties().getBoolean(CoreProperties.INVALIDATE_SESSION_ON_SHUTDOWN);
                if (invalidate) {
                    invalidateAppSessions(appDesc.getApplicationName());//fix for 385019
                }

                List<SipServletDesc> serlvets = appDesc.getSipServlets();
                //Remove listeners and remove from ServetsInstanceHolder
                //TODO Liberty test code might not work
                String appName = appDesc.getApplicationName();
                if (serlvets != null) {
                    removeSipServlets(appName, serlvets);
                }

                SipContainer.getInstance().unloadAppConfiguration(appDesc.getAppName());
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "stopping", "applications left running = " + SipContainer.getInstance().getNumOfRunningApplications());
                }
              

            }
            
        } catch (UnableToAdaptException e) {

        	if (c_logger.isTraceDebugEnabled()) {
        		c_logger.traceDebug(null, "stopping","not SipAppDesc found not a sip application: " +  moduleContainer.getName(), e);
            }
        }

    }

    /**
     * Invalidating all app sessions related to this application
     * fix for 385019
     * 
     * @param appName
     */
    private void invalidateAppSessions(String sipAppName) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "invalidateAppSessions", "SIP Application: " + sipAppName);
        }
        List<SipApplicationSessionImpl> snapshot = SessionRepository.getInstance().getAllAppSessions();
        for (Iterator<SipApplicationSessionImpl> itr = snapshot.iterator(); itr.hasNext();) {
            SipApplicationSession appSession = itr.next();
            String appName = appSession.getApplicationName();
            if (appName.equals(sipAppName)) {
                appSession.invalidate();
            }
        }
    }
    
    /**
     * DS method to activate this component.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(ComponentContext context) {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug("activated");
        }
    }

    /**
     * DS method to deactivate this component.
     * 
     * @param reason int representation of reason the component is stopping
     */
    public void deactivate(int reason) {
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug("deactivated, reason=" + reason);
        }

    }

    /**
     * REmoving servlets instances
     * @param appname
     * @param servlets
     */
    private void removeSipServlets(String appname, List<SipServletDesc> servlets) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "removeSipletServlets");
        }
        Iterator<SipServletDesc> it = servlets.iterator();
        while (it.hasNext()) {
            SipServletDesc desc = it.next();
            ServletsInstanceHolder.getInstance().removeSipletInstance(appname, desc.getClassName());
        }
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "removeSipServlets");
        }
    }

}