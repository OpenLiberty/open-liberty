/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.restfulWS.client.internal;

import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

/**
 * ServiceActivator is used to initialize global configurations for RESTEasy.
 * This activator ensures early initialization of the RuntimeDelegate.
 * 
 */
@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class ServiceActivator {

    /**
     * Called by declarative services to activate service.
     */
    @Activate
    protected void activate(ComponentContext cc) {
        RuntimeDelegate.setInstance(new ResteasyProviderFactoryImpl());
    }

    /**
     * Called by declarative services to deactivate service.
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        RuntimeDelegate.setInstance(null);
    }
}
