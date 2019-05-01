/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver;

/**
 *
 */
public class Activator implements BundleActivator {

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext context) throws Exception {
        context.registerService(FeatureResolver.class, new FeatureResolverImpl(), null);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext context) throws Exception {
        // nothing
    }

}
