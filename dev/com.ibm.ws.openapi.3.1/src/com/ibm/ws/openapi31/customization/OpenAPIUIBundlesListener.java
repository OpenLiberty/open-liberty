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
package com.ibm.ws.openapi31.customization;

import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * ServiceListener implementation that alerts OpenAPIUIBundleUpdater when all
 * the expected bundles are ready
 */
public class OpenAPIUIBundlesListener implements ServiceListener {

    private final Set<String> expectedBundleNames;
    private int foundBundles = 0;

    public OpenAPIUIBundlesListener(Set<String> expectedBundleNames) {
        this.expectedBundleNames = expectedBundleNames;
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<Bundle> sb = (ServiceReference<Bundle>) event.getServiceReference();
        String bundleKey = (String) sb.getProperty("web.module.key");
        String bundleName = bundleKey.substring(0, bundleKey.indexOf('#'));
        if (expectedBundleNames.contains(bundleName))
            foundBundles++;
        if (foundBundles == expectedBundleNames.size())
            OpenAPIUIBundlesUpdater.unlockThread();
    }

}
