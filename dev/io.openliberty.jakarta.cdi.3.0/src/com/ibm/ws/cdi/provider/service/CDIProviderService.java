/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.provider.service;

import java.security.AccessController;
import java.security.PrivilegedAction;

import jakarta.enterprise.inject.spi.CDIProvider;
import jakarta.enterprise.inject.spi.CDI;

import org.osgi.service.component.annotations.Component;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
/**
 * This is a thin wrapper to get hold of the real CDIProvider. It is loaded via ServiceLoader to workaround https://github.com/eclipse-ee4j/cdi/issues/461
 */
public class CDIProviderService implements CDIProvider {

    private static volatile CDIProvider cdiProvider = null;

    /** {@inheritDoc} */
    @Override
    public CDI<Object> getCDI() {

        if (cdiProvider == null) {
            fetchCDIRuntime();
        }

        return cdiProvider.getCDI();
    }

    private synchronized void fetchCDIRuntime() {
        if (cdiProvider == null) {
            final Bundle bundle = FrameworkUtil.getBundle(CDIProvider.class);
            CDIProvider cdiProvider = AccessController.doPrivileged(new PrivilegedAction<CDIProvider>() {
                @Override
                public CDIProvider run() {
                    BundleContext bCtx = bundle.getBundleContext();
                    ServiceReference<CDIProvider> svcRef = bCtx.getServiceReference(CDIProvider.class);
                    return svcRef == null ? null : bCtx.getService(svcRef);
                }
            });
            if (cdiProvider == null) {
                throw new IllegalStateException("Failed to get the cdiProvider.");
            }
            this.cdiProvider = cdiProvider;
        }
    }

}

