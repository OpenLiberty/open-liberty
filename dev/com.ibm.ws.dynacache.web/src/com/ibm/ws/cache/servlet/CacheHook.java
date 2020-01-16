/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
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
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.cache.CacheEntry;
import com.ibm.ws.cache.CacheConfig;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.ValueSet;
import com.ibm.ws.cache.util.SerializationUtility;
import com.ibm.ws.cache.web.ExternalCacheFragment;
import com.ibm.ws.cache.web.ServletCacheServiceImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.wsspi.webcontainer.WebContainerConstants;

/**
 * This class provides helper functions for the servlet engine to
 * use the dynacache.
 */
public class CacheHook {

   private static TraceComponent tc = Tr.register(CacheHook.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
   
   static WSThreadLocal <FragmentComposer> threadLocalFragmentComposer = new WSThreadLocal<FragmentComposer>();
   static WSThreadLocal <Boolean> threadLocalSkipCache = new WSThreadLocal <Boolean>();
   
   /**
	 * This is called by the ServletWrapper.service method. Casts req and resp
	 * to CacheProxyRequest and CacheProxyResponse. Calls static handleFragment
	 * method.
	 * 
	 * @param servlet The servlet handling the HTTP request.
	 * @param req     The HTTP request object.
	 * @param resp    The HTTP response object.
	 */
   public static void handleServlet(Servlet servlet, HttpServletRequest request, HttpServletResponse response)
   	throws IOException, ServletException 
   {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  
    	  Tr.entry(tc, "handleServlet",request.getDispatcherType());
      
      ServletWrapper sw = null;
      try {
         sw = (ServletWrapper) servlet;
      } catch (ClassCastException ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.CacheHook.handleServlet", "112");
         Tr.error(tc, "dynacache.error", "incorrectly initialized " + servlet + ", couldn't find a ServletWrapper");
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "wanted a ServletWrapper, got " + servlet + ". servicing as normal");
         servlet.service(request, response);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "handleServlet");
         return;
      }
           
      //build the template uri and full uri from the WADR we've received.
      String relativeUri = null;
      String absoluteUri = null;
      String contextPath = null;
      String queryString = null;
      boolean namedDispatch = false;
      String servletName = null;
      String servletClassName = null;
      int portletMethod = FragmentComposer.SERVLET_REQUEST;
      boolean include = false;
      boolean forward = false;
      DispatcherType dispatchType = request.getDispatcherType();
      
	  if (dispatchType.equals(DispatcherType.INCLUDE))
	  	include = true;
	  else if(dispatchType.equals(DispatcherType.FORWARD)) {
	      forward = true;
	  }
		
