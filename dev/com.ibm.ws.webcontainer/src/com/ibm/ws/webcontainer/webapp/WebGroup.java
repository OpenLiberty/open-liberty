/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp;

import java.util.ArrayList;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.ws.container.Container;
import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.webcontainer.VirtualHost;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.core.BaseContainer;
import com.ibm.ws.webcontainer.exception.WebAppNotLoadedException;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;

/*
 * The WebGroup will encapsulate a Web Application. The actual servlet
 * context will be implemented by the WebApp class, which will be managed
 * through this class. Versioning support will also be enabled, where
 * multiple web application configurations will coexist under the same context
 * root.
 * 
 * This is also the class that will be the runtime dispatcher of requests. In the
 * case of multiple web applications, a discrimination criterion will be used
 * to decide which WebApp will actually handle the request (see handleRequest()).
 */

public class WebGroup extends BaseContainer
{
	
protected final static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.webapp");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.webapp.WebGroup";

	
	private static String sessUrlRewritePrefix = WebContainer.urlPrefix;
	static{
		if (WCCustomProperties.SESSION_REWRITE_IDENTIFIER != null) // mam componentization missed: 208705 prop must be all lower case
			sessUrlRewritePrefix = ";"+WCCustomProperties.SESSION_REWRITE_IDENTIFIER+"=";
	};
	private static final String qMark = "?";
		
   private WebGroupConfiguration config;
	//begin 280649    SERVICE: clean up separation of core and shell    WASCC.web.webcontainer: make the scope protected versus private
   protected  WebApp webApp;  // PK25527
	//end 280649    SERVICE: clean up separation of core and shell    WASCC.web.webcontainer

   
   public WebGroup(String name, Container parent) 
   {
		super (name, parent);
   }
   
   public void initialize(WebGroupConfiguration c)
   {
   		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
   			logger.logp(Level.FINE, CLASS_NAME,"initialize", "");
        }
   			
