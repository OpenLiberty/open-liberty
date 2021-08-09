/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.monitor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;
import com.ibm.websphere.servlet.event.ServletContextEventSource;
import com.ibm.websphere.servlet.event.ServletErrorEvent;
import com.ibm.websphere.servlet.event.ServletErrorListener;
import com.ibm.websphere.servlet.event.ServletEvent;
import com.ibm.websphere.servlet.event.ServletInvocationEvent;
import com.ibm.websphere.servlet.event.ServletInvocationListener;
import com.ibm.websphere.servlet.event.ServletListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.util.RequestUtils;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.webcontainer.async.WSAsyncEvent;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.ServletUtil;


public class WebappMonitor
	implements ServletListener, ServletInvocationListener, ServletErrorListener, ApplicationListener, AsyncListener {
        protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.WebAppPmiListener";

	J2EEName _appName = null;
	private static final String defaultJ2eeNameStr = "defaultJ2eeNameStr"; //TODO: how does this default effect proper registration;
	boolean areAppAggregatesInited = false;

	// instace of WebAppPerf
	WebAppPerf appPmi = null;

	// track state of appPmi object to avoid "instanceof" checks
	// in checkAppCounters
        // 4@539186A
	private static final int APP_PMI_NULL = 0;
	private static final int APP_PMI_SMF = 1;
	private static final int APP_PMI_WEB = 2;
        private int appPmiState = APP_PMI_NULL;


        public WebappMonitor() {
        // TODO Auto-generated constructor stub
           // System.out.println("in constructor");
        }
	public void onApplicationAvailableForService(ApplicationEvent evt) {
//System.out.println("1st");
		//System.out.println("onApplicationAvailableForService: " + evt.getServletContext().getServerInfo());
		if (areAppAggregatesInited == false) {
		   // System.out.println("1");
			initializeAppCounters(getAppName());
			areAppAggregatesInited = true;
		}
	}

	public void onApplicationUnavailableForService(ApplicationEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onApplicationUnavailableForService: " + evt.getServletContext().getServerInfo());
		if (appPmi != null)
			appPmi.onApplicationUnavailableForService();
	}

	public void onApplicationStart(ApplicationEvent evt) {

		WebApp webApp = (WebApp) evt.getSource();
		
		ModuleMetaData mmd = (WebModuleMetaData)webApp.getModuleMetaData();
		//System.out.println(webApp.getName());
		
		_appName = mmd.getJ2EEName();
//System.out.println("AppName is "+_appName);
		//register as a listener
		ServletContextEventSource evtSource =
			(ServletContextEventSource) evt.getServletContext().getAttribute(ServletContextEventSource.ATTRIBUTE_NAME);
		evtSource.addServletErrorListener(this);
		evtSource.addServletListener(this);
		evtSource.addServletInvocationListener(this);

		//initialize aggregates for this app
		if (areAppAggregatesInited == false) {
		    //System.out.println("2");
			initializeAppCounters(mmd.getName());
			areAppAggregatesInited = true;
		}

		if (appPmi != null)
			appPmi.onApplicationStart();
	}

	public void onApplicationEnd(ApplicationEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onApplicationEnd: " + evt.getServletContext().getServerInfo());
		if (appPmi != null) {
			appPmi.onApplicationEnd();
		}
	}

	public void onServletStartService(ServletInvocationEvent evt) {
                checkAppCounters(); // @539186A

		if (appPmi != null)
		{
			HttpServletRequest hReq = evt.getRequest();
			if (hReq==null)
				appPmi.onServletStartService(evt.getServletName(),null);
			else
				appPmi.onServletStartService(evt.getServletName(),RequestUtils.getURIForCurrentDispatch(evt.getRequest()));
		}
	}

	public void onServletFinishService(ServletInvocationEvent evt) {
                checkAppCounters(); // @539186A

		if (appPmi != null){
			HttpServletRequest hReq = evt.getRequest();
			if (hReq==null)
				appPmi.onServletFinishService(evt.getServletName(), evt.getResponseTime(),null);
			else
				appPmi.onServletFinishService(evt.getServletName(), evt.getResponseTime(), RequestUtils.getURIForCurrentDispatch(evt.getRequest()));
		}
	}

	public void onServletStartInit(ServletEvent evt)
	{
		if (isStatisticsEnabled(evt)){
			Object obj = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
			String j2eeNameStr=null;
			if (obj instanceof WebComponentMetaData){
				WebComponentMetaData cmd =
					(WebComponentMetaData) ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
				J2EEName j2eeName = cmd.getJ2EEName();
				j2eeNameStr = cmd.getName();
			}
			if (j2eeNameStr==null){
				j2eeNameStr = defaultJ2eeNameStr;
			}

                        checkAppCounters(); // @539186A
			if (appPmi != null)
				appPmi.onServletStartInit(j2eeNameStr, evt.getServletName());
		}
	}

	public void onServletFinishInit(ServletEvent evt) {
                checkAppCounters(); // @539186A

		if (appPmi != null)
			appPmi.onServletFinishInit(evt.getServletName());
	}

	public void onServletStartDestroy(ServletEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onServletStartDestroy: " + evt.getServletName());
		if (appPmi != null)
			appPmi.onServletStartDestroy(evt.getServletName());
	}

	public void onServletFinishDestroy(ServletEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onServletFinishDestroy: " + evt.getServletName());
		if (appPmi != null)
			appPmi.onServletFinishDestroy(evt.getServletName());
	}

	public void onServletUnloaded(ServletEvent evt) {
                checkAppCounters(); // @539186A
		if (appPmi != null)
			appPmi.onServletUnloaded(evt.getServletName());
	}

	public void onServletAvailableForService(ServletEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onServletAvailableForService: " + evt.getServletName());
		if (appPmi != null)
			appPmi.onServletAvailableForService(evt.getServletName());
	}

	public void onServletUnavailableForService(ServletEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onServletUnavailableForService: " + evt.getServletName());
		if (appPmi != null)
			appPmi.onServletUnavailableForService(evt.getServletName());
	}

	public void onServletInitError(ServletErrorEvent evt) {
                checkAppCounters(); // @539186A

		//System.out.println("onServletInitError: " + evt.getServletName() + ":" + evt.getRootCause());
		if (appPmi != null)
			appPmi.onServletInitError(evt.getServletName());
	}

	public void onServletServiceError(ServletErrorEvent evt) {
                checkAppCounters(); // @539186A

		if (appPmi != null)
			appPmi.onServletServiceError(evt.getServletName());
	}

	public void onServletServiceDenied(ServletErrorEvent evt) {
                checkAppCounters(); // @539186A

		// System.out.println("onServletServiceDenied: " + evt.getServletName() + ":" + evt.getRootCause());
		if (appPmi != null)
			appPmi.onServletServiceDenied(evt.getServletName());
	}

	public void onServletDestroyError(ServletErrorEvent evt) {
                checkAppCounters(); // @539186A

		if (evt != null && evt.getServletName() != null && appPmi != null) {
			appPmi.onServletDestroyError(evt.getServletName());
		}
	}

	// This method initialize the module instance and register it.
	public void initializeAppCounters(String appName) {
 
	    
	  //  System.out.println(StatsFactory.isPMIEnabled());
			if (StatsFactory.isPMIEnabled()) {
			//    System.out.println("PMI is forcenbly ena bled");
				synchronized(this) {
                    appPmi = new WebAppModule (appName, true); // @539186C
                    appPmiState = APP_PMI_WEB; // @539186A
			    }    
			}    
        
    }

        // 19@539186A
        public void checkAppCounters() {
  
                               // System.out.println("checkAppCounters");
                                	     if (appPmiState != APP_PMI_WEB) {
                                	     //    System.out.println("asdfcxzvsadfasdfas");
                                             synchronized (this) {
                                        	     if (appPmiState != APP_PMI_WEB) {
                                                     appPmi = new WebAppModule(getAppName(), true);
                                                     appPmiState = APP_PMI_WEB;
                                        	     }    
                                        	 }    
                                        }      

                        
                
	}

	public boolean isStatisticsEnabled(ServletEvent evt){
		Object evtSrc = evt.getSource();
		if (evtSrc instanceof IServletWrapper){
			IServletConfig sConfig = (IServletConfig)((IServletWrapper)evtSrc).getServletConfig();
			if (sConfig==null||sConfig.isStatisticsEnabled()){
				if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
					logger.logp(Level.FINE, CLASS_NAME,"isStatisticsEnabled","pmi enabled for the servlet-->[" +evt.getServletName() +"]");
				return true;
			}
			else{
				if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
					logger.logp(Level.FINE, CLASS_NAME,"isStatisticsEnabled","pmi disabled for the servlet-->[" +evt.getServletName() +"]");
				return false;
			}
		}
		else{
			if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
				logger.logp(Level.FINE, CLASS_NAME,"isStatisticsEnabled","Event Source is not a ServletWrapper, we have to assume statistics enabled is true");
			return true;
		}
	}

	public String getAppName() {
		return _appName.toString();
	}

	@Override
	public void onComplete(AsyncEvent asyncEvent) throws IOException {
        checkAppCounters(); // @539186A

		if (appPmi != null)
		{
			ServletRequest request = asyncEvent.getSuppliedRequest();
			if (request==null){
				request = asyncEvent.getAsyncContext().getRequest();
			}
			
			long responseTime = ((WSAsyncEvent)asyncEvent).getElapsedTime();
			
			IExtendedRequest iExtReq = ServletUtil.unwrapRequest(request);
			String servletName = iExtReq.getWebAppDispatcherContext().getCurrentServletReference().getName();
			
			HttpServletRequest httpReq = null;
			
			if (request instanceof HttpServletRequest)
				httpReq = (HttpServletRequest)request;
			else
				httpReq = iExtReq;

			appPmi.onAsyncContextComplete(servletName,  responseTime, RequestUtils.getURIForCurrentDispatch(httpReq));
		}
	}

	@Override
	public void onStartAsync(AsyncEvent asyncEvent) throws IOException {

	}
	
	@Override
	public void onError(AsyncEvent asyncEvent) throws IOException {

	}

	@Override
	public void onTimeout(AsyncEvent asyncEvent) throws IOException {

	}

}
