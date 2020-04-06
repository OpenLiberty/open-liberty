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

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.load.ApplicationQueueCounter;
import com.ibm.ws.sip.container.load.ApplicationSessionCounter;
import com.ibm.ws.sip.container.load.ConcurentLoadListener;
import com.ibm.ws.sip.container.load.DisabledCounter;
import com.ibm.ws.sip.container.load.MPAPCounter;
import com.ibm.ws.sip.container.load.ResponseTimeCounter;
import com.ibm.ws.sip.container.load.Weighable;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.pmi.basic.SipContainerCounter;
import com.ibm.ws.sip.container.pmi.listener.ApplicationsPMIListener;
import com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr;

/**
 * @author Anat Fradin Nov 2, 2004 Manage calculate and forward information to
 *         the PMI (Performance Monitoring Interface)
 *         
 *
 * A declarative services component.
 * The component is responsible of PMI and overload detection.
 * Is injected when a first request recieved
 * 
 */
@Component(service = PerformanceMgr.class,
configurationPolicy = ConfigurationPolicy.OPTIONAL,
name = "com.ibm.ws.sip.container.pmi",
configurationPid = "com.ibm.ws.sip.container.pmi",
property = {"service.vendor=IBM"})
public class PerformanceMgr implements ConcurentLoadListener, StackExternalizedPerformanceMgr {

	/*
	 * Trace variable
	 */
	private static final TraceComponent tc = Tr.register(PerformanceMgr.class);
    /** Logger class */
    private static final LogMgr c_logger = Log.get(PerformanceMgr.class);
    
    /** Update the PMI every _statUpdateRange , default = 10000 <milisec> */
    private int _statUpdateRange = CoreProperties.STAT_UPDATE_RANGE_DEFAULT;
    
    /** The timer which is responsible for updating LoadMgr with new server Weight
     * will be executed every _loadUpdatePeriod*/
    private int _loadUpdatePeriod = CoreProperties.LOAD_UPDATE_PERIOD_DEFAULT;
    
    /**
     * PMI will be updated about num of the messages arrived and sent in the
     * __pmiUpdateTempo time , default = 1000 <milisec>
     * 
     * The _statAveragePeriod will be always less or equal as the _pmiUpdateTempo
     */
    private int _statAveragePeriod = CoreProperties.STAT_AVERAGE_PERIOD_DEFAULT;

    /** PMI module variable */
    private SipContainerPMIListener _pmiModule = null;
    
    /** Applications PMI module */
    private ApplicationsPMIListener _appsPMIModule = null;

    /** Application name used for PMI */
    public final static String _appName = "SipContainer";

    /** Incoming messages counter during STAT_UPDATE_RANGE */
    private long _receivedMsgCounter = 0;

    /** new Sip Application Session created during STAT_UPDATE_RANGE */
    private long _newSipAppCounter = 0;

    /**
     * Summary of time takes to response the incoming requests during
     * STAT_UPDATE_RANGE
     */
    private long _summaryResponseTime = 0;

    /** How many requests was responded in last STAT_UPDATE_RANGE */
    private long _responcesCounter = 0;
    
    /** Rejected messages counter = 0; */
    private long _rejectedMessagesCounter = 0;
    
    /** SIP timers invocations*/
    private long _sipTimersInvocations = 0;

     /** Maximum sipAppSessions allowed , default = 120000 */
    private int _maxSipAppSessionsAllowed = CoreProperties.MAX_APP_SESSIONS_DEFAULT;

    /** Maximum traffic allowed per averaging period , default = 5000 */
    private int _maxMessageRate = CoreProperties.MAX_MESSAGE_RATE_DEFAULT;
    
    /** Maximum response time allowed in millisecond, default = 0 (no limit) */
    private int _maxResponseTime = CoreProperties.MAX_RESPONSE_TIME_DEFAULT;

    /** Maximum waiting SIP messages in the Container Queue */
    //Default according to NativeMessageDispatchingHandler.s_maxDispatchPerThread * 3
    private int _maxMsgQueueSize = CoreProperties.MAX_MSG_QUEUE_SIZE_DEFAULT;
   
    /**
     * Variable that contain all applications loaded into the container */
    private LoadedApplicationsContainer _applicationsContainer;
   
    /** Counter that counts all statistics before update PMI */
    private SipContainerCounter _commonCounter = new SipContainerCounter();
    
    /**
     * Array of Weighable which are affect on the load and server weight.
     */
    private Weighable [] _loadCounters;

    /** Singleton, created on the service activation */
    private static PerformanceMgr s_singleton = null;

    /**
     * Indicates is load monitoring (overload protection) mechanism enables
     */
    private boolean _isOverloadProtectionEnabled = CoreProperties.ENABLE_LOAD_MONITORING_DEFAULT;       
    
	/**
	 * Holds the last index of counter which set the weight
	 */
    private int _lastWeightIndex = -1;
	
	/**
	 * Holds the last Weight.
	 */
    private int _lastWeight = -1;
    
    /**
     * Represents a scheduled {@link ScheduledFuture} for {@link TimerPMIListener}.
     */
    private ScheduledFuture<?> _pmiTimer;
    
    /**
     * Represents a scheduled {@link ScheduledFuture} for {@link TimeWeightListener}.
     */
    private ScheduledFuture<?> _weightTimer;
    
    /**
     * An injected ScheduledExecutorService declarative service
     */
    private ScheduledExecutorService scheduledExecutorService;
    
    private Object _sessionSynchronizer = new Object();
    
    private Object _appSessionSynchronizer = new Object();
    
    private Object _rejectedMessagesSynchronizer = new Object();
    
    private Object _sipTimersInvocationsSynchronizer = new Object();
    
    /** A synchronizer for creating a PMI timer. */
    private Object _pmiTimerCreationSynchronizer = new Object();
    
    /** Indicates whether the PMI timer was created. */
    private boolean _pmiTimerCreated = false;
    
    /** Indicates whether the performance manager was initialized. */
    private boolean _perfMgrInitialized = false;
    
