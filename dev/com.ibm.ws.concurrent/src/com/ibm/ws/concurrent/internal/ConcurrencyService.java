/*******************************************************************************
 * Copyright (c) 2020,2023 IBM Corporation and others.
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

import javax.enterprise.concurrent.ManagedScheduledExecutorService;

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
import com.ibm.ws.concurrent.TriggerService;
import com.ibm.ws.concurrent.ext.ConcurrencyExtensionProvider;
import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, service = { ConcurrencyService.class, ApplicationMetaDataListener.class })
public class ConcurrencyService implements ApplicationMetaDataListener {
    static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());
    private static final TraceComponent tc = Tr.register(ConcurrencyService.class);

    @Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.OPTIONAL, target = "(id=unbound)")
    ConcurrencyExtensionProvider extensionProvider;

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
            Tr.debug(this, tc, "applicationMetaDataDestroyed: " + event.getMetaData().getJ2EEName());

        BundleContext bc = priv.getBundleContext(FrameworkUtil.getBundle(getClass()));
        try {
            Collection<ServiceReference<ManagedScheduledExecutorService>> refs = //
                            priv.getServiceReferences(bc, ManagedScheduledExecutorService.class,
                                                      "(service.factoryPid=com.ibm.ws.concurrent.managedScheduledExecutorService)");
            for (ServiceReference<ManagedScheduledExecutorService> ref : refs) {
                ManagedScheduledExecutorService executor = priv.getService(bc, ref);
                if (executor instanceof ManagedScheduledExecutorServiceImpl) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "purge futures list for " + executor);
                    ((ManagedScheduledExecutorServiceImpl) executor).purgeFutures();
                }
            }
        } catch (InvalidSyntaxException x) {
            throw new RuntimeException(x); // should never occur because a valid filter is hard-coded
        }
    }
}
