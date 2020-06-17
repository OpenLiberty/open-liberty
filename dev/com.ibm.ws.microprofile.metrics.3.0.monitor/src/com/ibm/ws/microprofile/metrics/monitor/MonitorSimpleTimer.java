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
import com.ibm.ws.microprofile.metrics30.impl.SimpleTimer30Impl;

public class MonitorSimpleTimer extends SimpleTimer30Impl {
	
	private static final TraceComponent tc = Tr.register(MonitorSimpleTimer.class);
	
	MBeanServer mbs;
    String objectName, counterAttribute, counterSubAttribute, gaugeAttribute, gaugeSubAttribute;
    long time;
    boolean isComposite = false;
    
    HashSet<Long> usedTimes = new HashSet<Long>();

    private long cachedMBPreviousMinute_max = 0L;
    private long cachedMBPreviousMinute_min = 0L;
    private long cachedMBLatestMinute_max = 0L;
    private long cachedMBLatestMinute_min = 0L;
    
    private long displayMaxLatest_forThisMinute = 0L;
    private long displayMinLatest_forThisMinute = 0L;
    private long displayMaxLatest_forThisMinute_val = 0L;
    private long displayMinLatest_forThisMinute_val = 0L;
    
    
    private long displayMaxPrev_forThisMinute = 0L;
    private long displayMinPrev_forThisMinute = 0L;
    
    private long mbean_latest_min;
    private long mbean_latest_max;
    private long mbean_latestMinute;
    
    private long mbean_previous_min;
    private long mbean_previous_max;
    private long mbean_prevMinute;
    
    private long rollingBaseMinute = 0L;
    
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

		getMinMaxValues();
		long currentMinute = getCurrentMinuteFromSystem();

		/*
		 * 
		 * First check to ensure we're not displaying the mbean's previous values again
		 * UNLESS of course we're still within the current/on-going complete minute (i.e
		 * The value will be displayed for a full minute 12:00:00 to 12:00:59).
		 * 
		 * Second check just to ensure the mbean's previous minute is this ongoing
		 * minute. If it is the same, then that means the mbean has been created/updated
		 * this same minute. Does not make sense to display the mbean's previous value
		 * then. (i.e mbean was created this minute and we're querying this minute)
		 * 
		 * Third check to ensure mbean actually has a value (mbean's values are
		 * initialized to 0)
		 * 
		 * Fourth check is to ensure mbean's previous value is above the rolling window
		 * (this is to ensure we are not re-displaying a value .. see below for more
		 * details). And also check to see if the mbean's latest values were bumped to
		 * the mbean's previous values while we're still expected to display it. This an
		 * happen if we were displaying a latest value due to lack of querying and then
		 * the mbean was updated)
		 * 
		 */
		if ((cachedMBPreviousMinute_max != mbean_prevMinute || displayMaxPrev_forThisMinute == currentMinute) // first
																												// check
				&& currentMinute != mbean_prevMinute // second check
				&& mbean_prevMinute != 0 // third check
				&& (mbean_prevMinute > rollingBaseMinute // fourth check
						|| (currentMinute == displayMaxLatest_forThisMinute
								&& mbean_previous_max == displayMaxLatest_forThisMinute_val))) {
			// Used for the first check
			cachedMBPreviousMinute_max = mbean_prevMinute;
			displayMaxPrev_forThisMinute = currentMinute;
			return Duration.ofNanos(mbean_previous_max);
		}

