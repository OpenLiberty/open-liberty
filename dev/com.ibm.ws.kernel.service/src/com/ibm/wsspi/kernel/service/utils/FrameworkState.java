/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * Simple utility for querying the state of the framework for runtime operations.
 * <p>
 * The OSGi framework system bundle is the single best indicator of when the framework
 * is in the process of stopping: OSGi stop semantics dictate that the framework
 * change the system bundle's state to STOPPING before it begins stopping
 * bundles.
 * <p>
 * The interrelationship between declared services (DS or Blueprint) means that
 * service A might be deactivated because a service it requires from another bundle
 * has gone away (because that bundle has stopped) before its own bundle has stopped.
 * <p>
 * This utility can be used to curtail scheduling additional asynchronous work
 * if the framework is stopping.
 *
 */
public class FrameworkState {
    private final static class BundleHolder {
        final static Bundle systemBundle;
        static {
            Bundle b = FrameworkUtil.getBundle(FrameworkState.class);
            if (b != null) {
                systemBundle = b.getBundleContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            } else {
                systemBundle = null; // only in non-osgi env
            }
        }
    }

    /**
     * @return True if the framework is in a viable running state.
     *         Will return true when called outside of the osgi environment
     */
    public static boolean isValid() {
        if (BundleHolder.systemBundle != null) {
            switch (BundleHolder.systemBundle.getState()) {
                case Bundle.STOPPING:
                case Bundle.UNINSTALLED:
                    return false;
            }
        }
        return true;
    }

    /**
     * @return True if the framework is in the process of stopping,
     *         or has already stopped. If this method is called outside
     *         of an OSGi context, it will return false.
     */
    public static boolean isStopping() {
        if (BundleHolder.systemBundle != null) {
            switch (BundleHolder.systemBundle.getState()) {
                case Bundle.STOPPING:
                case Bundle.UNINSTALLED:
                    return true;
            }
        }
        return false;
    }
}
