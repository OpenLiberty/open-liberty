/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.internal;

import java.io.PrintWriter;
import java.security.AccessController;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.logging.Introspector;

/**
 * Introspector for persistent executors.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class PersistentExecutorIntrospector implements Introspector {
    @Override
    @Trivial
    public String getIntrospectorName() {
        return "PersistentExecutorIntrospector";
    }

    @Override
    @Trivial
    public String getIntrospectorDescription() {
        return "Persistent timers/tasks diagnostics";
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        SecureAction priv = AccessController.doPrivileged(SecureAction.get());
        BundleContext bundleContext = priv.getBundleContext(FrameworkUtil.getBundle(getClass()));

        for (ServiceReference<PersistentExecutor> ref : priv.getServiceReferences(bundleContext, PersistentExecutor.class, "(!(com.ibm.wsspi.resource.ResourceFactory=true))")) {
            PersistentExecutorImpl executor = (PersistentExecutorImpl) priv.getService(bundleContext, ref);
            if (executor == null) {
                String displayId = (String) ref.getProperty("config.displayId");
                String name = displayId.contains("]/persistentExecutor[") ? displayId : (String) ref.getProperty("id");
                if (name == null)
                    name = (String) ref.getProperty("jndiName");
                out.println("PersistentExecutor " + name + " is not available");
                out.println("Properties: " + ref.getProperties());
                out.println();
            } else {
                executor.introspect(out);
            }
        }

        ServiceReference<ApplicationTracker> appTrackerRef = bundleContext.getServiceReference(ApplicationTracker.class);
        bundleContext.getService(appTrackerRef);

        ApplicationTracker appTracker = priv.getService(bundleContext, ApplicationTracker.class);
        appTracker.introspect(out);
    }
}
