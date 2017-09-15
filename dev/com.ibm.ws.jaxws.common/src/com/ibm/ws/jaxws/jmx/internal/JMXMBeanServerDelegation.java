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
package com.ibm.ws.jaxws.jmx.internal;

import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

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
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.cxf.management.jmx.export.runtime.ModelMBeanAssembler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;

/**
 *
 */
public class JMXMBeanServerDelegation implements MBeanServer {

    private static final TraceComponent tc = Tr.register(JMXMBeanServerDelegation.class);

    private static final String ERR_UNSUPPORTED_METHOD = "This method is not supported.";
    private static final String ERR_FAIL_TO_INIT_RMMB = "Fail to instantiate instance of RequiredModelMBean.";
    private static final String ERR_BC_NOT_INIT = "BundleContext was not initialized yet.";
    private static final String ERR_NOT_MBEAN = "The object is not instance of MBean.";
    private static final String ERR_BAD_OBJECTNAME = "The object name is invalid.";

    private static final JMXMBeanServerDelegation INSTANCE = new JMXMBeanServerDelegation();
    private static final String WEBSPHERE_DOMAIN_NAME = "WebSphere:feature=jaxws,";
    private static final String MBEAN_FIELD_TYPE_KEY = "type";
    private static final String MBEAN_FIELD_INSTANCE_ID_KEY = "instance.id";
    private static final String MBEAN_FIELD_BUS_ID_KEY = "bus.id";
    private static final String MBEAN_FIELD_SERVICE_KEY = "service";
    private static final String MBEAN_FIELD_PORT_KEY = "port";
    private static final String MBEAN_FIELD_NAME_KEY = "name";
    private static final String MBEAN_FIELD_NAME_VALUE_DEFAULT = "NOT_SET";
    private static final String MBEAN_FIELD_NAME_DELIMITER = "@";
    private static final String MBEAN_TYPE_BUS = "Bus";
    private static final String MBEAN_TYPE_WORK_QUEUE_MANAGER = "WorkQueueManager";
    private static final String MBEAN_TYPE_SERVICE_ENDPOINT = "Bus.Service.Endpoint";
    private static final String STR_EMPTY_STRING = "";

    private BundleContext context = null;
    private Hashtable<String, ServiceRegistration> serviceTable;
    private Hashtable<String, String> objectNameMap;
    private JMXBusInitializer jmxInitializer = null;

    public static JMXMBeanServerDelegation getInstance() {
        return INSTANCE;
    }

    /*
     * Called by Declarative Services to activate service
     */
    protected void activate(ComponentContext cc) {
        // Save bundle context for later usage
        BundleContext context = cc.getBundleContext();
        JMXMBeanServerDelegation.INSTANCE.context = context;

        // Create instance of JmxInitializer, set single instance of this class
        // into JmxInitializer as MBeanServer
        LibertyApplicationBusFactory busFactory = LibertyApplicationBusFactory.getInstance();
        jmxInitializer = new JMXBusInitializer(JMXMBeanServerDelegation.getInstance());
        busFactory.registerApplicationBusListener(jmxInitializer);
    }

