/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

import java.time.Duration;
import java.util.HashSet;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.microprofile.metrics23.impl.SimpleTimerImpl;

public class MonitorSimpleTimer extends SimpleTimerImpl {
	
	private static final TraceComponent tc = Tr.register(MonitorSimpleTimer.class);
	
	MBeanServer mbs;
    String objectName, counterAttribute, counterSubAttribute, gaugeAttribute, gaugeSubAttribute;
    long time;
    boolean isComposite = false;
    
    HashSet<Long> usedTimes = new HashSet<Long>();

    private long cachedMaxOldMinute = 0L;
    private long cachedMinOldMinute = 0L;
    private long cachedMaxCurrentMinute = 0L;
    private long cachedMinCurrentMinute = 0L;
    
    private long displayMaxCurrent_thisMinute = 0L;
    private long displayMinCurrent_thisMinute = 0L;
    private long displayMaxCurrent_thisMinute_val = 0L;
    private long displayMinCurrent_thisMinute_val = 0L;
    
    
    private long displayMaxOld_thisMinute = 0L;
    private long displayMinOld_thisMinute = 0L;
    
    private long minCurrent;
    private long maxCurrent;
    
    private long minuteCurrent;

    private long rollingBaseMinute = 0L;
    
    private long minOld;
    private long maxOld;
    private long minuteOld;
    
    public MonitorSimpleTimer(MBeanServer mbs, String objectName, String counterAttribute,
    		String counterSubAttribute, String gaugeAttribute, String gaugeSubAttribute) {
    	this.mbs = mbs;
        this.objectName = objectName;
        this.counterAttribute = counterAttribute;
        this.counterSubAttribute = counterSubAttribute;
        this.gaugeAttribute = gaugeAttribute;
        this.gaugeSubAttribute = gaugeSubAttribute;
       
    }

