/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jpa.hibernate.LibertyJtaPlatform;

/**
 * Common JPAProviderIntegration implementation that provides general integration required for known
 * JPA providers.
 */
public abstract class AbstractJPAProviderIntegration implements JPAProviderIntegration {
    private static final TraceComponent tc = Tr.register(AbstractJPAProviderIntegration.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    // Known JPA provider implementation classes
    protected static final String PROVIDER_ECLIPSELINK = "org.eclipse.persistence.jpa.PersistenceProvider";
    protected static final String PROVIDER_HIBERNATE = "org.hibernate.jpa.HibernatePersistenceProvider";
    protected static final String PROVIDER_OPENJPA = "org.apache.openjpa.persistence.PersistenceProviderImpl";

    /**
     * As persistence providers are first used, they are added to this list so that version information is only logged once for them.
     */
    protected final ConcurrentSkipListSet<String> providersUsed = new ConcurrentSkipListSet<String>();

    /**
     * @see com.ibm.ws.jpa.JPAProviderIntegration#disablePersistenceUnitLogging(java.util.Map)
     */
    @Override
    public void disablePersistenceUnitLogging(Map<String, Object> integrationProperties) {
        integrationProperties.put("eclipselink.logging.level", "OFF");
        // Since we're disabling logging, we don't want this to conflict with other normally configured PUs. Give it a random
        // session-name such that when/if another EMF is created for this PU the EclipseLink JPAInitializer cache
        integrationProperties.put("eclipselink.session-name", // value of PersistenceUnitProperties.SESSION_NAME
                                  "disabled-logging-pu" + UUID.randomUUID().toString());
    }

    /**
     * Log version information about the specified persistence provider, if it can be determined.
     *
     * @param providerName fully qualified class name of JPA persistence provider
     * @param loader       class loader with access to the JPA provider classes
     */
    @FFDCIgnore(Exception.class)
    private void logProviderInfo(String providerName, ClassLoader loader) {
        try {
            if (PROVIDER_ECLIPSELINK.equals(providerName)) {
                // org.eclipse.persistence.Version.getVersion(): 2.6.4.v20160829-44060b6
                Class<?> Version = loadClass(loader, "org.eclipse.persistence.Version");
                String version = (String) Version.getMethod("getVersionString").invoke(Version.newInstance());
                Tr.info(tc, "JPA_THIRD_PARTY_PROV_INFO_CWWJP0053I", "EclipseLink", version);
            } else if (PROVIDER_HIBERNATE.equals(providerName)) {
                // org.hibernate.Version.getVersionString(): 5.2.6.Final
                Class<?> Version = loadClass(loader, "org.hibernate.Version");
                String version = (String) Version.getMethod("getVersionString").invoke(null);
                Tr.info(tc, "JPA_THIRD_PARTY_PROV_INFO_CWWJP0053I", "Hibernate", version);
            } else if (PROVIDER_OPENJPA.equals(providerName)) {
                // OpenJPAVersion.appendOpenJPABanner(sb): OpenJPA #.#.#\n version id: openjpa-#.#.#-r# \n Apache svn revision: #
                StringBuilder version = new StringBuilder();
                Class<?> OpenJPAVersion = loadClass(loader, "org.apache.openjpa.conf.OpenJPAVersion");
                OpenJPAVersion.getMethod("appendOpenJPABanner", StringBuilder.class).invoke(OpenJPAVersion.newInstance(), version);
                Tr.info(tc, "JPA_THIRD_PARTY_PROV_INFO_CWWJP0053I", "OpenJPA", version);
            } else {
                Tr.info(tc, "JPA_THIRD_PARTY_PROV_NAME_CWWJP0052I", providerName);
            }
        } catch (Exception x) {
            Tr.info(tc, "JPA_THIRD_PARTY_PROV_NAME_CWWJP0052I", providerName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "unable to determine provider info", x);
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private static Class<?> loadClass(final ClassLoader cl, final String className) throws ClassNotFoundException {
        if (System.getSecurityManager() == null)
            return cl.loadClass(className);
        else
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return cl.loadClass(className);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getCause() instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) e.getCause();
                else
                    throw new RuntimeException(e);
            }
    }

    @Override
    public void moduleStarting(ModuleInfo moduleInfo) {
    }

    @Override
    public void moduleStarted(ModuleInfo moduleInfo) {
    }

    @Override
    public void moduleStopping(ModuleInfo moduleInfo) {
    }

    @Override
    public void moduleStopped(ModuleInfo moduleInfo) {
    }

    /**
     * @see com.ibm.ws.jpa.JPAProviderIntegration#supportsEntityManagerPooling()
     */
    @Override
    public boolean supportsEntityManagerPooling() {
        return false;
    }

    /**
     * @see com.ibm.ws.jpa.JPAProvider#addIntegrationProperties(java.util.Properties)
     */
    @FFDCIgnore(ClassNotFoundException.class)
    @Override
    public void updatePersistenceProviderIntegrationProperties(PersistenceUnitInfo puInfo, java.util.Map<String, Object> props) {
        String providerName = puInfo.getPersistenceProviderClassName();
        if (PROVIDER_ECLIPSELINK.equals(providerName)) {
            props.put("eclipselink.target-server", "WebSphere_Liberty");
            if (puInfo instanceof com.ibm.ws.jpa.management.JPAPUnitInfo) {
                props.put("eclipselink.application-id", ((com.ibm.ws.jpa.management.JPAPUnitInfo) puInfo).getApplName());
            }

            Properties properties = puInfo.getProperties();
            /*
             * Section 4.8.5 of the JPA Specification:
             * If SUM, AVG, MAX, or MIN is used, and there are no values
             * to which the aggregate function can be applied, the result of
             * the aggregate function is NULL.
             *
             * Set this property to so that EclipseLink does not return null by default
             *
             * JPA 3.0: Do not force this override with JPA 3.0 and later.
             */
            if (!properties.containsKey("eclipselink.allow-null-max-min") &&
                JPAAccessor.getJPAComponent().getJPAVersion().lesserThan(JPAVersion.JPA30)) {
                props.put("eclipselink.allow-null-max-min", "false");
            }

            /*
             * EclipseLink Bug 567891: Case expressions that should return boolean instead
             * return integer values. The JPA spec is too vague to change this, so we set this
             * property to be safe for customers.
             *
             * Set this property to `false` so that EclipseLink will return the same integer
             * value it has always returned for CASE expressions and not change behavior
             *
             * NOTE: This property is applicable for JPA 21 & JPA 22. JPAVersion > JPA22 has changed
             * behavior by default
             */
            if (!properties.containsKey("eclipselink.sql.allow-convert-result-to-boolean") &&
                JPAAccessor.getJPAComponent().getJPAVersion().equals(JPAVersion.JPA22)) {
                props.put("eclipselink.sql.allow-convert-result-to-boolean", "false");
            }
            // The property is named differently for EclipseLink 2.6
            if (!properties.containsKey("eclipselink.allow-result-type-conversion") &&
                JPAAccessor.getJPAComponent().getJPAVersion().equals(JPAVersion.JPA21)) {
                props.put("eclipselink.allow-result-type-conversion", "false");
            }

            /*
             * EclipseLink Bug 559307: EclipseLink on all versions can dead-lock forever.
             * This property was added as a new feature in EclipseLink 3.0. However, the property
             * currently defaults to `true` and causes performance regression.
             *
             * Set this property to `false` so that EclipseLink will disable the debug/trace which
             * reduces performance.
             *
             * NOTE: This property is only applicable for JPA 30. TODO: Setting this property can be
             * removed from here after updating JPA 3.0 to >= EclipseLink 3.0.1
             */
            if (!properties.containsKey("eclipselink.concurrency.manager.allow.readlockstacktrace") &&
                JPAAccessor.getJPAComponent().getJPAVersion().equals(JPAVersion.JPA30)) {
                props.put("eclipselink.concurrency.manager.allow.readlockstacktrace", "false");
            }
        } else if (PROVIDER_HIBERNATE.equals(providerName)) {
            // Hibernate had vastly outdated built-in knowledge of WebSphere API, until version 5.2.13+ and 5.3+.
            // If the version of Hibernate has the Liberty JtaPlatform, use it
            // otherwise, tell Hibernate to use a proxy implementation of JtaPlatform that we will provide based on the WAS transaction manager
            ClassLoader loader = puInfo.getClassLoader();
            if (isWebSphereLibertyJtaPlatformAvailable(loader)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Detected WebSphereLibertyJtaPlatform, not applying dynamic proxy JtaPlatform.");
            } else {
                try {
                    Class<?> JtaPlatform = loadClass(loader, "org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform");
                    Object libertyJtaPlatform = Proxy.newProxyInstance(loader, new Class[] { JtaPlatform }, new LibertyJtaPlatform());
                    props.put("hibernate.transaction.jta.platform", libertyJtaPlatform);
                } catch (ClassNotFoundException x) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "Unable to provide JtaPlatform for Liberty TransactionManager to Hibernate", x);
                }
            }
        }

        // Log third party provider name and version info once per provider
        if (providersUsed.add(providerName))
            logProviderInfo(providerName, puInfo.getClassLoader());
    };

    /**
     * @see com.ibm.ws.jpa.JPAProvider#modifyPersistenceUnitProperties(java.lang.String, java.util.Properties)
     */
    @Override
    public void updatePersistenceUnitProperties(String providerClassName, Properties props) {
    }

    /**
     * As of Hibernate 5.2.13+ and 5.3+ built-in knowledge of Liberty's transaction integration was delivered as:
     * org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform
     * If this class is available, we do not need to set our dynamic proxy instance.
     */
    @FFDCIgnore(ClassNotFoundException.class)
    private boolean isWebSphereLibertyJtaPlatformAvailable(ClassLoader loader) {
        try {
            loadClass(loader, "org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform");
            return true;
        } catch (ClassNotFoundException notFound) {
            return false;
        }
    }
}
