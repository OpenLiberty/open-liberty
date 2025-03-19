/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.ClosedByInterruptException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.client.rest.internal.ClientConstants.HttpMethod;
import com.ibm.ws.jmx.connector.client.rest.internal.NotificationRegistry.ClientNotificationRegistration;
import com.ibm.ws.jmx.connector.client.rest.internal.resources.RESTClientMessagesUtil;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationRecord;
import com.ibm.ws.jmx.connector.datatypes.CreateMBean;
import com.ibm.ws.jmx.connector.datatypes.Invocation;
import com.ibm.ws.jmx.connector.datatypes.JMXServerInfo;
import com.ibm.ws.jmx.connector.datatypes.MBeanInfoWrapper;
import com.ibm.ws.jmx.connector.datatypes.MBeanQuery;
import com.ibm.ws.jmx.connector.datatypes.ObjectInstanceWrapper;

/**
 * This class is implemented such that it should be thread-safe.
 */
class RESTMBeanServerConnection implements MBeanServerConnection {

    private static final Logger logger = Logger.getLogger(RESTMBeanServerConnection.class.getName());

    protected static final String CLIENT_VERSION = "IBM_JMX_REST_client_v5";

    public enum PollingMode {
        FAILOVER, NOTIFICATION
    };

    private static final long NANOS_IN_A_MILLISECOND = 1000000L;

    private final AtomicBoolean isRecoveringConnection = new AtomicBoolean();

    private ServerPollingThread serverPollingThread;

    protected final Connector connector;
    private volatile boolean disconnected = false;
    protected int serverVersion;
    private final DynamicURL rootURL;
    private DynamicURL mbeansURL, createMBeanURL, instanceOfURL, mbeanCountURL, defaultDomainURL, domainsURL, notificationsURL, fileTransferURL;
    private final ConcurrentMap<ObjectName, DynamicURL> mbeanInfoURLMap;
    private final ConcurrentMap<ObjectName, DynamicURL> mbeanAttributesURLMap;
    private final ConcurrentMap<ObjectName, Map<String, DynamicURL>> mbeanAttributeURLsMap;
    private final ConcurrentMap<ObjectName, Map<String, DynamicURL>> mbeanOperationURLsMap;
    private final FileTransferClient fileTransferClient;

    private NotificationRegistry notificationRegistry;

    protected final HostnameVerifier hostnameVerificationDisabler;

    protected Map<String, Object> mapRouting = null;

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                String oldValue = System.getProperty("sun.net.http.retryPost");

                if (oldValue != null) {
                    if (logger.isLoggable(Level.INFO)) {
                        logger.logp(Level.INFO, logger.getName(), "StaticBlock[1]", "Value of retry post was already set: " + oldValue);
                    }
                } else {
                    //Set the value on the client-side to prevent the JDK from retrying invocations.
                    System.setProperty("sun.net.http.retryPost", "false");
                }