      try 
      {
      	String pathInfo, servletPath;
		if (include)
		{	
			String cp = (String) request.getAttribute(WebContainerConstants.CONTEXT_PATH_INCLUDE_ATTR);
			
			if (cp == null){
			    //If the attribute javax.servlet.include.context_path is null the request came from a NamedDispatcher.			
			    contextPath = "/"+ servlet.getServletConfig().getServletContext().getServletContextName();
			    servletName = servlet.getServletConfig().getServletName();
			    String clazzName = ((ServletWrapper)servlet).getProxiedServlet().getClass().getName() + ".class";
			    servletClassName = request.getContextPath()==null?clazzName:(request.getContextPath()+"/"+clazzName);
			    namedDispatch = true;
			    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				Tr.debug(tc, "Attribute javax.servlet.include.context_path was null.  " +
						"Request was made using a NamedDispatcher");
			}
			else {				
				contextPath = cp;
				absoluteUri = (String) request.getAttribute(WebContainerConstants.REQUEST_URI_INCLUDE_ATTR );
				pathInfo = (String) request.getAttribute(WebContainerConstants.PATH_INFO_INCLUDE_ATTR);
				servletPath = (String) request.getAttribute(WebContainerConstants.SERVLET_PATH_INCLUDE_ATTR);
				queryString = (String) request.getAttribute(WebContainerConstants.QUERY_STRING_INCLUDE_ATTR);
				String clazzName = ((ServletWrapper)servlet).getProxiedServlet().getClass().getName() + ".class";
				servletClassName = request.getContextPath()==null?clazzName:(request.getContextPath()+"/"+clazzName);
				if (pathInfo != null){		
					relativeUri = new StringBuffer(servletPath).append(pathInfo).toString();
					if(queryString != null) {				
						relativeUri = new StringBuffer(relativeUri).append("?").append(queryString).toString();	
						absoluteUri = new StringBuffer(absoluteUri).append("?").append(queryString).toString();
					}
				}

				else {			
					relativeUri = servletPath;
         		   	if(queryString != null) {				 			
         		   		relativeUri = new StringBuffer(relativeUri).append("?").append(queryString).toString();	
         		       	absoluteUri = new StringBuffer(absoluteUri).append("?").append(queryString).toString();
         		   	}
				}
			}
		}
		else
		{			
			String cp = (String) request.getAttribute(WebContainerConstants.CONTEXT_PATH_FORWARD_ATTR);
														 
			if (cp == null && forward){
			    //If the attribute javax.servlet.forward.context_path is null the request came from a NamedDispatcher.			
			    contextPath = "/"+servlet.getServletConfig().getServletContext().getServletContextName();
			    servletName = servlet.getServletConfig().getServletName();
			    String clazzName = ((ServletWrapper)servlet).getProxiedServlet().getClass().getName() + ".class";
			    servletClassName = request.getContextPath()==null?clazzName:(request.getContextPath()+"/"+clazzName);
			    namedDispatch = true;
			    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				Tr.debug(tc, "Attribute javax.servlet.forward.context_path was null.  " +
						"Request was made using a NamedDispatcher");
			}
			else {
				contextPath = request.getContextPath();
				absoluteUri = request.getRequestURI();
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
				String clazzName = ((ServletWrapper)servlet).getProxiedServlet().getClass().getName() + ".class";
				servletClassName = request.getContextPath()==null?clazzName:(request.getContextPath()+"/"+clazzName);
				
				if(forward)
					queryString = request.getQueryString();

				if (pathInfo != null) {			
					relativeUri = new StringBuffer(servletPath).append(pathInfo).toString();
					if(queryString != null) {				 			
						relativeUri = new StringBuffer(relativeUri).append("?").append(queryString).toString();
						absoluteUri = new StringBuffer(absoluteUri).append("?").append(queryString).toString();
					}
				}
				else {			
					relativeUri = servletPath;			
					if(queryString != null)	{			         			
						relativeUri = new StringBuffer(relativeUri).append("?").append(queryString).toString();	      
						absoluteUri = new StringBuffer(absoluteUri).append("?").append(queryString).toString();
					}
		        }
			}
		}
      } catch (ClassCastException e) {
         com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.servlet.CacheHook.handleServlet", "89");
         Tr.error(tc, "dynacache.error", "Dynacache received unexpected HttpServletRequest:" + request + ". Expected WebAppDispatcherRequest");
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "wanted a WebAppDispatcherRequest, got " + request + ". servicing as normal");
         sw.serviceProxied(request, response);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "handleServlet");
         return;
      }

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
      	 String debugString1;
      	 String debugString2;
      	 if (portletMethod == FragmentComposer.PORTLET_CACHE_REQUEST){
      	 	debugString1 = "handlePortletCache";
      	 	debugString2 = "portlet";
      	 }
      	 else{
      	 	debugString1 = "handleServlet";
      	 	debugString2 = "servlet";
      	 }
      	 
      	 Tr.debug(tc, debugString1 + ": absoluteUri = " + absoluteUri + ", relativeURI="+relativeUri+", contextPath="+contextPath+", include="+include);
      	 Tr.debug(tc, debugString1 + ": " + debugString2 + " = " + servlet + ", request = " + request + ", response = " + response);      	  
      }
      
      
      FragmentComposer parentFragmentComposer = null;
      if (dispatchType.equals(DispatcherType.ASYNC)){
    	  parentFragmentComposer = ((CacheProxyRequest) request).getFragmentComposer();
      } else {
    	  parentFragmentComposer = (FragmentComposer) threadLocalFragmentComposer.get();  
      }
      
      CacheProxyRequest cacheProxyRequest = new CacheProxyRequest(request);
      if(response.getClass().getName().equals( //hack for JSTL
    		  "org.apache.taglibs.standard.tag.common.core.ImportSupport$ImportResponseWrapper")) 
            parentFragmentComposer.setJSTLImport(true);

      //skip-cache-attribute
      checkDiscardSkipCacheAttribute(request, parentFragmentComposer, cacheProxyRequest);      

      cacheProxyRequest.setRequest(request);  
      cacheProxyRequest.setRelativeUri(relativeUri);
      cacheProxyRequest.setAbsoluteUri(absoluteUri);
      cacheProxyRequest.setInclude(include);
      cacheProxyRequest.setNamedDispatch(namedDispatch);
      cacheProxyRequest.setServletName(servletName);
      cacheProxyRequest.setServletClassName(servletClassName);
      cacheProxyRequest._setContextPath(contextPath);
      cacheProxyRequest.setPortletMethod(portletMethod);
      
      CacheProxyResponse cacheProxyResponse = ServletCacheServiceImpl.getInstance().cacheProxyResponseFactory.createCacheProxyResponse(response);
      try {
    	 
    	 /* Entry point into cache hit/miss/proxy code */ 
    	  handleFragment(sw, cacheProxyRequest, cacheProxyResponse, parentFragmentComposer);
         
      } finally {    
    	  
  		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(tc, "threadLocalFragmentComposer set to "+parentFragmentComposer);
		}
  		threadLocalFragmentComposer.set(parentFragmentComposer);
    	
  		cleanup(parentFragmentComposer, cacheProxyRequest, cacheProxyResponse);

      } //finally ends here      
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         Tr.exit(tc, "handleServlet");
   }
   
    private static void cleanup(
		    FragmentComposer parentFragmentComposer,
			CacheProxyRequest cacheProxyRequest,
			CacheProxyResponse cacheProxyResponse) throws IOException {

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.entry(tc, "cleanup", new Object[]{parentFragmentComposer, cacheProxyRequest, cacheProxyResponse});
		}
    	
    	/**
		 * cleanup parent and child ARD and Sync requests & fragmentComposers
		 */
		if (parentFragmentComposer == null) { // ANY PARENT
			setSkipCache(false);
			// TODO is this call to getFragmentComposer needed?
			cacheProxyRequest.getFragmentComposer();
		}		
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.exit(tc, "cleanup");
		}
	}
   
	/**
	 * 
	 * Put an item in the cache and post process the cached response to take
	 * care of ESI and consumed fragments
	 */
	public static void putInCache(FragmentComposer fragmentComposer)
			throws IOException {

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.entry(tc, "putInCache", fragmentComposer);
		}
		
		CacheProxyRequest request = fragmentComposer.getRequest();
		CacheProxyResponse response = fragmentComposer.getResponse();
		FragmentInfo fragInfo = fragmentComposer.getFragmentInfo();
		JSPCache jspCache = (JSPCache)ServerCache.getJspCache(fragmentComposer.getFragmentInfo().getInstanceName());

		// everything is ok, let's get our memento and store it
		String cacheGroup = fragInfo.getExternalCacheGroupId();
		FragmentComposerMemento memento = response.getFragmentComposer().getMemento(request);
		memento.setContainsESIContent(response.getContainsESIContent());

		// good estimate when it was completed.
		// The timeStamp is actually not set on the cache entry until the
		// memento is set into the jspCache
		fragmentComposer.setTimeStamp(System.currentTimeMillis());
		setValue(request, response, fragInfo, jspCache, fragmentComposer, memento, cacheGroup, false);	
			
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.exit(tc, "putInCache");
		}
	}
	
   private static void checkDiscardSkipCacheAttribute(
		   HttpServletRequest request, FragmentComposer parentFragmentComposer, CacheProxyRequest cacheProxyRequest) {
	  
	   if ( parentFragmentComposer == null){
		   
		   // AKS: This is the more common way, so do it first. If it doesn't set skip cache, try old method.
		   FragmentInfo fragmentInfo = (FragmentInfo) cacheProxyRequest.getFragmentInfo();
           String skipCacheAttribute = fragmentInfo.getSkipCacheAttribute();
           if ( skipCacheAttribute != null && request.getAttribute(skipCacheAttribute) != null) {
        	   setSkipCache(true);
        	   return;
           }

           Object previewRequest = request.getAttribute("previewRequest");
           if (previewRequest == null) // AKS: PERF, most of the time this attr will not exist, return immediately.
        	   return;
	    	 
           boolean isPreview = false;
           if (previewRequest instanceof Boolean){
        	   isPreview = ((Boolean)previewRequest).booleanValue();
           }
           else if (previewRequest instanceof String){
        	   isPreview = Boolean.parseBoolean((String)previewRequest);
           }
           if ( isPreview )
        	   setSkipCache(isPreview);      	 
      }
   }
   public static void checkDiscardJSPContentAttribute (HttpServletRequest request, FragmentComposer fc) {
	   
	   // Add for PK22899, need to disable writing out fragments
       Object discardJSPContent = request.getAttribute(CacheConfig.DISCARD_JSP_CONTENT);
       
       // AKS: PERF: Most of the time this will be null, return immediately.
       if (discardJSPContent == null)
    	   return;
       
       Boolean toDiscard = null;
       
       if (discardJSPContent instanceof Boolean)
      	 toDiscard = (Boolean) discardJSPContent;
       else if (discardJSPContent instanceof String)
      	 toDiscard = Boolean.parseBoolean((String) discardJSPContent);     
       if (toDiscard != null) {
      	 setDiscardJSPContent (fc, toDiscard.booleanValue());  	
       }
   }

	// IKEA JSTL Import with var fix...
	// The c:import core JSTL library i.e. org.apache.taglibs.standard.tag.common.core.ImportSupport
	// does a pageContext.getRequest().setAttribute("discardJSPContent",true) in doStartTag()
	// does a pageContext.getRequest().setAttribute("discardJSPContent",false) in doEndTag()
	// For JSPs setting & unsetting of discardJSPContent is done explicitly in the JSPs themselves
    // Note this  discardJSPContent only works when csf=true
	public static void setDiscardJSPContent(FragmentComposer fc, boolean discard) {		
	
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "setDiscardJSPContent", new Object[]{fc,Boolean.valueOf(discard)});
		
		fc.setDiscardJSPContent(discard);   
	}
   
   /**
    * This is called by the static handleServlet method.
    * Gets FragmentInfo from request.
    * Gets cache id from servlet by calling askServletForId static method.
    * Creates a FragmentInfo object, fills in its id and Template,
    * and sets it on the request.
    * Creates a FragmentComposer, fills it and pushes it in response.
    * Writes output to response if indicated by writeOutResponse parameter.
    * Calls static handleCacheableFragment method.
    *
    * @param servlet The servlet handling the HTTP request.
    * @param request The HTTP request object.
    * @param response The HTTP response object.
    */
   private static void handleFragment(ServletWrapper servlet, CacheProxyRequest request, CacheProxyResponse response, FragmentComposer parentFragmentComposer)
      throws ServletException, IOException {
      	   
	   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
	         Tr.entry(tc, "handleFragment "+request + " "+response);
        	 Tr.debug(tc, "parentFragmentComposer: "+parentFragmentComposer);
	   }

		boolean isExternalPage = (parentFragmentComposer == null);
		FragmentComposer fragmentComposer = getFragmentComposer(request, response, parentFragmentComposer);

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "threadLocalFragmentComposer set to " + fragmentComposer);
		}

      threadLocalFragmentComposer.set(fragmentComposer);
      fragmentComposer.setParent(parentFragmentComposer);
      
      // Add for PK22899, PK47988 need to disable writing out fragments
      checkDiscardJSPContentAttribute (request, fragmentComposer);
      
      //setup fragmentcomposer request information
      if ( request.getNamedDispatch())
      	fragmentComposer.setURI(request.getServletName());
      else
      	fragmentComposer.setURI(request.getRelativeUri());

      fragmentComposer.setServletClassName(request.getServletClassName());
      fragmentComposer.setExternalPage(isExternalPage);
      fragmentComposer.setInclude(request.getInclude());
      fragmentComposer.setNamedDispatch(request.getNamedDispatch());
      response.setFragmentComposer(fragmentComposer);
      request.setFragmentComposer(fragmentComposer);
      FragmentInfo fragmentInfo = (FragmentInfo) request.getFragmentInfo();
      //sets a template, which needs to be absolute
      fragmentInfo.setURI(request.getAbsoluteUri());
      fragmentComposer.setFragmentInfo(fragmentInfo);

      //moved attribute readying up so that the attrs are there for the IdGenerator's getId() in
      processRequestAttributes(request, fragmentComposer);
       
      servlet.prepareMetadata(request, response); //find out if the servlet is cacheable?
          
      fragmentComposer.setConsumeSubfragments(fragmentInfo == null ? false : 
    	  fragmentInfo.getConsumeSubfragments());
      fragmentComposer.setDoNotConsume(fragmentInfo == null ? false : 
    	  (fragmentInfo.getDoNotConsume() || fragmentInfo.getDoNotCache()));
   	
	  //code added for hasCacheableConsumingParent
      if (parentFragmentComposer != null){
     	   if (parentFragmentComposer.getHasCacheableConsumingParent() || 
     			   (parentFragmentComposer.getConsumeSubfragments() &&
     					   parentFragmentComposer.getRequest().getCaching())){     		   
     	   		fragmentComposer.setHasCacheableConsumingParent(true);
     	   		fragmentInfo.setHasCacheableConsumingParent(true);
     	   		
     	   }
      }

      String instanceName = fragmentInfo.getInstanceName();	 
      JSPCache jspCache = (JSPCache)ServerCache.getJspCache(instanceName);       
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())					    
		Tr.debug(tc, "CACHE INSTANCE name:" + instanceName); 

      String id = fragmentInfo.getId();	      
      String requestType = request.getMethod();	  
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())					    
	   Tr.debug(tc, "Request Type:" + requestType); 
      
      if (request.getDispatcherType().equals(DispatcherType.ASYNC) && 
    		  parentFragmentComposer.getURI().equals(fragmentComposer.getURI())) {
    	  fragmentComposer.setAsyncDoubleDip(true); // this is the second time I am entering the same servlet after an async dispatch
      }
      
      /*
       * Modified for do-not-cache for edge. If dnc = false, return true from its portion. If dnc = true and fragment is edgeable, return true
       * to make fragment cacheable in order to enter edge side logic. Handle do-not-cache part inside this logic.
       */
      boolean dncResult = !fragmentInfo.getDoNotCache() || (fragmentInfo.getDoNotCache() && fragmentInfo.isEdgeable()); 
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())					    
    	  Tr.debug(tc, "dncResult: " + dncResult+ " skipCache: "+isSkipCache()); 
      
      // if the id generated does not have request type info then the response is marked not cacheable
      // in order to differentiate between responses for different request types (GET, POST, HEAD etc..)
      boolean cacheable = (
    		  (fragmentInfo.getId() != null) && 
    		  jspCache != null && 
    		  !isSkipCache() &&
    		  dncResult); 
      if (!fragmentInfo.isIgnoreGetPost())
         cacheable = cacheable && (id.indexOf("requestType="+requestType)!=-1);
      
      /*
       * For do-not-cache on the edge, if dnc=true and fragment is edgeable, we want to treat it as if it should be cached
       * and then not actually cache it.
       */
      boolean setCaching = cacheable;
      if (fragmentInfo.getDoNotCache() && fragmentInfo.isEdgeable())
    	  setCaching = false;
      request.setCaching(setCaching);
      request.setUncacheable(!setCaching);
	  
      
      if (jspCache.isAutoFlushIncludes()){
          //auto flush imports for eliminating requirement of flush around JSP/JSTL imports d520891
    	  request.setAttribute(WebContainerConstants.DYNACACHE_REQUEST_ATTR_KEY, "true");  
    	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
    		  Tr.debug(tc, "Automatically flushing include: "+fragmentComposer.getURI());
    	  }
      }
     
      // let the parent know we are processing a child
      if (parentFragmentComposer != null)
         parentFragmentComposer.startChildFragmentComposer(fragmentComposer);

      boolean exceptionThrown = true;
      boolean handledSuccessfully = true;
      try {
         //do ESI processing if this is an external request or is a forward because
         //response must complete after a forward...
         ESISupport.handleESIPreProcessing(request, response, fragmentInfo);
         
         if (cacheable) {
            if (isExternalPage)
               externalPreInvoke(fragmentInfo.externalCacheGroupId, request, response);
            handledSuccessfully = false;
            
            //code path for cache miss or a cache hit
            handleCacheableFragment(servlet, request, response, fragmentInfo, jspCache);  
            
            handledSuccessfully = true;
            exceptionThrown = !handledSuccessfully;
         } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) 
               Tr.debug(tc, "CACHE?  NO, servlet: " + servlet);
            
            if (fragmentComposer.getHasCacheableConsumingParent()){
            	fragmentComposer.setCacheType(FragmentComposer.POPULATED_CACHE);
                if (jspCache.isCascadeCachespecProperties()){ //PK38811
                    cascadeFragmentInfoFromParent(fragmentComposer);
                }
            } else {
            	fragmentComposer.setCacheType(FragmentComposer.NOT_CACHED);
            }           	
            
            servlet.serviceProxied(request, response); //no cache policy for this request
            exceptionThrown = false;
         }
         
      } finally {

		if (!fragmentComposer.isAsyncDispatch()){
			postProcess(request, response, fragmentComposer, cacheable, exceptionThrown);					
		}			
		
      }

	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		Tr.exit(tc, "handleFragment");

   }
   
	private static FragmentComposer getFragmentComposer(
			CacheProxyRequest req, CacheProxyResponse resp, FragmentComposer parentFC) {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.entry(tc, "getFragmentComposer "+ req.getRelativeUri());	
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "SYNC INCLUDE " + req.getRelativeUri());
		}
		FragmentComposer fragmentComposer = ServletCacheServiceImpl.getInstance().fragmentComposerFactory.createFragmentComposer();
		fragmentComposer.reset(req, resp);

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.exit(tc, "getFragmentComposer", fragmentComposer);
		}
		
		return fragmentComposer;
	}

	static void postProcess(
			CacheProxyRequest request, 
			CacheProxyResponse response, 
			FragmentComposer fragmentComposer, 
			boolean cacheable, 
			boolean exceptionThrown)
			throws IOException {
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.entry(tc, "postProcess", new Object[]{fragmentComposer});
		}

		FragmentInfo fragmentInfo = fragmentComposer.getFragmentInfo();
				
		if (!fragmentComposer.isExternalPage())
			fragmentComposer.saveExternalInvalidationIds();
	
		if (cacheable && fragmentComposer.isExternalPage())
			externalPostInvoke(fragmentInfo.externalCacheGroupId, request, response);
		
		fragmentComposer.copyContentForParents();
		
		checkDiscardJSPContentAttribute(request, fragmentComposer); 
		
		fragmentComposer.requestFinished();
	
		ESISupport.handleESIPostProcessing(response, fragmentInfo, exceptionThrown);
		
		FragmentComposer parentFragmentComposer = fragmentComposer.parent; 
		if (null != parentFragmentComposer ) {			
			parentFragmentComposer.endChildFragmentComposer(fragmentComposer);			
		}
		
		request.clearSetTable(); // ensure there's nothing left in the requests set table
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.exit(tc,"postProcess");
		}		
	}

