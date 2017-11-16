/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.application;

import static com.ibm.ws.jsf.container.JSFContainer.MOJARRA_APP_FACTORY;
import static com.ibm.ws.jsf.container.JSFContainer.MYFACES_APP_FACTORY;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.jsf.container.JSFContainer;
import com.ibm.ws.jsf.container.JSFContainer.JSF_PROVIDER;
import com.ibm.ws.jsf.container.Messages;
import com.ibm.ws.jsf.container.cdi.CDIJSFInitializer;

/**
 * This class is the main entry point for the JSF Container feature. A jar containing this class
 * with a META-INF/service/ referencing this class as an implementation of ApplicationFactory will
 * be applied to the application classloader. At that point, the JSF application will use this
 * custom ApplicationFactory to get a hold of the javax.faces.application.Application object and
 * register it with CDI (and Bean Validation, if enabled). This ApplicationFactory is capable of
 * delegating to either the MyFaces or the Mojarra implementation.
 */
public class JSFContainerApplicationFactory extends ApplicationFactory {

    private static final String clazz = JSFContainerApplicationFactory.class.getCanonicalName();
    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf.container.application");

    private final ApplicationFactory delegate;
    private final JSF_PROVIDER providerType;
    private volatile boolean initialized = false;
    private volatile String appName = null;

    public JSFContainerApplicationFactory() {
        try {
            providerType = JSFContainer.getJSFProvider();
            delegate = (providerType == JSF_PROVIDER.MOJARRA) ? //
                            (ApplicationFactory) Class.forName(MOJARRA_APP_FACTORY).newInstance() : //
                            (ApplicationFactory) Class.forName(MYFACES_APP_FACTORY).newInstance();
        } catch (ReflectiveOperationException e) {
            throw noJsfProviderFound();
        }
    }

    @Override
    public Application getApplication() {
        Application a = delegate.getApplication();
        // Perform lazy initialization of CDI and Bval integration because this is
        // the earliest point where we have a valid reference to an Application object
        if (!initialized) {
            synchronized (this) {
                final String m = "getApplication";
                if (!initialized) {
                    try {
                        // Need to get the application name in order to register with CDI,
                        // so use JNDI to get the application name
                        appName = InitialContext.doLookup("java:app/AppName");
                    } catch (NamingException e) {
                        throw new RuntimeException(Messages.get("jsf.container.no.app.name", a.toString()), e);
                    }

                    if (log.isLoggable(Level.FINEST))
                        log.logp(Level.FINEST, clazz, m,
                                 "Performing first time initialization checks on: " + appName);

                    serviceabilityChecks();

                    // CDI will always be enabled with the jsfContainer feature
                    CDIJSFInitializer.initialize(a, appName);

                    if (JSFContainer.isBeanValidationEnabled())
                        JSFContainer.initializeBeanValidation();

                    initialized = true;
                    if (log.isLoggable(Level.INFO))
                        log.logp(Level.INFO, clazz, m, Messages.get("jsf.container.init", providerType.toString(), appName));
                }
            }
        }
        return a;
    }

    @Override
    public void setApplication(Application application) {
        delegate.setApplication(application);
    }

    @Override
    public ApplicationFactory getWrapped() {
        return delegate.getWrapped();
    }

    /**
     * Get a hold of the archives provides JSF spec API (specifically javax.faces.application.ApplicationFactory)
     * and make sure the 'Specification-Version' header in the archive manifest is valid.
     * Also do the same check for the JSF implementation (either MYFACES_APP_FACTORY or MOJARRA_APP_FACTORY)
     * If the API or Impl versions are bad, blow up.
     */
    private void serviceabilityChecks() {
        final String m = "serviceabilityChecks";

        // Check version of JSF spec
        String apiVersion = javax.faces.application.ApplicationFactory.class.getPackage().getSpecificationVersion();
        if (apiVersion != null && !isVersionValid(apiVersion)) {
            IllegalStateException ex = new IllegalStateException(Messages.get("jsf.container.bad.spec.api.version", appName, "[2.2,2.3)", apiVersion));
            if (log.isLoggable(Level.SEVERE))
                log.logp(Level.SEVERE, clazz, m, ex.getMessage(), ex);
            throw ex;
        }

        // Check version of JSF impl
        Class<?> appFactoryClass = delegate.getClass();
        while (appFactoryClass != Object.class
               && !appFactoryClass.getCanonicalName().equals(MYFACES_APP_FACTORY)
               && !appFactoryClass.getCanonicalName().equals(MOJARRA_APP_FACTORY))
            appFactoryClass = appFactoryClass.getSuperclass();
        String appFactoryClassName = appFactoryClass.getCanonicalName();
        if (appFactoryClassName.equals(MYFACES_APP_FACTORY) || appFactoryClassName.equals(MOJARRA_APP_FACTORY)) {
            String implVersion = appFactoryClass.getPackage().getSpecificationVersion();
            if (implVersion != null && !isVersionValid(implVersion)) {
                IllegalStateException ex = new IllegalStateException(Messages.get("jsf.container.bad.impl.version", appName, "[2.2,2.3)", implVersion));
                if (log.isLoggable(Level.SEVERE))
                    log.logp(Level.SEVERE, clazz, m, ex.getMessage(), ex);
                throw ex;
            }
        }
    }

    private static boolean isVersionValid(String version) {
        // A simple way of checking that version is within [2.2,2.3)
        return version.equals("2.2") || version.startsWith("2.2.");
    }

    private IllegalStateException noJsfProviderFound() {
        String message = Messages.get("jsf.container.no.jsf.impl", appName, "[ " + MOJARRA_APP_FACTORY + ", " + MYFACES_APP_FACTORY + " ]");
        IllegalStateException e = new IllegalStateException(message);
        if (log.isLoggable(Level.SEVERE))
            log.logp(Level.SEVERE, clazz, "noJsfProviderFound", message, e);
        return e;
    }
}