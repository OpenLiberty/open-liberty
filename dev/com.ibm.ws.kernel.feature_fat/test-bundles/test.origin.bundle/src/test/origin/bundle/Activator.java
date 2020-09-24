/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.origin.bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 *
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        new Thread(() -> testOriginBundle(context), "Origin Bundle Test").start();
    }

    private void testOriginBundle(BundleContext context) {
        try {
            final Set<Bundle> tracked = Collections.synchronizedSet(new HashSet<Bundle>());

            Set<Bundle> origins = new HashSet<>();
            for (int i = 0; i < 10; i++) {
                origins.add(installOrigin(i, tracked, context));
            }
            final CountDownLatch allTrackedRemoved = new CountDownLatch(tracked.size());
            // Sleeping to make sure the system has had time to successfully track
            // the origin bundles and the bundles the origin bundles have installed.
            // The bundle events are asynchronous so we need to make sure enough time
            // has passed to allow the system to process the events.
            Thread.sleep(5000);
            context.addBundleListener(new BundleListener() {
                @Override
                public void bundleChanged(BundleEvent event) {
                    if (BundleEvent.UNINSTALLED == event.getType()) {
                        if (tracked.remove(event.getBundle())) {
                            System.out.println("Uninstalled tracked bundle: " + event.getBundle().getSymbolicName());
                            allTrackedRemoved.countDown();
                        }
                    }
                }
            });

            for (Bundle bundle : origins) {
                bundle.uninstall();
            }
            allTrackedRemoved.await(30, TimeUnit.SECONDS);
            if (tracked.isEmpty()) {
                System.out.println("BundleInstallOriginTest: PASSED");
            } else {
                threadDump(context);
                System.out.println("BundleInstallOriginTest: FAILED");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void threadDump(BundleContext context) throws Exception {
        Class<?> threadInfoReportClass = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).loadClass("org.eclipse.osgi.framework.util.ThreadInfoReport");
        Exception e = (Exception) threadInfoReportClass.getConstructor(String.class).newInstance(new Object[] { null });
        e.printStackTrace();
    }

    private Bundle installOrigin(int originId, Set<Bundle> tracked, BundleContext context) throws BundleException, IOException {
        String originName = "origin." + originId;
        // first install another bundle so we can get its BundleContext
        Bundle origin = context.installBundle(originName, createBundle(originName));
        System.out.println("Installed origin bundle: " + originName);
        origin.adapt(BundleStartLevel.class).setStartLevel(1);
        origin.start();
        BundleContext originContext = origin.getBundleContext();
        // now use the originContext to install other bundles
        for (int i = 0; i < 10; i++) {
            String trackedName = originName + ".tracked." + i;
            tracked.add(originContext.installBundle(trackedName, createBundle(trackedName)));
            System.out.println("Installed tracked bundle: " + trackedName);
        }
        return origin;
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

    private InputStream createBundle(String symbolicName) throws IOException {
        Manifest m = new Manifest();
        Attributes attributes = m.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        ByteArrayOutputStream bundleContent = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(bundleContent, m);
        jos.flush();
        jos.close();
        return new ByteArrayInputStream(bundleContent.toByteArray());
    }
}
