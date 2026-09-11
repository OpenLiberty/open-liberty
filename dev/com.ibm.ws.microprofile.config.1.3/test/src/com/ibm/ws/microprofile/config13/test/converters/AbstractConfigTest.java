/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.test.converters;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Before;

import com.ibm.ws.microprofile.config13.impl.Config13ProviderResolverImpl;

import io.openliberty.microprofile.config.fat.repeat.UnitTestUtils;

public abstract class AbstractConfigTest {

    @Before
    public void before() throws Exception {
        UnitTestUtils.mockOSGiCallers();
        ConfigProviderResolver.setInstance(new Config13ProviderResolverImpl());
    }

    @After
    public void after() throws Exception {
        ((Config13ProviderResolverImpl) ConfigProviderResolver.instance()).shutdown();
        ConfigProviderResolver.setInstance(null);
        UnitTestUtils.restoreOSGiCallers();
    }
}
