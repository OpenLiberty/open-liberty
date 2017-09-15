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
 * <p>Convenience class to establish a JMX connection, invoke an MBean operation, and then then close the connection.</p>
 * <p>This class is useful when you want to perform one very simple task with an MBean, but inefficient if you want to do a lot of work with lots of MBeans.</p>
 * 
 * @author Tim Burns
 */
public abstract class SimpleJmxOperation {

    protected static class GetAttributeOperation extends SimpleJmxOperation {
        protected ObjectName objectName;
        protected String attribute;

        protected GetAttributeOperation(ObjectName objectName, String attribute) {
            this.objectName = objectName;
            this.attribute = attribute;
        }

        @Override
        protected Object invokeOperation(JmxConnection conn) throws JmxException {
            return conn.getAttribute(this.objectName, this.attribute);
        }
    }

    protected static class InvokeOperation extends SimpleJmxOperation {
        protected ObjectName objectName;
        protected String operationName;
        protected Object[] params;
        protected String[] signature;

        protected InvokeOperation(ObjectName objectName, String operationName, Object[] params, String[] signature) {
            this.objectName = objectName;
            this.operationName = operationName;
            this.params = params;
            this.signature = signature;
        }

        @Override
        protected Object invokeOperation(JmxConnection conn) throws JmxException {
            return conn.invoke(this.objectName, this.operationName, this.params, this.signature);
        }
    }

    protected static class LogMBeanInfoOperation extends SimpleJmxOperation {
        protected Level level;
        protected ObjectName objectName;

        protected LogMBeanInfoOperation(Level level, ObjectName objectName) {
            this.level = level;
            this.objectName = objectName;
        }

        @Override
        protected Object invokeOperation(JmxConnection conn) throws JmxException {
            conn.logMBeanInfo(this.level, this.objectName);
            return null;
        }
    }

    protected static class LogObjectNamesOperation extends SimpleJmxOperation {
        protected Level level;

        protected LogObjectNamesOperation(Level level) {
            this.level = level;
        }

        @Override
        protected Object invokeOperation(JmxConnection conn) throws JmxException {
            conn.logObjectNames(this.level);
            return null;
        }
    }

    /**
     * Establishes a JMX connection, gets the value of a specific attribute of a named MBean, and then then closes the connection.
     * 
     * @param url the JMX connection URL where you want to find the MBean
     * @param objectName The object name of the MBean from which the attribute is to be retrieved.
     * @param attribute The object name of the MBean from which the attribute is to be retrieved.
     * @return The object name of the MBean from which the attribute is to be retrieved.
     * @throws JmxException if the value can't be retrieved
     */
    public static Object getAttribute(JMXServiceURL url, ObjectName objectName, String attribute) throws JmxException {
        return new GetAttributeOperation(objectName, attribute).invoke(url);
    }

    /**
     * Establishes a JMX connection, invokes an MBean operation, and then then closes the connection.
     * 
     * @param url the JMX connection URL where you want to find the MBean
     * @param objectName the MBean to invoke
     * @param operationName the operation to invoke on the MBean
     * @param params parameters for the operation
     * @param signature the class signature of the operation
     * @return the result of the operation
     * @throws JmxException if the operation fails
     */
    public static Object invoke(JMXServiceURL url, ObjectName objectName, String operationName, Object[] params, String[] signature) throws JmxException {
        return new InvokeOperation(objectName, operationName, params, signature).invoke(url);
    }

    /**
     * Establishes a JMX connection, logs detailed information about a specific MBean, and then then closes the connection.
     * 
     * @param url the JMX connection URL where you want to find the MBean
     * @param level the level where you want to log MBean details
     * @param objectName the name of the MBean to log
     * @throws JmxException if MBean information can't be retrieved
     */
    public static void logMBeanInfo(JMXServiceURL url, Level level, ObjectName objectName) throws JmxException {
        new LogMBeanInfoOperation(level, objectName).invoke(url);
    }

    /**
     * Establishes a JMX connection, logs the canonical name of all registered MBeans, and then then closes the connection.
     * 
     * @param url the JMX connection URL where you want to find the MBeans
     * @param level the level where you want to log the names
     * @throws JmxException if the names can't be queried
     */
    public static void logObjectNames(JMXServiceURL url, Level level) throws JmxException {
        new LogObjectNamesOperation(level).invoke(url);
    }

    protected Object invoke(JMXServiceURL url) throws JmxException {
        JmxConnection conn = new JmxConnection();
        try {
            conn.open(url);
            return this.invokeOperation(conn);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    protected abstract Object invokeOperation(JmxConnection conn) throws JmxException;

}
