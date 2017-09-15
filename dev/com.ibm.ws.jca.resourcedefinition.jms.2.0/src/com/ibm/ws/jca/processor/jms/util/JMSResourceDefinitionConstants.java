/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.processor.jms.util;

/**
 *
 */
public interface JMSResourceDefinitionConstants {
    public static final String JMS_CONNECTION_FACTORY_INTERFACE = "javax.jms.ConnectionFactory";
    public static final String RESOURCE_ADAPTER_WASJMS = "wasJms";
    public static final String RESOURCE_ADAPTER_WMQJMS = "wmqJms";

    public static final String DEFAULT_JMS_RESOURCE_ADAPTER = "wasJms";
    public static final boolean DEFAULT_TRANSACTIONAL_VALUE = true;

    public static final String DESTINATION_NAME = "destinationName";

    //wasJms resource adapter related properties
    public static final String JMS_QUEUE_INTERFACE = "javax.jms.Queue";
    public static final String JMS_TOPIC_INTERFACE = "javax.jms.Topic";
    public static final String JMS_TOPIC_NAME = "topicName";
    public static final String JMS_QUEUE_NAME = "queueName";

    //wmqJms resource adapter related properties
    public static final String WMQ_TOPIC_NAME = "baseTopicName";
    public static final String WMQ_QUEUE_NAME = "baseQueueName";

    public static final String PROPERTIES_REF_KEY = "properties.0.";
}
