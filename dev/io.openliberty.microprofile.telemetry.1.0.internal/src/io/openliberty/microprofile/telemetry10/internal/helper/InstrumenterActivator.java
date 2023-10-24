/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry10.internal.helper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class InstrumenterActivator implements BundleActivator {

    private static final TraceComponent tc = Tr.register(InstrumenterActivator.class);

    //The J2S exception occurs due to a call during the initialization of Instrumenter and InstrumenterBuilder
    @Override
    public void start(BundleContext ctx) throws Exception {
        Class.forName(Instrumenter.class.getName());
        Class.forName(InstrumenterBuilder.class.getName());
        // Eagerly initialize context classes to avoid J2S exceptions
        Context.root();
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
        // Unregister the EclipseLinke provider
    }
}
