/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threading;

import java.io.Serializable;
import java.util.Map;

/**
 * to do: Description from UFO for javadoc
 */

/**
 * WorkContext types
 */
public interface WorkContext extends Map<String, Serializable> {
    static final String WORK_TYPE_IIOP = "IIOP";
    static final String WORK_TYPE_HTTP = "HTTP";
    static final String WORK_TYPE_JCA = "JCA";
    static final String WORK_TYPE_UNKNOWN = "Unknown";

    /**
     * WorkContext objects can contain a value for this key.
     */
    static final String APPLICATION_NAME = "com.ibm.wsspi.threading.work.applicationName";
    static final String MODULE_NAME = "com.ibm.wsspi.threading.work.moduleName";
    static final String BEAN_NAME = "com.ibm.wsspi.threading.work.beanName";
    static final String METHOD_NAME = "com.ibm.wsspi.threading.work.methodName";
    static final String WORK_NAME = "com.ibm.wsspi.threading.work.raName";
    static final String RA_NAME = "com.ibm.wsspi.threading.work.raName";
    static final String LONGRUNNING_HINT = "com.ibm.wsspi.threading.work.longrunninghint";
    static final String NAME_HINT = "com.ibm.wsspi.threading.work.namehint";
    static final String QUEUE_NAME = "com.ibm.wsspi.threading.work.queueName";
    static final String INBOUND_HOSTNAME = "com.ibm.wsspi.threading.work.inboundHostname";
    static final String INBOUND_PORT = "com.ibm.wsspi.threading.work.inboundPort";
    static final String IIOP_OPERATION_NAME = "com.ibm.wsspi.threading.work.operationName";
    static final String IIOP_REQUEST_ID = "com.ibm.wsspi.threading.work.requestId";
    static final String URI = "com.ibm.wsspi.threading.work.uri";

    public String getWorkType();

    // use Map.get() to retrieve property values using a defined set of property keys
}