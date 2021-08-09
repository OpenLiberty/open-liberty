/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.apache.aries.util.tracker;


import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @param <T>
 * 
 */
public final class RecursiveBundleTracker extends BundleTracker {

    //only process events for the composites once they are at least resolved
    private static final int compositeMask = Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE;

    private final int stateMask;
    private final BundleTrackerCustomizer customizer;

    public RecursiveBundleTracker(BundleContext ctx, int stateMask, BundleTrackerCustomizer customizer) {
        super(getSystemBundleContext(ctx), stateMask, customizer);
        //this tracks bundles on this bundle context
        this.stateMask = stateMask;
        this.customizer = customizer;
    }

    private static BundleContext getSystemBundleContext(BundleContext ctx) {
        // Aries subsystems hides no bundles from the system bundle context
        // If we can depend on that detail it is super simple to just use the
        // system bundle's context to track all bundles in the framework, including
        // the bundles contained in scoped subsystems.

        // NOTE that the system bundle location is used to find the system bundle
        // this is to guarantee we find the system bundle no matter what bundle FindHooks 
        // are filtering out.
        Bundle systemBundle = ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        if (systemBundle == null) {
            // this is really a framework error and should NEVER happen!
            // TODO perhaps an illegal state exception should be thrown.
            return ctx;
        }
        return systemBundle.getBundleContext();
    }
}
