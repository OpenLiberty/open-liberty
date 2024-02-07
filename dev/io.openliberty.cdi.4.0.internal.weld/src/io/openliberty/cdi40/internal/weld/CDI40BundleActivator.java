/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.weld;

import org.jboss.weld.lite.extension.translator.BuildServicesImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import jakarta.enterprise.inject.build.compatible.spi.BuildServices;

/**
 * Register Weld's implementation of BuildService as an OSGi Service
 */
public class CDI40BundleActivator implements BundleActivator {
    private ServiceRegistration<BuildServices> registration;

    @Override
    public void start(BundleContext ctx) throws Exception {
        registration = ctx.registerService(BuildServices.class, new BuildServicesImpl(), null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
