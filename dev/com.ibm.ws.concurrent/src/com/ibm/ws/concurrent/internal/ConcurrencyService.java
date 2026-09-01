/*******************************************************************************
 * Copyright (c) 2020,2026 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import java.security.AccessController;
import java.util.Collection;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.classloading.ClassLoaderIdentifierService;
import com.ibm.ws.concurrent.TriggerService;
import com.ibm.ws.concurrent.ext.ConcurrencyExtensionProvider;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, //
           service = { ConcurrencyService.class,
                       ApplicationMetaDataListener.class,
                       ApplicationStateListener.class })
public class ConcurrencyService implements //
                ApplicationMetaDataListener, //
                ApplicationStateListener {

    private static final String FILTER_MES_EXCLUDE_OSGI_JNDI = //
                    "(&(service.factoryPid=com.ibm.ws.concurrent.managed*ExecutorService)(!(osgi.jndi.service.name=*)))";
    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());
    private static final TraceComponent tc = Tr.register(ConcurrencyService.class);

    @Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    ConcurrencyExtensionProvider extensionProvider;

    @Reference
    ClassLoaderIdentifierService classLoaderIdentifierService;

    @Reference
    TriggerService triggerSvc;

    @Override
    @Trivial
    public void applicationMetaDataCreated(MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationMetaDataCreated: " + event.getMetaData().getJ2EEName());
    }

    @Override
    @Trivial
    public void applicationMetaDataDestroyed(MetaDataEvent<ApplicationMetaData> event) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationMetaDataDestroyed: " +
                               event.getMetaData().getJ2EEName());

        Bundle bundle = FrameworkUtil.getBundle(getClass());
        BundleContext bc = bundle == null ? null : priv.getBundleContext(bundle);
        Collection<ServiceReference<ManagedExecutorService>> refs;
        try {
            refs = priv.getServiceReferences(bc,
                                             ManagedExecutorService.class,
                                             FILTER_MES_EXCLUDE_OSGI_JNDI);
            for (ServiceReference<ManagedExecutorService> ref : refs) {
                ManagedExecutorService executor = priv.getService(bc, ref);
                if (executor instanceof ManagedExecutorServiceImpl) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "purge futures list for " + executor);
                    ((ManagedExecutorServiceImpl) executor).purgeFutures();
                }
            }
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never occur because a valid filter is hard-coded
        }
    }

    @Override
    @Trivial
    public void applicationStarted(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationStarted " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');
    }

    @Override
    @Trivial
    public void applicationStarting(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationStarting " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');
    }

    @Override
    @Trivial
    public void applicationStopping(ApplicationInfo appInfo) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "applicationStopping " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');

        // Use deployment name but fall back to generated name
        String appName = appInfo.getDeploymentName() == null //
                        ? appInfo.getName() //
                        : appInfo.getDeploymentName();

        Bundle bundle = FrameworkUtil.getBundle(getClass());
        BundleContext bc = bundle == null ? null : priv.getBundleContext(bundle);
        Collection<ServiceReference<ManagedExecutorService>> refs;
        try {
            refs = priv.getServiceReferences(bc,
                                             ManagedExecutorService.class,
                                             FILTER_MES_EXCLUDE_OSGI_JNDI);
            for (ServiceReference<ManagedExecutorService> ref : refs) {
                ManagedExecutorService executor = priv.getService(bc, ref);
                if (executor instanceof ManagedExecutorServiceImpl) {
                    ((ManagedExecutorServiceImpl) executor).cancelFutures(appName);
                }
            }
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never occur
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "applicationStopping");
    }

    @Override
    @Trivial
    public void applicationStopped(ApplicationInfo appInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationStoppied " + appInfo.getDeploymentName() +
                               " (" + appInfo.getName() + ')');
    }

    /**
     * Determines the name of the application that is running on the thread.
     * If unavailable, determines the name of the application that provides the
     * given class. Returns null if an application name cannot be determined.
     *
     * @param taskClass class of a task submitted by the application
     * @return the application name or null if unable to determine
     */
    @Trivial
    String findAppName(Class<?> taskClass) {
        // Primary: use the component metadata already on the thread. This is
        // available whenever findAppName is called during task submission from
        // application code, which is the normal case.
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl //
                        .getComponentMetaDataAccessor() //
                        .getComponentMetaData();
        if (cmd != null) {
            String appName = cmd.getJ2EEName().getApplication();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, taskClass.getName() +
                                   " submitted by application " + appName);
            return appName;
        }

        // Fallback: infer the application name from the class loader of the
        // task class. The identifier string for an application class loader has
        // the form "domain:appName#moduleName", so the app name is the portion
        // between the first colon and the first '#' (or end of string).
        for (ClassLoader cl = taskClass.getClassLoader(); //
                        cl != null; //
                        cl = cl.getParent()) {
            String identifier = classLoaderIdentifierService //
                            .getClassLoaderIdentifier(cl);
            if (identifier != null) {
                int colon = identifier.indexOf(':');
                if (colon >= 0) {
                    int hash = identifier.indexOf('#', colon);
                    String appName = identifier //
                                    .substring(colon + 1,
                                               hash > 0 ? hash : identifier.length());
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, taskClass.getName() +
                                           " provided by application " + appName);
                    return appName;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, taskClass + " not associated with any application",
                     cmd,
                     taskClass.getClassLoader());
        return null;
    }
}
