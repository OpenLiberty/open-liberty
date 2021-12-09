/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class IncludeCustomCacheKeyTest extends CommonTest {

    private static final Class<?> c = IncludeCustomCacheKeyTest.class;

    private static final String CUSTOM_CACHE_KEY = "customCacheKey: ";

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Setting up");
        commonSetUp("IncludeCustomCacheKeyTest", null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_START_SERVER);
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
     */

    @Test
    public void testIncludeCustomCacheKeyNotSpecified() {
        try {
            testHelper.reconfigureServer("includeCustomCacheKey_notSpecified.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            String response = commonSuccessfulSpnegoServletCall();

            expectation.responseContainsCustomCacheKey(response, CUSTOM_CACHE_KEY);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
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
     */

    @Test
    public void testIncludeCustomCacheKeyTrue() {
        try {
            testHelper.reconfigureServer("includeCustomCacheKey_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            String response = commonSuccessfulSpnegoServletCall();

            expectation.responseContainsCustomCacheKey(response, CUSTOM_CACHE_KEY);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
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
     */

    @Test
    public void testIncludeCustomCacheKeyFalse() {
        try {
            testHelper.reconfigureServer("includeCustomCacheKey_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            String response = commonSuccessfulSpnegoServletCall();

            expectation.responseContainsNullCacheKey(response, CUSTOM_CACHE_KEY);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

}