    /*
     * Called by Declarative Services to deactivate service
     */
    protected void deactivate(ComponentContext cc) {
        // Clean JmxInitializer
        LibertyApplicationBusFactory busFactory = LibertyApplicationBusFactory.getInstance();
        busFactory.unregisterApplicationBusListener(jmxInitializer);

        // Unregister all existing services for Delayed MBean
        // And clean up local variables.
        if (null != serviceTable) {
            Enumeration<String> keys = serviceTable.keys();
            while (keys.hasMoreElements()) {
                String k = keys.nextElement();
                ServiceRegistration<?> r = serviceTable.get(k);
                if (null != r) {
                    r.unregister();
                }
            }
            serviceTable.clear();
        }
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        // Get object name as string
        String objName = name.toString();
        String objNameForWebSphere = generateObjectNameForWebSphereDomain(object, name);
        ServiceRegistration<?> registration = null;
        ServiceRegistration<?> registration2 = null;

        if (null != context) {
            // Prepare properties to be picked up by DelayedMBeanActivator 
            // properties is used for original domain brought by MBean
            Hashtable<String, String> properties = new Hashtable<String, String>();
            properties.put(Constants.SERVICE_VENDOR, "IBM");
            properties.put("jmx.objectname", objName);

            // Prepare properties to be picked up by DelayedMBeanActivator 
            // properties2 is used for domain "WebSphere"
            Hashtable<String, String> properties2 = new Hashtable<String, String>();
            properties2.put(Constants.SERVICE_VENDOR, "IBM");
            properties2.put("jmx.objectname", objNameForWebSphere);

            // Wrapper object to RequiredModelMBean
            ModelMBeanAssembler assembler = new ModelMBeanAssembler();
            ModelMBeanInfo mbi = assembler.getModelMbeanInfo(object.getClass());
            RequiredModelMBean rtMBean = null;

            if (null != mbi) {
                // In most of case the object from CXF has @ManagedResource annotation,
                // so that the assembler can get mbi from scanning annotation
                try {
                    rtMBean = new CXFModelMBean();
                    rtMBean.setModelMBeanInfo(mbi);
                    rtMBean.setManagedResource(object, "ObjectReference");
                } catch (Exception e) {
                    throw new MBeanRegistrationException(e, ERR_FAIL_TO_INIT_RMMB);
                }
                // Register MBean as service, so that it will be picked up by DelayedMBeanActivator, and register it as MBean (lazy)
                registration = context.registerService(RequiredModelMBean.class.getName(), rtMBean, properties);
                // Do not register WorkQueueManager MBean to WebSphere domain
                if (shouldBeRegisteredToWebSphereDomain(name)) {
                    registration2 = context.registerService(RequiredModelMBean.class.getName(), rtMBean, properties2);
                }
            }
            else {
                // In case assembler cannot get mbi, we will register the object without
                // wrapping it to instance of RequiredModelMBean.
                String publishedInterface = null;
                Class<?>[] interfaceClasses = object.getClass().getInterfaces();
                for (Class<?> interfaceClass : interfaceClasses) {
                    String interfaceName = interfaceClass.getName();
                    if (interfaceName.endsWith("MBean")) {
                        publishedInterface = interfaceName;
                    }
                }

                if (null != publishedInterface) {
                    registration = context.registerService(publishedInterface, object, properties);
                    // Do not register WorkQueueManager MBean to WebSphere domain
                    if (shouldBeRegisteredToWebSphereDomain(name)) {
                        registration2 = context.registerService(publishedInterface, object, properties2);
                    }
                }
                else {
                    // The object does not seems like MBean object
                    throw new MBeanRegistrationException(null, ERR_NOT_MBEAN);
                }
            }

            if (null == serviceTable) {
                // Initialize if not yet
                serviceTable = new Hashtable<String, ServiceRegistration>();
            }

            if (null != registration) {
                // Save the reference for unregister
                serviceTable.put(objName, registration);
            }

            if (null != registration2) {
                // Save the reference for unregister2
                serviceTable.put(objNameForWebSphere, registration2);
            }

            // For service endpoint MBean, part of the objNameForWebSphere is get from
            // object instance's "Address" attribute. We keep this mapping relationship
            // between objName and objNameForWebSphere for quickly find objNameForWebSphere
            // when MBean unregister.
            if (null == objectNameMap) {
                // Initialize if not yet
                objectNameMap = new Hashtable<String, String>();
            }
            objectNameMap.put(objName, objNameForWebSphere);
        }
        else {
            // This should not happen.
            // Before this service is used, it should already be initialized by activator.
            throw new MBeanRegistrationException(null, ERR_BC_NOT_INIT);
        }

        // Return something so that the caller is happy
        return new ObjectInstance(name, RequiredModelMBean.class.getName());
    }

    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        // Get object name as string
        String objName = name.toString();
        String objNameForWebSphere = null;
        if (null != objectNameMap) {
            objNameForWebSphere = objectNameMap.get(objName);
        }

