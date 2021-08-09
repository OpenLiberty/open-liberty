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
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.CacheableServlet;
import com.ibm.ws.cache.CacheConfig;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.config.ConfigEntry;
import com.ibm.ws.cache.web.config.ConfigManager;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

public class ServletWrapper implements CacheableServlet, Servlet, HttpJspPage {
    
	private static final long serialVersionUID = 5502601840576150659L;
    private static  TraceComponent tc = Tr.register(ServletWrapper.class,
    		"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

                    Servlet     proxied                 = null;                    
                    boolean     wrapsCacheableServlet   = false;
    private         HashMap<String, ConfigEntry>     activeConfigEntries     = new HashMap<String, ConfigEntry>();

    public ServletWrapper(Servlet s) {
        proxied = s;
        wrapsCacheableServlet = s instanceof CacheableServlet;
    }

    public void configChanged() {
    }

    public void prepareMetadata(CacheProxyRequest request, CacheProxyResponse response) {
        if (tc.isEntryEnabled()) Tr.entry(tc, "prepareMetadata: "+proxied);
        FragmentInfo fragmentInfo = null;

        if (wrapsCacheableServlet) {
            CacheableServlet cacheableServlet = (CacheableServlet) proxied;
            String id = cacheableServlet.getId(request);
            int sharingPolicy = cacheableServlet.getSharingPolicy(request);
            if (id == null) {
                if (tc.isDebugEnabled()) Tr.debug(tc, "cacheId is null");
            } else {
                fragmentInfo = (FragmentInfo) request.getFragmentInfo();
				String requestType = request.getMethod(); 
				StringBuffer idBuf = new StringBuffer();
				if((id.indexOf("requestType="+requestType) == -1) && (requestType.equals("GET") || 
						requestType.equals("POST")) && !fragmentInfo.isIgnoreGetPost()){	    
					fragmentInfo.addDataId(id);
					idBuf.append(id).append(':').append("requestType="+requestType);
					id = idBuf.toString();
				}


                fragmentInfo.setId(id);
                if (tc.isDebugEnabled()) Tr.debug(tc, "sharingPolicy: " + sharingPolicy);
                fragmentInfo.setSharingPolicy(sharingPolicy);
            }

            if (tc.isEntryEnabled()) Tr.exit(tc, "prepareMetadata");
            return;
        }

        ConfigEntry configEntry = null;
        String contextRoot = request.getContextPath();
        String portletContextRoot = request._getContextPath();
        String uri = buildName(request);
       	configEntry = ConfigManager.getInstance().getServletConfigEntry(proxied, uri, contextRoot );
        
        fragmentInfo = (FragmentInfo) request.getFragmentInfo();
        if (configEntry == null) {
            fragmentInfo.setId(null);
        } else {
            activeConfigEntries.put(configEntry.name,configEntry);
            FragmentCacheProcessor fcp = (FragmentCacheProcessor) 
            	ConfigManager.getInstance().getCacheProcessor(configEntry);
            try {
                fcp.execute(request, response, proxied);
                fragmentInfo.setId(fcp.getId());
                if (fcp.getId() != null) {
                    //reuse fragmentinfo from cacheproxyrequest
                    fcp.populateFragmentInfo(fragmentInfo);
                }
                JSPCache jspCache = (JSPCache)ServerCache.getJspCache(configEntry.instanceName);
                if (jspCache != null) {
                    ArrayList invalidations = fcp.getInvalidationIds();
                    if (invalidations != null) {
                        int sz = invalidations.size();
                        for (int i = 0; i < sz; i++) {
                            jspCache.invalidateById((String) invalidations.get(i), i == sz - 1);
                        }
                    }
                    
                   
                }
            } finally {
                ConfigManager.getInstance().returnCacheProcessor(fcp);
            }

        }
        if (tc.isEntryEnabled()) Tr.exit(tc, "prepareMetadata");
    }

    //----------------------------------------------
    // Builds the context path + matched servlet path
    //----------------------------------------------
    String buildName(CacheProxyRequest request) {
        /*String s =  request.getAbsoluteUri();
        if (s == null)
            return null;
        int i = s.indexOf('?');
        if (i == -1)
            return s;
        return s.substring(0,i);*/
    	
    	String s = "";
    	String contextPath = request._getContextPath();
    	String relativeUri = request.getRelativeUri();
    	s = contextPath + relativeUri;
    	if (contextPath != null && contextPath.equals("/") && relativeUri != null && relativeUri.startsWith("/")) {  //PK58534
    		s = relativeUri;
    	}
        int i = s.indexOf('?');
        if (i == -1)
            return s;
        return s.substring(0,i);
    	
    }
    //----------------------------------------------

    /**
     * This executes the algorithm to compute the cache id.
     *
     * @param request The HTTP request object.
     * @return The cache id.  A null indicates that the JSP should
     * not be cached.
     */
    public String getId(HttpServletRequest req) {
        CacheProxyRequest request = (CacheProxyRequest) req;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getId() for " + proxied + " returned " + request.getFragmentInfo().getId());
        return request.getFragmentInfo().getId();
    }

    /**
     * This executes the algorithm to compute the cache id.
     *
     * @param request The HTTP request object.
     * @return The cache id.
     */
    public int getSharingPolicy(HttpServletRequest req) {
        CacheProxyRequest request = (CacheProxyRequest) req;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getSharingPolicy() for " + proxied + " returned " + request.getFragmentInfo().getSharingPolicy());
        return request.getFragmentInfo().getSharingPolicy();
    }

    //Cleans up whatever resources are being held (e.g., memory, file handles, threads) and makes sure that any persistent state is synchronized with the servlet's current in-memory state.
    public void destroy() {
        CacheConfig cacheConfig = ServerCache.getCacheService().getCacheConfig();
        if (false == FrameworkState.isStopping()) {
            //------------------------------------------------------------
            // Invalidate all cache extries associated with this wrapper
            //------------------------------------------------------------
            Iterator i = activeConfigEntries.values().iterator();
            while ( i.hasNext() ) {
                ConfigEntry ce = (ConfigEntry)i.next();
                JSPCache jspCache = (JSPCache)ServerCache.getJspCache(ce.instanceName);
                if (jspCache != null ) {
                    if (cacheConfig.disableTemplateInvalidation) {
                        //dynacache.error=DYNA0015I: Dynamic Servlet Caching encountered an error: {0}
                        Tr.info(tc, "dynacache.error", "Error type=destroy warning. Skipping template invalidation for JSP "+ce.name);
                    } else {
                        jspCache.invalidateByTemplate(ce.name, true);  
                    }
                }
            }
        }
        activeConfigEntries.clear();
        //------------------------------------------------------------
        proxied.destroy();
    }

    //Returns a servlet config object, which contains any initialization parameters and startup configuration for this servlet.
    public ServletConfig getServletConfig() {
        return proxied.getServletConfig();
    }

    //Returns a string containing information about the servlet, such as its author, version, and copyright.
    public String getServletInfo() {
        return proxied.getServletInfo();
    }

    public ServletContext getServletContext() {
        return((Servlet) proxied).getServletConfig().getServletContext();
    }

    //Initializes the servlet, and removes from the cache any entries created from previous
    //versions of this servlet
    public void init(ServletConfig sc) throws ServletException {
        if (tc.isDebugEnabled()) Tr.debug(tc, "init()");
        proxied.init(sc);
    }
    
    /**
	 * This replaces the Servlet method. It calls the static
	 * CacheHook.handleServlet method as the entry point into caching.
	 * 
	 * @param request
	 *            The HTTP request object.
	 * @param response
	 *            The HTTP response object.
	 */
	public void service(ServletRequest request, ServletResponse response)
			throws IOException, ServletException {

		try {

			if (tc.isDebugEnabled()) {
				Tr.entry(tc,"service", new Object[]{request, response});				
			}
			
			CacheHook.handleServlet(this, (HttpServletRequest) request,	(HttpServletResponse) response);

		} catch (ClassCastException ex) {
			if (tc.isDebugEnabled()){
				Tr.debug(tc, "request instanceof HttpServletRequest: "+ ( request instanceof HttpServletRequest));
				Tr.debug(tc, "response instanceof HttpServletRequest: "+ ( response instanceof HttpServletResponse));
				Tr.error(tc, "Exception while calling Dynacache: " +ex.getMessage());
			}
			
			skipCacheService(request, response);
		}
		
		if (tc.isDebugEnabled()){
			Tr.exit(tc,"service");
		}
	}

	private void skipCacheService(ServletRequest request,
			ServletResponse response) throws IOException, ServletException {
		
		if (tc.isDebugEnabled()){
			Tr.entry(tc, "skipCacheService");
		}
		
		if (tc.isDebugEnabled()) {
			Tr.debug(tc, "Wanted a HttpServletRequest, HttpServletResponse got " + request
					+ " and "+ response + "  servicing as normal");
		}

		serviceProxied(request, response);
		
		if (tc.isDebugEnabled()){
			Tr.exit(tc, "skipCacheService");
		}
	}
	
    /**
	 * This returns the servlet that this one proxies.
	 * 
	 * @return The proxied servlet.
	 */
    public Servlet getProxiedServlet()
    {
        return proxied;
    }

    //Carries out a single request from the client.
    public void serviceProxied(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        proxied.service(request, response);
    }

    /**
     * included for jsp 1.0 support
     */
    public void _jspService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ((HttpJspPage) proxied)._jspService(request, response);
    }

    public void jspInit() {
        ((HttpJspPage) proxied).jspInit();
    }


    public void jspDestroy() {
        ((HttpJspPage) proxied).jspDestroy();
    }

}
