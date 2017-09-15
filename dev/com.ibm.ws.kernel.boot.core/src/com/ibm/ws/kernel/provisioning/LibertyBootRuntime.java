/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.VersionRange;

/**
 * A service registered by liberty boot that is used to install bundles
 * available from the liberty boot runtime. These typically are bundles
 * that are on the class path.
 */
public interface LibertyBootRuntime {
    /**
     * Discovers and installs a boot bundle which has the specified
     * symbolic name and has a bundle version which matches the
     * specified range. The location used to install the bundle
     * is unspecified. The location prefix specified will be
     * used as the prefix for the bundle location.
     * <p>
     * A {@code null} value is returned if no boot bundle is found
     * that matches the symbolic name and version range.
     *
     * @param symbolicName the symbolic name of the boot bundle
     * @param range the version range the boot bundle version must match
     * @param bundlePrefix the prefix that will be used for the bundle location
     * @return the boot bundle that got installed or {@code null} if not found.
     * @throws BundleException if an error occurred installing the boot bundle.
     */
    Bundle installBootBundle(String symbolicName, VersionRange range, String locationPrefix) throws BundleException;
}
