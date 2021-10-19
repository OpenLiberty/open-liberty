/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Utility class for common OSGi-related functions.
 */
public class SecurityOSGiUtils {

    /**
     * Uses the <code>BundleContext</code> of the <code>callingClass</code> argument to look up the <code>serviceClass</code>
     * service instance. <b>Java 2 Security note:</b> This typically will need to be called within a PrivilegedAction instance.
     */
    public static <T> T getService(Class<?> callingClass, Class<T> serviceClass) {
        BundleContext bundleContext = getBundleContext(callingClass);
        return getService(bundleContext, serviceClass);
    }

    static <T> BundleContext getBundleContext(Class<T> clazz) {
        BundleContext context = null;
        if (FrameworkState.isValid()) {
            Bundle bundle = FrameworkUtil.getBundle(clazz);
            if (bundle != null) {
                context = bundle.getBundleContext();
            }
        }
        return context;
    }

    static <T> T getService(BundleContext bundleContext, Class<T> serviceClass) {
        if (!FrameworkState.isValid() || bundleContext == null) {
            return null;
        }
        ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
        T service = null;
        if (ref != null) {
            service = bundleContext.getService(ref);
        }
        return service;
    }

}