   		this.config = c;
   		webApp = null;  // PK25527
   }
   
	/**
	 * Method removeContextAttribute.
	 * @param string
	 */
	public void removeContextAttribute(String string)
	{
	}

	/**
	 * Method setContextAttribute.
	 * @param string
	 * @param hMap
	 */
	public void setContextAttribute(String string, Object value)
	{
		
	}
	
	public WebGroupConfiguration getConfiguration()
	{
		return config;
	}
	
	/**
	 * Method addWebApplication.
	 * @param deployedModule
     * @param extensionFactories
	 */
	//BEGIN: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)
    @SuppressWarnings("unchecked")
    public void addWebApplication(DeployedModule deployedModule, List extensionFactories)
								throws Throwable
	{
		WebAppConfiguration wConfig = deployedModule.getWebAppConfig();
		String displayName = wConfig.getDisplayName();
		logger.logp(Level.INFO, CLASS_NAME,"addWebApplication", "loading.web.module", displayName);
		WebApp webApp = deployedModule.getWebApp();
		try
		{
                        this.webApp = webApp;   // PK40127
			webApp.initialize(wConfig, deployedModule, extensionFactories);
		}
		catch (Throwable th)
		{
			webApp.failed();
			webApp.destroy();
			this.webApp = null;     // PK40127
			com.ibm.ws.ffdc.FFDCFilter.processException(th, "com.ibm.ws.webcontainer.webapp.WebGroup", "131", this);
			Object[] arg = {displayName};
			logger.logp(Level.SEVERE, CLASS_NAME,"addWebApplication", "Failed.to.initialize.webapp.{0}", arg);
			throw th;
		}
		//this.webApp = webApp;  // PK25527		
		
	}
	//END: NEVER INVOKED BY WEBSPHERE APPLICATION SERVER (Common Component Specific)

	/**
	 * Method getMimeType.
	 * @param file
	 * @return String
	 */
	public String getMimeType(String withDot, String withoutDot)
	{
		return ((VirtualHost) parent).getMimeType(withDot, withoutDot);
	}

	/**
	 * Method getSessionContext.
	 * @param moduleConfig
	 * @param webApp
	 * @return IHttpSessionContext
	 */
    @SuppressWarnings("unchecked")
	public IHttpSessionContext getSessionContext(com.ibm.ws.container.DeployedModule moduleConfig, WebApp webApp, ArrayList[] listeners) throws Throwable
	{
		//System.out.println("WebGroup createSC");
		return ((VirtualHost) parent).getSessionContext(moduleConfig, webApp, listeners);
	}

	/**
	 * Method findContext.
	 * @param path
	 */
	public ServletContext findContext(String path)
	{
		/*synchronized (this){ //synchronize on the WebGroup to see if we are restarting
        	// This could block if someone is destroying the webGroup
        	// Or the recently destroyed and are now restarting
        	
			if (destroying){ //If you make it in this block and destroying is true, that means we just destroyed, but haven't made it back to addWebApplication
				if (restarting) { //If we are destroying and the stop event was marked as restarting, we want to suspend this thread until addWebApplication happens
					try {
						this.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						
					}
				}
			}
	   }*/
		return ((VirtualHost) parent).findContext(path);
	}

	/**
	 * Method getContext.
	 * 
	 * We might require more information at this point in order
	 * to determine which webapp to actually return.
	 * 
	 * For now return the newest.
	 * @return ServletContext
	 */
	public ServletContext getContext()
	{
		return this.webApp;  // PK25527
	}
	
    
    //PK37449 synchronizing
    public synchronized void destroy()
    {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            logger.logp(Level.FINE, CLASS_NAME,"destroy", "entry");

        /*destroying=true;
        
        if (nServicing>0){
        	try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				
			}
        }*/
    	
        super.destroy();

        //parent.removeSubContainer(name);    	//PK37449
    	this.requestMapper = null;
    	this.config = null;
    	this.webApp = null;  // PK25527
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
        	logger.exiting(CLASS_NAME, "destroy");													//569469
    }
    

	/**
	 * Method removeWebApplication.
	 * @param deployedModule
	 */
        //PK37449 synchronizing
        public synchronized void removeWebApplication(DeployedModule deployedModule)
        {
		// Try to get more info from the deployedModule to see which one
		// to actually remove.
		
		// For now remove the oldest webapp only.
		/*WebApp webapp = (WebApp) webApps.get(0);
		webApps.remove(0);*/
		
		// subcontainer is responsible for removing itself from this parent
		//
		
		// PK25527
    /*	if (deployedModule.isRestarting()){
			this.restarting=true;
		} */
        	
		if (this.webApp == null) {
             return;
        }
		
		//this.webApp.destroy(); no need to destroy the webapp as it will be called in super.destroy
		
		//if (webApps.size() == 0)
			destroy();

	}

	/**
	 * @see com.ibm.ws.core.RequestProcessor#handleRequest(ServletRequest, ServletResponse)
	 */
	public void handleRequest(ServletRequest req, ServletResponse res)
		throws Exception
	{
		// TODO:
		// heres where we would do the session ID check and all the other
		// checks we haven't envisioned yet for versioning support.
		//
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"handleRequest", "WebGroup found --> "+getConfiguration().getContextRoot());
        }
        //Begin 284644, check size before handling of requests
        // PK25527
        /*int size = webApps.size();
        if (size < 1){
	        // Since we currently have only one webapp per webgroup, we should have never reached the handleRequest
	        // of this method. This means that somehow we got in here after removing a webapp. This was caused previously
	        // by removing the webapp before removing from the request mapper which could cause problems under load.
	        // The check here is somewhat redundant and should not happen now.
        	throw new WebGroupVHostNotFoundException (((Request) req).getRequestURI());
    	}
        else 
			((WebApp) webApps.get(size - 1)).handleRequest(req, res);*/
		//End 284644, check size before handling of requests
		
		if (this.webApp == null) {
	        		 throw new WebAppNotLoadedException (getConfiguration().getContextRoot());
	        	}
        
	    		this.webApp.handleRequest(req, res);
        
	    	}
	
        //enter the request assuming we aren't going to handle it
