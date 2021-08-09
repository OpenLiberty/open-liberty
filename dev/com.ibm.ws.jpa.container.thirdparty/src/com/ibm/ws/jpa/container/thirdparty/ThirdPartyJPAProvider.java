/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.thirdparty;

import static com.ibm.ws.jpa.management.JPAConstants.JPA_RESOURCE_BUNDLE_NAME;
import static com.ibm.ws.jpa.management.JPAConstants.JPA_TRACE_GROUP;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.spi.PersistenceProvider;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.AbstractJPAProviderIntegration;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAProviderIntegration;

@Component(service = { JPAProviderIntegration.class }, property = { "service.ranking:Integer=10" })
public class ThirdPartyJPAProvider extends AbstractJPAProviderIntegration {

    private static final TraceComponent tc = Tr.register(ThirdPartyJPAProvider.class, JPA_TRACE_GROUP, JPA_RESOURCE_BUNDLE_NAME);

    /**
     * Persistence provider implementation names that were placed in the service registry by the bells feature
     * when it finds library JARs that contain META-INF/services/javax.persistence.spi.PersistenceProvider.
     * To accommodate a rare case where multiple PersistenceProviders are found by the bells feature,
     * TreeSet allows us to choose the implementation.class name alphabetically to achieve deterministic behavior.
     */
    private final ConcurrentSkipListSet<String> providersFoundByBells = new ConcurrentSkipListSet<String>();

    private volatile String computedProvider;
    private volatile String inUseProvider;

    // Track component activation so we don't try to restart apps during server start
    private final AtomicBoolean activated = new AtomicBoolean();

    @Activate
    protected void activate(ComponentContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "activate");

        String p = computeProvider();
        activated.set(true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "activate", p);
    }

    @Override
    public String getProviderClassName() {
        String curComputedProvider = computedProvider;
        if (curComputedProvider == null)
            throw new IllegalStateException(Tr.formatMessage(tc, "NO_JPA_PROVIDER_FOUND_CWWJP0051E"));

        inUseProvider = curComputedProvider;
        return curComputedProvider;
    }

    @Reference(service = PersistenceProvider.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(implementation.class=*)")
    protected void setPersistenceProvider(ServiceReference<PersistenceProvider> ref) {
        providersFoundByBells.add((String) ref.getProperty("implementation.class"));
        computeProvider();
    }

    protected void unsetPersistenceProvider(ServiceReference<PersistenceProvider> ref) {
        String removedProvider = (String) ref.getProperty("implementation.class");
        providersFoundByBells.remove(removedProvider);
        // Recompute Provider (and possibly restart apps) if the provider in use was taken away
        if (removedProvider != null && removedProvider.equals(inUseProvider)) {
            computeProvider();
        }
    }

    private String computeProvider() {
        String originalProvider = computedProvider;
        String curProvider;
        if (providersFoundByBells.isEmpty()) {
            curProvider = null;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Choose first of providers found by the bells feature", providersFoundByBells);
            curProvider = providersFoundByBells.first();
            if (tc.isInfoEnabled())
                Tr.info(tc, "DEFAULT_PERSISTENCE_PROVIDER_LOADED_CWWJP0006I", curProvider);
        }

        // If the JPA provider has changed, recycle JPA applications.
        // __BEFORE__AFTER__ACTION___
        //   null -> null = no restart
        //   A    -> A    = no restart
        //   null -> A    = restart
        //   A    -> B    = restart
        //   A    -> null = restart
        boolean providerChanged = activated.get() &&
                                  ((originalProvider != null && !originalProvider.equals(curProvider)) ||
                                   (curProvider != null && !curProvider.equals(originalProvider)));
        if (providerChanged)
            inUseProvider = null;

        computedProvider = curProvider;

        if (providerChanged)
            JPAAccessor.getJPAComponent().recycleJPAApplications();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "compute provider " + originalProvider + " -> " + curProvider);

        return curProvider;
    }
}
