/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.restfulWS.client.ClientAsyncTaskWrapper;
import io.opentelemetry.context.Context;

/**
 * Ensures that the OTel context is used when either JAX-RS client or MP Rest Client makes an async request.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class TelemetryClientAsyncTaskWrapper implements ClientAsyncTaskWrapper {

    /** {@inheritDoc} */
    @Override
    public Runnable wrap(Runnable r) {
        return Context.current().wrap(r);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Callable<T> wrap(Callable<T> c) {
        return Context.current().wrap(c);
    }

    @Override
    public <T> Supplier<T> wrap(Supplier<T> s) {
        return Context.current().wrapSupplier(s);
    }

}
