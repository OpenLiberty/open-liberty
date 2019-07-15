/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.extension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.webcontainer.extension.WebExtensionProcessor;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.servlet.GenericServletWrapper;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

/**
 * @author Nitzan, May 27 2005
 * An extension processor that is accessed on before the first message gets to the 
 * destination sip servlet. once the sipservlet was loaded once, thos processor wll not be called again for 
 * that servlet  
 * 
 */
public class SipExtensionProcessor extends WebExtensionProcessor 
{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipExtensionProcessor.class);
    
    /**
     * A prefix that is added to the SipServlets URIs to make requests go through 
     * this processor 
     */
    public static final String URL_EXTENSION_PROCESSOR_INDICATOR = "/ibm_sip_ep/";
    
    /**
     * Lock for atomizing the target mapping search-replace action
     */
	private final Object m_lock;
	
	/**
	 * The associated Sip application description
	 */
	private SipAppDesc _sipApp;
	
	/**
	 * Ctor 
	 */
	public SipExtensionProcessor(IServletContext servletCtx, SipContainer sipContainer, SipAppDesc sipApp) 
	{
		super(servletCtx);
		m_lock = new Object();
		
		_sipApp = sipApp;
	}
	
	/**
	 * @see com.ibm.ws.webcontainer.extension.WebExtensionProcessor#createServletWrapper(com.ibm.wsspi.webcontainer.servlet.IServletConfig)
	 */
    public IServletWrapper createServletWrapper(IServletConfig config) throws Exception
    {
        if( c_logger.isTraceDebugEnabled())	
            c_logger.traceEntry( this, "createServletWrapper");
       
        GenericServletWrapper wrapper = new SipServletWrapper(extensionContext);
        //We are passing our config wrapper to the ServletWrapper
        //so it will eventually get to the SipServlet init method 
		wrapper.initialize(config);
		
		if( c_logger.isTraceDebugEnabled())	
            c_logger.traceExit( this, "createServletWrapper");
		
        return wrapper;
    }
    
    
    /**
     * Receiving HTTP requsts and creating a SipServletWrapper that locates the appropriate 
     * sip servlet according to URI.
     * The HTTP request are only used for servlet locating - the actual request/response
     * the SipServlet receives will be SipServletRequest and SipServletResponse objects
     * that are stored in a ThreadLocal variable.  
     * This method will only be called once. The next times the requests will go directly
     * to the ServletWrapper.
     * @param req The HTTPServletRequest 
     * @param res The HTTPServletResponse 
     */
    public void handleRequest( ServletRequest req, ServletResponse res) throws Exception{
		if( c_logger.isTraceDebugEnabled())	
	           c_logger.traceEntry( this, "handleRequest");
				
		if (req instanceof HttpServletRequest) {
			//We need an instance of HttpServletRequest so that later we can get the siplet
			//name from its Request-URI
			HttpServletRequest request = (HttpServletRequest)req;
			IServletConfig     sconfig = getConfig(request);
			String             path    = request.getServletPath();
			RequestProcessor   target  = null; 
			
			/*
			 * Replacing the mapping to make all next messages go directly 
			 * to the ServletWrapper
			 */
			target = extensionContext.getMappingTarget(path);
			if (target == this) {
				target = createServletWrapper(sconfig);
				target = extensionContext.getMappingTarget(path.replace(URL_EXTENSION_PROCESSOR_INDICATOR,"/"));
				synchronized(m_lock) {
					extensionContext.replaceMappingTarget(path, target);
				}
			}
			
			//Execute ServletWrapper 
			target.handleRequest(req, res);
		}
		else
			if( c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug( "handleRequest: request is not instance of HttpServletRequest. req=" + req);
			}
		    
		if( c_logger.isTraceDebugEnabled()) {	
		    c_logger.traceExit( this, "handleRequest");
		}
	}
	
	/**
	 * Retrun list of URI patterns. This processor will handle only 
	 * requests for servlets that match these patterns
	 */
	public List getPatternList() {
		ArrayList patterns = new ArrayList();
		
		Iterator itr = _sipApp.getSipServlets().iterator();
		
		while( itr.hasNext())
		{
			SipServletDesc desc = (SipServletDesc)itr.next();
			String name = desc.getName();
			patterns.add( URL_EXTENSION_PROCESSOR_INDICATOR + name);
			if( c_logger.isTraceDebugEnabled())	
		        c_logger.traceDebug( this, "getPatternList", "pattern = "+ URL_EXTENSION_PROCESSOR_INDICATOR + name);
		}
		
		return patterns;
	}
	
	/**
	 * Create a ServletConfig to supply ServletWrapper with needed data for 
	 * servlet locating. This ServletConfig will be wrapped with SipServletConfig.
	 * @param req
	 * @return
	 * @throws ServletException
	 */
	private IServletConfig getConfig(HttpServletRequest req) throws ServletException {
		String sipletName = getSipletName( req);
		return extensionContext.getWebAppConfig().getServletInfo(sipletName);
	}
	
	/**
	 * Returns siplet name
	 * @param req HttpServletRequest which we get the name from its request URI.
	 * @return siplet name
	 */
	private String getSipletName( HttpServletRequest req){
		String uri = req.getRequestURI();
		int lastSlash = uri.lastIndexOf( '/');
		return uri.substring( lastSlash + 1);
	}
}
