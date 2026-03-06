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
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.authorization.jacc.internal.proxy.JakartaPolicyConfigFactoryProxy;
import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Listeners for web applications to start and reads the init parameters for the web.xml
 * to configure that policy and configuration for Jakarta Security
 */
@Component(
           name = "io.openliberty.security.authorization.jacc.servlet.initializer",
           property = "service.vendor=IBM",
           configurationPolicy = IGNORE,
           immediate = true)
public class AuthorizationServletInitializer implements ServletContainerInitializer {

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
            String policyFactoryName = servletContext.getInitParameter(PolicyFactory.FACTORY_NAME);
            if (policyFactoryName != null) {
                try {
                    previousPolicyFactory = PolicyFactory.getPolicyFactory();
                } catch (SecurityException se) {
                }

                policyFactory = loadClass(appClassLoader, policyFactoryName, PolicyFactory.class, previousPolicyFactory);
                if (policyFactory != null) {
                    PolicyFactory.setPolicyFactory(policyFactory);
                    policyFactoryAdded = true;
                }
            }
            String configFactoryName = servletContext.getInitParameter(PolicyConfigurationFactory.FACTORY_NAME);
            if (configFactoryName != null) {
                try {
                    previousConfigFactory = PolicyConfigurationFactory.get();
                } catch (IllegalStateException se) {
                    // expected if there isn't one configured
                }
                configFactory = loadClass(appClassLoader, configFactoryName, PolicyConfigurationFactory.class, previousConfigFactory);
                if (configFactory != null) {
                    PolicyConfigurationFactory.setPolicyConfigurationFactory(configFactory);
                    configFactoryAdded = true;
                    JakartaPolicyConfigFactoryProxy configFactoryProxy = JakartaPolicyConfigFactoryProxy.getInstance();
                    if (configFactoryProxy != null) {
                        configFactoryProxy.ensurePolicyConfigInitialized();
                    }
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

    private <T> T loadClass(ClassLoader appClassLoader, String className, Class<T> classType, T factoryToWrap) {
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

            if (wrapperCtor != null) {
                return classType.cast(wrapperCtor.newInstance(factoryToWrap));
            } else if (defaultCtor != null) {
                return classType.cast(defaultCtor.newInstance());
            }
            return null;
        } catch (Throwable e) {
            // FFDC and message
        }
        return null;
    }
}
