package com.ibm.ws.webcontainer.security.metadata;

import static org.junit.Assert.assertNotNull;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelperFactory;

/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 *
 */
public class SecurityServletConfiguratorHelperFactoryTest {
    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final ServletConfigurator configurator = mockery.mock(ServletConfigurator.class);

    /**
     * Test method for
     * {@link com.ibm.ws.webcontainer.security.metadata.SecurityServletConfiguratorHelperFactory#createConfiguratorHelper(com.ibm.ws.container.service.config.ServletConfigurator)}
     * .
     */
    @Test
    public void testCreateConfiguratorHelper() {
        SecurityServletConfiguratorHelperFactory configHelperFactory = new SecurityServletConfiguratorHelperFactory();
        assertNotNull("The config helper factory should create a config helper instance that is not null", configHelperFactory.createConfiguratorHelper(configurator));
    }
}
