/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.jmx;

import java.util.logging.Level;

import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

/**
 * Encapsulates a simple, reusable MBean accessible from a specific JMX URL
 * 
 * @author Tim Burns
 */
public class SimpleMBean {

    /**
     * Convenience method to construct an {@link ObjectName}.
     * 
     * @param name A string representation of the object name
     * @return an {@link ObjectName}
     * @throws JmxException the string passed as a parameter does not have the right format
     */
    public static ObjectName getObjectName(String name) throws JmxException {
        try {
            return new ObjectName(name);
        } catch (Exception e) {
            throw new JmxException("Failed to construct the ObjectName of an MBean given the string representation: " + name, e);
        }
    }

    private final JMXServiceURL url;
    private final ObjectName objectName;

    /**
     * Encapsulate a particular MBean on a server
     * 
     * @param url the JMX connection URL where you want to find the MBean
     * @param objectName the name of the MBean you want to operate on
     */
    public SimpleMBean(JMXServiceURL url, ObjectName objectName) {
        this.url = url;
        this.objectName = objectName;
    }

    /**
     * @return the JMX connection URL
     */
    public JMXServiceURL getUrl() {
        return this.url;
    }

    /**
     * @return the name of the encapsulated MBean
     */
    public ObjectName getObjectName() {
        return this.objectName;
    }

    /**
     * Establishes a JMX connection, logs detailed information about a specific MBean, and then then closes the connection.
     * 
     * @param level the level where you want to log MBean details
     * @throws JmxException if MBean information can't be retrieved
     */
    public void logMBeanInfo(Level level) throws JmxException {
        SimpleJmxOperation.logMBeanInfo(this.getUrl(), level, this.getObjectName());
    }

}
