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

public class GoogleLoginConfigImplTest extends OidcLoginConfigImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        configImpl = new GoogleLoginConfigImpl();
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

    /************************************** getSignatureAlgorithm **************************************/

    @Test
    public void getSignatureAlgorithm_configNotInitialized() {
        try {
            String result = configImpl.getSignatureAlgorithm();
            assertNull("Signature algorithm should be null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSignatureAlgorithm_propsMissingJwksUri() {
        try {
            Map<String, Object> props = getRequiredConfigProps();
            configImpl.initProps(cc, props);

            String result = configImpl.getSignatureAlgorithm();
            assertNull("Signature algorithm should be null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSignatureAlgorithm_propsMissingJwksUri_includeSignatureAlgorithm() {
        try {
            Map<String, Object> props = getRequiredConfigProps();
            props.put(GoogleLoginConfigImpl.KEY_SIGNATURE_ALGORITHM, "some algorithm");
            configImpl.initProps(cc, props);

            String result = configImpl.getSignatureAlgorithm();
            assertNull("Signature algorithm should be null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSignatureAlgorithm_propsIncludeJwksUri_missingSignatureAlgorithm() {
        try {
            Map<String, Object> props = getRequiredConfigProps();
            props.put(GoogleLoginConfigImpl.KEY_jwksUri, "some JWK URI value");
            configImpl.initProps(cc, props);

            String result = configImpl.getSignatureAlgorithm();
            assertNull("Signature algorithm should be null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getSignatureAlgorithm_propsIncludeJwksUriAndSignatureAlgorithm() {
        try {
            String algorithm = "some algorithm";
            Map<String, Object> props = getRequiredConfigProps();
            props.put(GoogleLoginConfigImpl.KEY_jwksUri, "some JWK URI value");
            props.put(GoogleLoginConfigImpl.KEY_SIGNATURE_ALGORITHM, algorithm);
            configImpl.initProps(cc, props);

            String result = configImpl.getSignatureAlgorithm();
            assertEquals("Signature algorithm did not match expected value.", algorithm, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    protected GoogleLoginConfigImpl getActivatedConfig(Map<String, Object> props) throws SocialLoginException {
        GoogleLoginConfigImpl config = new GoogleLoginConfigImpl();
        config.activate(cc, props);
        return config;
    }

    protected Map<String, Object> getRequiredConfigProps() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(GoogleLoginConfigImpl.KEY_clientId, clientId);
        props.put(GoogleLoginConfigImpl.KEY_clientSecret, clientSecretPS);
        return props;
    }

    protected GoogleLoginConfigImpl getConfigImplWithHandleJwtElementMocked() {
        return new GoogleLoginConfigImpl() {
            @Override
            protected Configuration handleJwtElement(Map<String, Object> props, ConfigurationAdmin configurationAdmin) {
                return mockInterface.handleJwtElement();
            }
        };
    }

}
