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
package com.ibm.ws.webcontainer.srt;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.ws.webcontainer.facade.IFacade;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.ws.webcontainer.webapp.WebApp;

@SuppressWarnings("unchecked")
public class SRTRequestContext implements Cloneable
{
protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.SRTRequestContext";
    
    // objects requiring cloning
    //======================
    // need a synchronized datastructure to hold the sessions
    protected Hashtable webappToSessionMap;
    protected SRTServletRequest request;
    private Stack boundaries = new Stack();
    //======================
    
	public SRTRequestContext(SRTServletRequest request)
	{
		this.request = request;
		webappToSessionMap = new Hashtable();
	}
	
	public void sessionPreInvoke(WebApp webapp)
	{
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"sessionPreInvoke", "entry");
        }
		HttpSession session = (HttpSession)webappToSessionMap.get(webapp);
		if (session == null)
		{
		    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
	            logger.logp(Level.FINE, CLASS_NAME,"sessionPreInvoke", "null session, find or create");
	        }
            IHttpSessionContext sessionContext = webapp.getSessionContext();
            session = sessionContext != null ? sessionContext.sessionPreInvoke((HttpServletRequest) request, (HttpServletResponse) request.getResponse()) : null;
			if (session != null)
				webappToSessionMap.put(webapp, session);
		}
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"sessionPreInvoke", "exit");
        }
	}
	
	public boolean isRequestedSessionIdValid(WebApp webapp)
	{
		HttpSession session = getSession(false, webapp);
		return webapp.getSessionContext().isRequestedSessionIdValid((HttpServletRequest) request, session);
	}

	public void sessionPostInvoke()
	{	
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"sessionPostInvoke", "entry");
            logger.logp(Level.FINE, CLASS_NAME,"sessionPostInvoke", "exit");
        }
	}
	
	public HttpSession getSession(boolean create, WebApp webapp)
	{
	    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"getSession", " entry");
        }
	    HttpServletRequest localrequest = (HttpServletRequest)this.request; // PK01801 already unwrapped
		HttpSession session = (HttpSession) webappToSessionMap.get(webapp);
		if (session != null)
		{
		    if (webapp.getSessionContext().isValid(session, localrequest, create)) // PK01801
			{
				return (HttpSession)((IFacade)session).getFacade(); 
			}
		}

		// Looks like the session wasn't obtained during the preinvoke
		// call.  Should only happen if session doesn't exist at preinvoke.
		//
		session = webapp.getSessionContext().getIHttpSession(localrequest, (HttpServletResponse) request.getResponse(), create);
		if ( session == null) {
		    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
	            logger.exiting(CLASS_NAME,"getSession", "null");
	        }
		    return null;
		}
		else  {
                        webappToSessionMap.put(webapp, session);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
                            logger.logp(Level.FINE, CLASS_NAME,"getSession", " exit");
                        }
			return (HttpSession)((IFacade)session).getFacade(); 		
                }
		
		
		
	}

	public void finish()
	{
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"finish", " entry");
        }
	    try {
                Enumeration webapps = this.webappToSessionMap.keys();
                while (webapps.hasMoreElements()) {
                    WebApp wa = (WebApp)webapps.nextElement();
                    HttpSession s = (HttpSession)webappToSessionMap.get(wa);
                    IHttpSessionContext ctx =wa.getSessionContext();
                    if (ctx!=null){
                    	wa.getSessionContext().sessionPostInvoke(s);
                    }
                    else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) { 
                    	//Quietely logging is not the best option here, but otherwise
                    	//there would be new synchronizing in webApp to chec how many associated sessions are still
                    	//in existence which isn't worthwhile considering the tiny window this can happen in.
                        logger.logp(Level.FINE, CLASS_NAME,"finish", "Session Context was null so session data will not be persisted.");
                    }
                }
	    }
	    finally {
		    this.webappToSessionMap.clear();
		    boundaries.clear();
		    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
	            logger.logp(Level.FINE, CLASS_NAME,"finish", " exit");
	        }
	    }
	}

	/**
	 * @param _webapp
	 * @param request
	 * @param arg0
	 * @return
	 */
	public String encodeURL(WebApp webapp, HttpServletRequest request, String arg0)
	{
		HttpSession session = (HttpSession) webappToSessionMap.get(webapp);
		if (session != null)
		{
			return webapp.getSessionContext().encodeURL(session, request, arg0); 
		}
		else
			return arg0;
		
	}
	
	public void setCurrWebAppBoundary(WebApp webapp)
	{
	    boundaries.push(webapp);
	}
	
	public void rollBackBoundary()
	{
	    boundaries.pop();
	}

    /**
     * @param app
     * @return
     */
    public boolean isWithinModule(WebApp app) 
    {
        WebApp currWebAppBoundary = (WebApp) boundaries.peek();
        //setCurrWebAppBoundary(app);
        if (currWebAppBoundary.equals(app))
            return true;
        
        return false;
    }

    /**
     * @param app
     * @return
     */
    public boolean isWithinApplication(WebApp app) 
    {
        WebApp currWebAppBoundary = (WebApp) boundaries.peek();
        //setCurrWebAppBoundary(app);
        
        if (currWebAppBoundary.getApplicationName().equals(app.getApplicationName()))
        {
            return true;
        }
        
        return false;
    }
    protected Object clone(SRTServletRequest clonedRequest) throws CloneNotSupportedException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"clone", " entry");
        }
        SRTRequestContext clonedRequestContext = (SRTRequestContext) super.clone();
        if (boundaries!=null){
            clonedRequestContext.boundaries = (Stack) boundaries.clone();
        }
        if(webappToSessionMap!=null){
            clonedRequestContext.webappToSessionMap = (Hashtable) webappToSessionMap.clone();
        }
        clonedRequestContext.request = clonedRequest;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"clone", " exit original -->" + this  +" clone -->" + clonedRequestContext);
        }
        return clonedRequestContext;
    }

    public void destroy(){
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"destroy", " entry");
        }
        this.request = null;
        this.webappToSessionMap =null;
        this.boundaries = null;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  
            logger.logp(Level.FINE, CLASS_NAME,"destroy", " exit");
        }

    }

	
}

