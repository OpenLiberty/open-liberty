/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.monitor;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.microprofile.metrics.impl.CounterImpl;

public class MonitorCounter extends CounterImpl {
	
	private static final TraceComponent tc = Tr.register(MonitorCounter.class);
	
	MBeanServer mbs;
    String objectName, attribute, subAttribute;
    boolean isComposite = false;

    public MonitorCounter(MBeanServer mbs, String objectName, String attribute) {
    	this.mbs = mbs;
        this.objectName = objectName;
        this.attribute = attribute;
    }
    
    public MonitorCounter(MBeanServer mbs, String objectName, String attribute, String subAttribute) {
    	this.mbs = mbs;
        this.objectName = objectName;
        this.attribute = attribute;
        this.subAttribute = subAttribute;
        this.isComposite = true;
    }

    @Override
    public long getCount() {
        try {
        	if (isComposite) {
                CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), attribute);
                Number numValue = (Number) value.get(subAttribute);       
                return numValue.longValue();
        	} else {
                Number value = (Number) mbs.getAttribute(new ObjectName(objectName), attribute);
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
}
