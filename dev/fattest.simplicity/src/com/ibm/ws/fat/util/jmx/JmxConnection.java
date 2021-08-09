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

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.ws.fat.util.PropertyMap;
import com.ibm.ws.fat.util.Props;

/**
 * Convenience class to perform standard operations with a {@link JMXConnector} and {@link MBeanServerConnection}.
 * 
 * @author Tim Burns
 */
public class JmxConnection {

    //    static class ApplicationMBeanListener implements NotificationListener {
    //        private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    //
    //        @Override
    //        public void handleNotification(Notification notification, Object handback) {
    //            if (notification == null) {
    //                LOG.info("Received notification, but it's null");
    //            } else {
    //                LOG.info("Notification " + notification.getSequenceNumber());
    //                LOG.info("   Message    : " + notification.getMessage());
    //                LOG.info("   Time Stamp : " + dateFormat.format(new Date(notification.getTimeStamp())));
    //                LOG.info("   Type       : " + notification.getType());
    //                LOG.info("   Handback   : " + handback);
    //                LOG.info("   Source     : " + notification.getSource());
    //                LOG.info("   User Data  : " + notification.getUserData());
    //            }
    //        }
    //    }

    private static final Logger LOG = Logger.getLogger(JmxConnection.class.getName());

    private JMXConnector jmxConnector;
    private String connectionId;
    private MBeanServerConnection mbsc;

