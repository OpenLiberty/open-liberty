/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.validation.test;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OAuthFlowsValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OAuthFlowsValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testNewOAuthFlowsObject() {
        OAuthFlowsValidator validator = OAuthFlowsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OAuthFlowsImpl oauthflows = new OAuthFlowsImpl();

        validator.validate(vh, context, oauthflows);
        Assert.assertEquals(3, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"scopes\" field is missing or is set to an invalid value."));
    }

}
