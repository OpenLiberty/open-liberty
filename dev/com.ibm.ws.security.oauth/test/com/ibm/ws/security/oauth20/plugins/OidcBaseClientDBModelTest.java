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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ibm.ws.security.oidc.common.AbstractOidcRegistrationBaseTest;

/**
 * Unit test case to verify behavior integrity of OidcBaseClientDBModelTest bean
 */
public class OidcBaseClientDBModelTest extends AbstractOidcRegistrationBaseTest {
    private static final JsonObject CLIENT_METADATA = new JsonObject();

    private static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        CLIENT_METADATA.add(FIELD_APPLICATION_TYPE, new JsonPrimitive(APPLICATION_TYPE));
        CLIENT_METADATA.add(FIELD_SUBJECT_TYPE, new JsonPrimitive(SUBJECT_TYPE));
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    @Test
    public void testOidcBaseClientDBModelConstructor() {
        String methodName = "testOidcBaseClientDBModelConstructor";

        try {
            OidcBaseClientDBModel testOidcBaseClientDBModel =
                            new OidcBaseClientDBModel(COMPONENT_ID, CLIENT_ID, CLIENT_SECRET, CLIENT_NAME, REDIRECT_URI_1, IS_ENABLED ? 1 : 0, CLIENT_METADATA);

            //Assert expected values
            assertEquals(CLIENT_ID, testOidcBaseClientDBModel.getClientId());
            assertEquals(CLIENT_SECRET, testOidcBaseClientDBModel.getClientSecret());
            assertEquals(REDIRECT_URI_1, testOidcBaseClientDBModel.getRedirectUri());
            assertEquals(CLIENT_NAME, testOidcBaseClientDBModel.getDisplayName());
            assertEquals(COMPONENT_ID, testOidcBaseClientDBModel.getComponentId());
            assertEquals(IS_ENABLED ? 1 : 0, testOidcBaseClientDBModel.getEnabled());
            assertEquals(CLIENT_METADATA, testOidcBaseClientDBModel.getClientMetadata());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testOidcBaseClientDBModelAccessorModifiers() {
        String methodName = "testOidcBaseClientDBModelAccessorModifiers";
        try {
            OidcBaseClientDBModel testOidcBaseClientDBModel =
                            new OidcBaseClientDBModel("", "", "", "", "", IS_ENABLED ? 0 : 1 /** Reverse value, so test can set '1' **/
                            , new JsonObject());

            testOidcBaseClientDBModel.setClientId(CLIENT_ID);
            testOidcBaseClientDBModel.setClientSecret(CLIENT_SECRET);
            testOidcBaseClientDBModel.setRedirectUri(REDIRECT_URI_1);
            testOidcBaseClientDBModel.setDisplayName(CLIENT_NAME);
            testOidcBaseClientDBModel.setComponentId(COMPONENT_ID);
            testOidcBaseClientDBModel.setEnabled(IS_ENABLED ? 1 : 0);
            testOidcBaseClientDBModel.setClientMetadata(CLIENT_METADATA);

            //Assert expected values
            assertEquals(CLIENT_ID, testOidcBaseClientDBModel.getClientId());
            assertEquals(CLIENT_SECRET, testOidcBaseClientDBModel.getClientSecret());
            assertEquals(REDIRECT_URI_1, testOidcBaseClientDBModel.getRedirectUri());
            assertEquals(CLIENT_NAME, testOidcBaseClientDBModel.getDisplayName());
            assertEquals(COMPONENT_ID, testOidcBaseClientDBModel.getComponentId());
            assertEquals(IS_ENABLED ? 1 : 0, testOidcBaseClientDBModel.getEnabled());
            assertEquals(CLIENT_METADATA, testOidcBaseClientDBModel.getClientMetadata());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
