/*******************************************************************************
 * Copyright (c) 2006,2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import java.security.AccessController;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;

/**
 * Manifested Constants used by the JPA Service implementation.
 */
public final class JPAConstants {
    // ********** JPA general, trace and bundling constants
    public static final String JPA_TRACE_GROUP = "JPA";
    public static final String JPABYTECODE_TRACE_GROUP = "JavaPersistenceByteCode";

    public static final String JPA_RESOURCE_BUNDLE_NAME = "com.ibm.ws.jpa.jpa";

    static final String PROVIDER_EXTENSION_NAME_SPACE = "com.ibm.ws.jpa";

    static final String PROVIDER_EXTENSION_POINT_ID = "com.ibm.ws.jpa.jpaextension";

    static final String EAR_SCOPE_MODULE_NAME = "EAR_Scope_Module";

    // ********** Java EE component context and namespace constants
    static final String JNDI_NAMESPACE_JAVA_COMP_ENV = "java:comp/env/"; // d408321
    static final String JNDI_NAMESPACE_JAVA_APP_ENV = "java:app/env/";

    static final String JNDI_TX_SYNC_REGISTRY = "java:comp/TransactionSynchronizationRegistry"; // d416151

    static final String JNDI_UOW_SYNC_REGISTRY = "java:comp/websphere/UOWSynchronizationRegistry"; // d472866.1

    // ********** JAXB persistence.xml v1.0 parsing constant          F1879-16302
    public static final String PERSISTENCE_10_XML_JAXB_PACKAGE_NAME = "com.ibm.ws.jpa.pxml10";

    // ********** JAXB persistence.xml v2.0 parsing constant          F1879-16302
    public static final String PERSISTENCE_20_XML_JAXB_PACKAGE_NAME = "com.ibm.ws.jpa.pxml20";

    // ********** System properties keys
    // Uuser-defined default JPA provider class name.
    static final String JPA_PROVIDER_SYSTEM_PROPERTY_NAME = "com.ibm.websphere.jpa.default.provider";

    // User-defined default JTA and non-JTA data sources.
    static final String JPA_JTA_DATASOURCE_SYSTEM_PROPERTY_NAME = "com.ibm.websphere.jpa.default.jta.datasource";

    static final String JPA_NONJTA_DATASOURCE_SYSTEM_PROPERTY_NAME = "com.ibm.websphere.jpa.default.nonjta.datasource";

    // Override all <exclude-unlisted-classes> semantics to mean
    // <exclude-unlisted-classes>true</exclude-unlisted-classes>
    // Applies only to persistence.xml files at the 1.0 level of the spec. F1879-16302
    static final boolean JPA_OVERRIDE_EXCLUDE_UNLISTED_CLASSES = Boolean.getBoolean(AccessController.doPrivileged(new SystemGetPropertyPrivileged("com.ibm.websphere.jpa.override.exclude.unlisted.classes", "false")).toLowerCase());

    // EntityManager pool capacity per PersistenceContext reference.      d510184
    static final String JPA_ENTITY_MANAGER_POOL_CAPACITY = "com.ibm.websphere.jpa.entitymanager.poolcapacity";

    // ********** Persistence provider related constants
    static final String PROVIDER_SPI_META_INF_RESOURCE_NAME = "META-INF/services/javax.persistence.spi.PersistenceProvider";

    public static final String PERSISTENCE_XML_RESOURCE_NAME = "META-INF/persistence.xml";

    // Default JPA EntityManager pool capacity
    static final int DEFAULT_EM_POOL_CAPACITY = 10;

    // ********** MetaData constants             //d496032.1
    public static final int EJB_MODULE_VERSION_3_0 = 30;
    // wsjpa properties
    static final String WSOPENJPA_PREFIX = "wsjpa";
    static final String WSOPENJPA_EMF_POOL_PROPERTY_NAME = WSOPENJPA_PREFIX + "." + "PooledFactory";

    /**
     * This property allows the user to explicitly exclude applications from JPA processing. This
     * is useful to legacy applications using hibernate. Currently there is a performance issue
     * in hibernate that affects application start time when hibernate code is triggered by jeeruntime
     * code to create an EntityManagerFactory when the application contains a persistence.xml
     * file. This code is not needed to run for legacy Hibernate-related applications that do not
     * use openJPA or other JPA provider. <p> //PM20625
     *
     * <B>Value:</B> com.ibm.websphere.persistence.ApplicationsExcludedFromJpaProcessing <p>
     *
     * <B>Usage:</B> Optional JEERuntime Property <p>
     *
     * <B>Property values:</B>
     * appName1:appName2:appName3...or * for all applications <p>
     **/
    public static final String EXCLUDE_JPA_PROCESSING_FOR = "com.ibm.websphere.persistence.ApplicationsExcludedFromJpaProcessing"; //PM20625

    /**
     * This property allows the user to disable EntityManagerFactory proxies for
     * non stateful session beans. This is useful for applications that need to
     * directly cast to vendor-specific EntityManagerFactory interfaces and that
     * cannot be changed to use JPAEMFactory.unwrap. <p>
     *
     * <B>Value:</B> com.ibm.websphere.persistence.useEntityManagerFactoryProxy <p>
     *
     * <B>Property values:</B> "true" (default) and "false"
     */
//   public static final String USE_EMF_PROXY = JPAJndiLookupObjectFactory.USE_EMF_PROXY; // d706751

}
