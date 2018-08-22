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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.security.social.error.SocialLoginException;

import test.common.SharedOutputManager;

public class GithubLoginConfigImplTest extends Oauth2LoginConfigImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        configImpl = new GithubLoginConfigImpl();
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
    public void initProps_emptyProps() {
        try {
            configImpl.initProps(cc, new HashMap<String, Object>());

            assertNull("User API response ID should have been null but was [" + configImpl.getUserApiResponseIdentifier() + "].", configImpl.getUserApiResponseIdentifier());

            verifyAllMissingRequiredAttributes(outputMgr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void initProps_minimumProps() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();

            configImpl.initProps(cc, minimumProps);

            assertNull("User API response ID should have been null but was [" + configImpl.getUserApiResponseIdentifier() + "].", configImpl.getUserApiResponseIdentifier());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getUserApiResponseIdentifier **************************************/

    @Test
    public void getUserApiResponseIdentifier_userApi_containsEmails() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();
            minimumProps.put(GithubLoginConfigImpl.KEY_userApi, "some emails value");

            configImpl.initProps(cc, minimumProps);

            assertNull("User API response ID should have been null but was [" + configImpl.getUserApiResponseIdentifier() + "].", configImpl.getUserApiResponseIdentifier());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponseIdentifier_userApi_endsWithEmail() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();
            minimumProps.put(GithubLoginConfigImpl.KEY_userApi, "some value note lack of 's' on email");

            configImpl.initProps(cc, minimumProps);

            assertNull("User API response ID should have been null but was [" + configImpl.getUserApiResponseIdentifier() + "].", configImpl.getUserApiResponseIdentifier());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponseIdentifier_userApi_endsWithEmails() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();
            minimumProps.put(GithubLoginConfigImpl.KEY_userApi, "some value emails");

            configImpl.initProps(cc, minimumProps);

            assertEquals("User API response ID did not match expected value.", "primary", configImpl.getUserApiResponseIdentifier());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponseIdentifier_userNameAttribute_containsEmail() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();
            minimumProps.put(GithubLoginConfigImpl.KEY_userNameAttribute, "email, realm");

            configImpl.initProps(cc, minimumProps);

            assertNull("User API response ID should have been null but was [" + configImpl.getUserApiResponseIdentifier() + "].", configImpl.getUserApiResponseIdentifier());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getUserApiResponseIdentifier_userNameAttribute_email() {
        try {
            Map<String, Object> minimumProps = getRequiredConfigProps();
            minimumProps.put(GithubLoginConfigImpl.KEY_userNameAttribute, "email");

            configImpl.initProps(cc, minimumProps);

            assertEquals("User API response ID did not match expected value.", "primary", configImpl.getUserApiResponseIdentifier());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    protected GithubLoginConfigImpl getActivatedConfig(Map<String, Object> props) throws SocialLoginException {
        GithubLoginConfigImpl config = new GithubLoginConfigImpl();
        config.activate(cc, props);
        return config;
    }

    protected Map<String, Object> getRequiredConfigProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(GithubLoginConfigImpl.KEY_clientId, clientId);
        props.put(GithubLoginConfigImpl.KEY_clientSecret, clientSecretPS);
        return props;
    }

    protected GithubLoginConfigImpl getConfigImplWithHandleJwtElementMocked() {
        return new GithubLoginConfigImpl() {
            @Override
            protected Configuration handleJwtElement(Map<String, Object> props, ConfigurationAdmin configurationAdmin) {
                return mockInterface.handleJwtElement();
            }
        };
    }

}
