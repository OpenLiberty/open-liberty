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
package com.ibm.ws.microprofile.metrics21.monitor;

import javax.management.MBeanServer;

import com.ibm.ws.microprofile.metrics.monitor.MonitorGauge;

public class MonitorGauge21<T extends Number> extends MonitorGauge<T>{
	
    public MonitorGauge21(MBeanServer mbs, String objectName, String attribute) {
    	super( mbs,  objectName, attribute);
    }
    public MonitorGauge21(MBeanServer mbs, String objectName, String attribute, String subAttribute) {
    	super( mbs,  objectName, attribute,subAttribute);
    }
}
