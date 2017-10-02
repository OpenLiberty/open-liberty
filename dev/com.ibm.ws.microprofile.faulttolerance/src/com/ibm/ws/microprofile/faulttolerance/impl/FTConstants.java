/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.impl;

/**
 *
 */
public class FTConstants {

    public static final String SCHEDULED_EXECUTOR_SERVICE_JNDI = "java:comp/DefaultManagedScheduledExecutorService";

    public static final String JSE_FLAG = "com.ibm.ws.microprofile.faulttolerance.jse";

    public static final long MIN_TIMEOUT_NANO = 1000000; //1ms

}
