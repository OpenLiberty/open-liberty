/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.monitor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;
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
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.util.ServletUtil;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;


public class WebAppMonitorListener
	implements ServletListener, ServletInvocationListener, ServletErrorListener, ApplicationListener, AsyncListener {
        protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.WebAppPmiListener";

	private static final String defaultJ2eeNameStr = "defaultJ2eeNameStr"; //TODO: how does this default effect proper registration;

	// Per-app PMI modules keyed by module name (also the StatsInstance name). This listener is
	// registered once as a global listener and shared by every web app, so per-app state must
	// not live in scalar fields.
	private final ConcurrentHashMap<String, WebAppPerf> appModules = new ConcurrentHashMap<String, WebAppPerf>();


        public WebAppMonitorListener() {
        }

	public void onApplicationAvailableForService(ApplicationEvent evt) {
		String modName = getModuleName(evt.getServletContext());
		if (modName != null)
			initializeAppCounters(modName);
	}

	public void onApplicationUnavailableForService(ApplicationEvent evt) {
	               WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A
                if (appPmi != null)
			appPmi.onApplicationUnavailableForService();
	}

	public void onApplicationStart(ApplicationEvent evt) {
		WebApp webApp = (WebApp) evt.getSource();
		ModuleMetaData mmd = (WebModuleMetaData) webApp.getModuleMetaData();
		WebAppPerf appPmi = initializeAppCounters(mmd.getName());
		if (appPmi != null)
			appPmi.onApplicationStart();
	}

	public void onApplicationEnd(ApplicationEvent evt) {
		              WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A
		if (appPmi != null) {
			appPmi.onApplicationEnd();
		}
	}

	public void onServletStartService(ServletInvocationEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

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
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null){
			HttpServletRequest hReq = evt.getRequest();
			if (hReq==null)
				appPmi.onServletFinishService(evt.getServletName(), evt.getResponseTime(),null);
			else
				appPmi.onServletFinishService(evt.getServletName(), evt.getResponseTime(), RequestUtils.getURIForCurrentDispatch(evt.getRequest()));
		}
	}

	public void onServletStartInit(ServletEvent evt) {
		if (isStatisticsEnabled(evt)) {
			Object obj = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
			String j2eeNameStr = (obj instanceof WebComponentMetaData)
					? ((WebComponentMetaData) obj).getName()
					: defaultJ2eeNameStr;
			WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A
			if (appPmi != null)
				appPmi.onServletStartInit(j2eeNameStr, evt.getServletName());
		}
	}

	public void onServletFinishInit(ServletEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null)
			appPmi.onServletFinishInit(evt.getServletName());
	}

	public void onServletStartDestroy(ServletEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null)
			appPmi.onServletStartDestroy(evt.getServletName());
	}

	public void onServletFinishDestroy(ServletEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null)
			appPmi.onServletFinishDestroy(evt.getServletName());
	}

	public void onServletUnloaded(ServletEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A
		if (appPmi != null)
			appPmi.onServletUnloaded(evt.getServletName());
	}

	public void onServletAvailableForService(ServletEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null)
			appPmi.onServletAvailableForService(evt.getServletName());
	}

	public void onServletUnavailableForService(ServletEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null)
			appPmi.onServletUnavailableForService(evt.getServletName());
	}

	public void onServletInitError(ServletErrorEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A
		if (appPmi != null)
			appPmi.onServletInitError(evt.getServletName());
	}

	public void onServletServiceError(ServletErrorEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A

		if (appPmi != null)
			appPmi.onServletServiceError(evt.getServletName());
	}

	public void onServletServiceDenied(ServletErrorEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf(evt.getServletContext()); // @539186A
		if (appPmi != null)
			appPmi.onServletServiceDenied(evt.getServletName());
	}

	public void onServletDestroyError(ServletErrorEvent evt) {
                WebAppPerf appPmi = getOrCreateAppPerf((evt != null) ? evt.getServletContext() : null); // @539186A

		if (evt != null && evt.getServletName() != null && appPmi != null) {
			appPmi.onServletDestroyError(evt.getServletName());
		}
	}

	// Returns the module name of the web app that owns the given ServletContext, or null if it
	// cannot be determined (e.g. FileServletWrapper events, which carry a null context).
	// WebAppConfig.getModuleName() is the same value mmd.getName() returns in onApplicationStart.
	private String getModuleName(ServletContext ctx) {
		if (ctx instanceof IServletContext) {
			WebAppConfig cfg = ((IServletContext) ctx).getWebAppConfig();
			if (cfg != null)
				return cfg.getModuleName();
		}
		return null;
	}

	// Initializes (or replaces) the PMI module for the given app and returns it.
	// Returns null if PMI is not enabled.
	public WebAppPerf initializeAppCounters(String appName) {
		if (!StatsFactory.isPMIEnabled())
			return null;
		WebAppPerf appPmi = new WebAppModule(appName, true);
		appModules.put(appName, appPmi);
		return appPmi;
	}

	// 19@539186A
	// Returns the PMI module for the web app that owns the given ServletContext, creating it
	// if PMI is enabled but none exists yet.
	public WebAppPerf getOrCreateAppPerf(ServletContext ctx) {
		String modName = getModuleName(ctx);
		if (modName == null)
			return null;
		return appModules.computeIfAbsent(modName, name -> new WebAppModule(name, true));
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

	@Override
	public void onComplete(AsyncEvent asyncEvent) throws IOException {
		ServletRequest request = asyncEvent.getSuppliedRequest();
		if (request==null){
			request = asyncEvent.getAsyncContext().getRequest();
		}
		WebAppPerf appPmi = getOrCreateAppPerf(request.getServletContext()); // @539186A

		if (appPmi != null)
		{
			
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