		/*
		 * This logic is to display the latest mbean value only under a special
		 * circumstance. That is if the mbean has not been updated in awhile (i.e
		 * current minute does not match with latest minute). Due to the nature of
		 * interaction with the Mbean (in which the Mbean is only updated if a REST
		 * request occurs) we do not know if the Mbean will ever be updated again. Since
		 * we want to have as up to date information as possible, we will pro-actively
		 * retrieve the latest value and interpret it as the "previous" value and
		 * display it. Note that at this point in time the mbean's ACTUAL previous value
		 * would have already been displayed/retrieved before by the monitoring tool
		 * which would run at constant intervals.
		 * 
		 * First check to ensure we're not displaying the mbean's latest max value again
		 * UNLESS of course we're still within the current/on-going complete minute (i.e
		 * The value will be displayed for a full minute 12:00:00 to 12:00:59)
		 * 
		 * Second check to ensure that the actual current minute is not a match with the
		 * mbean's latest minute. We are suppose to display the mbean's latest value
		 * ONLY if the value is stale (i.e the mbean's latest minute is x-minutes ago)
		 * 
		 */
		if ((cachedMBLatestMinute_max != mbean_latestMinute || displayMaxLatest_forThisMinute == currentMinute) // first
																												// check
				&& currentMinute != mbean_latestMinute) // second check
		{
			// This value is used for FIRST check in the IF statement - to determine if
			// we're redisplaying latest values
			cachedMBLatestMinute_max = mbean_latestMinute;

			/*
			 * This value is for FIRST check in the IF statement - to determine if we're
			 * still ongoingly displaying current value
			 * 
			 * This value is also used to see if we want to display a mbean's previous
			 * value.
			 * 
			 * It could be the case that after displaying the stale latest minute, the MBean
			 * is updated again. We do not want to display this value again.
			 */
			displayMaxLatest_forThisMinute = currentMinute;

			/*
			 * This value is used in a check to see if we want to display a mbean's previous
			 * value.
			 * 
			 * It could be the case that after displaying the stale latest minute, the MBean
			 * is updated again. We do not want to display this value again.
			 */
			displayMaxLatest_forThisMinute_val = mbean_latest_max;

			/*
			 * Need to update a rolling window
			 * 
			 * Otherwise, depending on the scenario can erroneously display an mbeans
			 * "PREVIOUS" value again after displaying a stale mbean's "LATEST" value
			 */
			rollingBaseMinute = currentMinute;
			return Duration.ofNanos(mbean_latest_max);
		}

