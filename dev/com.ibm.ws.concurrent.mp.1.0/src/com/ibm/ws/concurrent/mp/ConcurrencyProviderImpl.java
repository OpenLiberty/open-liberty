/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp;

import org.eclipse.microprofile.concurrent.ManagedExecutorBuilder;
import org.eclipse.microprofile.concurrent.ThreadContextBuilder;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProvider;
import org.eclipse.microprofile.concurrent.spi.ConcurrencyProviderRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Registers this implementation as the provider of MicroProfile Concurrency.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ConcurrencyProviderImpl implements ConcurrencyProvider {
    private ConcurrencyProviderRegistration registration;

    @Activate
    protected void activate() {
        registration = ConcurrencyProvider.register(this);
    }

    @Deactivate
    protected void deactivate() {
        registration.unregister();
    }

    @Override
    public ManagedExecutorBuilder newManagedExecutorBuilder() {
        // TODO ThreadContextProvider implementations are discovered here? Or upon builder.build()? Or upon using the executor (awkward).
        return null; // TODO
    }

    @Override
    public ThreadContextBuilder newThreadContextBuilder() {
        // TODO ThreadContextProvider implementations are discovered here?
        return new ThreadContextBuilderImpl();
    }
}