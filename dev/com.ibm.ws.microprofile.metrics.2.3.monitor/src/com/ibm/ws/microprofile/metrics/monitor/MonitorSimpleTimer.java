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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.microprofile.metrics.impl.CounterImpl;
import com.ibm.ws.microprofile.metrics23.impl.SimpleTimerImpl;

public class MonitorSimpleTimer extends SimpleTimerImpl {
	
	private static final TraceComponent tc = Tr.register(MonitorSimpleTimer.class);
	
	MBeanServer mbs;
    String objectName, counterAttribute, counterSubAttribute, gaugeAttribute, gaugeSubAttribute;
    boolean isComposite = false;

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
}
