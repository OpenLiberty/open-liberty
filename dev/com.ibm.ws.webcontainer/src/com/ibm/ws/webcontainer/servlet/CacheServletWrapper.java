/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.ibm.wsspi.webcontainer.collaborator.CollaboratorHelper;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.ws.webcontainer.util.InvalidCacheTargetException;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer.servlet.ServletReferenceListener;

/**
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

public class CacheServletWrapper implements RequestProcessor, ServletReferenceListener
{
protected final static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.servlet");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.servlet.CacheServletWrapper";

	private String _servletPath;
	private String _pathInfo;
    private volatile IServletWrapper cacheTarget;

    private String cacheKeyStr;

    private WebApp webapp;
    private String requestUri;
    
    private boolean invalidated = false;
	/**
	 * 
	 */
	public CacheServletWrapper(IServletWrapper wrapper, HttpServletRequest req, String cacheKey, WebApp webapp)
	{
		super();
		cacheTarget = wrapper;
		cacheTarget.addServletReferenceListener(this);
		this._pathInfo=req.getPathInfo();
		this._servletPath = req.getServletPath();
		this.requestUri = req.getRequestURI();
		this.cacheKeyStr = cacheKey; 
		this.webapp = webapp;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.core.RequestProcessor#handleRequest(com.ibm.ws.webcontainer.core.Request, com.ibm.ws.webcontainer.core.Response)
	 */
	public void handleRequest(ServletRequest req, ServletResponse res) throws Exception
	{
	    IServletWrapper target = this.cacheTarget;

	    if (target != null) {
	        try {
	            webapp.getFilterManager().invokeFilters((HttpServletRequest) req, (HttpServletResponse) res, webapp, target, CollaboratorHelper.allCollabEnum);
	        } catch (Throwable th){
	            if((com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) && th instanceof IOException){
	                if((th.getMessage()!= null) && th.getMessage().contains("SRVE0918E")){
	                    // dont do anything
	                    // we have already given chance to servlet and filters to handle it
	                    // this is application error but should not be returned to client.
	                    logger.logp(Level.FINE, CLASS_NAME, "handleRequest", "donothandleexception SRVE0918E");   
	                    return;
	                }
	            }
	            WebApp webapp = this.webapp;
	            if (webapp == null) {
	                FFDCFilter.processException(th, getClass().getName(), "83");
	            } else {
	                webapp.handleException(th, req, res, this);
	            }
	        }
	    }
	    else
	        throw InvalidCacheTargetException.instance();

	}

	/**
	 * @return
	 */
	public String getPathInfo()
	{
		return _pathInfo;
	}

	/**
	 * @return
	 */
	public String getServletPath()
	{
		return _servletPath;
	}

	/**
	 * @param string
	 */
	public void setPathInfo(String string)
	{
		_pathInfo = string;
	}

	/**
	 * @param string
	 */
	public void setServletPath(String string)
	{
		_servletPath = string;
	}

	/**
	 * @return
	 */
	public WebApp getWebApp()
	{
		if (cacheTarget != null)
			return webapp;
		else
			throw InvalidCacheTargetException.instance();
	}
	
	/**
	 * @see com.ibm.ws.webcontainer.util.CacheWrapper#invalidate()
	 * 
	 * Called by the ServletWrapper when it is being destroy()ed. 
	 */
	public synchronized void invalidate()
	{
		// Remove this cached wrapper from the appropriate cache
        if (invalidated != true){
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
				logger.logp(Level.FINE, CLASS_NAME,"invalidate", "first invalidate");
			WebContainer.removeFromCache(cacheKeyStr);

			/* Avoid nulling things out so we can continue to handle requests in this wrapper
	                     * while the app is being destroyed
			cacheTarget = null;
			_servletPath = null;
			_pathInfo = null;
                        webapp = null;
			cacheKeyStr = null;
			*/
			invalidated = true;
		}
		else{
			if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))  //306998.15
				logger.logp(Level.FINE, CLASS_NAME,"invalidate", "additional invalidate");
		}
	}
	/**
	 * @return
	 */
	public String getRequestUri()
	{
		return requestUri;
	}

        // LIDB3816
        public String getCacheKeyString () {
             return this.cacheKeyStr;
        }

        // LIDB3816
        public IServletWrapper getCacheTarget () {
             return this.cacheTarget;
        }
        
    	public String toString(){
    		String s;
    		if (this.webapp==null)
    		{
    			s =
    			" Webapp nulled::  " + this.webapp + "  "
    					+ " ServletPath:: " + this._servletPath+ "  "
    					+ " CacheKey ::" + cacheKeyStr+ "  "
    					+" _pathInfo::"+this._pathInfo+ "  "
    					+" requestUri ::"+this.requestUri+ "  ";
    		}
    		else
    		{
    			 s =
    	    			" Webapp::  " + this.webapp + "  "
    	    					+ " ServletPath:: " + this._servletPath+ "  "
    	    					+ " CacheKey ::" + cacheKeyStr+ "  "
    	    					+" _pathInfo::"+this._pathInfo+ "  "
    	    					+" requestUri ::"+this.requestUri+ "  "
    	    					+"ApplicationName::"+this.webapp.getApplicationName()+ "  "
    	    					+"WebAppContxtPath::"+this.webapp.getContextPath();
    		}

    		return s;
    	}

    public boolean isInternal (){
    	return cacheTarget.isInternal();
    }
    
    public String getName (){
    	return cacheTarget.getServletName();
    }

}
