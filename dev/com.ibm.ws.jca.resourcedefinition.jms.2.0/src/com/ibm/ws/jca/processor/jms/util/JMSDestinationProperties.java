/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.jca.processor.jms.util;

/**
 * This enum holds the annotation properties and xml element name pair as constants.
 */
public enum JMSDestinationProperties {

    NAME("name", "name"),
    INTERFACE_NAME("interface-name", "interfaceName"),
    CLASS_NAME("class-name", "className"),
    RESOURCE_ADAPTER("resource-adapter", "resourceAdapter"),
    DESTINATION_NAME("destination-name", "destinationName"),
    PROPERTIES("properties", "properties"),
    DESCRIPTION("description", "description");

    private final String xmlKey;
    private final String annotationKey;

    private JMSDestinationProperties(String xmlKey, String annotationKey) {
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