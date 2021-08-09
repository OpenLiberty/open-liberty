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
package com.ibm.ws.sip.container.timer;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.properties.SipPropertiesMap;

/**
 * @author Nitzan Nissim
 *
 * Implementation of a timer service for the different container timers.
 * This implementation is using a declarative service ScheduledExecutorService for scheduled tasks execution. 
 */
@Component(
		service = BaseTimerService.class, 
		immediate = false,
		configurationPolicy = ConfigurationPolicy.OPTIONAL,
		property = "service.vendor=IBM" 
	)
public class TimerServiceImpl implements BaseTimerService
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(TimerServiceImpl.class);

    /**
     * An injected ScheduledExecutorService declarative service
     */
    private static ScheduledExecutorService s_timerService; 

    /**
     * Used for debug environment - when false - no timers will be scheduled
     */
    private boolean _enableTimers = CoreProperties.ENABLE_TIMERS_DEFAULT;
    
    /**
     * DS method to activate this component.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
	@Activate
    protected void activate(Map<String, Object> properties) {
		SipPropertiesMap props = PropertiesStore.getInstance().getProperties();
	    props.updateProperties(properties);
	    _enableTimers = props.getBoolean(CoreProperties.ENABLE_TIMERS);
 		if (!_enableTimers) {
 			if(c_logger.isErrorEnabled()){
 				c_logger.error("warning.timer.unavailable", 
 						Situation.SITUATION_AVAILABLE, null);
 			}
 		}
	}
	
	/**
     * DS method to deactivate this component.
     * 
     * @param reason int representation of reason the component is stopping
     */
	@Deactivate
    protected void deactivate(int reason) {
	}
	
	 /**
     * DS method to activate this component.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
	@Modified
    protected void modified(Map<String, Object> properties) {
		SipPropertiesMap props = PropertiesStore.getInstance().getProperties();
	    props.updateProperties(properties);
	    _enableTimers = props.getBoolean(CoreProperties.ENABLE_TIMERS);
    }

    /**
     * Schedule timer for invocation.
     * @see javax.servlet.sip.TimerService#schedule(javax.servlet.sip.Timer, 
     * 												boolean, long, long, boolean)
     */
    public void schedule(
        BaseTimer timer,
        boolean isPersistent,
        long delay,
        long period,
        boolean fixedDelay)
    {
        if(_enableTimers == false){
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "schedule", "timer service is disabled");
            }
            return;
        }
        
        timer.initTimer(isPersistent, delay, period, fixedDelay);
        SipTimerTask task = timer.getTimerTask();
        
        if(fixedDelay){
        	task.setScheduledFuture( s_timerService.scheduleWithFixedDelay(task, delay, period, TimeUnit.MILLISECONDS));
        }
        else{
        	task.setScheduledFuture( s_timerService.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Schedule a timer for invocation. 
     * @see javax.servlet.sip.TimerService#schedule(javax.servlet.sip.Timer, boolean, long)
     */
    public void schedule(BaseTimer timer, boolean isPersistent, long delay)
    {
        if(_enableTimers == false){
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "schedule", "timer service is disabled");
            }
            return;
        }
        timer.initTimer(isPersistent, delay);
        SipTimerTask task = timer.getTimerTask();
        task.setScheduledFuture( s_timerService.schedule(timer.getTimerTask(),delay, TimeUnit.MILLISECONDS));
    }
    
    /**
     * Injecting the ScheduledExecutorService DS
     * @param timerService
     */
    @Reference
    public void setTimerService(ScheduledExecutorService timerService) {
		s_timerService = timerService;
	}
    
    /**
     * Gets the timer service.
     * @return the timer service
     */
    public static ScheduledExecutorService getTimerSerivce() {
    	return s_timerService;
    }
}
