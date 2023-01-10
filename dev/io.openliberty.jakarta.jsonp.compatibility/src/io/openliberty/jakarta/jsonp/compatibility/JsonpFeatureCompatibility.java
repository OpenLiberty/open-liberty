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
package io.openliberty.jakarta.jsonp.compatibility;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class JsonpFeatureCompatibility implements BundleActivator {

    public static final String traceSpec = "JSONP";
    public static final String messageFile = "io.openliberty.jakarta.jsonp.compatibility.resources.JsonpMessages";

    private static final TraceComponent tc = Tr.register(JsonpFeatureCompatibility.class, traceSpec, messageFile);

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        if (TraceComponent.isAnyTracingEnabled()) {
            Tr.info(tc, "CWWKJ0351.feature.compatibility");
        }
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }

}
