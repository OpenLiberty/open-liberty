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
package com.ibm.ws.security.jaas.common.internal;

import java.security.AccessController;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * This class collects references to com.ibm.ws.app.manager pids, which are what classProviderRef can refer to.
 * It republishes intermediate forms of ClassProvider, with dependencies on the remaining services that
 * are necessary for them to be usable for loading classes.
 *
 * The need to do this is a result of security eagerly loading everything. If that ever changes, consider
 * replacing the mechanism that is provided by this class.
 */
public class ClassProviderCoordinator {
    static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.get());

    private final ConcurrentHashMap<ServiceReference<?>, ClassProviderProxy> proxyClassProvidersForResourceAdapters = //
                    new ConcurrentHashMap<ServiceReference<?>, ClassProviderProxy>();

    protected void setClassProvider(ServiceReference<?> ref) throws InvalidSyntaxException {
        String classProviderId = (String) ref.getProperty("id");
        String extendsFactoryPid = (String) ref.getProperty("ibm.extends.source.factoryPid");
        if ("com.ibm.ws.jca.resourceAdapter".equals(extendsFactoryPid)) {
            String filter = "(&"
                            + FilterUtils.createPropertyFilter("objectClass", ClassProvider.class.getName())
                            + FilterUtils.createPropertyFilter("id", classProviderId)
                            + ")";

            String appMgrPid = (String) ref.getProperty("service.pid");
            ClassProviderProxy proxy = new ClassProviderProxy(appMgrPid);
            BundleContext bundleContext = FrameworkUtil.getBundle(ClassProviderCoordinator.class).getBundleContext();
            bundleContext.addServiceListener(proxy, filter);

            // If the above was registered just as we were adding the ServiceListener
            // and so the ServiceListener didn't get notified, then we need to register manually,
            Collection<ServiceReference<ClassProvider>> refs = bundleContext.getServiceReferences(ClassProvider.class, filter);
            if (!refs.isEmpty())
                proxy.register(refs.iterator().next());

            ClassProviderProxy previous = proxyClassProvidersForResourceAdapters.put(ref, proxy);
            if (previous != null) {
                bundleContext.removeServiceListener(proxy);
                previous.unregister();
            }
        }
    }

    protected void unsetClassProvider(ServiceReference<?> ref) {
        ClassProviderProxy proxy = proxyClassProvidersForResourceAdapters.remove(ref);
        if (proxy != null) {
            BundleContext bundleContext = FrameworkUtil.getBundle(ClassProviderCoordinator.class).getBundleContext();
            bundleContext.removeServiceListener(proxy);
            proxy.unregister();
        }
    }
}
