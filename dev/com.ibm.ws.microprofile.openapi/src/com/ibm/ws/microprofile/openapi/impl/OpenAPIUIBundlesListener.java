/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * ServiceListener implementation that alerts OpenAPIUIBundleUpdater when all
 * the expected bundles are ready
 */
public class OpenAPIUIBundlesListener implements ServiceListener {

    private final ConcurrentHashMap<String, Boolean> expectedBundleNames;
    private final CountDownLatch countDownLatch;
    private final BundleContext bundleContext;

    public OpenAPIUIBundlesListener(Set<Bundle> openAPIUIBundles) throws InvalidSyntaxException {
        this.bundleContext = FrameworkUtil.getBundle(OpenAPIUIBundlesUpdater.class).getBundleContext();
        ConcurrentHashMap<String, Boolean> expectedBundleNames = new ConcurrentHashMap<>();
        for (Bundle bundle : openAPIUIBundles) {
            expectedBundleNames.put(bundle.getSymbolicName(), Boolean.TRUE);
        }
        this.expectedBundleNames = expectedBundleNames;
        this.countDownLatch = new CountDownLatch(expectedBundleNames.size());
    }

    private void removeIfExpectedBundle(ServiceReference<Bundle> ref) {
        String bundleKey = (String) ref.getProperty("web.module.key");
        if (bundleKey != null) {
            String bundleName = bundleKey.substring(0, bundleKey.indexOf('#'));
            if (expectedBundleNames.remove(bundleName) != null) {
                countDownLatch.countDown();
            }
        }
    }

    private void checkExistingServices(ServiceReference<Bundle>[] refs) {
        if (refs != null) {
            for (ServiceReference<Bundle> ref : refs) {
                removeIfExpectedBundle(ref);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void serviceChanged(ServiceEvent event) {
        ServiceReference<Bundle> ref = (ServiceReference<Bundle>) event.getServiceReference();
        removeIfExpectedBundle(ref);
    }

    @SuppressWarnings("unchecked")
    public void await() throws InterruptedException, InvalidSyntaxException {
        bundleContext.addServiceListener(this, "(&(objectClass=org.osgi.framework.Bundle)(installed.wab.contextRoot=*))");
        try {
            ServiceReference<Bundle>[] refs = (ServiceReference<Bundle>[]) bundleContext.getServiceReferences(Bundle.class.getName(), "(installed.wab.contextRoot=*)");
            checkExistingServices(refs);
            countDownLatch.await();
        } finally {
            bundleContext.removeServiceListener(this);
        }
    }
}
