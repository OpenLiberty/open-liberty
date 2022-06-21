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
package com.ibm.wsspi.threading;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public interface WorkContext extends Map<String, Serializable> {
    static final String WORK_TYPE_IIOP = "IIOP";
    static final String WORK_TYPE_EJB_TIMER = "EJBTimer";
    static final String WORK_TYPE_EJB_ASYNC = "EJBAsync";
    static final String WORK_TYPE_HTTP = "HTTP";
    static final String WORK_TYPE_JCA = "JCA";
    static final String WORK_TYPE_UNKNOWN = "Unknown";

    /**
     * All WorkContext objects will contain a value for this key.
     */

    static final String APPLICATION_NAME = "com.ibm.wsspi.threading.work.applicationName";
    static final String MODULE_NAME = "com.ibm.wsspi.threading.work.moduleName";
    static final String BEAN_NAME = "com.ibm.wsspi.threading.work.beanName";
    static final String METHOD_NAME = "com.ibm.wsspi.threading.work.methodName";
    static final String RA_NAME = "com.ibm.wsspi.threading.work.raName";
    static final String QUEUE_NAME = "com.ibm.wsspi.threading.work.queueName";
    static final String INBOUND_HOSTNAME = "com.ibm.wsspi.threading.work.inboundHostname";
    static final String INBOUND_PORT = "com.ibm.wsspi.threading.work.inboundPort";

    /**
     * WorkContext objects with WORK_TYPE_IIOP_EJB will contain a value of either "local" or
     * "remote" for this key.
     */
    static final String EJB_SOURCE = "com.ibm.wsspi.threading.work.ejbSource";
    static final String URI = "com.ibm.wsspi.threading.work.uri";

    public String getWorkType();

    // use Map.get() to retrieve property values using a defined set of property keys
}