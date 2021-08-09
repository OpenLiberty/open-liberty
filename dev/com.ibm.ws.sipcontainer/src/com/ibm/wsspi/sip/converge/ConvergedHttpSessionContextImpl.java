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
package com.ibm.wsspi.sip.converge;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHost;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.ws.webcontainer.session.impl.HttpSessionContextImpl;
import com.ibm.ws.webcontainer.session.impl.HttpSessionImpl;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStore;

public class ConvergedHttpSessionContextImpl extends HttpSessionContextImpl {

    /**
	 * Class Logger.
	 */
	private static final transient LogMgr c_logger = Log.get(ConvergedHttpSessionImpl.class);
	
	/**
     * @param smc
     * @param sap
     * @param sessionStoreService
     */
    public ConvergedHttpSessionContextImpl(SessionManagerConfig smc, SessionApplicationParameters sap, SessionStoreService sessionStoreService) {
        super(smc, sap, sessionStoreService, true);
    }


    /*
     * createSessionObject
     */
    public Object createSessionObject(ISession isess, ServletContext servCtx){
      return new ConvergedHttpSessionImpl(isess, this, servCtx);
    }
    
    /**
     * 
     * @param session
     * @param contextPath
     * @param relativePath
     * @param scheme
     * @return
     */
    public String getSipBaseUrlForEncoding(String contextPath, String relativePath, String scheme) {
        return ConvergedHttpSessionContextImpl.getSipBaseUrlForEncoding(_smc, contextPath, relativePath, scheme, this);
    }
    
    /**
     * Static implementation of the getSipBaseUrlForEncoding, to be able to call it from extended class for Servlet31
     * @param smc
     * @param contextPath
     * @param relativePath
     * @param scheme
     * @return
     */
    public static String getSipBaseUrlForEncoding(SessionManagerConfig smc,String contextPath, String relativePath, String scheme, HttpSessionContextImpl session){
    	StringBuffer returnUrl = new StringBuffer();
        int port;
        
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry("getSipBaseUrlForEncoding"," relativePath = " + relativePath);
        }
        
        String appName = session.getAppName();
        
        int temp = appName.indexOf('/');
        
        if(temp != 1){
        	appName = appName.substring(++temp);
        	
        }
		
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug("getSipBaseUrlForEncoding: ApplicationName = " + appName);
        }
        
        
        SipAppDesc app = SipAppDescManager.getInstance().getSipAppDescByAppName(appName);
		
        if(app == null){
        	if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceExit("getSipBaseUrlForEncoding"," Failed to find application by name " );
            }
        	return relativePath.toString();
        }
        
        int httpPort=-1;
        int httpsPort=-1;
        String hostName=null;
        boolean isSecure = false;
        boolean infoFound = false;
        
        if ("https".equalsIgnoreCase(scheme)){
        	isSecure = true;
        }
        
        DynamicVirtualHostManager dhostm = SipContainerComponent.getVirtualHostMgr(); 
    	DynamicVirtualHost dhost = dhostm.getVirtualHost(app.getVirtualHostName(), null);
    	
    	if(dhost == null){
    		if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit("getSipBaseUrlForEncoding", " DynamicVirtualHost was not found");
			}
        	return relativePath.toString();
        }
    	
    	List<String> aliases = dhost.getAliases();
    	
    	// Look for the http or https port according to the secure/unsecure condition
    	for (String aliase: aliases) {
    		httpPort = dhost.getHostConfiguration().getHttpPort(aliase);
    		httpsPort = dhost.getHostConfiguration().getSecureHttpPort(aliase);
    		hostName = dhost.getName();

    		if(isSecure ){
    			if(httpsPort != -1){
    				infoFound = true;
    				break;
    			}    			
    		}
    		else if(httpPort != -1){
    			infoFound = true;
    			break;
    		}
		}
    	
    	if(!infoFound){
    		if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceExit("getSipBaseUrlForEncoding",
                		" No appropriate port was found. Will not encode host:port to URL");
            }
    		return relativePath.toString();
    	}
    	
    	 if (c_logger.isTraceDebugEnabled()) {
             c_logger.traceDebug("getSipBaseUrlForEncoding: Encoding httpPort = " + httpPort + 
            		 					" httpsPort = " + httpsPort + " hostName = " + hostName);
         }
    	 
   		if (hostName.equals("*") || hostName.equals("default_host") ) {
   			try {
   				InetAddress addr = java.net.InetAddress.getLocalHost();
   				hostName = addr.getHostName();
	   			 if (c_logger.isTraceDebugEnabled()) {
	   	             c_logger.traceDebug("getSipBaseUrlForEncoding: local Addrr = " + addr + 
	   	            		 					" hostname of system = " + hostName);
	   	         }
   			} catch (UnknownHostException e) {
	   			 if (c_logger.isTraceDebugEnabled()) {
	   	             c_logger.traceDebug("getSipBaseUrlForEncoding: exception = " + e);
	   	         }
   			}
   		}

   		if ("https".equalsIgnoreCase(scheme)) {
           	port = httpsPort; 
               returnUrl.append("https://");
           } 
   		else {
               port = httpPort;
               returnUrl.append("http://");
           }
        
   		returnUrl.append(hostName).append(":").append(port);
		if (contextPath != null) {
			if (contextPath.startsWith("/")) {
				returnUrl.append(contextPath);
			} else {
				returnUrl.append("/").append(contextPath);
			}
		}
		if (relativePath != null) {
			if (relativePath.startsWith("/")) {
				returnUrl.append(relativePath);
			} else {
				returnUrl.append("/").append(relativePath);
			}
		}        
    	
        return returnUrl.toString();
    }
    
    /*
     * Added for SIP/HTTP Converged App Support. SIP container calls this method via
     * com.ibm.wsspi.servlet.session.ConvergedAppUtils to get an HTTP session reference
     * for those HTTP sessions that belong to application sessions.
     */
    public HttpSession getHttpSessionById(String sessId) {
    	HttpSessionImpl sd = null;
        IStore iStore = _coreHttpSessionManager.getIStore();

        if (c_logger.isTraceEntryExitEnabled()) {
        	StringBuffer sb = new StringBuffer(sessId).append(" ").append(iStore.getId());
            c_logger.traceEntry(this, "getHttpSessionById",sb.toString());
        }
        try {
            iStore.setThreadContext();
            sd = (HttpSessionImpl)_coreHttpSessionManager.getSession(sessId, true);
        } finally {
            iStore.unsetThreadContext();
        }
        
        if (sd!=null) {
        	if (c_logger.isTraceEntryExitEnabled()) {
                	 c_logger.traceExit(this, "getHttpSessionById","got a session");
            }
            return (HttpSession)sd.getFacade();
        } 
    	
        if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceExit(this, "getHttpSessionById", null);
        }
        return null;
    } 
    
}
