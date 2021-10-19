/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class OsgiServices {

    private static Optional<OsgiServices> instance = Optional.empty();

    @Reference(service = ManagedExecutorService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    private volatile ManagedExecutorService executorService;

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

    public static Optional<ExecutorService> getManagedExecutorService() {
        return instance.flatMap(tracker -> Optional.ofNullable(tracker.executorService));
    }

}
