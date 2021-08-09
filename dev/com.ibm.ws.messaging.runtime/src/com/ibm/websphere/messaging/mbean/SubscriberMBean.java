/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.messaging.mbean;

import javax.management.MXBean;

/**
 * <p>
 * The SubscriberMBean is enabled when a subscriber connects to the messaging engine.
 * A SubscriberMBean is initialized for each Subscriber connecting to the messaging engine.
 * Use the MBean programming interface to query runtime information about a Subscriber.
 * <br><br>
 * JMX clients should use the ObjectName of this MBean to query it
 * <br>
 * Partial Object Name: WebSphere:feature=wasJmsServer, type=Subscriber,name=* <br>
 * where name is unique for each subscriber and is equal to the name of the subscriber.
 * </p>
 * 
 * @ibm-api
 */
@MXBean
public interface SubscriberMBean {

    /**
     * The ID of the Subscriber represented
     * by this instance..
     * 
     * @return ID of the Subscriber
     */
    public String getId();

    /**
     * The name of the Subscriber represented
     * by this instance.
     * 
     * @return Name of the Subscriber
     */
    public String getName();

}