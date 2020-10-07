/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.matching;

import jain.protocol.ip.sip.ListeningPoint;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.parser.SipXMLParser;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.rules.Condition;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAlias;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAliasImpl;
import com.ibm.ws.sip.parser.util.InetAddressCache;
import com.ibm.ws.sip.stack.transport.virtualhost.SipVirtualHostAdapter;

/**
 * @author Amir Perlman, Jun 30, 2003
 * Matches Sip Servlet Request to Siplets according to matching conditions. 
 * Conditions are read through XML configuration and evaluated at runtime 
 * according to the request's details.  
 */
public class SipServletsMatcher
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipServletsMatcher.class);
        
    /**
     * List of SIP applications.  
     */
    private LinkedList<SipAppDesc> m_apps = new LinkedList<SipAppDesc>();
    
    /**
     * Map of started sip applications
     */
    private HashMap<String,SipAppDesc> activeApp = new HashMap<String,SipAppDesc>();

    /**
     * Parser for sip.xml files. 
     */
    private SipXMLParser m_parser;

    /**
     * Stores the VirtualHostAlias object that matches each listening point
     */
    private HashMap listeningPointsVHAliasesList = new HashMap();
    
    
    /**
     * Construct a new Matcher. 
     *
     */
    public SipServletsMatcher()
    {
//    	TODO Liberty Anat: uncomment when implement sip.xml
//        try
//        {
//            m_parser = new SipXMLParser();
//        }
//        catch (ParserConfigurationException e)
//        {
//			if (c_logger.isErrorEnabled())
//			{
//	            c_logger.error(
//	                "error.parser.configuration",
//	                Situation.SITUATION_CONFIGURE,
//	                null,
//	                e);
//			}
//        }
    }

    /**
     * Returns all active applications
     * @return
     */
    public LinkedList<SipAppDesc> getAllApps() {
		return m_apps;
	}
   
    /**
     * Adds the specified application to the list of application according to 
     * its start order. The first started application comes first. 
     * @param app
     */
    public void addAppToActiveApplicationsList(SipAppDesc app) 
    {
    	 if (c_logger.isTraceDebugEnabled()) {
             c_logger.traceDebug(
                      this, "addAppToActiveApplicationsList", "Adding App : " + 
                      app.getApplicationName() +
                      " to the active applications list. ");
         }
    	 
//        // Save the application according to display-name 
//        // from web.xml
//        String applicationKey = app.getWebAppName();
    	 String applicationKey = app.getApplicationName();
        activeApp.put(applicationKey, app);
        
        m_apps.add(app);
    }

    /**
     * Unload a Sip Application configuration from matcher. 
     * @param appName Application's name
     * @return The Sip Application object if found otherwise null, 
     */
    public SipAppDesc unloadApplicationConfiguration(String appName)
    {

    	if (c_logger.isTraceEntryExitEnabled())
        {
            Object[] params = { appName };
            c_logger.traceEntry(this, "unloadApplicationConfiguration", params);
        }

        SipAppDesc app = null;

        Iterator iter = m_apps.iterator();
        while (iter.hasNext())
        {
            SipAppDesc temp = (SipAppDesc) iter.next();
            if (temp.getWebAppName().equals(appName))
            {
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(
                        this,
                        "unloadApplicationConfiguration",
                        "application removed");
                }
                m_apps.remove(temp);
                app = temp;
                
                break;
            }
        }
        
        app = this.activeApp.remove(appName);
        
        PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null && app != null){
            // Do it only in case its a SIP application
            perfMgr.appUnloaded(appName,app.getAppIndexForPmi());
        }
        return app;
    }
    
    /**
     * That method should be called after the next application
     * has been already selected 
     * @param request
     * @param appName
     * @return
     */
    public SipServletDesc matchSipletForApplication(SipServletRequestImpl request, 
    		String appName){
    	
	        if (c_logger.isTraceEntryExitEnabled())
		    {
		        c_logger.traceEntry(this, "matchSipletForApplication", 
		        		new String[]{request.getMethod() , appName});
		    }

        	SipAppDesc app = null;
    		SipServletDesc matchedSiplet = null;
    	
	        app = this.activeApp.get(appName);
            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, "matchSipletForApplication", "Application router asked match for application: " + appName);
            }

	        if (app == null){	 

	        	// Send an error response.
	        	request.processCompositionErrorResponse();
	        	
	        	if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(this, "matchSipletForApplication", "There is no application been installed: " + appName );
                }
	        	
	        	return null;
	        }

	        if(shouldExcludeFromApplicationRouting(request, app)){
	        	return null;
	        }
	        
	        String virtualHost = null;
        	//In case the the request is already associated with a virtual host(VH)
        	//then we should only look for applications within the same VH. 
        	virtualHost = request.getVirtualHost();
        	if(!isModuleInCorrectVH( request, app, virtualHost)){
                return null;
        	}
	        
        	// JSR 289 if there is a main siplet defined
        	// the rules from JSR 116 are irrelevant
        	if (app.hasMainServlet()){
        		matchedSiplet = app.getMainSiplet();
        		if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(this, "matchSipletForApplication", 
                    		"Selecting main servlet="+matchedSiplet);
                }
        	} else {
		        for (SipServletDesc siplet : app.getSipServlets())
		        {
		            Condition rule = siplet.getTriggeringRule();
		
		            //Evaluate the siplet's condition for the given request. 
		            //Check also if the siplet is not the list of siplets excluded 
		            //from this request due to Application Composition.  
		            if (rule != null && rule.evaluate(request))
		            {
		                matchedSiplet = siplet;
		                if (c_logger.isTraceDebugEnabled())
		                {
		                    StringBuffer buffer = new StringBuffer(64);
		                    buffer.append("Siplet: ");
							buffer.append(matchedSiplet);
							buffer.append(" matched request: ");
							buffer.append(request.getMethod());
		                    c_logger.traceDebug(this, "matchSipletForApplication", buffer.toString());
		                }
		                break;
		            }
		        }
        	}
	
	    if (c_logger.isTraceEntryExitEnabled())
	    {
	        c_logger.traceExit(this, "matchSipletForApplication",matchedSiplet);
	    }
	
	    request.setNextApplication(null);
	    
	    return matchedSiplet;
    }
    
    /**
     * Checks whether should exclude the current application or the request originating application
     * from the application routing.
     * 
     * @param request request to match a SIP application
     * @param appDesc current SipAppDesc
     * @return true whether should exclude, otherwise false
     */
    private boolean shouldExcludeFromApplicationRouting(SipServletRequestImpl request, 
    		SipAppDesc appDesc){
    	
    	// check if the current application should be excluded
    	if(appDesc.shouldExcludeFromApplicationRouting()){
			if (c_logger.isTraceDebugEnabled()){
	            c_logger.traceDebug(this, "shouldExcludeFromApplicationRouting", "The application " + 
	            													    appDesc.getApplicationName() + 
	            													   " is configured to be excluded from application routing");
	        }	
    	   
    	   return true;
       }
       
       // check if the request originating application should be excluded
       SipApplicationSession sas = request.getApplicationSession();
       if(sas != null){
    	   String originatingAppName = sas.getApplicationName();
    	   if(originatingAppName != null){
    		   SipAppDesc originatingSipAppDesc = this.activeApp.get(originatingAppName);
    		   if(originatingSipAppDesc != null && originatingSipAppDesc.shouldExcludeFromApplicationRouting()){
    			   if (c_logger.isTraceDebugEnabled()){
   		            c_logger.traceDebug(this, "shouldExcludeFromApplicationRouting", "The originating application " + 
   		            														originatingAppName + 
   		            													   " is configured to be excluded from application routing");
   		        	}	
   	    	   
   				return true;
    		   }
    	   }
       }
    	
    	return false;
    }
    
    /**
     * Checks if the request received from a host:port that belongs to the 
     * module virtual host.
     * On a case of application composition, the first time this method is invoked
     * the host:port of the listening point should be used to determine what is
     * the VH the request originated from. 
     * On the next times, i.e. when  looking for the next applications this request 
     * should be forward to, we use the virtual host name that was already resolved
     * on the first visit, and try to match it with the VH name stored on each 
     * app description object.   
     * @param request
     * @param app
     * @param virtualHostName
     * @return
     */
    private boolean isModuleInCorrectVH( SipServletRequestImpl request,
            						  	 SipAppDesc app,
            						  	 String virtualHostName) {
        
        
        if( virtualHostName != null){
            return app.getVirtualHostName().equals( virtualHostName);
        }
        
        
        ListeningPoint listeningPoint = request.getSipProvider().getListeningPoint();
        VirtualHostAlias vha = 
            (VirtualHostAlias)listeningPointsVHAliasesList.get(listeningPoint);
       
        if( vha == null){
            vha = new VirtualHostAliasImpl();
            
            String hostName = listeningPoint.getHost();
            //try to find the listening point host name to set on the virtual host
            try {
				InetAddress addr = InetAddressCache.getByName(hostName);
				hostName = addr.getHostName();
			} catch (UnknownHostException e) {
				 if( c_logger.isTraceDebugEnabled()){
		                c_logger.traceDebug(this, "isModuleInCorrectVH", 
		                    "failed to lookup host, " + hostName);
		            }
			}
			
            vha.init( hostName, listeningPoint.getPort());
            listeningPointsVHAliasesList.put( listeningPoint, vha);
        }
        
        boolean isVhMatch = SipVirtualHostAdapter.isHostAliasMatchVirtualHost(vha, app);
        
        if( c_logger.isTraceDebugEnabled()){
        	if(isVhMatch){
        	      c_logger.traceDebug( this, "isModuleInCorrectVH", 
             		     "found match for virtual host alias");
            }
            else{
                c_logger.traceDebug( this, "isModuleInCorrectVH", 
           		     "no match for virtual host alias");
            }
        }
        
        return isVhMatch;
    }

    /**
     * Gets the default handler for unmatched request. By default we will use
     * the first siplet in the first app loaded. 
     * @return
     */
    public SipServletDesc getDefaultHandler()
    {
        SipServletDesc sipletDesc = null;

        if (m_apps.size() > 0)
        {
            SipAppDesc appDesc = (SipAppDesc) m_apps.getFirst();

            if (null != appDesc)
            {
                sipletDesc = appDesc.getDefaultSiplet();
            }
        }

        return sipletDesc;
    }

    /**
     * Gets a Sip Servlet description object according to the siplet's names. Searches
     * through all application and tries to find matching siplet.   
     * @param name The name of the siplet as appears in the sip.xml file. 
     * @return The matching Sip Servlet Descriptor if available otherwise null. 
     */
    public SipServletDesc getSipletByName(String name)
    {
    	
    	for(SipAppDesc appDesc : this.activeApp.values()){
    		SipServletDesc siplet = 
    			appDesc.getSipServlet(name);
    		
    		if (siplet != null) return siplet;
    	}
    	
        return null;
    }

    /**
     * Gets the SIP App descriptor for the given application name. 
     * @param appName The name of the SIP Application. 
     * @return The SIP App Descriptor if available, otherwise null
     */
    public SipAppDesc getSipApp(String appName)
    {
        return this.activeApp.get(appName);
    }
    
    /**
     * Gives number of running SIP applications
     * @return
     */
    public int getNumOfRunningApplications(){
    	return this.activeApp.values().size();
    }

	/**
	 * Load the application configuration for the sip application
	 * @param app
	 */
	public void loadAppConfiguration(SipAppDesc app) {
	     
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { app };
			c_logger.traceEntry(this, "loadAppConfiguration", params);
		}

		addAppToActiveApplicationsList(app);

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "loadAppConfiguration",
					"Sip App loaded successfully: " + app.getApplicationName());
		}

		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
			perfMgr.appLoaded(app.getApplicationName(),app);	       
		}
	}
}
