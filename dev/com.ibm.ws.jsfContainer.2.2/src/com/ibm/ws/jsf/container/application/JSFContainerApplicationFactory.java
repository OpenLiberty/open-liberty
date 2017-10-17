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
import com.ibm.ws.jsf.container.cdi.CDIJSFInitializer;

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
            // TODO check for user-defined app factory
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
                if (!initialized) {
                    try {
                        appName = InitialContext.doLookup("java:app/AppName");
                    } catch (NamingException e) {
                        throw new RuntimeException("[TODO_NLS] Unable to obtain application name from JSF application " + a, e);
                    }

                    if (log.isLoggable(Level.FINEST))
                        log.logp(Level.FINEST, JSFContainerApplicationFactory.class.getName(), "getApplication",
                                 "Performing first time initialization checks on: " + appName);

                    serviceabilityChecks();

                    // CDI will always be enabled with the jsfContainer feature
                    CDIJSFInitializer.initialize(a, appName);

                    if (JSFContainer.isBeanValidationEnabled())
                        JSFContainer.initializeBeanValidation();

                    initialized = true;
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

    private void serviceabilityChecks() {
        final String m = "serviceabilityChecks";
        // Log which JSF provider is being used
        if (log.isLoggable(Level.INFO))
            log.logp(Level.INFO, clazz, m, "[TODO_NLS] Initializing Liberty JSF integrations for " + providerType + " on application " + appName);

        if (!log.isLoggable(Level.WARNING))
            return;

        // Check version of JSF spec
        String apiVersion = javax.faces.application.ApplicationFactory.class.getPackage().getSpecificationVersion();
        if (apiVersion != null && !isVersionValid(apiVersion))
            log.logp(Level.WARNING, clazz, m,
                     "[TODO_NLS] The JSF specification API version available to application " + appName + " should be within [2.2,2.3) but was " + apiVersion);

        // Check version of JSF impl
        Class<?> appFactoryClass = delegate.getClass();
        while (appFactoryClass != Object.class
               && !appFactoryClass.getCanonicalName().equals(MYFACES_APP_FACTORY)
               && !appFactoryClass.getCanonicalName().equals(MOJARRA_APP_FACTORY))
            appFactoryClass = appFactoryClass.getSuperclass();
        String appFactoryClassName = appFactoryClass.getCanonicalName();
        if (appFactoryClassName.equals(MYFACES_APP_FACTORY) || appFactoryClassName.equals(MOJARRA_APP_FACTORY)) {
            String implVersion = appFactoryClass.getPackage().getSpecificationVersion();
            if (implVersion != null && !isVersionValid(implVersion))
                log.logp(Level.WARNING, clazz, m,
                         "[TODO_NLS] The JSF implementation version available to application " + appName + " should be within [2.2,2.3) but was " + implVersion);
        }
    }

    private static boolean isVersionValid(String version) {
        // A simple way of checking that version is within [2.2,2.3)
        return version.equals("2.2") || version.startsWith("2.2.");
    }

    private IllegalStateException noJsfProviderFound() {
        String message = "No JSF implementations found.  One of the following javax.faces.application.ApplicationFactory implementations must be available: "
                         + MOJARRA_APP_FACTORY + " or " + MYFACES_APP_FACTORY;
        if (log.isLoggable(Level.SEVERE))
            log.severe(message);
        return new IllegalStateException(message);
    }
}