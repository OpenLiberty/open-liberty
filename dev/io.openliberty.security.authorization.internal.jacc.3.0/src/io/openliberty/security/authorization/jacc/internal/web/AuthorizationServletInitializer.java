/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.web;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.authorization.jacc.internal.proxy.JakartaPolicyConfigFactoryProxy;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Listener for web applications to start and reads the init parameters for the web.xml
 * to configure that policy and configuration factories for Jakarta Authorization
 */
@Component(
           name = "io.openliberty.security.authorization.jacc.servlet.initializer",
           property = "service.vendor=IBM",
           configurationPolicy = IGNORE,
           immediate = true)
public class AuthorizationServletInitializer implements ServletContainerInitializer {

    private static final TraceComponent tc = Tr.register(AuthorizationServletInitializer.class);

    @Override
    @FFDCIgnore({ IllegalStateException.class, SecurityException.class })
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        PolicyFactory policyFactory = null;
        PolicyFactory previousPolicyFactory = null;
        boolean policyFactoryAdded = false;
        PolicyConfigurationFactory configFactory = null;
        PolicyConfigurationFactory previousConfigFactory = null;
        boolean configFactoryAdded = false;

        try {
            ClassLoader appClassLoader = servletContext.getClassLoader();

            // Set the PolicyFactory if there is one defined in the web.xml
            String policyFactoryName = servletContext.getInitParameter(PolicyFactory.FACTORY_NAME);
            if (policyFactoryName != null) {
                try {
                    previousPolicyFactory = PolicyFactory.getPolicyFactory();
                } catch (SecurityException se) {
                }

                policyFactory = loadClass(appClassLoader, policyFactoryName, PolicyFactory.class, previousPolicyFactory, servletContext);
                if (policyFactory != null) {
                    PolicyFactory.setPolicyFactory(policyFactory);
                    Tr.info(tc, "JACC_AUTHORIZATION_MODULE_CONFIGURED", "PolicyFactory", policyFactoryName, servletContext.getServletContextName());
                    policyFactoryAdded = true;
                }
            }

            // Set the PolicyConfigurationFactory if there is one defined in the web.xml
            String configFactoryName = servletContext.getInitParameter(PolicyConfigurationFactory.FACTORY_NAME);
            if (configFactoryName != null) {
                try {
                    previousConfigFactory = PolicyConfigurationFactory.get();
                } catch (IllegalStateException se) {
                    // expected if there isn't one configured
                }
                configFactory = loadClass(appClassLoader, configFactoryName, PolicyConfigurationFactory.class, previousConfigFactory, servletContext);
                if (configFactory != null) {
                    PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory);
                    configFactoryAdded = true;
                    JakartaPolicyConfigFactoryProxy configFactoryProxy = JakartaPolicyConfigFactoryProxy.getInstance();
                    configFactoryProxy.ensurePolicyConfigInitialized();
                    Tr.info(tc, "JACC_AUTHORIZATION_MODULE_CONFIGURED", "PolicyConfigurationFactory", configFactoryName, servletContext.getServletContextName());
                }
            }

        } finally {
            // If one or both of the Factories was configured, create a ServletListener to remove them when the servlet is destroyed
            if (policyFactoryAdded || configFactoryAdded) {
                servletContext.addListener(new AuthorizationServletListener(policyFactoryAdded ? policyFactory : null, //
                                configFactoryAdded ? configFactory : null, previousPolicyFactory, previousConfigFactory));
            }
        }
    }

    private <T> T loadClass(ClassLoader appClassLoader, String className, Class<T> classType, T factoryToWrap, ServletContext servletContext) {
        try {
            Class<?> loadedClass = appClassLoader.loadClass(className);
            Constructor<?>[] ctors = loadedClass.getConstructors();
            Constructor<?> defaultCtor = null;
            Constructor<?> wrapperCtor = null;
            for (Constructor<?> ctor : ctors) {
                Class<?>[] parmTypes = ctor.getParameterTypes();
                if (parmTypes.length == 0) {
                    defaultCtor = ctor;
                } else if (parmTypes.length == 1 && parmTypes[0] == classType) {
                    wrapperCtor = ctor;
                }
            }

            // If there is a constructor that wraps the previously set factory, use that one
            // as required by the specification.
            if (wrapperCtor != null) {
                return classType.cast(wrapperCtor.newInstance(factoryToWrap));
            }
            if (defaultCtor != null) {
                return classType.cast(defaultCtor.newInstance());
            }

            // If the expected constructors signatures were not found, it either means that there were
            // no constructors found or that the constructors that were found did not have the correct
            // arguments signatures.  Provide appropriate exceptions that are specific to each scenario.
            if (ctors.length == 0) {
                // If the class is not public, it will also show no constructors
                // Differentiate between the two in order to give the user additional
                // information to help debug.
                int classModifiers = loadedClass.getModifiers();
                if ((classModifiers & Modifier.PUBLIC) == 0) {
                    throw new IllegalAccessException("Class is not public");
                }
                throw new IllegalAccessException("No public constructors were found");
            }
            throw new IllegalArgumentException("Constructor with no arguments or with " + classType.getSimpleName() + " argument was not found");
        } catch (Throwable e) {
            Tr.error(tc, "JACC_AUTHORIZATION_MODULE_CREATION_FAILURE", classType.getSimpleName(), className, servletContext.getServletContextName(), e.toString());
            return null;
        }
    }
}
