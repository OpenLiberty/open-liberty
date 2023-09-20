/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.optenliberty.microprofile.telemetry.internal.common.helpers;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.management.ServiceNotFoundException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.service.utils.FrameworkState;

public class OSGIHelpers {

    //OSGi Helper methods

    /**
     * Get the OSGi bundle context for the given class
     *
     * @param clazz the class to find the context for
     * @return the bundle context
     */
    private static BundleContext getBundleContext(Class<?> clazz) {
        BundleContext context = null; //we'll return null if not running inside an OSGi framework (e.g. unit test) or framework is shutting down
        if (FrameworkState.isValid()) {
            Bundle bundle = FrameworkUtil.getBundle(clazz);

            if (bundle != null) {
                context = AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> {
                    return bundle.getBundleContext();
                });
            }
        }
        return context;
    }

    /**
     * Find a service of the given type
     *
     * @param serviceClass The class of the required service
     * @return the service instance
     * @throws InvalidFrameworkStateRuntimeException if the server OSGi framework is being shutdown
     * @throws ServiceNotFoundException              if an instance of the requested service can not be found
     */
    public static <T> T getService(Class<T> serviceClass, Class<?> callingClass) throws InvalidFrameworkStateRuntimeException {
        BundleContext bundleContext = getBundleContext(callingClass);
        if (!FrameworkState.isValid()) {
            throw new InvalidFrameworkStateRuntimeException("Invalid OSGi Framework State");
        }

        ServiceReference<T> ref = AccessController.doPrivileged((PrivilegedAction<ServiceReference>) () -> {
            return bundleContext.getServiceReference(serviceClass);
        });

        T service = null;
        if (ref != null) {
            service = AccessController.doPrivileged((PrivilegedAction<T>) () -> {
                return bundleContext.getService(ref);
            });
        }

        if (service == null) {
            //One last check to make sure the framework didn't start to shutdown after we last looked
            if (!FrameworkState.isValid()) {
                throw new InvalidFrameworkStateRuntimeException("Invalid OSGi Framework State");
            } else {
                throw new InvalidFrameworkStateRuntimeException("Service " + serviceClass.getCanonicalName() + " not Found");
            }
        }
        return service;
    }

//RuntimeException so we can pass it out a doPriv.
    private static class InvalidFrameworkStateRuntimeException extends RuntimeException {

        /**
         * @param message
         */
        public InvalidFrameworkStateRuntimeException(String s) {
            super(s);
        }

        /**  */
        private static final long serialVersionUID = 1L;

    }

}
