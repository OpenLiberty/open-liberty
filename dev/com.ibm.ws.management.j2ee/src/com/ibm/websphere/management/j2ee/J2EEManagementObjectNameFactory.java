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
package com.ibm.websphere.management.j2ee;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Shared JSR77 object name factory.
 */
public class J2EEManagementObjectNameFactory {
    /**
     * Module types. Allows for a single parameterized factory
     * method for module mbeans object names.
     * 
     * The enum constants must be kept exactly the same as the
     * corresponding JSR 77 key values, as their {@link Enum#name()} values are used as key names.
     */
    public enum ModuleType {
        WebModule,
        EJBModule,
        ResourceAdapterModule,
        AppClientModule
    }

    /**
     * EJB types. Allows for a parameterized EJB mbean name factory method.
     */
    public enum EJBType {
        StatelessSessionBean,
        StatefulSessionBean,
        // This is not defined by JSR 77, but its what tWAS does.
        SingletonSessionBean,
        EntityBean,
        MessageDrivenBean
    }

    /**
     * Resource types. Allows for a parameterized resource factory method.
     */
    public enum ResourceType {
        JCAResource,
        JTAResource,
        JDBCResource,
        JMSResource,
        JNDIResource,
        RMI_IIOPResource,
        JavaMailResource,
        URLResource
    }

    // Defined constants from the JSR77 Specification.

    public static final String DOMAIN_NAME = "WebSphere";

    public static final String KEY_TYPE = "j2eeType";
    public static final String KEY_NAME = "name";

    public static final String KEY_SERVER = "server";
    public static final String KEY_JAVA_VMS = "javaVMs";

    public static final String DEPLOYMENT_DESCRIPTOR = "deploymentDescriptor";
    public static final String SERVLETS = "servlets";

    public static final String TYPE_DOMAIN = "J2EEDomain";
    public static final String TYPE_SERVER = "J2EEServer";
    public static final String TYPE_JVM = "JVM";
    public static final String TYPE_APPLICATION = "J2EEApplication";
    public static final String TYPE_EJB_MODULE = "EJBModule";
    public static final String TYPE_WEB_MODULE = "WebModule";
    public static final String TYPE_RESOURCE_ADAPTER_MODULE = "ResourceAdapterModule";
    public static final String TYPE_APP_CLIENT_MODULE = "AppClientModule";
    public static final String TYPE_SERVLET = "Servlet";
    public static final String TYPE_JAVA_MAIL_RESOURCE = "JavaMailResource";
    public static final String TYPE_JNDI_RESOURCE = "JNDIResource";
    public static final String TYPE_ORB_RESOURCE = "RMI_IIOPResource";

    public static final String NAME_JAVA_MAIL_RESOURCE = "JavaMailResourceMBeanImpl";
    public static final String MAIL_SESSION_ID = "mailSessionID";
    public static final String RESOURCE_ID = "resourceID";

    //

    /**
     * Common object name factory method.
     * 
     * The properties table must be an {@link Hashtable} because the object name
     * constructor {@link Object#ObjectName String, Hashtable)} takes a parameter of
     * this type.
     * 
     * @param type The type of the object name. See {@link #KEY_TYPE}.
     * @param name The name of the object name. See {@link #KEY_NAME}.
     * @param props Properties expressing the attribute values of the object name.
     */
    private static ObjectName createObjectName(String type, String name, Hashtable<String, String> props) {
        props.put(KEY_TYPE, type);
        props.put(KEY_NAME, name);

        try {
            return new ObjectName(DOMAIN_NAME, props);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Top level (Domain, Server, JVM) factory methods ...

    public static ObjectName createJ2EEDomainObjectName() {
        return createObjectName(TYPE_DOMAIN, DOMAIN_NAME, new Hashtable<String, String>());
    }

    public static ObjectName createJ2EEServerObjectName(String name) {
        return createObjectName(TYPE_SERVER, name, new Hashtable<String, String>());
    }

    public static ObjectName createJVMObjectName(String serverName) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(TYPE_SERVER, serverName);
        return createObjectName(TYPE_JVM, TYPE_JVM, props);
    }

    // Application and module factory methods ...

    public static ObjectName createApplicationObjectName(String name, String serverName) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(TYPE_SERVER, serverName);
        return createObjectName(TYPE_APPLICATION, name, props);
    }

    public static ObjectName createModuleObjectName(ModuleType moduleType, String uri, String appName, String serverName) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(TYPE_APPLICATION, appName == null ? "null" : appName);
        props.put(TYPE_SERVER, serverName);
        return createObjectName(moduleType.name(), uri, props);
    }

    // EJB factory methods ...

    public static ObjectName createEJBModuleObjectName(String uri, String appName, String serverName) {
        return createModuleObjectName(ModuleType.EJBModule, uri, appName, serverName);
    }

    public static ObjectName createEJBObjectName(EJBType type, String name, String moduleURI, String appName, String serverName) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(ModuleType.EJBModule.name(), moduleURI);
        props.put(TYPE_APPLICATION, appName == null ? "null" : appName);
        props.put(TYPE_SERVER, serverName);
        return createObjectName(type.name(), name, props);
    }

    // Servlet factory methods ...

    public static ObjectName createWebModuleObjectName(String moduleURI, String appName, String serverName) {
        return createModuleObjectName(ModuleType.WebModule, moduleURI, appName, serverName);
    }

    public static ObjectName createServletObjectName(String servletName, String moduleURI, String appName, String serverName) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(ModuleType.WebModule.name(), moduleURI);
        props.put(TYPE_APPLICATION, ((appName == null) ? "null" : appName));
        props.put(TYPE_SERVER, serverName);
        return createObjectName(TYPE_SERVLET, servletName, props);
    }

    // Java mail factory methods ...

    public static ObjectName createJavaMailObjectName(String serverName, String mailSessionID, int resourceCounter) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(TYPE_SERVER, serverName);
        props.put(MAIL_SESSION_ID, mailSessionID);
        props.put(RESOURCE_ID, TYPE_JAVA_MAIL_RESOURCE + "-" + resourceCounter);
        ObjectName objectName;
        try {
            objectName = createObjectName(TYPE_JAVA_MAIL_RESOURCE, NAME_JAVA_MAIL_RESOURCE, props);
        } catch (IllegalArgumentException e) {
            // mailSessionID contains illegal characters
            props.remove(MAIL_SESSION_ID);
            objectName = createObjectName(TYPE_JAVA_MAIL_RESOURCE, NAME_JAVA_MAIL_RESOURCE, props);
        }
        return objectName;
    }

    /**
     * Creates a Resource ObjectName for a Resource MBean
     * 
     * @param serverName
     * @param keyName
     * @return ObjectName is the JSR77 spec naming convention for Resource MBeans
     */
    public static ObjectName createResourceObjectName(String serverName, String resourceType, String keyName) {
        ObjectName objectName;
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(TYPE_SERVER, serverName);

        objectName = createObjectName(resourceType, keyName, props);

        return objectName;

    }
}
