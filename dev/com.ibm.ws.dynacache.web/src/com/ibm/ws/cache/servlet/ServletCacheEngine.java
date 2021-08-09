/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.web.ServletCacheServiceImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.webcontainer.cache.CacheManager;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * This class provides a hook to enable dynamic implementation of the
 * servlet/JSP caching code.
 */
@Component(configurationPolicy=ConfigurationPolicy.IGNORE, property="service.vendor=IBM")
public class ServletCacheEngine implements CacheManager {

	private static TraceComponent tc = Tr.register(ServletCacheEngine.class,"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	private AtomicReference<ServletCacheServiceImpl> servletCacheServiceReference = new AtomicReference<ServletCacheServiceImpl>(null);
	protected static List<String> staticContentPolicies = new ArrayList<String>();
	public static Set<String> excludedServlets = new HashSet<String>(9);
	public static Set<String> contextRootsWithCachespecXMLs = new ConcurrentSkipListSet<String>();

	static {
		excludedServlets.add("org.apache.jasper.runtime.JspServlet");
		excludedServlets.add("com.ibm.ws.webcontainer.jsp.servlet.JspServlet");
		excludedServlets.add("com.ibm.ws.webcontainer.servlet.InvokerServlet");
		excludedServlets.add("com.ibm.wps.portletcontainer.jasper.runtime.JspServlet");
		excludedServlets.add("com.ibm.ws.jsp.servlet.JspServlet");
		excludedServlets.add("com.ibm.ws.console.core.servlet.DownloadFileServlet");
		excludedServlets.add("com.ibm.ws.management.filetransfer.servlet.FileTransferServlet");
		excludedServlets.add("com.ibm.ws.portletcontainer.portletserving.PortletServingServlet");
		excludedServlets.add("com.ibm.ws.websvcs.transport.http.WASAxis2Servlet");
	}

	public javax.servlet.Servlet getProxiedServlet(javax.servlet.Servlet s) {

		if (tc.isEntryEnabled())
			Tr.entry(tc, "getProxiedServlet", s);

		try {

			String name = s.getClass().getName().toLowerCase();
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "looking for ", s.getServletConfig(), name );
			}
			
			if (null != s.getServletConfig()){
				String contextRoot = s.getServletConfig().getServletContext().getContextPath().isEmpty() ? 
		                    "/" : s.getServletConfig().getServletContext().getContextPath();
				if (null != getServletCache() || !excludedServlets.contains(name)) {
					if (contextRootsWithCachespecXMLs.contains(contextRoot)) {
						s = new ServletWrapper(s);
					}
				}
			} else { // Figure out the contextRoot from the thread
				ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
				  if (cmd != null) {
			          ModuleMetaData mmd = cmd.getModuleMetaData();
			          if (mmd instanceof WebModuleMetaData){
			        	  WebModuleMetaData wmmd = (WebModuleMetaData) mmd;
			        	  String contextRoot = wmmd.getConfiguration().getContextRoot();
			        	  if (tc.isDebugEnabled()){
			        		  Tr.debug(tc,"Pulled out contextRoot ", contextRoot);
			        	  }
			        	  if (isStaticFileCachingEnabled(contextRoot)){
			        		  s = new ServletWrapper(s);
			        	  }
			          }
			      }
			}
		} catch (Exception e){
            FFDCFilter.processException(e, this.getClass().getName() + ".getProxiedServlet()", "90");
		}
		if (tc.isEntryEnabled())
			Tr.exit(tc, "getProxiedServlet", s);
		return s;
	}
	

	public void handleServlet(javax.servlet.Servlet s, HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		CacheHook.handleServlet(s, req, res);
	}

	public static void addStaticContentPolicy(String uri) throws Exception {
		staticContentPolicies.add(uri);
	}

	public ServletCacheServiceImpl getServletCache() {
		return servletCacheServiceReference.get();
	}

	@Reference
	protected void setServletCache(ServletCacheServiceImpl sc) {
		servletCacheServiceReference.set(sc);
	}

	protected void unsetServletCache(ServletCacheServiceImpl serverCache) {
		servletCacheServiceReference.compareAndSet(serverCache, null);
	}

	@Override
	public boolean isStaticFileCachingEnabled(String contextRoot) {
		return staticContentPolicies.contains(contextRoot);
	}

}