        if (null != serviceTable) {
            // Get the reference of service registry
            ServiceRegistration<?> registration = serviceTable.get(objName);
            ServiceRegistration<?> registration2 = null;
            if ((null != objNameForWebSphere) && (objNameForWebSphere.length() > 0)) {
                registration2 = serviceTable.get(objNameForWebSphere);
            }

            if ((null == registration) && (null == registration2)) {
                throw new InstanceNotFoundException("Specific object name is not registered. ObjectName:" + objName);
            }

            // Unregister
            if (null != registration) {
                try {
                    registration.unregister();
                } catch (IllegalStateException e) {
                    // do nothing if the registration has been unregistered.
                }
            }
            if (null != registration2) {
                try {
                    registration2.unregister();
                } catch (IllegalStateException e) {
                    // do nothing if the registration has been unregistered.
                }
            }
        }
        else {
            // This should not happen.
            // Before this service is used, it should already be initialized by activator.
            throw new MBeanRegistrationException(null, ERR_BC_NOT_INIT);
        }
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        // This JmxMBeanServerDelegation class is designed to serve CXF integration with Liberty only.
        // So far only support instantiate of one class as known case.
        if (className.equalsIgnoreCase("javax.management.modelmbean.RequiredModelMBean")) {
            RequiredModelMBean rtMBean = new RequiredModelMBean();
            return rtMBean;
        }
        else {
            return null;
        }
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        boolean found = false;