    @Override
    public long getCount() {
        try {
        	if (counterSubAttribute != null) {
                CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), counterAttribute);
                Number numValue = (Number) value.get(counterSubAttribute);       
                return numValue.longValue();
        	} else {
                Number value = (Number) mbs.getAttribute(new ObjectName(objectName), counterAttribute);
                return value.longValue();        		
        	}
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getCount exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "getCount:Exception");
            }
        }
        return 0;
    }
    
    @Override
    public Duration getElapsedTime() {
        try {   
        	if (gaugeSubAttribute != null) {
                CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), gaugeAttribute);
                Number numValue = (Number) value.get(gaugeSubAttribute);
                return Duration.ofNanos(numValue.longValue());
        	} else {
        		 Number numValue = (Number) mbs.getAttribute(new ObjectName(objectName), gaugeAttribute);
                 return Duration.ofNanos(numValue.longValue());
        	}

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getElapsedTime exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "getElapsedTime:Exception");
            }
        }
        return Duration.ZERO;
    }
    

    @Override
    public synchronized Duration getMaxTimeDuration() {
    	
    	//Below logic to be introduced in separate PR
    	
//    	getMinMaxValues();
//    	long currentMinute = getCurrentMinuteFromSystem();
//    	
//    	//If there exists no 'Previous' data AND the current minute DOES NOT match the latest minute AND if haven't already used this value
//    	if ((cachedMaxOldMinute != minuteOld || displayMaxOld_thisMinute == currentMinute) //NOT stale unless still displaying
//    			&& currentMinute != minuteOld
//    			&& minuteOld != 0
//    			&& (minuteOld > rollingBaseMinute || (currentMinute == displayMaxCurrent_thisMinute && maxOld == displayMaxCurrent_thisMinute_val))) //Because we may have used a "current" value before... and that value may now be an "old" value.
//    	{
//    		cachedMaxOldMinute = minuteOld;
//    		displayMaxOld_thisMinute = currentMinute;
//    		return Duration.ofNanos(maxOld);
//    	}
//    	
//    	   	    	
//    	if ((cachedMaxCurrentMinute != minuteCurrent || displayMaxCurrent_thisMinute == currentMinute) //NOT stale unless still displaying
//    			&& currentMinute != minuteCurrent)
//    	{
//    		cachedMaxCurrentMinute = minuteCurrent;
//    		displayMaxCurrent_thisMinute = currentMinute;
//    		displayMaxCurrent_thisMinute_val = maxCurrent;
//    		rollingBaseMinute = currentMinute;
//    		return Duration.ofNanos(maxCurrent);
//    	}
    	
		return null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Duration getMinTimeDuration() {
    	
    	//Below logic to be introduced in separate PR
    	
    	
//    	getMinMaxValues();
//    	long currentMinute = getCurrentMinuteFromSystem();
//    	
//    	
//    	/*
//    	 * For every invocation of this block:
//    	 * cachedMinuteOld is set to the current minute
//    	 * displayMinOld_thisMinute is set to the current minute - indicates that for this current minute, the OLD value from the mbean is too be displayed
//    	 * 
//    	 * If cahed value and old minute value is not the same (i.e stale) AND we're not still displaying
//    	 * AND currentMinute doesn't equal the recorded OLD minute
//    	 * AND there is a vlue (minuteOld != 0)
//    	 * AND the previous minute is greater than our rolling window UNLESS we were displaying a CURRENT/NEW value when it was bumped into the PREVIOUS mbean value) 
//    	 */
//    	
//    	if ((cachedMinOldMinute != minuteOld || displayMinOld_thisMinute == currentMinute) //NOT stale unless still displaying
//    			&& currentMinute != minuteOld
//    			&& minuteOld != 0
//    			&& ( minuteOld > rollingBaseMinute || (currentMinute == displayMinCurrent_thisMinute && displayMinCurrent_thisMinute_val == minOld))) //Because we may have used a "current" value before... and that value may now be an "old" value.
//    	{
//    		cachedMinOldMinute = minuteOld;
//    		displayMinOld_thisMinute = currentMinute;
//    		return Duration.ofNanos(minOld);
//    	}
//    	
//    	   	    	
//    	if ((cachedMinCurrentMinute != minuteCurrent || displayMinCurrent_thisMinute == currentMinute) //NOT stale unless still displaying
//    			&& currentMinute != minuteCurrent)
//    	{
//    		cachedMinCurrentMinute = minuteCurrent;
//    		displayMinCurrent_thisMinute = currentMinute;
//    		displayMinCurrent_thisMinute_val = minCurrent;
//    		rollingBaseMinute = currentMinute;
//    		return Duration.ofNanos(minCurrent);
//    	}
    	
		return null;
    }

	private void getMinMaxValues() {
		try {
			synchronized(this) {
				
				
				CompositeData value= (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinuteLatestMinimumDuration");
				minCurrent = ((Number) value.get("currentValue")).longValue();
				 
				value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), "MinuteLatestMaximumDuration");
				 maxCurrent = ((Number) value.get("currentValue")).longValue();
				 
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinuteLatest");
				 minuteCurrent = ((Number) value.get("currentValue")).longValue();
				 
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinutePreviousMinimumDuration");
				 minOld = ((Number) value.get("currentValue")).longValue();
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinutePreviousMaximumDuration");
				 maxOld = ((Number) value.get("currentValue")).longValue();
				 
				 value = (CompositeData) mbs.getAttribute(new ObjectName(objectName),"MinutePrevious");
				 minuteOld  = ((Number) value.get("currentValue")).longValue();
			}

		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "getMinMaxValues exception message: ", e.getMessage());
				FFDCFilter.processException(e, getClass().getSimpleName(), "getMinMaxValues:Exception");
			}
		}
	}
    
    // Get the current system time in minutes, truncating. This number will increase by 1 every complete minute.
    private long getCurrentMinuteFromSystem() {
        return System.currentTimeMillis() / 60000;
    }
}