    /**
     * Establishes a JMX connection.
     * The {@link #close()} method must be called after you are done with the connection.
     * If a connection is already open, it will be closed before a new connection is established.
     * 
     * @param url the address to connect to
     * @throws JmxException if the connection fails
     * @see #close()
     * @see {@link JmxServiceUrlFactory}
     */
    public void open(JMXServiceURL url) throws JmxException {
        this.close(); // in case a connection is already open
        try {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Establishing a JMX conection to URL: " + url);
            }
            this.jmxConnector = JMXConnectorFactory.connect(url);
            this.connectionId = this.jmxConnector.getConnectionId();
            this.mbsc = this.jmxConnector.getMBeanServerConnection();
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Connection established! ID: " + this.connectionId);
            }
        } catch (IOException e) {
            // something's wrong with the connection, disconnect immediately and throw an exception
            try {
                this.close();
            } catch (Exception e1) {
                // don't throw this exception, throw the original exception
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Failed to close a JMX connection after a connection attempt failed", e1);
                }
            }
            throw new JmxException("Failed to establish a JMX connection to URL: " + url.toString(), e);
        }
    }

    /**
     * Closes a JMX connection. If no connection has been established, this method does nothing.
     * 
     * @throws JmxException if the connection cannot be closed cleanly.
     *             If this exception is thrown, it is not known whether the server
     *             end of the connection has been cleanly closed.
     */
    public void close() throws JmxException {
        if (this.jmxConnector != null) {
            try {
                try {
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info("Closing JMX connection with ID: " + this.connectionId);
                    }
                    this.jmxConnector.close();
                    if (LOG.isLoggable(Level.INFO)) {
                        LOG.info("Connection closed.");
                    }
                } catch (IOException e) {
                    throw new JmxException("Failed to close JMX connection with ID: " + this.connectionId, e);
                }
            } finally {
                this.jmxConnector = null;
                this.connectionId = null;
                this.mbsc = null;
            }
        }
    }

    /**
     * Retrieve the current {@link MBeanServerConnection}
     * 
     * @return the current {@link MBeanServerConnection}
     * @throws IllegalStateException if no connection exists
     */
    protected MBeanServerConnection getMBeanServerConnection() throws IllegalStateException {
        if (this.mbsc == null) {
            throw new IllegalStateException("An attempt was made to operate on MBean without a JMX connection.");
        }
        return this.mbsc;
    }

    /**
     * Invokes an operation on an MBean with the current connection
     * 
     * @param objectName the MBean to invoke
     * @param operationName the operation to invoke on the MBean
     * @param params parameters for the operation
     * @param signature the class signature of the operation
     * @return the result of the operation
     * @throws JmxException if the operation fails
     */
    public Object invoke(ObjectName objectName, String operationName, Object[] params, String[] signature) throws JmxException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Invoking an MBean operation");
            PropertyMap args = new PropertyMap();
            args.put("Object Name", objectName);
            args.put("Operation Name", operationName);
            args.put("Param", params);
            args.put("Signature", signature);
            args.log(Level.INFO, "  ", false);
        }
        Object result;
        try {
            MBeanServerConnection conn = this.getMBeanServerConnection();
            //          String handback = "Operation: " + operationName;
            //          ApplicationMBeanListener listener = new ApplicationMBeanListener();
            //          conn.addNotificationListener(name, listener, null, handback);
            result = conn.invoke(objectName, operationName, params, signature);
        } catch (Exception e) {
            throw new JmxException("Failed to invoke the \"" + operationName + "\" operation on " + objectName, e);
        }
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("MBean operation completed. Result: " + result);
        }
        return result;
    }

    /**
     * Logs the canonical name of all registered MBeans
     * 
     * @param level the level where you want to log the names
     * @throws JmxException if the names can't be queried
     */
    public void logObjectNames(Level level) throws JmxException {
        if (!LOG.isLoggable(level)) {
            return;
        }
        try {
            Set<ObjectName> objectNames = this.getMBeanServerConnection().queryNames(null, null);
            for (ObjectName objectName : objectNames) {
                LOG.log(level, objectName.getCanonicalName());
            }
        } catch (Exception e) {
            throw new JmxException("Failed to log canonical name of all registered MBeans", e);
        }
    }

    /**
     * Logs detailed information about a specific MBean
     * 
     * @param level the level where you want to log MBean details
     * @param name the name of the MBean to log
     * @throws JmxException if MBean information can't be retrieved
     */
    public void logMBeanInfo(Level level, ObjectName name) throws JmxException {
        if (!LOG.isLoggable(level)) {
            return;
        }
        Props props = Props.getInstance();
        String mediumDelimiter = props.getProperty(Props.LOGGING_BREAK_MEDIUM);
        String smallDelimiter = props.getProperty(Props.LOGGING_BREAK_SMALL);
        LOG.log(level, mediumDelimiter);
        LOG.log(level, "MBean: " + name.getCanonicalName());
        MBeanInfo bean;
        try {
            bean = this.getMBeanServerConnection().getMBeanInfo(name);
        } catch (Exception e) {
            throw new JmxException("Failed to get MBeanInfo for: " + name, e);
        }
        MBeanOperationInfo[] operations = bean.getOperations();
        if (operations == null || operations.length < 1) {
            LOG.log(level, "(No operations)");
        } else {
            for (MBeanOperationInfo operation : operations) {
                LOG.log(level, smallDelimiter);
                LOG.log(level, "Operation   : " + operation.getName());
                LOG.log(level, "Description : " + operation.getDescription());
                LOG.log(level, "Returns     : " + operation.getReturnType());
                LOG.log(level, "Impact      : " + operation.getImpact());
                MBeanParameterInfo[] params = operation.getSignature();
                if (params == null || params.length < 1) {
                    LOG.log(level, "Parameters  : (none)");
                } else {
                    for (int i = 0; i < params.length; i++) {
                        MBeanParameterInfo param = params[i];
                        LOG.log(level, "Parameter " + i + " : ");
                        LOG.log(level, "   Type        : " + param.getType());
                        LOG.log(level, "   Name        : " + param.getName());
                        LOG.log(level, "   Description : " + param.getDescription());
                    }
                }
            }
        }
        MBeanAttributeInfo[] attrs = bean.getAttributes();
        if (attrs == null || attrs.length < 1) {
            LOG.log(level, "(No attributes)");
        } else {
            for (int i = 0; i < attrs.length; i++) {
                MBeanAttributeInfo attr = attrs[i];
                LOG.log(level, smallDelimiter);
                LOG.log(level, "Attribute   : " + attr.getName());
                LOG.log(level, "Type        : " + attr.getType());
                LOG.log(level, "Description : " + attr.getDescription());
            }
        }
        MBeanNotificationInfo[] notifications = bean.getNotifications();
        if (notifications == null || notifications.length < 1) {
            LOG.log(level, "(No notifications)");
        } else {
            for (int i = 0; i < notifications.length; i++) {
                MBeanNotificationInfo notification = notifications[i];
                LOG.log(level, smallDelimiter);
                LOG.log(level, "Notification : " + notification.getName());
                LOG.log(level, "Description  : " + notification.getDescription());
                LOG.log(level, "Types        :");
                String[] notifTypes = notification.getNotifTypes();
                for (String type : notifTypes) {
                    LOG.log(level, "      " + type);
                }
            }
        }
    }

    /**
     * Gets the value of a specific attribute of a named MBean.
     * 
     * @param objectName The object name of the MBean from which the attribute is to be retrieved.
     * @param attribute The object name of the MBean from which the attribute is to be retrieved.
     * @return The object name of the MBean from which the attribute is to be retrieved.
     * @throws JmxException if the value can't be retrieved
     */
    public Object getAttribute(ObjectName objectName, String attribute) throws JmxException {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Geting the \"" + attribute + "\" attribute of the the MBean " + objectName);
        }
        Object value;
        try {
            value = this.getMBeanServerConnection().getAttribute(objectName, "State");
        } catch (Exception e) {
            throw new JmxException("Failed to get the \"" + attribute + "\" attribute of the the MBean " + objectName, e);
        }
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info(attribute + "=" + value);
        }
        return value;
    }

}
