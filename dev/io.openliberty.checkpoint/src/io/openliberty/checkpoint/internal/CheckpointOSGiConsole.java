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
package io.openliberty.checkpoint.internal;

import static io.openliberty.checkpoint.spi.CheckpointPhase.CHECKPOINT_ACTIVE_FILTER;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@Component(property = { CheckpointHook.MULTI_THREADED_HOOK + ":Boolean=true" })
public class CheckpointOSGiConsole implements CheckpointHook {
    private final Bundle console;
    private final FrameworkWiring fwkWiring;

    @Activate
    public CheckpointOSGiConsole(@Reference(target = CHECKPOINT_ACTIVE_FILTER) CheckpointPhase phase, BundleContext context) {
        Requirement consolePkg = new Requirement() {

            @Override
            public Resource getResource() {
                return null;
            }

            @Override
            public String getNamespace() {
                return PackageNamespace.PACKAGE_NAMESPACE;
            }

            @Override
            public Map<String, String> getDirectives() {
                return Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=org.eclipse.equinox.console.common)");
            }

            @Override
            public Map<String, Object> getAttributes() {
                return Collections.emptyMap();
            }
        };

        this.fwkWiring = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
        this.console = this.fwkWiring.findProviders(consolePkg).stream() //
                        .findFirst() //
                        .map(BundleCapability::getRevision) //
                        .map(BundleRevision::getBundle)//
                        .orElse(null);
    }

    @Override
    public void prepare() {
        if (console == null) {
            return;
        }
        try {
            // stop the console so we can restore the telnet port on restart
            console.stop();
            // The console bundle has an issue some static vars that do not get
            // properly reset on stop.  Refresh the bundle to start fresh on restore
            CountDownLatch refreshDone = new CountDownLatch(1);
            fwkWiring.refreshBundles(Collections.singleton(console), (e) -> {
                refreshDone.countDown();
            });
            refreshDone.await(30, TimeUnit.SECONDS);
        } catch (BundleException e) {
            // ignore
            // auto FFDC is fine
        } catch (InterruptedException e1) {
            // auto FFDC is fine
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void restore() {
        if (console == null) {
            return;
        }
        try {
            console.start();
        } catch (BundleException e) {
            // ignore
            // auto FFDC is fine
        }
    }

}
