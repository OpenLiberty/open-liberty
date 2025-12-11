/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import org.junit.Before;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ManagedExecutorService;
import com.ibm.websphere.simplicity.config.ManagedScheduledExecutorService;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public abstract class ConcurrencyAppAbstractTests extends AbstractSpringTests {

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {

        ManagedExecutorService executorService = new ManagedExecutorService();
        executorService.setJndiName("taskExecutor1");
        config.getManagedExecutorServices().add(executorService);

        ManagedScheduledExecutorService scheduledExecutorService = new ManagedScheduledExecutorService();
        scheduledExecutorService.setJndiName("taskScheduler1");
        config.getManagedScheduledExecutorServices().add(scheduledExecutorService);

    }

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_CONCURRENCY;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }
}
