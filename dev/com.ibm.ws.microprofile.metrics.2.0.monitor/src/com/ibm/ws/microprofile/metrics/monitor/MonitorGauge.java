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
package com.ibm.ws.microprofile.metrics.monitor;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.eclipse.microprofile.metrics.Gauge;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

public class MonitorGauge<T> implements Gauge<T> {
	
	private static final TraceComponent tc = Tr.register(MonitorGauge.class);
	
	MBeanServer mbs;
    String objectName, attribute, subAttribute;
    boolean isComposite = false;

    public MonitorGauge(MBeanServer mbs, String objectName, String attribute) {
    	this.mbs = mbs;
        this.objectName = objectName;
        this.attribute = attribute;
    }

    public MonitorGauge(MBeanServer mbs, String objectName, String attribute, String subAttribute) {
    	this.mbs = mbs;
        this.objectName = objectName;
        this.attribute = attribute;
        this.subAttribute = subAttribute;
        isComposite = true;
    }

    public T getValue() {
        try {
            if (isComposite) {
                CompositeData value = (CompositeData) mbs.getAttribute(new ObjectName(objectName), attribute);
                return (T) value.get(subAttribute);
            } else {
                T value = (T) mbs.getAttribute(new ObjectName(objectName), attribute);
                return value;
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getValue exception message: ", e.getMessage());
                FFDCFilter.processException(e, getClass().getSimpleName(), "getValue:Exception");
            }
        }
        return null;
    }
}