    private static boolean _isApplicationDurationPMIEnabled = true; // todo
    
    /**
     * cached timer service
     */
    CachedTimerService _timerService =null;
    
    /**
     * timer granularity
     */
    int _timerServiceGranularity = CoreProperties.TIME_GRANULARITY_OF_CHACHED_TIMER_SERVICE_DEFAULT;
    
    /**
     * Gets the single instance of Performance Manager
     * The instance is created on the service activation. If the service was not activated, the instance will be null.
     * 
     * @return instance of PerformanceMgr or null if not activated
     */
    public static PerformanceMgr getInstance() {
        return s_singleton;
    }
   
    /**
     * Ctor
     */
    public PerformanceMgr() {
    }
    
    /**
	 * DS method to activate this component.
	 * 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 *            
	 * Note: meanwhile there is no configured properties for PMI
	 */
    protected void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "PerformanceMgr activated", properties);
        
        // Keep the instance created in DS activation
        s_singleton = this;
    }
    
    /**
	 * DS method to modify this component.
	 * 
	 * @param properties : Map containing service & config properties
	 *            populated/provided by config admin
	 *            
	 * Note: meanwhile there is no configured properties for PMI
	 */
    @Modified
	protected void modified(Map<String, Object> properties) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, "PerformanceMgr modified", properties);
		
		SipPropertiesMap prop = PropertiesStore.getInstance().getProperties();
        prop.updateProperties(properties);
        
        if (isPMIEnabled()) {
	        int newRange = readProperty(prop, CoreProperties.STAT_UPDATE_RANGE, _statUpdateRange, false); 
	        if (newRange != _statUpdateRange) {
	        	_statUpdateRange = newRange;
	        	
	        	//Canceling the timer for update the statistic periodically
	        	cancelTimer(_pmiTimer, TimerPMIListener.class.getName()); 
	        	
	        	if (c_logger.isTraceDebugEnabled()) {
	    			c_logger.traceDebug(this, "modified", "Re-creating TimerPMIListener, statUpdateRange=" + _statUpdateRange);
	    		}
	    		// Create and run a timer for update the statistic periodically
	    		_pmiTimer = scheduledExecutorService.scheduleAtFixedRate(new TimerPMIListener(), 0, _statUpdateRange, TimeUnit.MILLISECONDS);

	        }
        }
    }
    
    /**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int representation of reason the component is stopping
	 */
    public void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "PerformanceMgr deactivated, reason="+reason);
    }	
	
    /**
     * Initialization of the PreformanceManager module
     * 
     * @param standaloneMode
     *            identify working mode
     * @param prop
     *            properties object
     */
    public void init(SipPropertiesMap prop) {    	
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "PerformanceMgr -> init");
        }
       _applicationsContainer = LoadedApplicationsContainer.getInstance();       
               
       // read the maximum active SIP APP Sessions allowed;
       _maxSipAppSessionsAllowed = readProperty(prop, 
       	CoreProperties.MAX_APP_SESSIONS,_maxSipAppSessionsAllowed, true);

       // read the maximum traffic allowed per averaging period;
       _maxMessageRate = readProperty(prop, 
       	CoreProperties.MAX_MESSAGE_RATE,_maxMessageRate, true);

       // read the limit for allowed response time
       _maxResponseTime = readDurationProperty(prop,
       	CoreProperties.MAX_RESPONSE_TIME, _maxResponseTime, true);

