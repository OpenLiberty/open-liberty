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
package com.ibm.ws.management.j2ee.mbeans;

/**
 *
 */
public interface J2EETypeConstants {
    String DOMAIN_NAME = "WebSphere";

    String KEY_J2EETYPE = "j2eeType";
    String KEY_NAME = "name";
    String WILD_CARD = ",*";

    // J2EE Top Level
    String J2EEDomain = "J2EEDomain";
    String J2EEServer = "J2EEServer";
    String JVM = "JVM";

    // J2EE Deployed Object
    String J2EEApplication = "J2EEApplication";
    String J2EEModule = "J2EEModule";
    String AppClientModule = "AppClientModule";
    String EJBModule = "EJBModule";
    String ResourceAdapterModule = "ResourceAdapterModule";
    String WebModule = "WebModule";

    // J2EE Resources
    String JCAResource = "JCAResource";
    String JTAResource = "JTAResource";
    String JDBCResource = "JDBCResource";
    String JMSResource = "JMSResource";
    String JNDIResource = "JNDIResource";
    String RMI_IIOPResource = "RMI_IIOPResource";
    String JavaMailResource = "JavaMailResource";
    String URLResource = "URLResource";

    // J2EE Domain Attributes
    String J2EE_DOMAIN_SERVERS = "servers";

    // J2EE Server Attributes
    String J2EE_SERVER_DEPLOYED_OBJECTS = "deployedObjects";
    String J2EE_SERVER_RESOURCES = "resources";
    String J2EE_SERVER_JAVA_VMS = "javaVMs";
    String J2EE_SERVER_VENDOR = "serverVendor";
    String J2EE_SERVER_VERSION = "serverVersion";

    // JVM Attributes
    String JVM_NODE = "node";
    String JVM_JAVA_VENDOR = "javaVendor";
    String JVM_JAVA_VERSION = "javaVersion";
}
