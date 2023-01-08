/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.rest.client30.internal;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.enterprise.concurrent.ManagedExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Provides access to OSGi services to non-osgi components
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class OsgiServices {

    private static volatile Optional<OsgiServices> instance = Optional.empty();

    private volatile Optional<ExecutorService> executorService = Optional.empty();

    @Activate
    protected void activate() {
        instance = Optional.of(this);
    }

    @Deactivate
    protected void deactivate() {
        if (instance.isPresent() && instance.get() == this) {
            instance = Optional.empty();
        }
    }

    @Reference(service = ManagedExecutorService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setExecutorService(ExecutorService executor) {
        executorService = Optional.of(executor);
    }

    protected void unsetExecutorService(ExecutorService executor) {
        if (executorService.isPresent() && executorService.get() == executor) {
            executorService = Optional.empty();
        }
    }

    /**
     * Get a ManagedExecutorService, if one is available
     * <p>
     * @return an Optional containing a ManagedExecutorService, or an empty Optional if one is not available
     */
    public static Optional<ExecutorService> getManagedExecutorService() {
        return instance.flatMap(tracker -> tracker.executorService);
    }

}
