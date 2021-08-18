/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Spnego;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.ApacheKDCCommonTest;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class IncludeCustomCacheKeyTest extends ApacheKDCCommonTest {

    private static final Class<?> c = IncludeCustomCacheKeyTest.class;

    private static final String CUSTOM_CACHE_KEY = "customCacheKey: ";

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Setting up");

        ApacheKDCCommonTest.commonSetUp("IncludeCustomCacheKeyTest", null,
                                        SPNEGOConstants.NO_APPS,
                                        SPNEGOConstants.NO_PROPS,
                                        SPNEGOConstants.DONT_CREATE_SSL_CLIENT,
                                        SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB,
                                        SPNEGOConstants.DEFAULT_REALM,
                                        SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN,
                                        SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN,
                                        SPNEGOConstants.USE_CANONICAL_NAME,
                                        SPNEGOConstants.USE_COMMON_KEYTAB,
                                        SPNEGOConstants.START_SERVER);
    }

    @Before
    public void beforeTestCase() throws Exception {
        Log.info(c, "beforeTestCase", "============================= BEGIN TEST =============================");
        preTestCheck();
    }

    @After
    public void afterTestCase() {
        //base config is shared for most testCases so we do not need to log it every time
        if (myServer.isLogOnUpdate())
            myServer.setLogOnUpdate(false);
    }

    /**
     * Test description:
     * - includeCustomCacheKeyInSubject attribute is not set in server.xml (default value is true).
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned contains the custom cache key.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - Client subject should contain the custom cache key.
     *
     * @throws Exception
     */

    @Test
    public void testIncludeCustomCacheKeyNotSpecified() throws Exception {
        setDefaultSpnegoServerConfig();

        String response = commonSuccessfulSpnegoServletCall();
        expectation.responseContainsCustomCacheKey(response, CUSTOM_CACHE_KEY);
    }

    /**
     * Test description:
     * - includeCustomCacheKeyInSubject attribute is set to true in server.xml.
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned contains the custom cache key.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - Client subject should contain the custom cache key.
     *
     * @throws Exception
     */

    @Test
    public void testIncludeCustomCacheKeyTrue() throws Exception {
        Spnego spnego = getDefaultSpnegoConfigElement();
        spnego.includeCustomCacheKeyInSubject = "true";

        updateServerSpnegoConfigDynamically(spnego);

        String response = commonSuccessfulSpnegoServletCall();
        expectation.responseContainsCustomCacheKey(response, CUSTOM_CACHE_KEY);
    }

    /**
     * Test description:
     * - includeCustomCacheKeyInSubject attribute is set to false in server.xml.
     * - Access a protected resource by including the SPNEGO token in the request.
     * - Check that the subject returned does not contain the custom cache key.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - Client subject should not contain the custom cache key.
     *
     * @throws Exception
     */

    @Test
    public void testIncludeCustomCacheKeyFalse() throws Exception {
        Spnego spnego = getDefaultSpnegoConfigElement();
        spnego.includeCustomCacheKeyInSubject = "false";

        updateServerSpnegoConfigDynamically(spnego);

        String response = commonSuccessfulSpnegoServletCall();
        expectation.responseContainsNullCacheKey(response, CUSTOM_CACHE_KEY);
    }

}