                return null;
            }
        });
    }

    RESTMBeanServerConnection(Connector connector) throws IOException {
        this.connector = connector;

        rootURL = new DynamicURL(connector, connector.getServiceURL().getURLPath());

        // Set up HostnameVerifier to allow all host names if hostname verification is disabled
        if (connector.isHostnameVerificationDisabled()) {
            hostnameVerificationDisabler = new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
        } else {
            hostnameVerificationDisabler = null;
        }

        //First we need set our initial endpoint
        findInitialEndpoint();

        // Load JMX server info to verify server is reachable
        try {
            loadJMXServerInfo();
        } catch (IOException e) {
            //If we got an IOException for the server being down, we will have tried a recovery
            //and then either we have a valid endpoint or the connector will have been automatically
            //disconnected.  So, if the connector is still connected, we can try 1 more time to connect
            if (isConnected()) {
                loadJMXServerInfo();
            } else {
                throw e;
            }
        }

        mbeanInfoURLMap = new ConcurrentHashMap<ObjectName, DynamicURL>();
        mbeanAttributesURLMap = new ConcurrentHashMap<ObjectName, DynamicURL>();
        mbeanAttributeURLsMap = new ConcurrentHashMap<ObjectName, Map<String, DynamicURL>>();
        mbeanOperationURLsMap = new ConcurrentHashMap<ObjectName, Map<String, DynamicURL>>();
        fileTransferClient = new FileTransferClient(this);

        //Unless the user asked for fail interval polling to be off (by specifying a negative value), we start the thread.
        if (connector.getServerFailoverInterval() >= 0) {
            setPollingMode(PollingMode.FAILOVER);
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "constructor", "Initiated connector " + RESTClientMessagesUtil.getObjID(this) + " within connection: "
                                                                      + connector.getConnectionId());
        }
        if (connector.logFailovers()) {
            String connectMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.MEMBER_CONNECT, connector.getCurrentEndpoint());
            logger.logp(Level.INFO, logger.getName(), "constructor", connectMsg);
        }
    }

    //Lazy initialization of NotificationRegistry
    private NotificationRegistry getNotificationRegistry() throws IOException {
        if (notificationRegistry == null) {
            notificationRegistry = new NotificationRegistry(this);
        }
        return notificationRegistry;
    }

    protected void setPollingMode(PollingMode mode) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "setPollingMode", "Entering setPollingMode with new mode " + mode.toString());
        }

        if (serverPollingThread != null) {
            serverPollingThread.changeMode(mode);
        } else {
            serverPollingThread = new ServerPollingThread(mode);
            serverPollingThread.start();
        }
    }

    protected void discardNotificationRegistry() {
        if (connector.getServerFailoverInterval() >= 0) {
            setPollingMode(PollingMode.FAILOVER);
        } else {
            //if the user doesn't want to go into FAILOVER mode, then we're done
            //with this thread for now
            closePollingThread();
        }
        notificationRegistry.close();
        notificationRegistry = null;
    }

    private void loadJMXServerInfo() throws IOException {
        final String sourceMethod = "loadJMXServerInfo";
        checkConnection();

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), sourceMethod, "Loading server info for endpoint: " + connector.getCurrentEndpoint());
        }
        URL rootURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get JMX server info URL
            rootURL = getRootURL();

            // Get connection to server
            connection = getConnection(rootURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, rootURL);
        }

        JMXServerInfo jmx = null;
        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce, true);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be a JMXServerInfo
                    jmx = converter.readJMX(connection.getInputStream());
                    break;
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, rootURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                // Server response should be a serialized Throwable
                Throwable t = getServerThrowable(sourceMethod, connection);
                IOException ioe = t instanceof IOException ? (IOException) t : new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                throw ioe;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException io = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(io, true);
                throw io;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }

        serverVersion = jmx.version;
        mbeansURL = new DynamicURL(connector, jmx.mbeansURL);
        createMBeanURL = new DynamicURL(connector, jmx.createMBeanURL);
        instanceOfURL = new DynamicURL(connector, jmx.instanceOfURL);
        mbeanCountURL = new DynamicURL(connector, jmx.mbeanCountURL);
        defaultDomainURL = new DynamicURL(connector, jmx.defaultDomainURL);
        domainsURL = new DynamicURL(connector, jmx.domainsURL);
        notificationsURL = new DynamicURL(connector, jmx.notificationsURL);
        fileTransferURL = new DynamicURL(connector, jmx.fileTransferURL);
    }

    /** {@inheritDoc} */
    @Override
    public ObjectInstance createMBean(String className,
                                      ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
        try {
            return createMBean(className, name, null, null, null, false, false);
        } catch (InstanceNotFoundException inf) {
            throw new IOException(inf); // Should never happen
        }
    }

    /** {@inheritDoc} */
    @Override
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        return createMBean(className, name, loaderName, null, null, true, false);
    }

    /** {@inheritDoc} */
    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params,
                                      String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, IOException {
        try {
            return createMBean(className, name, null, params, signature, false, true);
        } catch (InstanceNotFoundException inf) {
            throw new IOException(inf); // Should never happen
        }
    }

    /** {@inheritDoc} */
    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
                                      String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        return createMBean(className, name, loaderName, params, signature, true, true);
    }

    private ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature, boolean useLoader,
                                       boolean useSignature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        final String sourceMethod = "createMBean";
        checkConnection();
        if (className == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.CLASS_NAME_NULL)));
        else if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name)));

        URL createURL = null;
        HttpsURLConnection connection = null;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            // Get URL for creating MBeans
            createURL = getCreateMBeanURL();

            // Get connection to server
            connection = getConnection(createURL, HttpMethod.POST);

            // Create CreateMBean object
            CreateMBean createMBean = new CreateMBean();
            createMBean.className = className;
            createMBean.objectName = name;
            createMBean.loaderName = loaderName;
            createMBean.params = params;
            createMBean.signature = signature;
            createMBean.useLoader = useLoader;
            createMBean.useSignature = useSignature;

            // Write CreateMBean JSON to connection output stream
            OutputStream output = connection.getOutputStream();
            converter.writeCreateMBean(output, createMBean);
            output.flush();
            output.close();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, createURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be an ObjectInstanceWrapper
                    ObjectInstanceWrapper wrapper = converter.readObjectInstance(connection.getInputStream());
                    mbeanInfoURLMap.put(wrapper.objectInstance.getObjectName(), new DynamicURL(connector, wrapper.mbeanInfoURL));
                    return wrapper.objectInstance;
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, createURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (RuntimeOperationsException roe) {
                    throw roe;
                } catch (ReflectionException re) {
                    throw re;
                } catch (InstanceAlreadyExistsException iae) {
                    throw iae;
                } catch (MBeanRegistrationException mbr) {
                    throw mbr;
                } catch (MBeanException me) {
                    throw me;
                } catch (RuntimeMBeanException rme) {
                    throw rme;
                } catch (NotCompliantMBeanException ncm) {
                    throw ncm;
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        final String sourceMethod = "unregisterMBean";
        checkConnection();
        if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));

        URL mbeanURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for MBean
            mbeanURL = getMBeanURL(name);

            // Get connection to server
            connection = getConnection(mbeanURL, HttpMethod.DELETE);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, mbeanURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:
                // Clean up cached URLs
                purgeMBeanURLs(name);
                return;
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (MBeanRegistrationException mbr) {
                    throw mbr;
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
        checkConnection();
        if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));

        @SuppressWarnings("unchecked")
        Set<ObjectInstance> results = queryMBeans(name, null, null, true);

        if (results.size() == 1)
            return results.toArray(new ObjectInstance[1])[0];
        else
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INSTANCE_NOT_FOUND, name));
    }

    @SuppressWarnings("unchecked")
    private Set queryMBeans(ObjectName name, QueryExp query, String className, boolean objectInstance) throws IOException {
        final String sourceMethod = "queryMBeans";
        final boolean usePOST = query != null;

        URL baseMBeansURL = null;
        URL mbeansURL = null;
        HttpsURLConnection connection = null;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            // Get URL for MBeans
            baseMBeansURL = getMBeansURL();
            mbeansURL = usePOST ? baseMBeansURL : getMBeansURL(name, className);

            // Get connection to server
            connection = getConnection(mbeansURL, usePOST ? HttpMethod.POST : HttpMethod.GET);

            if (usePOST) {
                // Create MBeanQuery object
                MBeanQuery mbeanQuery = new MBeanQuery();
                mbeanQuery.objectName = name;
                mbeanQuery.queryExp = query;
                mbeanQuery.className = className;

                // Write MBeanQuery JSON to connection output stream
                OutputStream output = connection.getOutputStream();
                converter.writeMBeanQuery(output, mbeanQuery);
                output.flush();
                output.close();
            }
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, mbeansURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be a Set of ObjectInstanceWrapper
                    ObjectInstanceWrapper[] wrappers = converter.readObjectInstances(connection.getInputStream());
                    Set mbeans = new HashSet(wrappers.length);
                    Map<ObjectName, String> tempMBeanInfoURLMap = new HashMap<ObjectName, String>(wrappers.length);
                    for (ObjectInstanceWrapper wrapper : wrappers) {
                        tempMBeanInfoURLMap.put(wrapper.objectInstance.getObjectName(), wrapper.mbeanInfoURL);
                        if (objectInstance)
                            mbeans.add(wrapper.objectInstance);
                        else
                            mbeans.add(wrapper.objectInstance.getObjectName());
                    }

                    processMBeanInfoURLs(tempMBeanInfoURLMap, getMBeansURL().getPath(), name == null && query == null && className == null);

                    return mbeans;
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, mbeansURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                Throwable t = getServerThrowable(sourceMethod, connection);
                IOException ioe = t instanceof IOException ? (IOException) t : new IOException(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE, t);
                throw ioe;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException io = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(io);
                throw io;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
        checkConnection();
        return queryMBeans(name, query, null, true);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
        checkConnection();
        return queryMBeans(name, query, null, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRegistered(ObjectName name) throws IOException {
        checkConnection();
        if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            return false;

        @SuppressWarnings("unchecked")
        Set<ObjectInstance> results = queryMBeans(name, null, null, true);
        return results.size() == 1;
    }

    /** {@inheritDoc} */
    @Override
    public Integer getMBeanCount() throws IOException {
        final String sourceMethod = "getMBeanCount";
        checkConnection();

        URL mbeanCountURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for MBean count
            mbeanCountURL = getMBeanCountURL();

            // Get connection to server
            connection = getConnection(mbeanCountURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, mbeanCountURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response
                    return converter.readInt(connection.getInputStream());
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, mbeanCountURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                // Server response should be a serialized Throwable
                Throwable t = getServerThrowable(sourceMethod, connection);
                IOException ioe = t instanceof IOException ? (IOException) t : new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                throw ioe;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException io = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(io);
                throw io;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);

        }
    }

    /** {@inheritDoc} */
    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        final String sourceMethod = "getAttribute";
        checkConnection();
        if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));
        else if (attribute == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.ATTRIBUTE_NAME_NULL)));

        URL attributeURL;

        try {
            // Get URL for attribute
            attributeURL = getAttributeURL(name, attribute);

        } catch (IntrospectionException intro) {
            throw getRequestErrorException(sourceMethod, intro);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io);
        }

        HttpsURLConnection connection;
        try {
            connection = getConnection(attributeURL, HttpMethod.GET); // Get connection to server
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, attributeURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be a POJO
                    return converter.readPOJO(connection.getInputStream());
                } catch (ClassNotFoundException cnf) {
                    // Not a REST connector bug per se; not need to log this case
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_RESULT_EXCEPTION), cnf);
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, attributeURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (MBeanException me) {
                    throw me;
                } catch (AttributeNotFoundException anf) {
                    throw anf;
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (ReflectionException re) {
                    throw re;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        final String sourceMethod = "getAttributes";
        checkConnection();
        if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));
        else if (attributes == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.ATTRIBUTE_NAMES_NULL)));

        URL attributesURL;
        try {
            // Get URL for attributes
            attributesURL = getAttributesURL(name, attributes);
        } catch (IntrospectionException intro) {
            throw getRequestErrorException(sourceMethod, intro);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io);
        }

        HttpsURLConnection connection;

        try {
            // Get connection to server
            connection = getConnection(attributesURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, attributesURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be an AttributeList
                    return converter.readAttributeList(connection.getInputStream());
                } catch (ClassNotFoundException cnf) {
                    // Not a REST connector bug per se; not need to log this case
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_RESULT_EXCEPTION), cnf);
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, attributesURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (ReflectionException re) {
                    throw re;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttribute(ObjectName name,
                             Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
        final String sourceMethod = "setAttribute";
        checkConnection();
        if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));
        else if (attribute == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.ATTRIBUTE_NULL)));

        URL attributeURL;
        HttpsURLConnection connection;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            // Get URL for attribute
            attributeURL = getAttributeURL(name, attribute.getName());
        } catch (ConnectException ce) {
            // Server is down; not a client bug
            throw ce;
        } catch (IntrospectionException intro) {
            getRequestErrorException(sourceMethod, intro);
            throw new IOException(intro);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        try {

            // Get connection to server
            connection = getConnection(attributeURL, HttpMethod.PUT);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, attributeURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        try {
            // Write Invocation JSON to connection output stream
            OutputStream output = connection.getOutputStream();
            converter.writePOJO(output, attribute.getValue());
            output.flush();
            output.close();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, attributeURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }
        switch (responseCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:
                // No content expected
                return;
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (MBeanException me) {
                    throw me;
                } catch (RuntimeMBeanException rme) {
                    throw rme;
                } catch (AttributeNotFoundException anf) {
                    throw anf;
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (InvalidAttributeValueException iav) {
                    throw iav;
                } catch (ReflectionException re) {
                    throw re;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }

    }

    /** {@inheritDoc} */
    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        final String sourceMethod = "setAttributes";
        checkConnection();
        if (name == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_NULL)));
        else if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));
        else if (attributes == null)
            throw new RuntimeOperationsException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.ATTRIBUTE_LIST_NULL)));

        URL attributesURL;
        HttpsURLConnection connection;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            // Get URL for attributes
            attributesURL = getAttributesURL(name);
        } catch (ConnectException ce) {
            // Server is down; not a client bug
            throw ce;
        } catch (IntrospectionException intro) {
            throw getRequestErrorException(sourceMethod, intro);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        try {

            // Get connection to server
            connection = getConnection(attributesURL, HttpMethod.POST);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, attributesURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }
        try {
            // Write Invocation JSON to connection output stream
            OutputStream output = connection.getOutputStream();
            converter.writeAttributeList(output, attributes);
            output.flush();
            output.close();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, attributesURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be an AttributeList
                    return converter.readAttributeList(connection.getInputStream());
                } catch (ClassNotFoundException cnf) {
                    // Not a REST connector bug per se; not need to log this case
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_RESULT_EXCEPTION), cnf);
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, attributesURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (ReflectionException re) {
                    throw re;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params,
                         String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        final String sourceMethod = "invoke";

        try {
            //Look for routing MBean
            if (ClientProvider.CONNECTION_ROUTING_NAME.equals(name.getKeyProperty("name")) &&
                ClientProvider.CONNECTION_ROUTING_DOMAIN.equals(name.getDomain())) {

                //Handle server-level routing
                if (ClientProvider.CONNECTION_ROUTING_OPERATION_ASSIGN_SERVER.equals(operationName)) {
                    if (params.length == 3) {
                        //routing at server level
                        this.mapRouting = new HashMap<String, Object>();
                        this.mapRouting.put(ClientProvider.ROUTING_KEY_HOST_NAME, params[0]);
                        this.mapRouting.put(ClientProvider.ROUTING_KEY_SERVER_USER_DIR, params[1]);
                        this.mapRouting.put(ClientProvider.ROUTING_KEY_SERVER_NAME, params[2]);
                        return Boolean.TRUE;
                    }

                    //Handle host-level routing
                } else if (ClientProvider.CONNECTION_ROUTING_OPERATION_ASSIGN_HOST.equals(operationName)) {
                    if (params.length == 1) {
                        //routing at host level
                        this.mapRouting = new HashMap<String, Object>();
                        this.mapRouting.put(ClientProvider.ROUTING_KEY_HOST_NAME, params[0]);
                        return Boolean.TRUE;
                    }
                }
            }

        } catch (Exception e) {
            throw new MBeanException(e);
        }

        checkConnection();

        //Special handling for file transfer MBean invocations
        if (ClientProvider.FILE_TRANSFER_NAME.equals(name.getKeyProperty("name")) &&
            ClientProvider.FILE_TRANSFER_DOMAIN.equals(name.getDomain())) {
            return fileTransferClient.handleOperation(operationName, params);
        }

        URL invokeURL = null;
        HttpsURLConnection connection = null;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            // Get URL for invoke operation
            invokeURL = getOperationURL(name, operationName);

            // Get connection to server
            connection = getConnection(invokeURL, HttpMethod.POST);

            // Create Invocation object
            Invocation invocation = new Invocation();
            invocation.params = params;
            invocation.signature = signature;

            // Write Invocation JSON to connection output stream
            OutputStream output = connection.getOutputStream();
            converter.writeInvocation(output, invocation);
            output.flush();
            output.close();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        } catch (IntrospectionException intro) {
            throw getRequestErrorException(sourceMethod, intro, invokeURL);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, invokeURL);
        } finally {
            JSONConverter.returnConverter(converter);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be a POJO
                    return converter.readPOJO(connection.getInputStream());
                } catch (ClassNotFoundException cnf) {
                    // Not a REST connector bug per se; not need to log this case
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_RESULT_EXCEPTION), cnf);
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, invokeURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (RuntimeMBeanException rme) {
                    throw rme;
                } catch (MBeanException me) {
                    throw me;
                } catch (ReflectionException re) {
                    throw re;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                IOException e = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultDomain() throws IOException {
        final String sourceMethod = "getDefaultDomain";
        checkConnection();

        URL defaultDomainURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for default domain
            defaultDomainURL = getDefaultDomainURL();

            // Get connection to server
            connection = getConnection(defaultDomainURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, defaultDomainURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response
                    return converter.readString(connection.getInputStream());
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, defaultDomainURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                // Server response should be a serialized Throwable
                Throwable t = getServerThrowable(sourceMethod, connection);
                IOException ioe = t instanceof IOException ? (IOException) t : new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                throw ioe;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException io = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(io);
                throw io;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String[] getDomains() throws IOException {
        final String sourceMethod = "getDomains";
        checkConnection();

        URL domainsURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for domains
            domainsURL = getDomainsURL();

            // Get connection to server
            connection = getConnection(domainsURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, domainsURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response
                    return converter.readStringArray(connection.getInputStream());
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, domainsURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                // Server response should be a serialized Throwable
                Throwable t = getServerThrowable(sourceMethod, connection);
                IOException ioe = t instanceof IOException ? (IOException) t : new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                throw ioe;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException io = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(io);
                throw io;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
        checkConnection();
        getNotificationRegistry().addNotificationListener(name, listener, filter, handback);
    }

    /** {@inheritDoc} */
    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
        checkConnection();
        getNotificationRegistry().addNotificationListener(name, listener, filter, handback);
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        checkConnection();
        getNotificationRegistry().removeNotificationListener(name, listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter,
                                           Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        checkConnection();
        getNotificationRegistry().removeNotificationListener(name, listener, filter, handback);
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        checkConnection();
        getNotificationRegistry().removeNotificationListener(name, listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
                                           Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        checkConnection();
        getNotificationRegistry().removeNotificationListener(name, listener, filter, handback);
    }

    /** {@inheritDoc} */
    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        final String sourceMethod = "getMBeanInfo";
        checkConnection();

        URL mbeanURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for MBean
            mbeanURL = getMBeanURL(name);

            // Get connection to server
            connection = getConnection(mbeanURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, mbeanURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be an MBeanInfoWrapper
                    MBeanInfoWrapper wrapper = converter.readMBeanInfo(connection.getInputStream());
                    mbeanAttributesURLMap.put(name, new DynamicURL(connector, wrapper.attributesURL));

                    //Attributes
                    Map<String, DynamicURL> attributeURLsMap = mbeanAttributeURLsMap.get(name);
                    final boolean updateAttributes = attributeURLsMap != null;
                    if (!updateAttributes) {
                        // Create a new Map - this map is used for future requests and must be thread safe
                        attributeURLsMap = new ConcurrentHashMap<String, DynamicURL>();
                    }
                    processAttributeOrOperationURLs(attributeURLsMap, wrapper.attributeURLs, updateAttributes);

                    if (!updateAttributes) {
                        //Another thread might have created/set this Map already and is about to use it, which is why
                        //we wait until *after* we have a valid Map and are ready to push that in.
                        mbeanAttributeURLsMap.putIfAbsent(name, attributeURLsMap);
                    }

                    //Operations
                    Map<String, DynamicURL> operationURLsMap = mbeanOperationURLsMap.get(name);
                    final boolean updateOperations = operationURLsMap != null;
                    if (!updateOperations) {
                        // Create a new Map - this map is used for future requests and must be thread safe
                        operationURLsMap = new ConcurrentHashMap<String, DynamicURL>();
                    }
                    processAttributeOrOperationURLs(operationURLsMap, wrapper.operationURLs, updateOperations);

                    if (!updateOperations) {
                        //Another thread might have created/set this Map already and is about to use it, which is why
                        //we wait until *after* we have a valid Map and are ready to push that in.
                        mbeanOperationURLsMap.putIfAbsent(name, operationURLsMap);
                    }

                    return wrapper.mbeanInfo;

                } catch (ClassNotFoundException cnf) {
                    // Not a REST connector bug per se; not need to log this case
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_RESULT_EXCEPTION), cnf);
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, mbeanURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (IntrospectionException ie) {
                    throw ie;
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (ReflectionException re) {
                    throw re;
                } catch (IOException io) {
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
        final String sourceMethod = "isInstanceOf";
        checkConnection();
        if (name.isPattern())
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OBJECT_NAME_PATTERN, name));

        URL instanceOfURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for instanceOf
            instanceOfURL = getInstanceOfURL(name, className);

            // Get connection to server
            connection = getConnection(instanceOfURL, HttpMethod.GET);
        } catch (IOException io) {
            throw getRequestErrorException(sourceMethod, io, instanceOfURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be a boolean
                    return converter.readBoolean(connection.getInputStream());
                } catch (Exception e) {
                    throw getResponseErrorException(sourceMethod, e, instanceOfURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw getServerThrowable(sourceMethod, connection);
                } catch (ClassNotFoundException cnf) {
                    throw new IOException(cnf);
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw getBadCredentialsException(responseCode, connection);
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                IOException ioe = getResponseCodeErrorException(sourceMethod, responseCode, connection);
                recoverConnection(ioe);
                throw ioe;
            default:
                throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }

    }

    //
    // URL Methods
    //

    protected URL getRootURL() throws IOException {
        return rootURL.getURL();
    }

    private URL getDefaultDomainURL() throws IOException {
        return defaultDomainURL.getURL();
    }

    private URL getDomainsURL() throws IOException {
        return domainsURL.getURL();
    }

    URL getNotificationsURL() throws IOException {
        return notificationsURL.getURL();
    }

    URL getFileTransferURL() throws IOException {
        return fileTransferURL.getURL();
    }

    private URL getMBeanCountURL() throws IOException {
        return mbeanCountURL.getURL();
    }

    private URL getAttributeURL(ObjectName name,
                                String attributeName) throws IOException, AttributeNotFoundException, InstanceNotFoundException, IntrospectionException, ReflectionException {
        if (!mbeanAttributeURLsMap.containsKey(name))
            getMBeanInfo(name);
        if (!mbeanAttributeURLsMap.containsKey(name))
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INSTANCE_NOT_FOUND, name));
        else if (!mbeanAttributeURLsMap.get(name).containsKey(attributeName))
            throw new AttributeNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.ATTRIBUTE_NOT_FOUND, name, attributeName));

        return mbeanAttributeURLsMap.get(name).get(attributeName).getURL();
    }

    private URL getAttributesURL(ObjectName name) throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException {
        if (!mbeanAttributesURLMap.containsKey(name))
            getMBeanInfo(name);
        if (!mbeanAttributesURLMap.containsKey(name))
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INSTANCE_NOT_FOUND, name));

        return mbeanAttributesURLMap.get(name).getURL();
    }

    private URL getAttributesURL(ObjectName name, String[] attributes) throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException {
        if (attributes != null && attributes.length > 0) {
            StringBuilder sb = new StringBuilder();
            final int length = attributes.length;
            for (int i = 0; i < length - 1; i++) {
                sb.append("attribute=");
                sb.append(URLEncoder.encode(attributes[i], "UTF-8"));
                sb.append("&");
            }
            sb.append("attribute=");
            sb.append(URLEncoder.encode(attributes[length - 1], "UTF-8"));
            String attributeList = sb.toString();
            return new URL(getAttributesURL(name).toString() + "?" +
                           attributeList);
        } else {
            return getAttributesURL(name);
        }
    }

    private URL getOperationURL(ObjectName name, String operationName) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        if (!mbeanOperationURLsMap.containsKey(name))
            getMBeanInfo(name);
        if (!mbeanOperationURLsMap.containsKey(name) || !mbeanOperationURLsMap.get(name).containsKey(operationName))
            throw new ReflectionException(new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.OPERATION_NOT_FOUND, name, operationName)));

        return mbeanOperationURLsMap.get(name).get(operationName).getURL();
    }

    private URL getCreateMBeanURL() throws IOException {
        return createMBeanURL.getURL();
    }

    private URL getInstanceOfURL() throws IOException {
        return instanceOfURL.getURL();
    }

    private URL getInstanceOfURL(ObjectName name, String className) throws IOException {
        return new URL(getInstanceOfURL().toString() + "?" +
                       (name != null ? "objectName=" + URLEncoder.encode(name.getCanonicalName(), "UTF-8") + "&" : "") +
                       (className != null ? "className=" + URLEncoder.encode(className, "UTF-8") : ""));
    }

    private URL getMBeansURL() throws IOException {
        return mbeansURL.getURL();
    }

    private URL getMBeansURL(ObjectName name, String instanceOf) throws IOException {
        return new URL(getMBeansURL().toString() + "?" +
                       (name != null ? "objectName=" + URLEncoder.encode(name.getCanonicalName(), "UTF-8") + "&" : "") +
                       (instanceOf != null ? "className=" + URLEncoder.encode(instanceOf, "UTF-8") : ""));
    }

    private URL getMBeanURL(ObjectName name) throws IOException, InstanceNotFoundException {
        if (!mbeanInfoURLMap.containsKey(name))
            queryMBeans(name, null, null, true);
        if (!mbeanInfoURLMap.containsKey(name))
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INSTANCE_NOT_FOUND, name));
        return mbeanInfoURLMap.get(name).getURL();
    }

    private void purgeMBeanURLs(ObjectName name) {
        mbeanInfoURLMap.remove(name);
        mbeanAttributesURLMap.remove(name);
        mbeanAttributeURLsMap.remove(name);
        mbeanOperationURLsMap.remove(name);
    }

    private void processAttributeOrOperationURLs(Map<String, DynamicURL> destination, Map<String, String> source, boolean update) {
        if (update) {
            // Remove any elements that are not present in the new map
            Set<String> missingKeys = new HashSet<String>(destination.keySet());
            missingKeys.removeAll(source.keySet());
            for (String missingKey : missingKeys) {
                destination.remove(missingKey);
            }
        }

        for (Map.Entry<String, String> e : source.entrySet()) {
            if (!update || !destination.containsKey(e.getKey()) ||
                !destination.get(e.getKey()).getName().equals(e.getValue())) {
                destination.put(e.getKey(), new DynamicURL(connector, e.getValue()));
            }
        }
    }

    private void processMBeanInfoURLs(Map<ObjectName, String> source, String parentPath, boolean complete) {
        if (complete) {
            // This is the complete set of MBeanInfo URLs, so remove any elements that are not present in the new map
            Set<ObjectName> missingKeys = new HashSet<ObjectName>(mbeanInfoURLMap.keySet());
            missingKeys.removeAll(source.keySet());
            for (ObjectName missingKey : missingKeys) {
                purgeMBeanURLs(missingKey);
            }
        }

        for (Map.Entry<ObjectName, String> e : source.entrySet()) {
            if (!mbeanInfoURLMap.containsKey(e.getKey()) ||
                !mbeanInfoURLMap.get(e.getKey()).getName().equals(e.getValue())) {
                // if updating the MBeanInfo URL (because new URL did not match), other maps are now invalid,
                // so purge before re-adding
                if (mbeanInfoURLMap.containsKey(e.getKey()))
                    purgeMBeanURLs(e.getKey());
                mbeanInfoURLMap.put(e.getKey(), new DynamicURL(connector, e.getValue()));
            }
        }
    }

    //
    // Exception Message and Logging Methods
    //

    synchronized void logSevereException(String sourceMethod, String errorMsg, Exception e) {
        logger.logp(Level.SEVERE, logger.getName(), sourceMethod, errorMsg, e);
    }

    synchronized IOException getRequestErrorException(String sourceMethod, Exception e, URL url) {
        String urlString = (url != null) ? url.toString() : "null";
        String connectionId = connector.getConnectionId();
        String errorMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.REQUEST_ERROR,
                                                            urlString, connectionId);
        logger.logp(Level.SEVERE, logger.getName(), sourceMethod, errorMsg, e);
        return new IOException(errorMsg, e);
    }

    synchronized IOException getRequestErrorException(String sourceMethod, Exception e) {
        String connectionId = connector.getConnectionId();
        String errorMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.REQUEST_ERROR,
                                                            null, connectionId);
        logger.logp(Level.SEVERE, logger.getName(), sourceMethod, errorMsg, e);
        return new IOException(errorMsg, e);
    }

    synchronized IOException getResponseErrorException(String sourceMethod, Exception e, URL url) {
        String urlString = (url != null) ? url.toString() : "null";
        String connectionId = connector.getConnectionId();
        String errorMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.RESPONSE_ERROR,
                                                            urlString, connectionId);
        logger.logp(Level.SEVERE, logger.getName(), sourceMethod, errorMsg, e);
        return new IOException(errorMsg, e);
    }

    synchronized IOException getResponseCodeErrorException(String methodName, int responseCode, HttpsURLConnection connection) {
        // Did not understand response code; create an IOException with response message and log error
        String responseMessage = null;
        try {
            responseMessage = connection.getResponseMessage();
        } catch (IOException io) {
            // Use null for message
        }
        String errorMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.RESPONSE_CODE_ERROR,
                                                            responseCode, responseMessage,
                                                            connection.getURL().toString(),
                                                            connector.getConnectionId());
        logger.logp(Level.SEVERE, logger.getName(), methodName, errorMsg);
        return new IOException(errorMsg);
    }

    synchronized Throwable getServerThrowable(String methodName, HttpsURLConnection connection) {
        Throwable t;
        JSONConverter converter = JSONConverter.getConverter();
        try {
            t = converter.readThrowable(connection.getErrorStream());
        } catch (ClassNotFoundException cnf) {
            // Not a REST connector bug per se; no need to log this case
            t = new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_THROWABLE_EXCEPTION), cnf);
        } catch (Exception e) {
            t = getResponseErrorException(methodName, e, connection.getURL());
        } finally {
            JSONConverter.returnConverter(converter);
        }

        return t;
    }

    IOException getBadCredentialsException(int responseCode, HttpsURLConnection connection) {
        // Received response code 401 or 403; problem with credentials
        String responseMessage = null;
        try {
            responseMessage = connection.getResponseMessage();
        } catch (IOException io) {
            // Use null for message
        }
        return new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.BAD_USER_CREDENTIALS, responseCode, responseMessage));
    }

    // -- Connection methods

    // When the connection normally closes, we disconnect after we have finished closing everything, to give everyone a chance to
    //do the proper cleaning procedures.
    void close() {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "close", "Close called for " + RESTClientMessagesUtil.getObjID(this) + " within connection: "
                                                                + connector.getConnectionId());
        }

        closePollingThread();

        if (notificationRegistry != null) {
            notificationRegistry.close();
        }

        if (connector.logFailovers()) {
            String disconnectMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.MEMBER_DISCONNECT, connector.getCurrentEndpoint());
            logger.logp(Level.INFO, logger.getName(), "close", disconnectMsg);
        }

        disconnect();
    }

    private void closePollingThread() {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "closePollingThread", "Closing thread: " + serverPollingThread.getCustomId());
        }
        if (serverPollingThread != null) {
            serverPollingThread.interrupt();

            try {
                serverPollingThread.join(2000);
            } catch (InterruptedException e) {
                //ignore..but we really shouldn't be interrupted by anybody else
            } finally {
                serverPollingThread = null;
            }
        }
    }

    //When the connection fails we disconnect right away to keep anything else from happening (ie: bad connection)
    synchronized void connectionFailed(Throwable t) {
        if (!isConnected()) {
            //another thread already disconnected this connector, so return
            return;
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "connectionFailed", "Connection failed: " + RESTClientMessagesUtil.getObjID(this));
        }

        disconnect();

        if (notificationRegistry != null) {
            notificationRegistry.close();
        }

        //Emit the notification
        connector.connectionFailed(t);
    }

    private void disconnect() {
        if (logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, logger.getName(), "disconnect", "Disconnect called for " + RESTClientMessagesUtil.getObjID(this));
        }
        this.disconnected = true;
    }

    boolean isConnected() {
        return !disconnected;
    }

    private void checkConnection() throws IOException {
        if (!isConnected())
            throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.NOT_CONNECTED));
    }

    HttpsURLConnection getBasicConnection(URL url, HttpMethod method) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(method == HttpMethod.POST || method == HttpMethod.PUT);
        connection.setUseCaches(false);
        connection.setRequestMethod(method.toString());
        connection.setRequestProperty("Content-Type", ClientConstants.JSON_MIME_TYPE);
        connection.setReadTimeout(connector.getReadTimeout());
        // Only add the Authorization header if we have one to add. It may
        // not be present in certain flows, such as the certificate-based
        // authentication.
        if (connector.getBasicAuthHeader() != null) {
            connection.setRequestProperty("Authorization", connector.getBasicAuthHeader());
        }
        if (connector.isHostnameVerificationDisabled())
            connection.setHostnameVerifier(hostnameVerificationDisabler);
        if (this.connector.getCustomSSLSocketFactory() != null) {
            connection.setSSLSocketFactory(this.connector.getCustomSSLSocketFactory());
        }

        connection.setRequestProperty("User-Agent", CLIENT_VERSION);

        return connection;
    }

    Connector getConnector() {
        return connector;
    }

    protected boolean isServerLevelRouting() {

        if ((mapRouting != null && mapRouting.size() == 3 &&
             isValueSet((String) mapRouting.get(ClientProvider.ROUTING_KEY_HOST_NAME)) && isValueSet((String) mapRouting.get(ClientProvider.ROUTING_KEY_SERVER_NAME))
             && isValueSet((String) mapRouting.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR)))) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isHostLevelRouting() {
        if ((mapRouting != null && mapRouting.size() == 1 && isValueSet((String) mapRouting.get(ClientProvider.ROUTING_KEY_HOST_NAME)))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if a value is null or empty.
     *
     * @param s The variable need to be checked.
     * @return <true> if the variable is not null and not empty. Otherwise return <code>false</code>.
     */
    public boolean isValueSet(String s) {
        return (s != null && s.trim().isEmpty() == false) ? true : false;
    }

    HttpsURLConnection getConnection(URL originalUrl, HttpMethod method) throws IOException {
        return getConnection(originalUrl, method, false);
    }

    HttpsURLConnection getConnection(URL originalUrl, HttpMethod method, boolean ignoreProxy) throws IOException {
        return getConnection(originalUrl, method, ignoreProxy, null);
    }

    HttpsURLConnection getConnection(URL originalUrl, HttpMethod method, boolean ignoreProxy, Map<String, Object> routingInfo) throws IOException {
        URL url = originalUrl;
        String sURL = originalUrl.toString();

        //For MBean routing we only care about server level routing, so that's our proxy mode.  Other users/extenders of this class,
        //such as file transfer, might also check for host level routing.
        final boolean inProxyMode = !ignoreProxy && isServerLevelRouting();
        if (inProxyMode && sURL.indexOf(ClientConstants.ROUTER_URI) < 0) {
            //If we're talking to a server that has a version at or greater than 4, then we don't need the /router URL.
            final String asURL = serverVersion >= 4 ? sURL : sURL.replaceFirst(ClientConstants.CONNECTOR_URI, ClientConstants.ROUTER_URI);
            try {
                url = AccessController.doPrivileged(
                                                    new PrivilegedExceptionAction<URL>() {
                                                        @Override
                                                        public URL run() throws MalformedURLException {
                                                            return new URL(asURL);
                                                        }
                                                    });
            } catch (PrivilegedActionException e) {
                throw new IOException(e.getMessage());
            }

        }
        HttpsURLConnection connection = getBasicConnection(url, method);
        // If routing info is explicitly provided to this method always set the request properties with the routing information.
        if (inProxyMode || routingInfo != null) {
            // Use the MBeanServerConnection's current routing context if routing info wasn't explicitly provided to this method.
            if (routingInfo == null) {
                routingInfo = mapRouting;
            }
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_HOST_NAME, (String) routingInfo.get(ClientProvider.ROUTING_KEY_HOST_NAME));
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_SERVER_USER_DIR, (String) routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR));
            connection.addRequestProperty(ClientProvider.ROUTING_KEY_SERVER_NAME, (String) routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_NAME));
            if (inProxyMode) {
                connection.addRequestProperty(ClientProvider.READ_TIMEOUT, String.valueOf(connector.getReadTimeout()));
            }
        }
        return connection;
    }

    /**
     * Attemps to recover the connection, either to the current endpoint or to other endpoints, depending
     * on the configuration of this connector and availability of endpoints.
     *
     * If we cannot connect to any of the available endpoints, this method will disconnect the connector and any subsequent call
     * attempts will throw exceptions. If we can connect to a new endpoint, or recover the connection to our old endpoint, then
     * the notification area and all URLs will be setup to use the connected endpoint.
     *
     * Appropriate JMXConnector-level notifications are sent for all events.
     *
     * @param t represents the exception that led us in here, or null.
     */
    protected void recoverConnection(Throwable t) {
        recoverConnection(t, false);
    }

    protected void recoverConnection(Throwable t, boolean skipCurrentEndpoint) {
        final String methodName = "recoverConnection";

        //We don't want to synchronize this method because we don't want a pile of threads
        //to queue up waiting to come in, each doing the same connection recovery.  Instead, we
        //use an atomic boolean to guard entry so that only 1 thread does the actual recovery.
        if (!isRecoveringConnection.compareAndSet(false, true)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), methodName, "Exiting.  Another thread is doing the recovery");
            }

            //another thread is currently trying to recover the connection, so we can exit.
            return;
        }

        //We wrap the entire code below in a try{} so that we're sure to reset the isRecoveringConnection value.
        try {

            if (!isConnected()) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), methodName, "Connection has been closed.");
                }
                //the connection has been closed
                return;
            }

            //The first thing we do is emit the notification that we have temporarily lost connection
            connector.connectionTemporarilyLost(t);

            //Get the current endpoint
            final String originalEndpoint = connector.getCurrentEndpoint();

            //Try to re-connect to the current endpoint first.  This is to keep the same behaviour for non-WLM scenarios, where there is
            //only 1 endpoint and the user may have configured an amount of time that they want to wait for that server to be up.  For WLM
            //scenarios, users can set this time to 0 so that we don't retry the current endpoint and instead move onto the other endpoints
            //right away.  Alternatively, WLM users can set this time to be > 0 so that they retry the current endpoint first, if they wish.

            final int maxServerRestartTime = connector.getMaxServerWaitTime();

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), methodName, "[" + RESTClientMessagesUtil.getObjID(this) + "] Waiting for current endpoint [" + originalEndpoint
                                                                       + "] to come up.  Max time: "
                                                                       + maxServerRestartTime);
            }

            if (!skipCurrentEndpoint && maxServerRestartTime > 0) {
                final int serverStatusPollingInterval = connector.getServerStatusPollingInterval();
                final long endTime = System.nanoTime() + maxServerRestartTime * NANOS_IN_A_MILLISECOND;

                while (System.nanoTime() < endTime) {
                    // Try to set up new NotificationArea, re-register notifications if successful
                    try {
                        if (testConnection(originalEndpoint)) {
                            //Our notification registry will be null if we came into recovery mode during the connector's init
                            if (notificationRegistry == null) {
                                if (logger.isLoggable(Level.FINER)) {
                                    logger.logp(Level.FINER, logger.getName(), methodName, "Returning sucessfully.  No notification to recover.");
                                }
                                //We still need to send the connection restored
                                connector.connectionRestored(null);
                                return;
                            }
                            notificationRegistry.setupNotificationArea();
                            if (notificationRegistry.restoreNotificationRegistrations(true)) {
                                if (logger.isLoggable(Level.FINER)) {
                                    logger.logp(Level.FINER, logger.getName(), methodName, "Returning sucessfully, notification restored for current endpoint.");
                                }
                                return;
                            }
                        }
                    } catch (Throwable throwable) {
                        // ignore and try again
                    }
                    long currentTime = System.nanoTime();
                    if (currentTime < endTime) {
                        try {
                            long millisToWait = (endTime - currentTime + (NANOS_IN_A_MILLISECOND - 1)) / NANOS_IN_A_MILLISECOND; // round up - to avoid 0
                            Thread.sleep(Math.min(millisToWait, serverStatusPollingInterval));
                        } catch (InterruptedException ie) {
                            // Connection being closed; return false so thread exits
                            if (logger.isLoggable(Level.FINER)) {
                                logger.logp(Level.FINER, logger.getName(), methodName, "Returning false, interrupted:" + ie);
                            }
                            //disconnect (this will also emit a FAILED notification)
                            connectionFailed(t);
                            return;
                        }
                    }
                }
            }

            //Get the full list of endpoints.
            List<String> endpoints = connector.getEndpointList();

            for (String endpoint : endpoints) {
                //we already tried the original endpoint, so skip that one.
                if (endpoint.equals(originalEndpoint)) {
                    continue;
                }

                if (testConnection(endpoint)) {
                    //Set our tentative new endpoint so we can attempt to setup notifications.
                    //If this fails we don't need to worry about re-instating the old endpoint because we
                    //will either override that value with another endpoint further down the list, or if none are
                    //available we will fail the entire connection.
                    connector.setCurrentEndpoint(endpoint);

                    try {

                        if (notificationRegistry == null) {
                            //haven't setup the notification thread yet
                            if (logger.isLoggable(Level.FINER)) {
                                logger.logp(Level.FINER, logger.getName(), methodName, "Returning sucessfully.");
                            }
                            //NOTE: we don't emit an OPEN notification because we're in the connector' initialization process,
                            //which will already emit an OPEN notification.
                            return;
                        }

                        //Setup notification area on the new endpoint
                        notificationRegistry.setupNotificationArea();

                        //restore notifications (don't send restored notification, since we're actually using a new endpoint)
                        if (notificationRegistry.restoreNotificationRegistrations(false)) {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.logp(Level.FINER, logger.getName(), methodName, "Returning sucessfully, notification restored.");
                            }

                            //everything is ready to go, so emit OPENED notification
                            connector.connectionOpened();

                            //Re-load the server info because it could have changed (ie: server version, URLs)
                            loadJMXServerInfo();

                            if (connector.logFailovers()) {
                                String connectMsg = RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.MEMBER_CONNECT, endpoint);
                                logger.logp(Level.INFO, logger.getName(), methodName, connectMsg);
                            }

                            return;
                        }
                        //could not restore notifications, so try next endpoint
                        continue;
                    } catch (Throwable e) {
                        //ignore and try next endpoint
                        continue;
                    }
                }
            }

            //disconnect (this will also emit a FAILED notification)
            connectionFailed(t);

            return;
        } finally {
            //we're done with the recovery (either by return or by runtime exceptions), so restore atomic state
            isRecoveringConnection.set(false);
        }
    }

    private void findInitialEndpoint() throws IOException {
        //Get the full list of endpoints.
        List<String> endpoints = connector.getEndpointList();
        String connectMsg;

        if (endpoints != null) {
            if (endpoints.size() == 1) {
                connector.setCurrentEndpoint(endpoints.get(0));
                return;
            }

            for (String endpoint : endpoints) {
                if (testConnection(endpoint)) {
                    connector.setCurrentEndpoint(endpoint);
                    return;
                }
            }
        }

        throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.NO_AVAILABLE_ENDPOINTS));
    }

    private URL getSimpleURL(String endpoint) throws MalformedURLException {
        String[] endpointSegments = splitEndpoint(endpoint);
        return new URL("https", endpointSegments[0], Integer.valueOf(endpointSegments[1]), "/IBMJMXConnectorREST/mbeanServer");
    }

    private boolean testConnection(String endpoint) {
        if (logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, logger.getName(), "testConnection", "Testing connection for endpoint " + endpoint);
        }

        //Build our test URL
        URL testURL = null;
        try {
            //Make the most simple URL available on the server side
            testURL = getSimpleURL(endpoint);
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "testConnection", "Failed while making URL:" + e.getMessage());
            }
            //Very unlikely to fail here, but return false if it happens
            return false;
        }

        //Try to connect to the simple URL
        try {
            HttpsURLConnection connection = getBasicConnection(testURL, HttpMethod.GET);
            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), "testConnection", "Successful!");
                }
                return true;
            }

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "testConnection", "Failed connection attempt with response code:" + responseCode);
            }
            return false;

        } catch (IOException e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "testConnection", "Failed connection attempt with exception:" + e.getMessage());
            }
            return false;
        }

    }

    public class ServerPollingThread extends Thread {

        /**
         * The current polling mode we're in. Failover vs Notification.
         */
        private PollingMode mode = null;

        /**
         * The amount of time this thread waits betweeen server polls. Needs
         * to be volatile because other threads will trigger a change to this value.
         * The actual wait interval will be directly related to the polling mode we're in.
         */
        private volatile long waitInterval = 0;

        /**
         * Flag used to synchronize stand-by operations
         */
        private final Object waitFlag = new PollingWaitFlag();

        /**
         * Inner class to use for the standby-lock (as suggested by findbugs)
         */
        private class PollingWaitFlag {}

        //Constructor
        private ServerPollingThread(PollingMode mode) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "ServerPollingThread", "Created thread: " + getCustomId());
            }
            // polling thread should be run as a daemon by default
            setDaemon(true);
            setName("JMX-REST-Client-Polling");
            changeMode(mode);
        }

        public String getCustomId() {
            return "ThreadID[" + this.getId() + "], from [" + connector.getConnectionId() + "]";
        }

        //This method gets called by the RESTMBeanServerConnection when certain events occur
        protected void changeMode(PollingMode newMode) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "changeMode", "Changing mode from " + mode + " to " + newMode + " in thread: " + getCustomId());
            }

            //Only take action if we're changing polling mode
            if (mode != newMode) {
                synchronized (waitFlag) {
                    //Update mode
                    mode = newMode;

                    if (mode == PollingMode.NOTIFICATION) {
                        waitInterval = connector.getNotificationFetchInterval();
                    } else {
                        waitInterval = connector.getServerFailoverInterval();
                    }

                    if (logger.isLoggable(Level.FINEST)) {
                        logger.logp(Level.FINEST, logger.getName(), "changeMode", "waitInterval is now: " + waitInterval);
                    }

                    //Notify thread to wake up.  Only 1 thread (serverPollingThread) will be
                    //potentially waiting on this flag, so we can use notify() instead of notifyAll().
                    waitFlag.notify();
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void run() {
            final String sourceMethod = "run";

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), sourceMethod, "Running thread: " + getCustomId());
            }

            JSONConverter converter = JSONConverter.getConverter();

            mainLoop: while (!interrupted() && isConnected()) {
                URL targetURL = null;
                HttpsURLConnection connection = null;

                //We cache the value of "mode" during every iteration of the while loop in case
                //another thread changes our polling mode during a HTTP request. We want each
                //HTTP request/response to have equal modes, and only change in the next iteration.
                final PollingMode currentMode = mode;

                try {
                    if (currentMode == PollingMode.NOTIFICATION) {
                        //Connect to inboxURL
                        targetURL = notificationRegistry.getInboxURL();
                        connection = getConnection(targetURL, HttpMethod.GET, true);
                        connection.setReadTimeout(getConnector().getNotificationReadTimeout());
                    } else {
                        //Connect to a simple URL to ping server
                        targetURL = getSimpleURL(connector.getCurrentEndpoint());
                        connection = getConnection(targetURL, HttpMethod.GET, true);
                    }

                    if (logger.isLoggable(Level.FINEST)) {
                        logger.logp(Level.FINEST, logger.getName(), sourceMethod, "Making a call to URL [" + targetURL + "] inside thread: " + getCustomId());
                    }

                } catch (IOException io) {
                    //If we got here we have problems other than server connection, so we must fail right away
                    logger.logp(Level.FINE, logger.getName(), sourceMethod, io.getMessage(), io);
                    connectionFailed(getRequestErrorException(sourceMethod, io, targetURL));
                    break mainLoop;
                }

                try {
                    // Check response code from server
                    int responseCode = 0;
                    try {
                        responseCode = connection.getResponseCode();

                        if (logger.isLoggable(Level.FINER)) {
                            logger.logp(Level.FINER, logger.getName(), sourceMethod, "Response code: " + responseCode);
                        }

                    } catch (ConnectException ce) {
                        logger.logp(Level.FINE, logger.getName(), sourceMethod, ce.getMessage(), ce);
                        recoverConnection(ce);
                        continue mainLoop;
                    } catch (IOException io) {
                        logger.logp(Level.FINE, logger.getName(), sourceMethod, io.getMessage(), io);
                        connection.disconnect();
                        try {
                            synchronized (waitFlag) {
                                waitFlag.wait(connector.getServerStatusPollingInterval());
                            }
                        } catch (InterruptedException e) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, logger.getName(), sourceMethod, "Interrupted sleep in thread: " + getCustomId());
                            }
                        }
                        continue mainLoop;
                    }

                    switch (responseCode) {
                        case HttpURLConnection.HTTP_OK:
                            try {

                                if (currentMode == PollingMode.NOTIFICATION) {
                                    // Process and return server response, which should be an array of Notifications
                                    NotificationRecord[] notificationRecords = converter.readNotificationRecords(connection.getInputStream());

                                    if (notificationRecords != null && isConnected()) {

                                        if (logger.isLoggable(Level.FINEST)) {
                                            logger.logp(Level.FINEST, logger.getName(), sourceMethod, "Received " + notificationRecords.length + " notifications");
                                        }

                                        for (NotificationRecord nr : notificationRecords) {
                                            Notification n = nr.getNotification();
                                            Object source = n.getSource();
                                            if (!(source instanceof ObjectName)) {
                                                logger.logp(Level.FINE, logger.getName(), sourceMethod, "Notification source was not ObjectName: " + source);
                                                getConnector().notificationLost(n);
                                                continue;
                                            }

                                            // Deliver the notification
                                            ClientNotificationRegistration localRegistration = notificationRegistry.getRegistrationMap().get(nr.getNotificationTargetInformation());
                                            if (localRegistration != null) {
                                                try {
                                                    localRegistration.handleNotification(n);
                                                } catch (Exception e) {
                                                    logSevereException(sourceMethod, RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.NOTIFICATION_LOST), e);
                                                    getConnector().notificationLost(n);
                                                }
                                            } else {
                                                getConnector().notificationLost(n);
                                            }
                                        }

                                        if (notificationRecords.length > 0) {
                                            //try to fetch right away, in case this is a burst of notifications
                                            continue mainLoop;
                                        }
                                    }
                                } else {
                                    //no-op for failover polling, just break into the sleep segment
                                    // Server sends a string back that needs to be consumed to close the connection
                                    try (InputStream is = connection.getInputStream()) {
                                        int data = is.read();
                                        while (data != -1) {
                                            data = is.read();
                                        }
                                    }
                                }
                                break;
                            } catch (ClassNotFoundException cnf) {
                                // Not a REST connector bug per se; not need to log this case
                                throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.SERVER_RESULT_EXCEPTION), cnf);
                            } catch (Exception e) {
                                logger.logp(Level.FINE, logger.getName(), sourceMethod, e.getMessage(), e);
                                throw getResponseErrorException(sourceMethod, e, targetURL);
                            }
                        case HttpURLConnection.HTTP_NOT_FOUND:
                            Throwable ex = new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.URL_NOT_FOUND));
                            logger.logp(Level.FINE, logger.getName(), sourceMethod, ex.getMessage());
                            recoverConnection(ex);
                            continue mainLoop;

                        case HttpURLConnection.HTTP_GONE:
                            // Notification area went away; try to re-establish
                            Throwable t = getServerThrowable(sourceMethod, connection);
                            logger.logp(Level.FINE, logger.getName(), sourceMethod, t.getMessage());
                            recoverConnection(t);
                            continue mainLoop;

                        case HttpURLConnection.HTTP_BAD_REQUEST:
                        case HttpURLConnection.HTTP_INTERNAL_ERROR:
                            // Server response should be a serialized Throwable
                            Throwable ie = getServerThrowable(sourceMethod, connection);
                            logger.logp(Level.FINE, sourceMethod, logger.getName(), ie.getMessage());
                            throw ie;
                        case HttpURLConnection.HTTP_UNAUTHORIZED:
                        case HttpURLConnection.HTTP_FORBIDDEN:
                            throw getBadCredentialsException(responseCode, connection);
                        default:
                            throw getResponseCodeErrorException(sourceMethod, responseCode, connection);
                    }
                } catch (ClosedByInterruptException ie) {
                    logger.logp(Level.FINE, logger.getName(), sourceMethod, ie.getMessage());
                    break mainLoop;
                } catch (Throwable t) {
                    logger.logp(Level.FINE, logger.getName(), sourceMethod, t.getMessage());
                    //If we get an unknown exception in this polling thread we must try to reconnect, which
                    //will disconnect the connector if the server is not returning proper requests.
                    recoverConnection(t);
                    continue mainLoop;
                }

                //We need to wait a certain amount of time before making the next request
                if (waitInterval > 0) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.logp(Level.FINEST, logger.getName(), sourceMethod, "Calling sleep for " + waitInterval + " milliseconds on thread: " + getCustomId());
                    }
                    try {
                        synchronized (waitFlag) {
                            waitFlag.wait(waitInterval);
                        }
                    } catch (InterruptedException e) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.logp(Level.FINE, logger.getName(), sourceMethod, "Interrupted sleep in thread: " + getCustomId());
                        }
                    }
                }
            }

            JSONConverter.returnConverter(converter);
        }
    }

    /**
     * Properly split a given endpoint into host/port.
     *
     * @param endpoint in the form host:port, where host could be a named host, an IPv4 or IPv6 address.
     * @return a String array where [0] is the host and [1] is the port.
     */
    public static String[] splitEndpoint(String endpoint) {
        //The way our endpoints are constructed ensures that we will always have a ":" present..so don't need to check for -1
        final int lastColon = endpoint.lastIndexOf(":");
        String[] splitString = new String[2];
        splitString[0] = endpoint.substring(0, lastColon);
        splitString[1] = endpoint.substring(lastColon + 1, endpoint.length());

        if (logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, logger.getName(), "splitEndpoint", "Split " + endpoint + " into " + splitString[0] + " and " + splitString[1]);
        }

        return splitString;
    }

}
