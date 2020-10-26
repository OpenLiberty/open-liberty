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
package test.concurrent.work.wm;

import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.concurrent.ext.ConcurrencyExtensionProvider;
import com.ibm.ws.concurrent.ext.ManagedExecutorExtension;
import com.ibm.wsspi.resource.ResourceInfo;

/**
 * User feature service that serves as an executor service factory.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class WorkManagerProvider implements ConcurrencyExtensionProvider {
    @Activate
    protected void activate() {
        System.out.println("WorkManagerProvider is installed and probably being used");
    }

    @Override
    public ManagedExecutorExtension provide(WSManagedExecutorService executor, ResourceInfo resourceInfo) {
        if (executor instanceof ScheduledExecutorService)
            return new SchedulingWorkManagerImpl(executor, resourceInfo);
        else
            return new WorkManagerImpl(executor, resourceInfo);
    }
}