        if (null != serviceTable) {
            // Get the reference of service registry
            ServiceRegistration<?> registration = serviceTable.get(name.toString());

            if (null != registration) {
                found = true;
            }
        }
        return found;
    }

    private String generateObjectNameForWebSphereDomain(Object object, ObjectName name) throws MBeanRegistrationException {

        // Check the source first
        if (null == name) {
            throw new MBeanRegistrationException(null, ERR_BAD_OBJECTNAME);
        }

        // Get original domain name
        String objectNameStr = name.toString();
        String domainName = name.getDomain();
        if ((null == objectNameStr) || (objectNameStr.length() <= 0) ||
            (null == domainName) || (domainName.length() <= 0)) {
            throw new MBeanRegistrationException(null, ERR_BAD_OBJECTNAME);
        }

        // Set domain to WebSphere according to Liberty convention
        String targetNameStr = objectNameStr.replaceFirst((domainName + ":"), WEBSPHERE_DOMAIN_NAME);

        // Get original name attribute, if it is not present then add it.
        String nameValue = name.getKeyProperty(MBEAN_FIELD_NAME_KEY);
        if ((null == nameValue) || (nameValue.length() <= 0)) {

            // No "name" attribute, need to add it.
            String nameTargetValue = MBEAN_FIELD_NAME_VALUE_DEFAULT;
            String typeValue = name.getKeyProperty(MBEAN_FIELD_TYPE_KEY);
            if ((null != typeValue) && (typeValue.equalsIgnoreCase(MBEAN_TYPE_SERVICE_ENDPOINT))) {

                // If it is web service endpoint, we will reformat the name field to format: <BUS>@<SERVICE>@<PORT>@<URL_PATTERN>
                String busIdValue = name.getKeyProperty(MBEAN_FIELD_BUS_ID_KEY);
                String serviceValue = name.getKeyProperty(MBEAN_FIELD_SERVICE_KEY);
                String portValue = name.getKeyProperty(MBEAN_FIELD_PORT_KEY);
                String instanceIdValue = name.getKeyProperty(MBEAN_FIELD_INSTANCE_ID_KEY);

                // Get url pattern, by default this is the same value as serviceName specified in WebService annotation.
                // It can be customized by specifying <url-pattern> in web.xml
                // In some case the customer might have multiple web service instance, which busId/service/endpoint are
                // the same, the only difference between instances is this urlPattern.
                // Therefore we will combine this information in name field to make it as unique for each instance.
                final String addressValue = ((org.apache.cxf.endpoint.ManagedEndpoint) object).getAddress();
                String urlPattern = STR_EMPTY_STRING;

                // We could get address likes "/testService" or we could get likes "http://localhost:80/TestApp/testService".
                // In some special case the value will be null
                if (null != addressValue) {
                    // Get only the path part of URL, get ride of protocol/hostname/port
                    urlPattern = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        @FFDCIgnore(MalformedURLException.class)
                        public String run() {
                            try {
                                // If address value is initialized (web service endpoint is invoked) it will be valid URL format.
                                URL addressURL = new URL(addressValue);
                                return addressURL.getPath();
                            } catch (MalformedURLException e) {
                                // URL Parse Error, means the addressValue is not valid URL format, this could happen before
                                // address value is initialized (web service endpoint was not invoked), in such case use the
                                // address value directly.
                                return addressValue;
                            }
                        }
                    });
                }

                if (((null == urlPattern) || (urlPattern.length() <= 0)) && (null != instanceIdValue)) {
                    // If urlPattern is null or empty value, but instance id from CXF is available, use it instead
                    urlPattern = instanceIdValue;
                }

                if ((null != busIdValue) && (null != serviceValue) && (null != portValue) && (null != urlPattern)) {

                    // Value of name is combination as format "<BUS>@<SERVICE>@<PORT>@<URL_PATTERN>"
                    nameTargetValue = "\"" + busIdValue.replace("\"", "") + MBEAN_FIELD_NAME_DELIMITER +
                                      serviceValue.replace("\"", "") + MBEAN_FIELD_NAME_DELIMITER +
                                      portValue.replace("\"", "") + MBEAN_FIELD_NAME_DELIMITER +
                                      urlPattern + "\"";

                    // Re-format object name for WebSphere domain, please note the instance.id is not included.
                    // The instance.id is meaningless as it changes every time when server is restarted.
                    targetNameStr = (new StringBuffer(WEBSPHERE_DOMAIN_NAME)).
                                    append(MBEAN_FIELD_BUS_ID_KEY).append("=").append(busIdValue).append(",").
                                    append(MBEAN_FIELD_TYPE_KEY).append("=").append(typeValue).append(",").
                                    append(MBEAN_FIELD_SERVICE_KEY).append("=").append(serviceValue).append(",").
                                    append(MBEAN_FIELD_PORT_KEY).append("=").append(portValue).append(",").
                                    append(MBEAN_FIELD_NAME_KEY).append("=").append(nameTargetValue).toString();
                }
                else {
                    // No enough information to spell name value.
                    targetNameStr += "," + MBEAN_FIELD_NAME_KEY + "=" + nameTargetValue;
                }
            }
            else {
                // Not web service endpoint, no rule defined, so use default.
                targetNameStr += "," + MBEAN_FIELD_NAME_KEY + "=" + nameTargetValue;
            }
        }
        else {
            // if "name" is already presents in object name, then no need to add it.
        }

        return targetNameStr;
    }

    private boolean shouldBeRegisteredToWebSphereDomain(ObjectName name) {
        boolean result = true;
        String typeValue = name.getKeyProperty(MBEAN_FIELD_TYPE_KEY);
        if (null == typeValue) {
            // No type identified
            result = true;
        }
        else if (typeValue.equalsIgnoreCase(MBEAN_TYPE_BUS) ||
                 typeValue.equalsIgnoreCase(MBEAN_TYPE_WORK_QUEUE_MANAGER)) {
            result = false;
        }
        return result;
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        throw new MBeanRegistrationException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        throw new MBeanRegistrationException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        throw new MBeanRegistrationException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        throw new MBeanRegistrationException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        // This method is not supported by this delegation class.
        return null;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        // This method is not supported by this delegation class.
        return null;
    }

    @Override
    public Integer getMBeanCount() {
        // This method is not supported by this delegation class.
        return null;
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public String getDefaultDomain() {
        // This method is not supported by this delegation class.
        return null;
    }

    @Override
    public String[] getDomains() {
        // This method is not supported by this delegation class.
        return null;
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        throw new MBeanException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        throw new MBeanException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        throw new MBeanException(null, ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        throw new OperationsException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        throw new InstanceNotFoundException(ERR_UNSUPPORTED_METHOD);
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        // This method is not supported by this delegation class.
        return null;
    }
}
