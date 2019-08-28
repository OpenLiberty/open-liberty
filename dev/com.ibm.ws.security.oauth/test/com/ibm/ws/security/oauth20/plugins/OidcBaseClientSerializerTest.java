/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.google.gson.JsonElement;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 * Validate the OidcBaseClientSerializer functionality
 */
public class OidcBaseClientSerializerTest extends AbstractOidcRegistrationBaseTest {
    private static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    @Test
    public void testClientSecretMaskSerializer() {
        String methodName = "testClientSecretMaskSerializer";
        String maskedValue = "*";

        try {
            OidcBaseClientSerializer oidcSerializer = new OidcBaseClientSerializer();
            OidcBaseClient testOidcBaseClient = getSampleOidcBaseClient();

            //Ensure clientSecret property has some value that is not '*'
            assertTrue(testOidcBaseClient.getClientSecret() != null
                       && !testOidcBaseClient.getClientSecret().isEmpty()
                       && !testOidcBaseClient.getClientSecret().equals(maskedValue));

            JsonElement jsonElement = oidcSerializer.serialize(getSampleOidcBaseClient(), null, null);

            String serializedClientSecret = jsonElement.getAsJsonObject().get(FIELD_CLIENT_SECRET).getAsString();

            assertEquals(serializedClientSecret, maskedValue);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
