/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class ServerXMLConfigurationMBeanTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final SystemConfiguration sc = mock.mock(SystemConfiguration.class);
    private ServerXMLConfigurationMBeanImpl mBean;

    @Before
    public void setUp() throws Exception {
        mBean = new ServerXMLConfigurationMBeanImpl();
        mBean.setSystemConfiguration(sc);
    }

    @After
    public void tearDown() throws Exception {
        mBean.unsetSystemConfiguration(sc);
        mBean = null;
    }

    @Test
    public void testGetConfigutationFilePaths() throws Exception {
        final Collection<String> configFilePaths = new ArrayList<String>();
        configFilePaths.add("${server.config.dir}/server.xml");
        configFilePaths.add("${server.config.dir}/include.xml");
        mock.checking(new Expectations() {
            {
                one(sc).fetchConfigurationFilePaths();
                will(returnValue(configFilePaths));
            }
        });
        final Collection<String> _configFilePaths = mBean.fetchConfigurationFilePaths();
        assertEquals("Expected config file path collections to be equal.",
                     configFilePaths, _configFilePaths);
    }
}
