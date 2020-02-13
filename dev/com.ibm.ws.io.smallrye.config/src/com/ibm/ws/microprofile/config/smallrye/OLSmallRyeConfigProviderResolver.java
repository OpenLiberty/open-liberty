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
package com.ibm.ws.microprofile.config.smallrye;

import java.io.IOException;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.smallrye.config.SmallRyeConfigProviderResolver;

@Component(service = ConfigProviderResolver.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigProviderResolver extends SmallRyeConfigProviderResolver { // implements ApplicationStateListener {
    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        ConfigProviderResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
        ConfigProviderResolver.setInstance(null);
    }
}
