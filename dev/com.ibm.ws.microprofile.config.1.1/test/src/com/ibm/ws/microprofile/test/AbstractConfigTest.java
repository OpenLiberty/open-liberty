/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.test;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.After;
import org.junit.Before;

import com.ibm.ws.microprofile.config.impl.ConfigProviderResolverImpl;

/**
 *
 */
public abstract class AbstractConfigTest {

    @Before
    public void before() {
        ConfigProviderResolver.setInstance(new ConfigProviderResolverImpl());
    }

    @After
    public void after() {
        ((ConfigProviderResolverImpl) ConfigProviderResolver.instance()).shutdown();
        ConfigProviderResolver.setInstance(null);
    }

}