		return null;
	}

    /** {@inheritDoc} */
    @Override
    public synchronized Duration getMinTimeDuration() {

		getMinMaxValues();
		long currentMinute = getCurrentMinuteFromSystem();

		/*
		 * 
		 * First check to ensure we're not displaying the mbean's previous values again
		 * UNLESS of course we're still within the current/on-going complete minute (i.e
		 * The value will be displayed for a full minute 12:00:00 to 12:00:59).
		 * 
		 * Second check just to ensure the mbean's previous minute is this ongoing
		 * minute. If it is the same, then that means the mbean has been created/updated
		 * this same minute. Does not make sense to display the mbean's previous value
		 * then. (i.e mbean was created this minute and we're querying this minute)
		 * 
		 * Third check to ensure mbean actually has a value (mbean's values are
		 * initialized to 0)
		 * 
		 * Fourth check is to ensure mbean's previous value is above the rolling window
		 * (this is to ensure we are not re-displaying a value .. see below for more
		 * details). And also check to see if the mbean's latest values were bumped to
		 * the mbean's previous values while we're still expected to display it. This an
		 * happen if we were displaying a latest value due to lack of querying and then
		 * the mbean was updated)
		 * 
		 */
		if ((cachedMBPreviousMinute_min != mbean_prevMinute || displayMinPrev_forThisMinute == currentMinute) // first
																												// check
				&& currentMinute != mbean_prevMinute // second check
				&& mbean_prevMinute != 0 // third check
				&& (mbean_prevMinute > rollingBaseMinute || (currentMinute == displayMinLatest_forThisMinute //fourth check
						&& displayMinLatest_forThisMinute_val == mbean_previous_min))) {
			cachedMBPreviousMinute_min = mbean_prevMinute;
			displayMinPrev_forThisMinute = currentMinute;
			return Duration.ofNanos(mbean_previous_min);
		}
    	
		/*
		 * This logic is to display the latest mbean value only under a special
		 * circumstance. That is if the mbean has not been updated in awhile (i.e
		 * current minutes doesn't match with latest). Due to the nature of interaction
		 * with the Mbean (in which the Mbean is only updated if a REST request occurs)
		 * we do not know if the Mbean will ever be updated again. Since we want to have
		 * as up to date information as possible, we will pro-actively retrieve the
		 * latest value and interpret it as the "previous" value and display it. Note
		 * that at this point in time the mbean's ACTUAL previous value would have
		 * already been displayed/retrieved before by the monitoring tool which would
		 * run at constant intervals.
		 * 
		 * First check to ensure we're not displaying the mbean's latest min value again
		 * UNLESS of course we're still within the current/on-going complete minute (i.e
		 * The value will be displayed for a full minute 12:00:00 to 12:00:59)
		 * 
		 * Second check to ensure that the actual current minute is not a match with the
		 * mbean's latest minute. We are suppose to display the mbean's current value
		 * ONLY if the value is stale (i.e the mbean's latest minute is x-minutes ago)
		 * 
		 */
		if ((cachedMBLatestMinute_min != mbean_latestMinute || displayMinLatest_forThisMinute == currentMinute) // first
																												// check
				&& currentMinute != mbean_latestMinute) // second check
		{
			// This value is used for FIRST check in the IF statement - to determine if
			// we're redisplaying current values
			cachedMBLatestMinute_min = mbean_latestMinute;

			/*
			 * This value is for FIRST check in the IF statement - to determine if we're
			 * still ongoingly displaying current value
			 * 
			 * This value is also used to see if we want to display a mbean's previous
			 * value.
			 * 
			 * It could be the case that after displaying the stale latest minute, the MBean
			 * is updated again. We do not want to display this value again.
			 */
			displayMinLatest_forThisMinute = currentMinute;

			/*
			 * This value is used in a check to see if we want to display a mbean's previous
			 * value.
			 * 
			 * It could be the case that after displaying the stale latest minute, the MBean
			 * is updated again. We do not want to display this value again.
			 */
			displayMinLatest_forThisMinute_val = mbean_latest_min;

			/*
			 * Need to update a rolling window
			 * 
			 * Otherwise, depending on the scenario can erroneously display an mbeans
			 * "PREVIOUS" value again after displaying a stale mbean's "LATEST" value
			 */
			rollingBaseMinute = currentMinute;
			return Duration.ofNanos(mbean_latest_min);
		}
    	
		return null;
    }

	private void getMinMaxValues() {
		try {
			synchronized (this) {
				mbean_latest_min = ((Number) mbs.getAttribute(new ObjectName(objectName), "MinuteLatestMinimumDuration")).longValue();

				mbean_latest_max = ((Number) mbs.getAttribute(new ObjectName(objectName), "MinuteLatestMaximumDuration")).longValue();

				mbean_latestMinute = ((Number) mbs.getAttribute(new ObjectName(objectName), "MinuteLatest")).longValue();

				mbean_previous_min = ((Number) mbs.getAttribute(new ObjectName(objectName), "MinutePreviousMinimumDuration")).longValue();
				
				mbean_previous_max = ((Number) mbs.getAttribute(new ObjectName(objectName), "MinutePreviousMaximumDuration")).longValue();
				
				mbean_previous_max = ((Number) mbs.getAttribute(new ObjectName(objectName), "MinutePrevious")).longValue();
			}

		} catch (Exception e) {
			if (tc.isDebugEnabled()) {
				Tr.debug(tc, "getMinMaxValues exception message: ", e.getMessage());
				FFDCFilter.processException(e, getClass().getSimpleName(), "getMinMaxValues:Exception");
			}
		}
	}
    
	// Get the current system time in minutes, truncating. This number will increase
	// by 1 every complete minute.
	private long getCurrentMinuteFromSystem() {
		return System.currentTimeMillis() / 60000;
	}
}