//     read the maximum queue size allowed;
       _maxMsgQueueSize = readProperty(prop, 
       	CoreProperties.MAX_MSG_QUEUE_SIZE, _maxMsgQueueSize, true);
        
       // read the tempo for statistic updates;
        _statUpdateRange = readProperty(prop, 
        	CoreProperties.STAT_UPDATE_RANGE, _statUpdateRange, false);
 
        // read the period that traffic data will be updated for
        _statAveragePeriod = readProperty(prop, 
        	CoreProperties.STAT_AVERAGE_PERIOD, _statAveragePeriod, false);
        
        // read the period on which server Weight will be recalculated and
        // updated if needed
        _loadUpdatePeriod = readProperty(prop, 
        	CoreProperties.LOAD_UPDATE_PERIOD, _loadUpdatePeriod, false);
        
        //Retrieve PMI queue duration counters time service granularity
        _timerServiceGranularity = readProperty(prop, 
             	CoreProperties.TIME_GRANULARITY_OF_CHACHED_TIMER_SERVICE, _timerServiceGranularity, false);       

        // If the update tempo is less than update period -
        //change the tempo to be equal to the period
        if (_statAveragePeriod > _statUpdateRange)
        	_statUpdateRange = _statAveragePeriod;

         if (c_logger.isTraceDebugEnabled()) {
            StringBuffer b = new StringBuffer(180);
            b.append("_maxSipAppSessionsAllowed = ");
            b.append(_maxSipAppSessionsAllowed);
            b.append("_maxTrafficAllowed = ");
            b.append(_maxMessageRate);
            b.append("_maxResponseTime = ");
            b.append(_maxResponseTime);
            b.append("_statAveragePeriod = ");
            b.append(_statAveragePeriod);
            b.append("_pmiUpdateTempo = ");
            b.append(_statUpdateRange);
            b.append("_loadUpdateTempo = ");
            b.append(_loadUpdatePeriod);
            b.append("_maxMsgQueueSize = ");
            b.append(_maxMsgQueueSize);
            
            c_logger.traceDebug(this, "PerformanceMgr -> init", b.toString());
         }
         
         _isOverloadProtectionEnabled = prop.getBoolean(CoreProperties.ENABLE_LOAD_MONITORING);
         //Overload protection is enabled by default
         if (!_isOverloadProtectionEnabled) {
             if (c_logger.isTraceDebugEnabled()) {
                 c_logger.traceDebug(this, "init", "Overload protection is disabled...");
             }
         }
         else{
             c_logger.traceDebug(this, "init", "Overload protection is enabled...");
             createLoadConters();         
             createWeightTimer();
         }
         
         // Synchronize on the PMI creation such that it's going to be created only once - 
         // here or in SetSipContainerPMIListener 
         synchronized(_pmiTimerCreationSynchronizer) {
        	 if (!_pmiTimerCreated && isPMIEnabled()) {
        		// The timer is created in case SipContainerPMIListener is injected before init().
        		 createPMITimer();
        		 _pmiTimerCreated = true;
        	 }
        	 _perfMgrInitialized = true;
         }
    }
    
    /**
     * Injecting the ScheduledExecutorService DS
     * @param scheduledExecutorService
     */
    @Reference
    protected void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
		this.scheduledExecutorService = scheduledExecutorService;
	}
    
    /**
     * Private helper method which creates all timers used by the PerfomanceMgr
     */
    private void createWeightTimer() {	
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "createWeightTimer", "Creating TimeWeightListener, loadUpdatePeriod=" + _loadUpdatePeriod);
    	}
    	// Create and run a timer for server weight update
    	_weightTimer = scheduledExecutorService.scheduleAtFixedRate(new TimeWeightListener(), 0, _loadUpdatePeriod, TimeUnit.MILLISECONDS);
    }
    
    public void createPMITimer() {    	
    	// Create PMI timer if the service is activated and the PMIListener is set
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createPMITimer", "Creating TimerPMIListener, statUpdateRange=" + _statUpdateRange);
		}
		// Create and run a timer for update the statistic periodically
		_pmiTimer = scheduledExecutorService.scheduleAtFixedRate(new TimerPMIListener(), 0, _statUpdateRange, TimeUnit.MILLISECONDS);
 	
		if (_timerService == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createPMITimer", "Creating CachedTimerService, timerServiceGranularity=" + _timerServiceGranularity);
			}
			_timerService = new CachedTimerService(_timerServiceGranularity);
		}
    }

	/**
     * Helper method which is creates all loadCounters.
     *
     */
    private void createLoadConters(){
    	
    	_loadCounters = new Weighable [PerfUtil.WEIGHT_ARRAY_SIZE];
    	
    	// Create a new instance of ApplicationSessionCounter
    	_loadCounters[PerfUtil.APP_WEIGHT_INT] = 
    			new ApplicationSessionCounter(_maxSipAppSessionsAllowed,
    					LoadManager.getInstance().getLowWaterMarkSize(),
       	 								      PerfUtil.INITIAL_WEIGHT,
       	 								      this);
              
		// Create a new instance of MPAPCounter
       _loadCounters[PerfUtil.MPAP_WEIGHT_INT] = 
    	   			new MPAPCounter( 	_maxMessageRate,
    	   					LoadManager.getInstance().getLowWaterMarkSize(),
									    PerfUtil.INITIAL_WEIGHT,
									    _statAveragePeriod);

       // Create a new instance of ResponseTimeCouner
       if (_maxResponseTime == 0) {
			// When _maxResponseTime == 0, meaning that the timer is disabled.
			_loadCounters[PerfUtil.RESPONSE_WEIGHT_INT] = 
				new DisabledCounter(PerfUtil.RESPONSE_WEIGHT_INT);
		} 
       else {
    	   //The response time couter will be calculated with average of 1 sec.
			_loadCounters[PerfUtil.RESPONSE_WEIGHT_INT] = 
				new ResponseTimeCounter(_maxResponseTime, 
						LoadManager.getInstance().getLowWaterMarkSize(),
										PerfUtil.INITIAL_WEIGHT, 
										1000);
		}
       
        // Create a new instance of ApplicationQueueCounter
         _loadCounters[PerfUtil.MSG_QUEUE_SIZE_INT] = 
        	 	new ApplicationQueueCounter(_maxMsgQueueSize,
        	 			LoadManager.getInstance().getLowWaterMarkSize(),
        	 									PerfUtil.INITIAL_WEIGHT,
        	 									this);
    }
    /**
     * Read property from the properties
     * 
     * @param prop
     *            properties object
     * @param propertyName
     *            property name to read
     * @return int default value of property
     */
    private int readProperty(SipPropertiesMap prop, String propertyName,
            int defaultValue, boolean canBeDisabled) {
        int int_value = prop.getInt(propertyName);

        if(int_value <= 0){
        	
        	if (canBeDisabled){
        		int_value = 0;
        	}
        	else{
        		if (c_logger.isTraceDebugEnabled()) {
        			StringBuffer buff = new StringBuffer();
        			buff.append("propertyName = ");
        			buff.append(" Set default Value ");
					c_logger.traceDebug(this, "readProperty", buff.toString());
				}
        		int_value = defaultValue;
        	}
        }
		
        return int_value;
    }
    
    /**
     * Read property from the properties
     * 
     * @param prop
     *            properties object
     * @param propertyName
     *            property name to read
     * @return long default value of property
     */
    private int readDurationProperty(SipPropertiesMap prop, String propertyName,
            int defaultValue, boolean canBeDisabled) {
        int int_value = prop.getDuration(propertyName);

        if(int_value <= 0){
        	
        	if (canBeDisabled){
        		int_value = 0;
        	}
        	else{
        		if (c_logger.isTraceDebugEnabled()) {
        			StringBuffer buff = new StringBuffer();
        			buff.append("propertyName = ");
        			buff.append(" Set default Value ");
					c_logger.traceDebug(this, "readProperty", buff.toString());
				}
        		int_value = defaultValue;
        	}
        }
		
        return int_value;
    }
    
    /**
     * Update PMI about new loaded application 
     * 
     * @param applicationName
     *            name of loaded application
     * @param appIndex 
     * 				index of the application used by PMI
     */
    public void appLoaded(String applicationName, SipAppDesc desc) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "appLoaded", new Object[] {applicationName, desc});
        }
        if (isToCreateAppModule()) {
            _applicationsContainer.appLoaded( applicationName, desc);
        }
    }

    /**
     * Update PMI about unloaded application 
     * 
     * @param applicationName
     *            name of unloaded application
     * @param appIndex 
     * 				index of the application used by PMI
     */
    public void appUnloaded(String applicationName,Integer appIndex) {
    	if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "appUnloaded , name = " + applicationName);
        }
        if (isToCreateAppModule()) {
            _applicationsContainer.appUnloaded(applicationName,appIndex);
        }
    }

    /**
     * Called when new SipApplicationSession created
     * 
     * @param applicationName
     *            name of the application that new Sip Application Session
     *            belonging to
     * @param appIndex 
     * 				index of the application used by PMI
     
     */
    public void sipAppSessionCreated(String applicationName, Integer appIndex) {    	
        synchronized (_appSessionSynchronizer) {
        	if (isPMIEnabled()) {        	
	            _applicationsContainer.sipAppSessionCreated(applicationName,appIndex);
	            _newSipAppCounter++;
	           	_commonCounter.getSessionsCounter().sipAppSessionIncrement();
        	}        
	        if (_isOverloadProtectionEnabled) {
	        	_loadCounters[PerfUtil.APP_WEIGHT_INT].increment();
			} 
        }
    }

    /**
     * Called when SipApplicationSession destroyed
     * 
     * @param applicationName
     *            name of the application that new Sip Application Session
     *            belongs to
     * @param appIndex 
     * 				index of the application used by PMI     
     */
    public void sipAppSessionDestroyed(String applicationName,Integer appIndex) {
    	synchronized (_appSessionSynchronizer) {
    		if (isPMIEnabled()) {  
	    		_applicationsContainer.sipAppSessionDestroyed(applicationName,appIndex);
	    		_commonCounter.getSessionsCounter().sipAppSessionDecrement();
	    	}    	
	    	if (_isOverloadProtectionEnabled) {
	    		_loadCounters[PerfUtil.APP_WEIGHT_INT].decrement();
	        }
    	}
    }
    
    /**
     * Implement call from the MBean
     * 
     * @param weight
     *            weight that should be set for the container
     */
    public void setServerWeight(int weight) {
		// defect 448307.1
		// We will not update server weight anymore as Proxy is not
		// reading this paramether and we passed the
		// customer performance tests without this update
		// setServerLoad(_needToSetServerWeight);
		// TODO Add ability for the client to set "overloaded"
		// or "not overloaded" from MBEAN
// 		_loadMgr.setServerWeight(weight);

    }
  
    /**
     * Return the current server weight
     * 
     * @return
     */
    public int getCurrentServerWeight() {
        return LoadManager.getInstance().getCurrentWeight();
    }
    
    /**
     * Called when new SipSession created
     * 
     * @param applicationName
     *            name of the application that Sip Session belongs to
     * @param appIndex 
     * 				index of the application used by PMI
     
     */
    public void sipSessionCreated(String applicationName,Integer appIndex) {
    	if (!isPMIEnabled()) {
        	return;
        }
        
    	synchronized (_sessionSynchronizer) {
            _applicationsContainer.sipSessionCreated(applicationName,appIndex);
            _commonCounter.getSessionsCounter().sipSessionIncrement();
        }
    }

    /**
     * Called when SipSession ended
     * 
     * @param applicationName
     *            name of the application that Sip Session belongs to
     * @param appIndex 
     * 				index of the application used by PMI
     
     */
    public void sipSessionDestroyed(String applicationName,Integer appIndex) {
    	if (!isPMIEnabled()) {
        	return;
        }
        
    	synchronized (_sessionSynchronizer) {
    		_applicationsContainer.sipSessionDestroyed(applicationName,appIndex);
    		_commonCounter.getSessionsCounter().sipSessionDecrement();
        }
    }

    /**
     * Incoming Sip request received by container
     */
    public void requestReceived() {
    	if (_isOverloadProtectionEnabled) {
			_loadCounters[PerfUtil.MPAP_WEIGHT_INT].increment();
    	}
    	
    	if (isPMIEnabled()) {
			incrementSIPMsgCounter();
		}
    }

    /**
     * Incoming Sip Response received by container
     */
    public void responseReceived() {
    	if (_isOverloadProtectionEnabled) {
			_loadCounters[PerfUtil.MPAP_WEIGHT_INT].increment();
    	}
    	
    	if (isPMIEnabled()) {
			incrementSIPMsgCounter();
		}
    }
    
    /**
     * Called when new request received by application
     * 
     * @param applicationName
     *            name of the application that Sip Session belongs to
     * @param appIndex 
     * 				index of the application used by PMI
     * @param method 
     * 				the method name of the request     
     */
    public void inRequest(String applicationName,Integer appIndex, String method) {
    	if (isPMIEnabled()) {
            _applicationsContainer.inRequest(applicationName,appIndex, method);
        }
    }
    
    /**
     * Called when new request sent by application
     * 
     * @param applicationName
     *            name of the application that Sip Session belongs to
     * @param appIndex 
     * 				index of the application used by PMI
     * @param method 
     * 				the method name of the request
     
     */
    public void outRequest(String applicationName,Integer appIndex, String method) {
    	if (isPMIEnabled()) {
            _applicationsContainer.outRequest(applicationName,appIndex, method);
        }
    }
    
    /**
     * Called when new response received by application
     * 
     * @param applicationName
     *            name of the application that Sip Session belongs to
     * @param appIndex 
     * 				index of the application used by PMI
     * @param status 
     * 				the status number of the response
     
     */
    public void inResponse(String applicationName,Integer appIndex, int status) {
    	if (isPMIEnabled()) {
           _applicationsContainer.inResponse(applicationName,appIndex, status);
        }
    }
    
    /**
     * Called when new response sent by application
     * 
     * @param applicationName
     *            name of the application that Sip Session belongs to
     * @param appIndex 
     * 				index of the application used by PMI
     * @param status 
     * 				the status number of the response
     
     */
    public void outResponse(String applicationName,Integer appIndex, int status) {
    	if (isPMIEnabled()) {
           _applicationsContainer.outResponse(applicationName,appIndex, status);
        }
    }

    /**
     * new incoming message received by container
     */
    private void incrementSIPMsgCounter() {
        _receivedMsgCounter++;
    }
    
    /**
     * Update Rejected SIP Messages
     */
    public void updateRejectedMessagesCounter() {
    	synchronized (_rejectedMessagesSynchronizer) {
    		_rejectedMessagesCounter++;
    		if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "updateRejectedMessagesCounter, rejected messages counter = " 
                		+ _rejectedMessagesCounter);
            }
    	}
    }
    
    /**
     * Update SIP timers invocations counter
     */
    public void updateSipTimersInvocationsCounter() {
    	synchronized(_sipTimersInvocationsSynchronizer) {
    		_sipTimersInvocations++;
    	}
    	if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceEntry(this, "updateSipTimersInvocationsCounter, sip timers invocations counter = " 
            		+ _sipTimersInvocations);
        }
    }
    
    /**
     * Routed Task finished - take time measurements 
     */
    public void measureTaskDurationProcessingQueue(long ms) {
    	if(_pmiModule != null) {
    		_pmiModule.updateTaskDurationProcessingQueueStatistics(ms);
    	}
    }
    
    /**
     * Stack Task finished - take time measurements 
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#measureTaskDurationOutboundQueue(long)
     */
    public void measureTaskDurationOutboundQueue(long ms) {
    	if(_pmiModule != null) {
    		_pmiModule.updateTaskDurationOutboundQueueStatistics(ms);
    	}
    }
    
    /**
     * Application code finished - take time measurements
     */
    public void measureInApplicationTaskDuration(String applicationName,Integer appIndex, long ms) {
    	if (isPMIEnabled()) {
            _applicationsContainer.updateApplicationTaskDurationStatistics(applicationName, appIndex, ms);
         }
    }
    
    public boolean isApplicationDurationPMIEnabled() {
    	if (isPMIEnabled()) {
            return _isApplicationDurationPMIEnabled;
         }
    	return false;
    }
    
    public static void setIsApplicationDurationPMIEnabled(boolean isEnabled) {
    	_isApplicationDurationPMIEnabled = isEnabled;
    }
    
    /**
     * Outgoing Sip Response sent from Container
     * 
     * @param arrivedTime -
     *            time when the related Request was arrived arrivedTime = -1 if
     *            the arrive time wasn't set
     */
    public void responseSent(long arrivedTime) {    	
    	long responseTime = System.currentTimeMillis() - arrivedTime;					
		if (isPMIEnabled()) {
			// set the processing time of the message
			if (arrivedTime != -1) {
				_summaryResponseTime += (responseTime);
				_responcesCounter++;
			}
		}
		
		if (_isOverloadProtectionEnabled) {
			// Save to ResponseTimeCounter Only if this counter is enabled
			_loadCounters[PerfUtil.RESPONSE_WEIGHT_INT].setCounter(responseTime);
		}
    }
    
    /**
	 * Method used by counters that should affect immediately of the server
	 * weight and thay will update the PMI each time when they weight changed.
	 * 
	 * @param index
	 * @param newWeight
	 */
    public synchronized void setNewWeight(Weighable counter,long currentLoad){
    	int newWeight = counter.getWeight();
    	if(newWeight < _lastWeight){
    	// This new weight is worser than previous one.
    		_lastWeight = newWeight;
    		_lastWeightIndex = counter.getCounterID();
    		callToSetNewWeight(currentLoad);
    	}
    	else if(newWeight > _lastWeight){
			// meaning that this new Weight is better the previous one
    		if(_lastWeightIndex == counter.getCounterID()){
    			// Only when this new weight comes from the index which set
    			// the prevoiuse one - meaning that weight should be
    			// recalculated and If changed - update LoadMgr about it.
    			if(calculateNewWeight()){
    				callToSetNewWeight(currentLoad);
    			}
    		}
		}
    	
    }

    /**
     * This method executes every _loadUpdateTempo period.
     * It is responsible to get new weight and to call LoadManager 
     * to update server weight if needed.
     * synchronized - to prevent calculateNewWeight during the updateServerWeight
     * and vice versa.
     */
    public synchronized void updateServerWeight() {
    	
    	// if not quiesce do the work !
    	
    	for (int i = 0; i < _loadCounters.length; i++) {
    		_loadCounters[i].calculateWeight();
		}
    	if (calculateNewWeight()) {
			// Call to LoadMgr only if weight changed.
    		callToSetNewWeight(_loadCounters[_lastWeightIndex].getLoadUsedForLastWeightCalc());
		}
	}
    
    /**
     * Helper method which calls to LoadMgr and set the new weight
     *
     */
    private void callToSetNewWeight(long currentLoad){
    	// Set new Weight
		boolean changed = LoadManager.getInstance().updateNewWeight(	_loadCounters[_lastWeightIndex],
															_lastWeight,
															currentLoad);
		if (changed && c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < _loadCounters.length; i++) {
				Weighable counter = (Weighable) _loadCounters[i];
				buff.append("\n\r CounterInfo = ");
				buff.append(counter.getCurrentState());
			}
			c_logger.traceDebug(this, "callToSetNewWeight", buff.toString());
		}
	}
    
    /**
	 * Helper method which is calculates the new weight according to the each
	 * weight of each counter.
	 * 
	 */
    private synchronized boolean calculateNewWeight(){
    	
    	int index = 0;
    	int weight = _loadCounters[index].getWeight();
    	boolean weightChanged = false;
    	
    	// look for new worst weight (lower one)
    	// and index of the counter that changed it.
    	for (int i = 1; i < _loadCounters.length; i++) {
			if( weight > _loadCounters[i].getWeight()){
				weight = _loadCounters[i].getWeight();
				index = i;
			}
		}
    	
    	if(index != _lastWeightIndex){
    		// If the index changed it doesn't mean that weight changed.
    		_lastWeightIndex = index;
    	}
    	if(weight != _lastWeight){
    		_lastWeight = weight;
    		weightChanged = true;
    	}
    	
    	if (weightChanged) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("New weight is = ");
				buff.append(_lastWeight);
				buff.append(" changed by counter ");
				buff.append(PerfUtil.getOverloadedMsgByType(index));
				c_logger
						.traceDebug(this, "calculateNewWeight", buff.toString());
			}
		}
    	
    	return weightChanged;
    }
    
    /**
     * This method will called periodically and it is responsible to update
     * statistic timers
     */
    private void updateStatistic() {
    	if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceEntry(this, "updateStatistic");
        }
    	
    	if (isPMIEnabled()) {
    		//update PMI about all the sipSessions and
    		// sipApplicationSessions that
    		// exist for now
    		_pmiModule.updateAppSessionNum(_commonCounter.getSipAppSessions());
    		_pmiModule.updateSipSessionNum(_commonCounter.getSipSessions());
    		_pmiModule.updateInvokeCounter(_commonCounter.getInvokeQueueSize());
    		_pmiModule.updateRejectedMessagesCounter(_rejectedMessagesCounter);
    		_pmiModule.updateSipTimersInvocationsCounter(_sipTimersInvocations);
    		_applicationsContainer.updatePmi();

    		// avarage of request processing during the _pmiUpdateTempo
    		long requestProcessing_avg = 0;
    		if (_responcesCounter > 0) {
    			requestProcessing_avg = _summaryResponseTime / _responcesCounter;
    		}

    		_pmiModule.updatedProcessingRequest(requestProcessing_avg);

    		//	average of the received SIP messages per _pmiUpdateTempo
    		long receivedMsg_avg = calculateAvarageOfTheCounter(_receivedMsgCounter);

    		_pmiModule.updateReceivedMsgs(receivedMsg_avg);

    		// average of new SIP Application sessions created per
    		// _pmiUpdateTempo
    		long newSipApp_avg = calculateAvarageOfTheCounter(_newSipAppCounter);
    		_pmiModule.updateNewSipAppCreated(newSipApp_avg);
    		//Update task duration in outbound queue statistics
    		_pmiModule.updateTaskDurationPMICounters();
    		_pmiModule.updateQueueMonitoringPMICounters();
    	}

    	nullifyInternalCounters();
    }
    
    /**
     * Nullify all the performance manager counters.
     */
    private void nullifyInternalCounters() {
    	_summaryResponseTime = 0;
    	_responcesCounter = 0;
    	_receivedMsgCounter = 0;
    	_newSipAppCounter = 0;
    	synchronized(_rejectedMessagesSynchronizer) {
    		_rejectedMessagesCounter = 0;
    	}
    	synchronized(_sipTimersInvocationsSynchronizer) {
    		_sipTimersInvocations = 0;
    	}
    }
    
    /**
     * Helper method that calculates the avarage of the counter
     * e.g :
     * Received Messages = 1000;
     * _pmiUpdateTempo = 5 sec (update PMI every 5 sec)
     * _statAveragePeriod = 3 sec (update PMI about number of messages received within 3 sec)
     * num of the messages in 3 sec = (1000/5) * 3
     * 
     * The _statAveragePeriod is always less or equal to the _pmiUpdateTempo
     * @param counter
     * @return
     */
    private long calculateAvarageOfTheCounter(long counter) {
    	// multiply before divide in case counter/_pmiUpdateTempo < 1
    	long average = counter * _statAveragePeriod / _statUpdateRange;
    	return average;
    }

    /**
     * new invoke request was added to the invokeQueue
     */
    public void incrementInvokeCounter() {
        if (isPMIEnabled()) {
            _commonCounter.invokeQueueIncrement();
        }
    }

    /**
     * Invoke request was removed from the invokeQueue
     */
    public void decrementInvokeCounter() {
        if (isPMIEnabled()) {
            _commonCounter.invokeQueueDecrement();
        }
    }

    /**
     * Setting the new max queue size. Invoked to set maximum queue size
     * when it is changed.
     * @param size
     */
    public void setQueueSize(long size){
    	if (_isOverloadProtectionEnabled) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setQueueSize", 
						"new Size is = " + size);
			}
			_loadCounters[PerfUtil.MSG_QUEUE_SIZE_INT].setCounter(size);
		}
    }
    
    /**
     * @return Returns the overload.
     */
    public boolean isOverload() {
        if (!_isOverloadProtectionEnabled) {
            return false;
        }

        if (LoadManager.getInstance().getLastWeight() == 0) {
            return true;
        }
        return false;
    }

    /**
     * @author Anat Fradin , Nov 3, 2004 Implement the TimerTask. Timer will
	 *         notify it periodically. When the timer is fired the
	 *         PerformanceMgr will update statistics in the PMI according to the
	 *         update time.
	 */
	class TimerPMIListener  extends TimerTask { //implements Runnable {

		@Override
		public void run() {
			updateStatistic();
		}
	}

    /**
	 * @author Anat Fradin , Nov 3, 2004 Implement the TimerTask. Timer will
	 *         notify it periodically. When the timer is fired the
	 *         PerformanceMgr will collect all weight from each counter and
	 *         update the Server Weight.
	 */
    class TimeWeightListener implements Runnable {

		@Override
		public void run() {
			updateServerWeight();
		}
	}

    /**
     * Stops all timers used by the PerfomanceMgr.
     */
    public void stopTimers() {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "stopTimers", "Stop timers");
		}
    	//Canceling the timer for update the statistic periodically
    	cancelTimer(_pmiTimer, TimerPMIListener.class.getName()); 
    	
    	//Canceling the timer for server weight update
    	cancelTimer(_weightTimer, TimeWeightListener.class.getName());
    	
        if(isPMIEnabled() && _timerService != null) {
	    	//stop cached timer service for task duration monitors
	    	_timerService.destroy();
	    	_timerService = null;
        }
    }
    
    /**
     * Cancels a specific timer. 
     * 
     * @param timerToCancel the timer to cancel
     * @param timerClass    the name of the class that represents the timer (for trace)
     * 
     */
    private void cancelTimer(ScheduledFuture<?> timerToCancel, String timerClass) {
    	if (timerToCancel != null) {
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancelTimer", "Canceling " + timerClass);
			}
			timerToCancel.cancel(true);
			timerToCancel= null;				
		}
    }
    
    /**
     * Returns the averaging period
     * @return
     */
    public int getAveragingPeriod(){
    	return _statAveragePeriod;
    }
    	
    /**
     * Helper method that decide is AppModule should be created
     * @return
     */
    private boolean isToCreateAppModule(){
    	boolean isAppModulesShouldBeCreated = false;
    	
    	if(isPMIEnabled()){
    		isAppModulesShouldBeCreated = true;
    	}
    	return isAppModulesShouldBeCreated;
    }

    /**
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#getCurrentCachedTime()
     */
	public long getCurrentCachedTime() {
		return _timerService.getCurrentTime();
	}

	/**
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#isPMIEnabled()
     */
	public boolean isPMIEnabled() {
		return (_pmiModule != null);
	}
	
	/**
     * Is processing queue monitoring enabled
     */
    public boolean isTaskDurationProcessingQueuePMIEnabled() {
    	if(_pmiModule != null) {
    		return _pmiModule.isProcessingQueuePMIEnabled();
    	}
    	else return false;
    }

	/**
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#isTaskDurationOutboundQueuePMIEnabled()
     */
	public boolean isTaskDurationOutboundQueuePMIEnabled() {		
		if(_pmiModule != null) {
			return _pmiModule.isTaskDurationOutboundQueuePMIEnabled();
		}
		else return false;
	}
	
	/**
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#isQueueMonitoringOutboundQueuePMIEnabled()
     */
	public boolean isQueueMonitoringOutboundQueuePMIEnabled() {		
		if(_pmiModule != null) {
			return _pmiModule.isQueueMonitoringOutboundQueuePMIEnabled();
		}
		else return false;
	}
	
	public boolean isQueueMonitoringProcessingQueuePMIEnabled() {
		if(_pmiModule != null) {
			return _pmiModule.isQueueMonitoringProcessingQueuePMIEnabled();
		}
		else return false;
	}
	
	/**
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#updateQueueMonitoringTaskQueuedInOutboundQueue()
     */
	public void updateQueueMonitoringTaskQueuedInOutboundQueue() {
		if(_pmiModule != null) {
			_pmiModule.updateQueueMonitoringTaskQueuedInOutboundQueue();
		}
	}
	
	/**
     * @see com.ibm.ws.sip.stack.util.StackExternalizedPerformanceMgr#updateQueueMonitoringTaskDequeuedFromOutboundQueue()
     */
	public void updateQueueMonitoringTaskDequeuedFromOutboundQueue() {
		if(_pmiModule != null) {
			_pmiModule.updateQueueMonitoringTaskDequeuedFromOutboundQueue();
		}
	}
	
	public void updateQueueMonitoringTaskQueuedInProcessingQueue()	{
		if(_pmiModule != null) {
			_pmiModule.updateQueueMonitoringTaskQueuedInProcessingQueue();
		}
	}
	
	public void updateQueueMonitoringTaskDequeuedFromProcessingQueue()	{
		if(_pmiModule != null) {
			_pmiModule.updateQueueMonitoringTaskDequeuedFromProcessingQueue();
		}
	}
	
	/**
     * Increment replicated Sip Sessions counter
     */
    public void incrementReplicatedSipSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.incrementReplicatedSipSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "incrementReplicatedSipSessionsCounter, replicated Sip Sessions num = " 
                		+ _commonCounter.getReplicatedSipSessionsCounter());
            }
    	}
    }
    
    /**
     * Decrement replicated Sip Sessions counter
     */
    public void decrementReplicatedSipSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.decrementReplicatedSipSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "decrementReplicatedSipSessionsCounter, replicated Sip Sessions num = " 
                		+ _commonCounter.getReplicatedSipSessionsCounter());
            }
    	}
    }
    
    /**
     * Increment not replicated Sip Sessions counter
     */
    public void incrementNotReplicatedSipSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.incrementNotReplicatedSipSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "incrementNotReplicatedSipSessionsCounter, not replicated Sip Sessions num = " 
                		+ _commonCounter.getNotReplicatedSipSessionsCounter());
            }
    	}
    }
    
    /**
     * Increment replicated Sip App Sessions counter
     */
    public void incrementReplicatedSipAppSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.incrementReplicatedSipAppSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "incrementReplicatedSipAppSessionsCounter, replicated Sip App Sessions num = " 
                		+ _commonCounter.getReplicatedSipAppSessionsCounter());
            }
    	}
    }
    
    /**
     * Decrement replicated Sip App Sessions counter
     */
    public void decrementReplicatedSipAppSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.decrementReplicatedSipAppSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "decrementReplicatedSipAppSessionsCounter, replicated Sip App Sessions num = " 
                		+ _commonCounter.getReplicatedSipAppSessionsCounter());
            }
    	}
    }
    
    /**
     * Increment not replicated Sip App Sessions counter
     */
    public void incrementNotReplicatedSipAppSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.incrementNotReplicatedSipAppSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "incrementNotReplicatedSipAppSessionsCounter, not replicated Sip App Sessions num = " 
                		+ _commonCounter.getNotReplicatedSipAppSessionsCounter());
            }
    	}
    }
    
    /**
     * Decrement not replicated Sip App Sessions counter
     */
    public void decrementNotReplicatedSipAppSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.decrementNotReplicatedSipAppSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "decrementNotReplicatedSipAppSessionsCounter, not replicated Sip App Sessions num = " 
                		+ _commonCounter.getNotReplicatedSipAppSessionsCounter());
            }
    	}
    }
    
    /**
     * Decrement not replicated Sip Sessions counter
     */
    public void decrementNotReplicatedSipSessionsCounter() {
    	if (isPMIEnabled()) {
            _commonCounter.decrementNotReplicatedSipSessionsCounter();
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceEntry(this, "decrementNotReplicatedSipSessionsCounter, not replicated Sip Sessions num = " 
                		+ _commonCounter.getNotReplicatedSipSessionsCounter());
            }
    	}
    }    
 
	
    /**
	 * Update Performance manager about new In Request
	 * @param method The method name from request
	 * @param tuWrapper The transaction user of the request
	 * @param sipDesc The sip servlet description 
	 */
	public void updatePmiInRequest(String method, TransactionUserWrapper tuWrapper, SipServletDesc sipDesc ) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { tuWrapper.getAppName(), tuWrapper.getId(), method};
			c_logger.traceEntry(tuWrapper.getTuImpl(), "updatePmiInRequest", params);
		}
		SipAppDesc appDesc;
		if (sipDesc != null){
			appDesc = sipDesc.getSipApp();
		}
		else {
			appDesc = tuWrapper.getSipServletDesc().getSipApp();
		}
		if (null != appDesc) {
			inRequest(appDesc.getApplicationName(), appDesc
					.getAppIndexForPmi(),method);
		} 
		else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(tuWrapper.getTuImpl(), "updatePmiInRequest",
						"Unable to update PerfManager "
						+ "SIP app descriptor not available");
			}
		}
	}

	/**
	 * Update Performance manager about new Out Request
	 * @param method The method name from request
	 * @param tuWrapper The transaction user of the request
	 * @param sipDesc The sip servlet description 
	 * 
	 */
	public void updatePmiOutRequest(String method, TransactionUserWrapper tuWrapper) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { tuWrapper.getAppName(), tuWrapper.getId()};
			c_logger.traceEntry(tuWrapper.getTuImpl(), "updatePmiOutRequest", params);
		}
		SipServletDesc servletDesc = tuWrapper.getSipServletDesc();
		SipAppDesc appDesc = null;
		if(servletDesc != null){
			appDesc = tuWrapper.getSipServletDesc().getSipApp();
		}
		if (null != appDesc) {
			outRequest(appDesc.getApplicationName(), appDesc
					.getAppIndexForPmi(),method);
		} 
		else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(tuWrapper.getTuImpl(), "updatePmiOutRequest",
						"Unable to update PerfManager "
						+ "SIP app descriptor not available");
			}
		}
	}

	/**
	 * Update Performance manager about new In Response
	 * @param method The method name from response
	 * @param tuWrapper The transaction user of the response
	 * 
	 */
	public void updatePmiInResponse(int status, TransactionUserWrapper tuWrapper) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { tuWrapper.getAppName(), tuWrapper.getId(), status};
			c_logger.traceEntry(tuWrapper.getTuImpl(), "updatePmiInResponse", params);
		}
		SipAppDesc appDesc = tuWrapper.getSipServletDesc().getSipApp();
		if (null != appDesc) {
			inResponse(appDesc.getApplicationName(), appDesc
					.getAppIndexForPmi(), status);
		} 
		else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(tuWrapper.getTuImpl(), "updatePmiInResponse",
						"Unable to update PerfManager "
						+ "SIP app descriptor not available");
			}
		}
	}

	/**
	 * Update Performance manager about new Out Response
	 * @param method The method name from response
	 * @param tuWrapper The transaction user of the response
	 * 
	 */
	public void updatePmiOutResponse(int status, TransactionUserWrapper tuWrapper) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = { tuWrapper.getAppName(), tuWrapper.getId()};
			c_logger.traceEntry(tuWrapper.getTuImpl(), "updatePmiOutResponse", params);
		}
		SipAppDesc appDesc = null;

		// When the response is in stateless routing scenario
		// the servlet is not saved in TransactionUserWrapper
		if (tuWrapper != null && tuWrapper.getSipServletDesc() != null)
			appDesc = tuWrapper.getSipServletDesc().getSipApp();

		if (null != appDesc) {
			outResponse(appDesc.getApplicationName(), appDesc
					.getAppIndexForPmi(), status);
		} 
		else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(tuWrapper.getTuImpl(), "updatePmiOutResponse",
						"Unable to update PerfManager "
						+ "SIP app descriptor not available");
			}
		}
	}
	
	/**
	 * Injection for PMI listener DS
	 * @param pmiModule
	 */
	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setSipContainerPMIListener(SipContainerPMIListener pmiModule) {
		if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipContainerPMIListener set listener", this._pmiModule);
		
		// check the monitor configuration
        // Following line with traditional PMI support
        if(pmiModule.isTraditionalPMIEnabled()){
        	c_logger.warn("warning.traditional.enabled", null);
        }
		else {
			
			// Synchronize on the PMI creation such that it's going to be created only once - 
			// here or in init(SipPropertiesMap)
			synchronized(_pmiTimerCreationSynchronizer) {
				this._pmiModule = pmiModule;
				if (_perfMgrInitialized && !_pmiTimerCreated) {
					// The timer is created in case the service is injected after init() is called.
					createPMITimer();
				}
			}
		}
	}
	
	protected synchronized void unsetSipContainerPMIListener(SipContainerPMIListener pmiModule){
		if (c_logger.isTraceEntryExitEnabled())
            c_logger.traceEntry(this, "SipContainerPMIListener unset listener");
    
		this._pmiModule = null;
		//Canceling the timer for update the statistic periodically
    	cancelTimer(_pmiTimer, TimerPMIListener.class.getName()); 
    	
    }

	/**
	 * Injection for Applications PMI listener DS
	 * @param pmiModule
	 */
	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	protected void setApplicationsPMIListener(ApplicationsPMIListener appsModule) {	
		this._appsPMIModule = appsModule;
		if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("ApplicationsPMIListener set listener", this._appsPMIModule);
	}
	
	protected synchronized void unsetApplicationsPMIListener(ApplicationsPMIListener appModule){
		this._appsPMIModule = null;
		if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("ApplicationsPMIListener unset listener");
    }
    
	
	public ApplicationsPMIListener getApplicationsPMIListener() {
		return _appsPMIModule;
	}
}
