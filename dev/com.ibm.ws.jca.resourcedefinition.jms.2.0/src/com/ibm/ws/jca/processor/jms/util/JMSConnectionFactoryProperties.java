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
 * This enum holds the annotation properties and xml element name pair as constants.
 */
public enum JMSConnectionFactoryProperties {

    INTERFACE_NAME("interface-name", "interfaceName"),
    CLASS_NAME("class-name", "className"),
    RESOURCE_ADAPTER("resource-adapter", "resourceAdapter"),
    USER("user", "user"),
    PASSWORD("password", "password"),
    CLIENT_ID("client-id", "clientId"),
    PROPERTIES("properties", "properties"),
    TRANSACTIONAL("transactional", "transactional"),
    MAX_POOL_SIZE("max-pool-size", "maxPoolSize"),
    MIN_POOL_SIZE("min-pool-size", "minPoolSize"),
    DESCRIPTION("description", "description");

    private final String xmlKey;
    private final String annotationKey;

    private JMSConnectionFactoryProperties(String xmlKey, String annotationKey) {
        this.xmlKey = xmlKey;
        this.annotationKey = annotationKey;
    }

    /**
     * @return the xmlElementName
     */
    public String getXmlKey() {
        return xmlKey;
    }

    /**
     * @return the key
     */
    public String getAnnotationKey() {
        return annotationKey;
    }
}