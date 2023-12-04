/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mutiny.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.smallrye.mutiny.infrastructure.Infrastructure;

public class MutinyActivator implements BundleActivator {

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext ctx) throws Exception {
        // Call a method in order to initialize this class eagerly to avoid AccessControlExceptions
        // if it's initialized on demand in response to a user call
        Infrastructure.getMultiOverflowDefaultBufferSize();
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext ctx) throws Exception {
        // Do nothing
    }

}
