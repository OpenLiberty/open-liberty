/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.monitor;

import java.util.HashMap;

import com.ibm.websphere.monitor.annotation.Args;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.session.ISession;


@Monitor(group = "Session")
public class SessionMonitor extends StatisticActions {

    @PublishedMetric
    private MeterCollection<SessionStats> sessionCountByName = new MeterCollection<SessionStats>("Session",this);           
    private static final TraceComponent tc = Tr.register(SessionMonitor.class,"Monitor");
    private static HashMap<String, SessionStats> tMap = new HashMap<String, SessionStats>();    
    //For Legacy PMI
    //Define stats ID here.
    public static final int ACTIVE_SESSIONS = 6;
    public static final int LIVE_SESSIONS = 7;
    public static final int CREATE_SESSIONS = 1;
    public static final int INVALIDATED_SESSIONS = 2;
    public static final int INVALIDATED_SESSIONS_BYTIMEOUT = 16;
    //-----------------------

    private static final String template = "/com/ibm/ws/session/monitor/xml/servletSessionsModule.xml";
    private StatsGroup grp;    
    private HashMap<String,LegacyMonitor> tCPMIMap = new HashMap<String, LegacyMonitor>();    
    public SessionMonitor() {
        try {
            /*
             * Below code comes into picture only when traditional PMI is enabled
             * */
        	if(StatsFactory.isPMIEnabled()){        		
             grp = StatsFactory.createStatsGroup("SessionStats", template, null, this);
        	}
        } catch (StatsFactoryException e) {
            //If PMI Is disabled, we get this.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Session Module is not registered with PMI");
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "SessionMonitor");
        }
    }
    /**
     * @param appName
     * @param sessionStats
     */
    private synchronized SessionStats initializeSessionStats(String appName, SessionStats sessionStats) {
        // TODO Auto-generated method stub
    	SessionStats newStats = this.sessionCountByName.get(appName);
    	try{
        if(newStats==null){
        	newStats = new SessionStats();
            this.sessionCountByName.put(appName,  newStats);
            /*
             * Below code comes into picture only when traditional PMI is enabled
             * */
            if(StatsFactory.isPMIEnabled()){
            tCPMIMap.put(appName, new LegacyMonitor(appName,grp));
            tMap.put(appName, newStats);                       
            }
        }        
    	}catch(Exception e)
    	{
    		if(tc.isDebugEnabled()){
    			Tr.debug(tc, "Exception occured ", e.getMessage());
    		}
    	}
    	return newStats;
    }
    /**
     * @param myargs
     * Increment LiveCount.
     * Initially we check if the application for whose sessions are being monitored is part of 
     * MeterCollection or not.If not we add it and then increment/decrement the liveCount.Same approach is taken
     * for every application.
     * @Args provides us with the arguments of the particular method which is part of the ProbeSite annotation.In this
     * case we get ISession object using which we retrieve the appName which will be the key for our MeterCollection
     * as Sessions are collected per Application.
     */
    //TO DO --Use StoreCallback instead of SessionEventDispatcher??
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionLiveCountInc",args="java.lang.Object")
    public void IncrementLiveCount(@Args Object[] myargs )
    {                      	
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "IncrementLiveCount");
        }
        if(myargs == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "IncrementLiveCount","Args list is null");
            }
            return;
        }
        ISession session = (ISession) myargs[0];
        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc,"IncrementLiveCount","session object is null"); 
            }
              return;
        }        
        String appName =  session.getIStore().getId();        
        SessionStats sStats = sessionCountByName.get(appName);        
        if(sStats==null){
            sStats = initializeSessionStats(appName ,new SessionStats() );
        }      
        
        sStats.liveCountInc();    
        if(tc.isEntryEnabled()){
            Tr.exit(tc, "IncrementLiveCount");
        }
    }
    /**
     * @param myargs
     * Decrement LiveCount
     */
    //TO DO --Use StoreCallback instead of SessionEventDispatcher??
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionLiveCountDec",args="java.lang.Object")
    public void DecrementLiveCount(@Args Object[] myargs )
    {              
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "DecrementLiveCount");
        }
        if(myargs == null){
            if(tc.isEntryEnabled()){
            Tr.exit(tc, "DecrementLiveCount", "Args is null");
            }
            return;
        }
        ISession session = (ISession) myargs[0];
        
        if(session == null){
            if(tc.isEntryEnabled()){
            Tr.exit(tc, "DecrementLiveCount", "session is null");
            }
              return;
        }
        String appName =  session.getIStore().getId();
        
        if((sessionCountByName.get(appName))==null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "DecrementLiveCount", "AppName Not found in MeterCollection");
            }
              return ;
        }      
        SessionStats sStats = sessionCountByName.get(appName);        
        sStats.liveCountDec();    
        if(tc.isEntryEnabled()){
            Tr.exit(tc, "DecrementLiveCount");
        }
    }
    
    
    
    
    /**
     * @param myargs
     * Increments ActiveCount
     */    
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionAccessed",args="com.ibm.wsspi.session.ISession")
    public void IncrementActiveCount(@Args Object[] myargs )
    {      
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "IncrementActiveCount");
        }
        if(myargs == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "IncrementActiveCount", "Args is null");
            }
            return;
        }
        ISession session = (ISession) myargs[0];
        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "IncrementActiveCount", "Session Object is null");
            }
              return;
        }
        String appName =  session.getIStore().getId();   
        SessionStats sStats = sessionCountByName.get(appName);
        if(sStats == null){            
        	sStats =initializeSessionStats(appName ,new SessionStats() );
        }
        sStats.activeCountInc();  
        if(tc.isEntryEnabled()){
            Tr.exit(tc, "IncrementActiveCount");
        }
    }
    /**
     * @param myargs
     * Decrement ActiveCount
     */
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionReleased",args="com.ibm.wsspi.session.ISession")
    public void DecrementActiveCount(@Args Object[] myargs )
    {         
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "DecrementActiveCount");
        }
        if(myargs == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "DecrementActiveCount", "Args is null");
            }
            return;
        }
        ISession session = (ISession) myargs[0];
        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "DecrementActiveCount", "Session Object is null");
            }
              return;
        }
        String appName =  session.getIStore().getId();  
        if((sessionCountByName.get(appName))==null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "DecrementActiveCount", "AppName Not found in MeterCollection");
            }
            return ;
        }      
        SessionStats sStats = sessionCountByName.get(appName);        
        sStats.activeCountDec();
        if(tc.isEntryEnabled()){
            Tr.exit(tc, "DecrementActiveCount");
        }
    }
    
    /**
     * @param myargs
     * Increments CreateCount
     */
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionCreated",args="com.ibm.wsspi.session.ISession")
    public void incrementCreateCount(@Args Object[] myargs )
    {    
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "incrementCreateCount");
        }
        if(myargs == null){
            if(tc.isEntryEnabled()){
              Tr.exit(tc, "incrementCreateCount", "Args is null");  
            }
            return;
        }
        ISession session = (ISession) myargs[0];
        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "incrementCreateCount", "Session Object is null");
            }
              return;
        }
        String appName =  session.getIStore().getId(); 
        SessionStats sStats = sessionCountByName.get(appName); 
        if(sStats==null){                                  
        	sStats = initializeSessionStats(appName ,new SessionStats() );
        }      
        sStats.incCreateCount();
        sStats.activeCountInc(); 
        if(tc.isEntryEnabled()){
            Tr.exit(tc, "incrementCreateCount");
        }
    }
    
    /**
     * @param myargs
     * Increments invalidatedCountbyTimeout
     */
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionDestroyedByTimeout",args="com.ibm.wsspi.session.ISession")
    public void InvalidatedSessionsByTimeout(@Args Object[] myargs )
    {   
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "InvalidatedSessionsByTimeout");
        }
        if(myargs == null){
            if(tc.isEntryEnabled())
            {
                Tr.exit(tc, "InvalidatedSessionsByTimeout", "Args is null");
            }
            return;
        }
        ISession session = (ISession) myargs[0];
        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessionsByTimeout", "Session Object is null");
            }
              return;
        }
        
        String appName =  session.getIStore().getId();                
        if((sessionCountByName.get(appName))==null){  
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessionsByTimeout", "AppName Not found in MeterCollection");
            }
            return;
        }      
        SessionStats sStats = sessionCountByName.get(appName);                
        sStats.setInvalidatedCountbyTimeout();      

        if(tc.isEntryEnabled()){
            Tr.exit(tc, "InvalidatedSessionsByTimeout");
        }
    }

    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionEventDispatcher",method="sessionDestroyed",args="com.ibm.wsspi.session.ISession")                                                                    
    public void InvalidatedSessions(@Args Object[] myargs )
    {           
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "InvalidatedSessions");
        }
        if(myargs == null){
            if(tc.isEntryEnabled())
            {
                Tr.exit(tc, "InvalidatedSessions", "Args is null");
            }
            return;
        }        
        ISession session = (ISession) myargs[0];        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessions", "Session Object is null");
            }
              return;
        }
        
        String appName =  session.getIStore().getId();                
        if((sessionCountByName.get(appName))==null){  
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessions", "AppName Not found in MeterCollection");
            }
            return;
        }      
        SessionStats sStats = sessionCountByName.get(appName);                
        sStats.setInvalidatedCount();      

        if(tc.isEntryEnabled()){
            Tr.exit(tc, "InvalidatedSessions");
        }
    }
    

    /**
     * @param myargs
     * This below method is mainly used to track invalidated sessions in DB.The DB code which invokes invalidations
     * is present in com.ibm.ws.session.db project and from there call is made to SessionStatistics to track invalidated
     * count.So we are injecting code inside sessionStatistics sessionDestroyed.
     * 
     * TO-DO Revisit this code in future to make sure as in here we are assuming that this code is only accesses when 
     * sessions in DB comes into picture.Need to have a common injection point for sessions invalidation's irrespective
     * of inMemory sessions or DB sessions.
     */
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionStatistics",method="sessionDestroyed",args="com.ibm.wsspi.session.ISession")                                                                    
    public void InvalidatedSessionsinDB(@Args Object[] myargs )
    {           
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "InvalidatedSessionsinDB");
        }
        if(myargs == null){
            if(tc.isEntryEnabled())
            {
                Tr.exit(tc, "InvalidatedSessionsinDB", "Args is null");
            }
            return;
        }        
        ISession session = (ISession) myargs[0];        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessionsinDB", "Session Object is null");
            }
              return;
        }
        
        String appName =  session.getIStore().getId();                
        if((sessionCountByName.get(appName))==null){  
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessionsinDB", "AppName Not found in MeterCollection");
            }
            return;
        }      
        SessionStats sStats = sessionCountByName.get(appName);                
        sStats.setInvalidatedCount();      

        if(tc.isEntryEnabled()){
            Tr.exit(tc, "InvalidatedSessionsinDB");
        }
    }
    
    
    /**
     * @param myargs
     * This below method is mainly used to track invalidatedbyTimeout sessions in DB.The DB code which invokes invalidations
     * is present in com.ibm.ws.session.db project and from there call is made to SessionStatistics to track invalidated
     * count.So we are injecting code inside sessionStatistics sessionDestroyed.
     * 
     * TO DO-Revisit this code in future to make sure as in here we are assuming that this code is only accesses when 
     * sessions in DB comes into picture.Need to have a common injection point for sessions invalidation's irrespective
     * of inMemory sessions or DB sessions.
     */
    @ProbeAtEntry
    @ProbeSite(clazz="com.ibm.ws.session.SessionStatistics",method="sessionDestroyedByTimeout",args="com.ibm.wsspi.session.ISession")                                                                    
    public void InvalidatedSessionsbyTimeoutinDB(@Args Object[] myargs )
    {           
        if(tc.isEntryEnabled()){
            Tr.entry(tc, "InvalidatedSessionsbyTimeoutinDB");
        }
        if(myargs == null){
            if(tc.isEntryEnabled())
            {
                Tr.exit(tc, "InvalidatedSessionsbyTimeoutinDB", "Args is null");
            }
            return;
        }        
        ISession session = (ISession) myargs[0];        
        if(session == null){
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessionsbyTimeoutinDB", "Session Object is null");
            }
              return;
        }
        
        String appName =  session.getIStore().getId();                
        if((sessionCountByName.get(appName))==null){  
            if(tc.isEntryEnabled()){
                Tr.exit(tc, "InvalidatedSessionsbyTimeoutinDB", "AppName Not found in MeterCollection");
            }
            return;
        }      
        SessionStats sStats = sessionCountByName.get(appName);                
        sStats.setInvalidatedCountbyTimeout();      

        if(tc.isEntryEnabled()){
            Tr.exit(tc, "InvalidatedSessionsbyTimeoutinDB");
        }
    } 
    
    
    /**
     * @param appName
     * @return
     * Utility method which comes into picture in case of traditional PMI
     */
    static SessionStats  getSessionStatsOB(String appName)
    {
    	return tMap.get(appName);
    }
    
}