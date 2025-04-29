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

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public abstract class ConcurrencyAppAbstractTests extends AbstractSpringTests {

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_CONCURRENCY;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }
}
