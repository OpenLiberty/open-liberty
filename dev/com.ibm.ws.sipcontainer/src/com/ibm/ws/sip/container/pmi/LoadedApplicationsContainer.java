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
package com.ibm.ws.sip.container.pmi;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.parser.SipAppDesc;

/**
 * @author anat, Mar 22, 2005
 */
public class LoadedApplicationsContainer {
    
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log
        .get(LoadedApplicationsContainer.class);
    
    /**
     * Contain all Application running in the WAS and invoked by SIPContainer
     */
    private HashMap _appTable = new HashMap(10);
    
    
    /** Singleton */
    private static LoadedApplicationsContainer s_singelton;
    
    /**
     * This is the last PMI index allocated for a loaded application.
     * Each application will receive this index that will serve as
     * the key for connecting an application descriptor with the PMI module
     * of the same application.
     */
    private static int c_lastPMIIndex = 0;
    
    /**
     * Gets the single instance of Performance Manager
     * 
     * @return pointer to the PerformanceMgr
     */
    public static LoadedApplicationsContainer getInstance() {
        if (s_singelton == null) {
            s_singelton = new LoadedApplicationsContainer();
        }
        return s_singelton;
    }
    
    /**
     * Find and return application object by name - if object not exist create a
     * new one and add if "createIfNotExist" flag is true
     * 
     * @param applicationName -
     *            name of the application
     * @param appIndex 
     * 				index of the application in application array    
     * @return
     */
    public ApplicationModuleInterface getAppObj(Integer appIndex) {
       if( appIndex == null){ 
    	   return null;
       }
       try {
            return (ApplicationModuleInterface) _appTable.get(appIndex);
        } catch (IndexOutOfBoundsException e) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger
                    .traceDebug(this, "getAppObj",
                                "appIndex is out of range. Index = " + appIndex);
            }
        }
       return null;
    }

    /**
     * New Application was loaded to the container
     * 
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     */
    public void appLoaded(String applicationName,SipAppDesc desc) {

        ApplicationModuleInterface application = getAppObj(desc.getAppIndexForPmi());
        
        if (application == null) {    
        	
        	application = 
        			PerformanceMgr.getInstance().getApplicationsPMIListener().getApplicationModule(applicationName);
        	
            //Timer thread can access the _appTable
            synchronized (_appTable){
            	Integer index = new Integer(c_lastPMIIndex++);
            	_appTable.put(index, application);
            	desc.setAppIndexForPmi( index);
            	if (c_logger.isTraceDebugEnabled()) {
                    StringBuffer buff = new StringBuffer(100);
                    buff.append("Application loaded name < ");
                    buff.append(applicationName);
                    buff.append("> index < ");
                    buff.append(index);
                    buff.append(">");
                    c_logger.traceDebug(this, "appLoaded",buff.toString());
               }
            }
            
        }else if(c_logger.isWarnEnabled())
        {			
            Object[] args = { applicationName }; 
            c_logger.error("warn.server.application.exist", Situation.SITUATION_REPORT, args); 
        }
    }
    
  /**
   * Application was unloaded from the container
   * 
   * @param applicationName
   * @param appIndex 
     * 				index of the application in application array    
   */
    public void appUnloaded(String applicationName, Integer appIndex) {
        
        ApplicationModuleInterface application = null;
        
        synchronized (_appTable ){
        	application = (ApplicationModuleInterface) _appTable
            .remove(appIndex);
        	if(_appTable.isEmpty()) {
        		PerformanceMgr.setIsApplicationDurationPMIEnabled(false);
        	}
        }
        
        if (application == null) {
            StringBuffer buff = new StringBuffer(100);
            buff.append("Failed to find application name < ");
            buff.append(applicationName);
            buff.append("> index < ");
            buff.append(appIndex);
            buff.append(">");
            c_logger.traceDebug(this, "appUnloaded",buff.toString());

        }
        
        else{
        	application.destroy();
        	if (c_logger.isTraceDebugEnabled()) {
	            StringBuffer buff = new StringBuffer(100);
	            buff.append("Application UnLoaded name < ");
	            buff.append(applicationName);
	            buff.append("> index < ");
	            buff.append(appIndex);
	            buff.append(">");
	            c_logger.traceDebug(this, "appUnloaded",buff.toString());
        	}
        }
        
    }
    
    
    /**
     * New SipApplication session was created..
     * 
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array     
     */
    public void sipAppSessionCreated(String applicationName, Integer applicationIndex) {
        
    	ApplicationModuleInterface appObjModule = getAppObj(applicationIndex);

        if (appObjModule != null) {
            appObjModule.getSessionModule().incrementSipAppSessionCount();
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(applicationIndex);
                buff.append(">");
                c_logger.traceDebug(this, "sipAppSessionCreated",buff.toString());
            }
        }
    }
     
     /**
      * SipApplicationSession was destroyed
      * 
      * @param applicationName
      * @param appIndex 
     * 				index of the application in application array    
     */
    public void sipAppSessionDestroyed(String applicationName, Integer appIndex) {

        ApplicationModuleInterface appObjModule = getAppObj(appIndex);

        if (appObjModule != null) {
            appObjModule.getSessionModule().decrementSipAppSessionCount();
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "sipAppSessionDestroyed",buff.toString());
            }
        }

    }
    
    /**
     * SipSessionCreated
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     */
    public void sipSessionCreated(String applicationName,Integer appIndex) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);
        if (appObjModule != null) {
            appObjModule.getSessionModule().incrementSipSessionCount();
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "sipSessionCreated",buff.toString());
            }
        }
    }
    
    /**
     * SipSessin was destroyed
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     */
    public void sipSessionDestroyed(String applicationName,Integer appIndex) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);

        if (appObjModule != null) {
            appObjModule.getSessionModule().decrementSipSessionCount();
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "sipSessionDestroyed",buff.toString());
            }
        }
    }
    
    /**
     * inbound request received
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     * @param method the request method name
     */
    public void inRequest(String applicationName,Integer appIndex, String method) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);
        if (appObjModule != null) {
            appObjModule.getRequestModule().incrementInRequest(method);
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "inRequest",buff.toString());
            }
        }
    }
    
    /**
     * outbound request received
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     * @param method the request method name
     */
    public void outRequest(String applicationName,Integer appIndex, String method) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);
        if (appObjModule != null) {
            appObjModule.getRequestModule().incrementOutRequest(method);
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "outRequest",buff.toString());
            }
        }
    }
    
    /**
     * inbound response received
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     * @param method the request method name
     */
    public void inResponse(String applicationName,Integer appIndex, int code) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);
        if (appObjModule != null) {
            appObjModule.getResponseModule().incrementInResponse(code);
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "inResponse",buff.toString());
            }
        }
    }
    
    /**
     * outbound response received
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     * @param method the request method name
     */
    public void outResponse(String applicationName,Integer appIndex, int code) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);
        if (appObjModule != null) {
            appObjModule.getResponseModule().incrementOutResponse(code);
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name < ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "outResponse",buff.toString());
            }
        }
    }
    
    /**
     * task duration measurement received
     * @param applicationName
     * @param appIndex 
     * 				index of the application in application array    
     * @param ms task duration
     */
    public void updateApplicationTaskDurationStatistics(String applicationName,Integer appIndex, long ms) {
        ApplicationModuleInterface appObjModule = getAppObj(appIndex);
        if (appObjModule != null && appObjModule.getApplicationTaskDurationModule() != null) {
            appObjModule.getApplicationTaskDurationModule().updateTaskDurationInApplication(ms);
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer(100);
                buff.append("Application not found by name or ApplicationTaskDurationModule disabled< ");
                buff.append(applicationName);
                buff.append("> index < ");
                buff.append(appIndex);
                buff.append(">");
                c_logger.traceDebug(this, "task Duration",buff.toString());
            }
        }
    }
    
    /**
     * This method will update PMI about all counters that were
     * counted till now
     */
    public final void updatePmi() {
        ApplicationModuleInterface obj = null;
        synchronized (_appTable) {
	        for (Iterator itr = _appTable.values().iterator(); itr.hasNext();) {
	            obj = (ApplicationModuleInterface)itr.next();
	            if(obj != null){
	                obj.updateCounters();
	            }
	        }
        }
        
    }

}


