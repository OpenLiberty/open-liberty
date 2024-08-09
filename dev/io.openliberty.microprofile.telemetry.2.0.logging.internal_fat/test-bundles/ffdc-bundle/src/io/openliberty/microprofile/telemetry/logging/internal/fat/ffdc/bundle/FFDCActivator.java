/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal.fat.ffdc.bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.ffdc.FFDCFilter;

public class FFDCActivator implements BundleActivator {

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext arg0) throws Exception {
        try {
            throw new IllegalArgumentException("FFDC_TEST_BUNDLE_START");
        } catch (IllegalArgumentException e) {
            FFDCFilter.processException(e, getClass().getName(), "24");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext arg0) throws Exception {
    }

}
