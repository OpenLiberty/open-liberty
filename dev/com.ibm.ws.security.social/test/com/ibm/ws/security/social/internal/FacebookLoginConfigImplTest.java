/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.security.social.error.SocialLoginException;

import test.common.SharedOutputManager;

public class FacebookLoginConfigImplTest extends Oauth2LoginConfigImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        configImpl = new FacebookLoginConfigImpl();
        mockery.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** initProps **************************************/

    @Test
    public void alwaysRunsTest() {
        // No unique tests needed for this class beyond what's already in Oauth2LoginConfigImplTest
    }

    /************************************** Helper methods **************************************/

    protected FacebookLoginConfigImpl getActivatedConfig(Map<String, Object> props) throws SocialLoginException {
        FacebookLoginConfigImpl config = new FacebookLoginConfigImpl();
        config.activate(cc, props);
        return config;
    }

    protected Map<String, Object> getRequiredConfigProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FacebookLoginConfigImpl.KEY_clientId, clientId);
        props.put(FacebookLoginConfigImpl.KEY_clientSecret, clientSecretPS);
        return props;
    }

    protected FacebookLoginConfigImpl getConfigImplWithHandleJwtElementMocked() {
        return new FacebookLoginConfigImpl() {
            @Override
            protected Configuration handleJwtElement(Map<String, Object> props, ConfigurationAdmin configurationAdmin) {
                return mockInterface.handleJwtElement();
            }
        };
    }

}
