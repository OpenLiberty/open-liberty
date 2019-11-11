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

package com.ibm.ws.jbatch.jms.internal;

/**
 * Define system properties to help trigger certain behaviors
 * in the batch Jms path for testing.
 *
 */
public class BatchJmsEnvHelper {
    
    public static final String DISPATCHER_EXCEPTION_QUEUE_JMS = "com.ibm.ws.jbatch.internal.test.queue.exception";
    public static final String DISPATCHER_EXCEPTION_DB = "com.ibm.ws.jbatch.internal.test.db.exception";
    public static final String ENDPOINT_EXCEPTION_DB = "com.ibm.ws.jbatch.internal.test.endpoint.db.exception";
    public static final String ENDPOINT_EXCEPTION_JMS = "com.ibm.ws.jbatch.internal.test.endpoint.jms.exception";

    private static boolean triggerDispatcherQueueException = false;
    private static boolean triggerDispatcherDbException = false;
    private static boolean triggerEndpointDbException = false;
    private static boolean triggerEndpointJmsException = false;

    // Use to control test code so that exception only trigger one time
    private static int exceptionCount = 0;

    /**
     * Static constructor that will initialize all of the 'constants' based
     * on the corresponding system property. <p>
     *
     **/
    static
    {
        triggerDispatcherQueueException = Boolean.getBoolean(DISPATCHER_EXCEPTION_QUEUE_JMS);
        triggerDispatcherDbException = Boolean.getBoolean(DISPATCHER_EXCEPTION_DB);
        triggerEndpointDbException = Boolean.getBoolean(ENDPOINT_EXCEPTION_DB);
        triggerEndpointJmsException = Boolean.getBoolean(ENDPOINT_EXCEPTION_JMS);
    }
    
    public static boolean isTriggerDispatcherQueueException() {
        return triggerDispatcherQueueException;
    }

    public static boolean isTriggerDispatcherDbException() {
       return triggerDispatcherDbException;
    }
    
    public static boolean isTriggerEndpointDbException() {
        return triggerEndpointDbException;
    }

    public static boolean isTriggerEndpointJmsException() {
        return triggerEndpointJmsException;
    }

    public static int getExceptionCount() {
        return exceptionCount;
    }

    public static void incrementExceptionCount() {
        BatchJmsEnvHelper.exceptionCount = BatchJmsEnvHelper.exceptionCount + 1;
    }

}