// this method is used to handle request attribute processing on included fragments
   protected static void processRequestAttributes(CacheProxyRequest request, FragmentComposer fragmentComposer)
   throws IOException {    
	  FragmentComposer parentFragmentComposer = fragmentComposer.parent;
      if (parentFragmentComposer != null) {
         int cacheType = parentFragmentComposer.getCacheType();
         CacheProxyRequest parentCacheProxyRequest = parentFragmentComposer.getRequest();
         if (cacheType == FragmentComposer.POPULATED_CACHE) {
            //Attributes must be saved for the benefit of the
            // parent Composer.  The parent Composer's resulting
            // memento will serve up these saved attributes to its request
            // when the memento is retrieved from the cache, so the
            // child can transfer them to the current request to receive consistent state.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(tc, "CacheHook: saving Attributes for parent" + parentFragmentComposer.getURI());
	    
             CacheProxyRequest.Attribute[] saveList = parentFragmentComposer.getSaveAttributeList(parentCacheProxyRequest); //ST-begin
	    //if parent is shared and saveList != null store the attributelist in bytes
	    if(!parentFragmentComposer.getFragmentInfo().isNotShared() && saveList != null){
		byte[] attributeTableBytes = getChangedAttributeBytes(parentCacheProxyRequest, saveList);
                fragmentComposer.setAttributeTableBytes(attributeTableBytes);
	    }
	    else
		 fragmentComposer.setAttributeTable(saveList); //ST-end
	    //When attributeTableBytes == null, this signals
            // uncacheability of the parent servlet/jsp to the
            // FragmentComposer

         }
         if (cacheType == FragmentComposer.WAS_CACHED) {
	     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
		Tr.debug(tc, "cacheType == FragmentComposer.WAS_CACHED");
	     CacheProxyRequest.Attribute[] attr = parentCacheProxyRequest.removeAttributeTableUnReadied();
	     if (attr != null){	    
	          request.setAttributeTableUnReadied(attr);	      
	     }
	     else {	    
                  request.setAttributeTableBytes(parentCacheProxyRequest.removeAttributeTableBytes());
	     }
	 }      

         //must ready attributes now, so they're ready for the getId()
         request.readyAttributes();
      }
   }

   /**
    * This is called by the static handleFragment method.
    * It gets the FragmentComposer from the response and
    * gets the cache id from it.
    * It gets a mutex on the id and pins it.
    * If in the jspCache, it calls the static handleCacheHit method.
    * It releases the mutex and unpins the id.
    * If not in the jspCache, it calls the static handleCacheMiss method.
    *
    * @param servlet The servlet handling the HTTP request.
    * @param request The HTTP request object.
    * @param response The HTTP response object.
    * @param fragmentInfo This contains the cache id and Template.
    * @param shouldReadyAttributes A true implies that the request
    * attributes should be converted from its byte array version to
    * its hashtable version.
    */
   private static void handleCacheableFragment(ServletWrapper servlet,
			CacheProxyRequest request, CacheProxyResponse response,
			FragmentInfo fragmentInfo, JSPCache jspCache)
			throws ServletException, IOException {
		
	   if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.entry(tc, "handleCacheableFragment");

		if (ESISupport.shouldBuildESIInclude(request, response, fragmentInfo)) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
				Tr.exit(tc, "handleCacheableFragment");
			return;
		}

		FragmentComposer fragmentComposer = response.getFragmentComposer();
		String id = fragmentInfo.getId();
		CacheEntry cacheEntry = null; 

		try {
			Object fragmentValue = null;
			boolean didMiss = false;
			boolean isInvalidEntry = false;

			if (fragmentComposer.isAsyncDoubleDip()){
				cacheEntry = jspCache.getEntry(fragmentInfo, true);
			} else {
				cacheEntry = jspCache.getEntry(fragmentInfo);
			}

			if (cacheEntry != null) {
				fragmentValue = cacheEntry.getValue();
				if (fragmentValue == null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
						Tr.debug(tc, "CACHE MISS id: " + id);
					didMiss = true;
					handleCacheMiss(servlet, request, response, fragmentInfo,
							fragmentComposer, jspCache, cacheEntry, fragmentValue);
				}
			    /*
			     * Check to see if cache entry is in the "invalid" state for portlet 
			     * validation-based caching. If it is, we need to treat it like a cache
			     * miss even though we have the entry. The result will determine if we go into the
			     * cache hit scenario or not.
			     */
				else if (cacheEntry.isInvalid()) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
						Tr.debug(tc, "CACHE MISS? on invalid cache entry id: " + id);
					didMiss = true;
					isInvalidEntry = true;
					handleCacheMiss(servlet, request, response, fragmentInfo,
							fragmentComposer, jspCache, cacheEntry, fragmentValue);
				}
			} else {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, "CACHE MISS id: " + id);
				didMiss = true;
				handleCacheMiss(servlet, request, response, fragmentInfo,
						fragmentComposer, jspCache, cacheEntry, fragmentValue);

			}
			// }
			// handle cache hit outside the mutex...
			if (!didMiss) { // the value was found in the cache
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
					Tr.debug(tc, "CACHE HIT id:" + id);

				handleCacheHit(servlet, request, response, fragmentInfo,
						fragmentComposer, jspCache, cacheEntry, fragmentValue, false);
			}
		} finally {
			if (cacheEntry != null) {
				cacheEntry.finish();
			}
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			Tr.exit(tc, "handleCacheableFragment");
	}

 /**
	 * This handles the case where the cache entry is in the jspCache. It gets
	 * the jspCache id from the fragmentInfo. It tells the FragmentComposer that
	 * it was cached. It calls the JSPCache.setFragmentInfo method to change any
	 * caching metadata. It displays the page by calling the
	 * FragmentComposerMemento.displayPage method. It returns the doOver
	 * indicator, which indicates when a cache entry has been removed or
	 * invalidated while it is being rendered (esp by a contained cache entry).
	 * 
	 * @param request
	 *            The HTTP request object.
	 * @param response
	 *            The HTTP response object.
	 * @param fragmentInfo
	 *            This contains the cache id and Template.
	 * @param fragmentComposer
	 *            The current FragmentComposer.
	 * @param jspCache
	 *            This JVM's JSPCache.
	 * @param fragmentValue
	 *            The value of the cache entry in the jspCache.
	 * @param isUseCachedContent
	 * 			  For validation-based caching, indicates that we are using an invalid entry
	 * 			  so that we can reset the expiration time on the entry.
	 */
   private static final void handleCacheHit(ServletWrapper servlet, CacheProxyRequest request, CacheProxyResponse response, FragmentInfo fragmentInfo, FragmentComposer fragmentComposer, 
		   JSPCache jspCache, CacheEntry cacheEntry, Object fragmentValue, boolean isUseCachedContent)
      throws ServletException, IOException {

      fragmentComposer.setCacheType(FragmentComposer.WAS_CACHED);
      FragmentComposerMemento memento = (FragmentComposerMemento) fragmentValue;

      memento.displayPage(servlet, request, response);
      
      //Have to check the cache entry because fragmentInfo doesn't
      //have this timeStamped information for a jspCache hit.
      //The information is normally put in the fragmentInfo
      //by the called servlet (which, for a jspCache hit, isn't called).
      fragmentComposer.setExpirationTime(cacheEntry.getExpirationTime());
      fragmentInfo.setExpirationTime(cacheEntry.getExpirationTime());
      fragmentComposer.setTimeStamp(cacheEntry.getTimeStamp());
      String externalCacheGroupId = fragmentInfo.getExternalCacheGroupId();
      
      /*
       * Update expiration time for "invalid cache hits"
       */
      if (isUseCachedContent) {
    	  jspCache.setValidatorExpirationTime(fragmentInfo, memento);
    	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    		  Tr.debug(tc, "USE_CACHED_CONTENT for vbc; setting validator expiration time to "
    				  + fragmentInfo.getValidatorExpirationTime());
      }
      
      if (!fragmentComposer.isExternalPage() ||
    		  (externalCacheGroupId == null) || 
    		  (externalCacheGroupId.equals(""))) {
         return;
      }
     
       // this is an external cache fragment, handle it...
      //Adding the dataIds back to the fragmentInfo so that they can used on a republish ot external fragments
      Enumeration dataIdsEnum = cacheEntry.getDataIds();
      while(dataIdsEnum.hasMoreElements()){
    	  Object dataId = dataIdsEnum.nextElement();
    	  fragmentInfo.addDataId(dataId);
      }

      Enumeration tempEnum = cacheEntry.getTemplates();
      while(tempEnum.hasMoreElements()){
    	  String temp = (String)tempEnum.nextElement();
    	  fragmentInfo.addTemplate(temp);
      }

      setValue(request, response, fragmentInfo, jspCache, fragmentComposer, memento, externalCacheGroupId, true);
   }

   /**
    * This handles the case when the cache entry is not in the jspCache.
    * It calls the Servlet.service method.
    * If the cache entry has an external jspCache,
    * it gets the external URL using the dispatcher info's getFullURI() method
    * and sets the external URL on the FragmentInfo.
    * While holding the cache entry mutex,
    * it calls the static setValue method.
    * It returns the doOver indicator, which indicates when a cache entry
    * has been removed or invalidated while it is being rendered
    * (esp by a contained cache entry).
    * If request attributes have been set that are not serializable,
    * then the JSP cannot be cached because it cannot be executed
    * without its parent being executed, so the itsUncacheable method
    * is propagated back to the coordinating CacheUnit so it can
    * be removed from the workInProgressTable.
    *
    * @param servlet The servlet handling the HTTP request.
    * @param request The HTTP request object.
    * @param response The HTTP response object.
    * @param fragmentInfo This contains the jspCache id and Template.
    * @param fragmentComposer The current FragmentComposer.
    * @param jspCache This JVM's JSPCache.
    * @param shouldReadyAttributes A true implies that the request
    * attributes should be converted from its byte array version to
    * its hashtable version.
    * @param cacheEntry The cache entry. Needed for handleCacheHit
    * @param fragmentValue The fragment value. Needed for handleCacheHit
    * @param isInvalidEntry Indicates whether this method was called for an invalid cache entry
    * @return The the doOver indicator.
    */
   private static final boolean handleCacheMiss(ServletWrapper servlet, CacheProxyRequest request, CacheProxyResponse response, FragmentInfo fragmentInfo, 
		   FragmentComposer fragmentComposer, JSPCache jspCache, CacheEntry cacheEntry, Object fragmentValue) throws ServletException, IOException {
      String id = fragmentInfo.getId();
      fragmentComposer.setCacheType(FragmentComposer.POPULATED_CACHE);
      boolean exceptionDuringService = true;
      boolean responseNotCacheable = false; 
      Exception ex = null;
      
      try {
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "servicing " + id);
         servlet.serviceProxied(request, response);
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "servicing " + id + " finished.");
         exceptionDuringService = false;
      } 
      catch (Exception e) {
          FFDCFilter.processException(e, CacheHook.class.getName() + ".handleCacheMiss()", "867");
          ex = e;
      }
      
      finally {
        Object wsInvokeStatus = (Object) request.getAttribute("httpResponseIsCacheable");
    	  if (null != wsInvokeStatus){
    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    			  Tr.debug(tc, "httpResponseIsCacheable: " + wsInvokeStatus );
    	  }          

     	  if (wsInvokeStatus instanceof Boolean){                 
    		  //HTTP_RESPONSE_IS_CACHEABLE request attribute is set to false if there is a SOAP Fault
    		  boolean  wsHttpResponseIsCacheable =  ((Boolean) wsInvokeStatus).booleanValue();
    		  if (false == wsHttpResponseIsCacheable){
    			  responseNotCacheable = true;
    		  }
    	  } 

    	  if (response.statusCode != 0) {
        	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        		  Tr.debug(tc, "handleCacheMiss: statusCode=" + response.statusCode);
    		  int [] filteredStatusCodes = jspCache.getFilteredStatusCodes();
    		  if ( null != filteredStatusCodes && filteredStatusCodes.length > 0 ) {
    			  for (int i=0;i<filteredStatusCodes.length;i++) {
    				  if (response.statusCode == filteredStatusCodes[i]) {
    			    	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    			    		  Tr.exit(tc, "handleCacheMiss: returning false because statusCode is filtered. " + filteredStatusCodes[i]);
    					  return false;    				  
    				  }
    			  }
    		  }
    	  }
        
  	    if (responseNotCacheable) {
    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    			  Tr.debug(tc, "response of " + id + " is not cacheable.  httpResponseIsCacheable is true.");
    		  return false;
    	  } 
        
    	  if (exceptionDuringService) {
    		  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    			  Tr.debug(tc, "invocation of " + id + " threw an exception.  Not Caching.");
    		  throw new ServletException(ex);
    	  }
      }
      
      // check if the fragment is uncacheable (usually due to
      // unserializeable attributes...
      if (fragmentInfo.isUncacheable() || isSkipCache()) {
    	  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
              Tr.debug(tc, "Not Caching: " + id + 
                      " fragmentInfo.isUncacheable(): "+fragmentInfo.isUncacheable()+" isSkipCache(): "+isSkipCache() );
    	  }
         return false;
      }

      
      /* 
         * everything is ok, let's get our memento and store it 
		 * In case of ARD the last child processing the include inserts the
		 * parent into the cache. The last child fragment processed will put
		 * this in the cache
		 */
		if (!fragmentComposer.isAsyncDispatch()) {
			putInCache(fragmentComposer);
		} else {
			if (tc.isDebugEnabled()){
				Tr.debug(tc, "Skipped putting in cache");
			}
		}

      return true;
   }

   /**
    * This is called by the static handleFragment method.
    * This returns the attributes as they were in the request object
    * just prior to execution of the cache entry.  This will only serialize attributes that were
    * modified.
    *
    * @param request The HTTP request object.
    * @return The hashtable containing the attributes.
    */
   static final byte[] getChangedAttributeBytes(CacheProxyRequest request, CacheProxyRequest.Attribute[] attributes) {
    //  CacheProxyRequest.Attribute attributes[] = request.getChangedAttributes();
      if (attributes == null)
         return null;

      byte[] array = null;
      try {
         array = SerializationUtility.serialize(attributes);
      } catch (Exception ex) {
         com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.CacheHook.getChangedAttributeBytes", "335");
         String id = request.getFragmentInfo().getId();
         ((FragmentInfo) request.getFragmentInfo()).setUncacheable(true);
         Tr.error(tc, "dynacache.notSerializable", id + ":" + ex.getMessage());
         return null;
      }
      return array;
   }

   /**
    * This is called by the static handleCacheMiss method.
    * It creates and sets the ExternalCacheFragment from values in the
    * FragmentComposer.
    * If not a jspCache hit, it republishes the page to the jspCache/s.
    *
    * @param fragmentInfo This contains the cache id and template.
    * @param fragmentComposer The current FragmentComposer.
    * @param jspCache This JVM's JSPCache.
    * @param memento This cache entry's FragmentComposerMemento.
    * @param externalCacheGroupId This cache entry's external jspCache group if any.
    * @param cacheHit True implies that it was a jspCache hit.
    */
   /* package */
   static void setValue(CacheProxyRequest request, CacheProxyResponse response, FragmentInfo fragmentInfo, JSPCache jspCache, FragmentComposer fragmentComposer, FragmentComposerMemento memento, String externalCacheGroupId, boolean cacheHit) {
      ExternalCacheFragment externalCacheFragment = null;
      //can only publish external pages to external caches
      if ((fragmentComposer.isExternalPage()) && (externalCacheGroupId != null)) {

         try {
            ValueSet dataIds = fragmentComposer.getAllInvalidationIds();
            ValueSet uris = fragmentComposer.getAllTemplates();
            long expirationTime = fragmentComposer.getExpirationTime();
            long timeStamp = fragmentComposer.getTimeStamp();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(tc, "dataIds: " + dataIds);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(tc, "uris: " + uris);
            if (expirationTime >= 0) {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(tc, "expirationTime: " + new Date(expirationTime));
            } else {
               if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                  Tr.debug(tc, "no expiration");
            }
            byte[] content = fragmentComposer.toByteArray(response.getCharacterEncoding());
            String queryString = request.getQueryString();
            externalCacheFragment = new ExternalCacheFragment();
		    if(queryString != null){
		    	externalCacheFragment.setUri(fragmentInfo.getURI() +"?"+queryString);
		    } else {
		    	externalCacheFragment.setUri(fragmentInfo.getURI());
		    }
            externalCacheFragment.setExternalCacheGroupId(externalCacheGroupId);
            externalCacheFragment.setHost(request.getHeader("host"));
		    externalCacheFragment.setInvalidationIds(dataIds);
            externalCacheFragment.setTemplates(uris);
            externalCacheFragment.setContent(content);
            externalCacheFragment.setExpirationTime(expirationTime);
            externalCacheFragment.setTimeStamp(timeStamp);
            externalCacheFragment.setHeaderTable(response.getHeaderTable());

         } catch (UnexternalizablePageException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.CacheHook.setValue", "804");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(tc, "cannot externalize page: " + fragmentInfo.getId());
         } catch (IOException ioe) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ioe, "com.ibm.ws.cache.servlet.CacheHook.setValue", "807");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
               Tr.debug(tc, "cannot externalize page: " + fragmentInfo.getId());
         }
      }
      if (cacheHit) {
         if (externalCacheFragment == null) {
            return;
         }
         if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "republishing cache entry to external jspCache: " + fragmentInfo.getId());
         jspCache.setExternalCacheFragment(fragmentInfo.getId(), externalCacheFragment);
         return;
      }
      jspCache.setValue(fragmentInfo, memento, externalCacheFragment);
   }

   protected static void externalPreInvoke(String cacheGroup, CacheProxyRequest request, CacheProxyResponse response) {
      if (cacheGroup != null) {
    	  ((JSPCache)ServerCache.jspCache).preInvoke(cacheGroup, request, response);
      }
   }

   protected static void externalPostInvoke(String cacheGroup, CacheProxyRequest request, CacheProxyResponse response) {
      if (cacheGroup != null) {
    	  ((JSPCache)ServerCache.jspCache).postInvoke(cacheGroup, request, response);
      }
   }
   
   public static void setSkipCache(boolean skipCache) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "setSkipCache: " + skipCache);
		threadLocalSkipCache.set(new Boolean(skipCache));
	}

	public static boolean isSkipCache() {
		Boolean b = (Boolean) threadLocalSkipCache.get();
		if (b == null)
			b = new Boolean(false);
		return b.booleanValue();
	}
   
   /**
	 * This method is called when the CASCADE_CACHESPEC_PROPERTIES custom
	 * property is defined. If the value of this custom property is set to true
	 * then a fragment that is NOT cacheable will inherit the storeAttributes,
	 * storeCookies and attributeExcludeList properties from its parent. This
	 * method was introduced in PK38811.
	 * 
	 * @param fragmentComposer
	 *            Current FragmentComposer being processed.
	 */
	private static void cascadeFragmentInfoFromParent(
			FragmentComposer fragmentComposer) {

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.entry(tc, "cascadeFragmentInfoFromParent",
					new Object[] { fragmentComposer.getURI() });
		}

		FragmentComposer parentFragmentComposer = fragmentComposer.getParent();
		FragmentInfo pfi = null;

		if (null != parentFragmentComposer) {
			pfi = parentFragmentComposer.getFragmentInfo();
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,
						"Parent Fragment Composer is null !!! Orphan fragment");
				Tr.exit(tc, "cascadeFragmentInfoFromParent");
			}
			return;
		}

		FragmentInfo fi = fragmentComposer.getFragmentInfo();

		if (null != pfi) {			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc, "Applying "+ pfi.getURI() +" attributes to "+fi.getURI());
			}
			fi.setStoreAttributes(pfi.getStoreAttributes());
			fi.setAttributeExcludeList(pfi.getAttributeExcludeList());
			fi.setStoreCookies(pfi.getStoreCookies());
		} else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,"FragmentInfo of Parent Fragment " +
						"Composer was null. Could NOT cascade info.");
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.exit(tc, "cascadeFragmentInfoFromParent");
		}
	}   
}