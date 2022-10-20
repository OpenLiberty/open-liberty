/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.helper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;

public class InstrumenterActivator implements BundleActivator {
    //The J2S exception occurs due to a call during the initialization of Instrumenter and InstrumenterBuilder
    @Override
    public void start(BundleContext ctx) throws Exception {
        Class.forName(Instrumenter.class.getName());
        Class.forName(InstrumenterBuilder.class.getName());
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        // Unregister the EclipseLinke provider
    }
}