//    boolean handleRequest=false;
//        synchronized (this){ //synchronize on the WebGroup to see if we are restarting
//        	// This could block if someone is destroying the webGroup
//        	// Or the recently destroyed and are now restarting
//        	
//        	if (destroying){ //If you make it in this block and destroying is true, that means we just destroyed, but haven't made it back to addWebApplication
//	        	if (restarting) { //If we are destroying and the stop event was marked as restarting, we want to suspend this thread until addWebApplication happens
//	        		this.wait();
//	        		if (destroying){ //If we are still in a destroy state, the notification was from the last request being handled and not the restart of the app.
//	    	        	if (restarting){
//	    	        		this.wait();
//	    	        	}
//	        		}
//	        		handleRequest=true;
//	        		incNServicing(); //Increment nServicing inside the synchronize, so that when removeWebApplication get the lock, it will wait if the number of requests > 0.
//	        		// If we moved this method outside the sync, there could be a thread switch where the remove starts, but we still think handleRequest is okay.
//	        	} 
//	        	else  {
//	        		 throw new WebAppNotLoadedException (getConfiguration().getContextRoot());
//	        	}
//        	}
//        	else { //Default case of not destroying or restarting
//        		handleRequest=true;
//        		incNServicing();
//        	}
//        }
//        
//        try {
//	        if (handleRequest){
//	    		this.webApp.handleRequest(req, res);
//	    	}
//        }
//        finally {
//        	if (handleRequest){
//        		synchronized (this){
//        			decNServicing(); // do the decrement inside the sync so that we don't get two threads that call notifyAll
//                	if (destroying&&nServicing==0){
//                		this.notifyAll();
//                	}
//                }
//        	}
//        }
//	}

	//	Begin 293696    ServletRequest.getPathInfo() fails    WASCC.web.webcontainer
	/**
	 * This method strips out the session identifier and the queryString
	 * from a request URI, and returns the resultant 'pure' URI
	 * 
	 * @param url
	 * @return
	 */
	public static String stripURL(String url)
	{
		return stripURL(url,true);
	}
	
	/**
	 * This method strips out the session identifier and the queryString (based on the boolean value)
	 * from a request URI, and returns the resultant 'pure' URI
	 * 
	 * @param url
	 * @param checkQuestionMark
	 * @return
	 */
	public static String stripURL(String url,boolean checkQuestionMark)
	{
		int index1 = url.indexOf(sessUrlRewritePrefix);
		if (checkQuestionMark){
			int index2 = url.indexOf("?");
	
			if (index2 != -1)
			{
				if ( index1 > index2 )
					throw new IllegalArgumentException( "'jsessionid' Must occur before '?' in URL." );
				url = url.substring(0, index2);
			}
		}

		if (index1 != -1)
		{
			// No query string so just remove the jsessionid
			url = url.substring(0, index1);
		}

		return url;     
	}
	//End 293696    ServletRequest.getPathInfo() fails    WASCC.web.webcontainer
	

	/**
	 * Strips out the sessionId only form the URI
	 * 
	 * @param url
	 * @return
	 */
	public static String decodeUri(String url)
	{
		int index1 = url.indexOf(sessUrlRewritePrefix);
		int index2 = url.indexOf(qMark);

		String tmp = null;
		if (index2 != -1 && index2 > index1)
		{
			tmp = url.substring(index2);
		}
		if (index1 != -1)
		{
			url = url.substring(0, index1);
			if (tmp != null)
			{
				url = url + tmp;
			}
		}
		return url;
	}
	
    @SuppressWarnings("unchecked")
    public ArrayList getWebApps() {
        // PK25527
        ArrayList webApps = new ArrayList();
        
        webApps.add (webApp);
        return webApps;
    }
	

//	public synchronized int incNServicing() {
//		nServicing++;
//		return nServicing;
//	}
//
//	public synchronized int decNServicing() {
//		nServicing--;
//		return nServicing;
//	}
//
//	public synchronized boolean getDestroying() {
//		// TODO Auto-generated method stub
//		return this.destroying;
//	}
//
//	public synchronized boolean getRestarting() {
//		// TODO Auto-generated method stub
//		return this.restarting;
//	}
//	
	public void notifyStart() {
		 if (this.webApp!=null){
			 this.webApp.notifyStart();
		 }
	}

}
