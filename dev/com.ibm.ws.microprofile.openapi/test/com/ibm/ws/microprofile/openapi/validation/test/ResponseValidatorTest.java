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
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ResponseValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ResponseValidatorTest {

    String key = null;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectResponse() {
        ResponseValidator validator = ResponseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        APIResponseImpl response = new APIResponseImpl();
        response.description("This is a test response object.");

        validator.validate(vh, context, key, response);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullResponse() {
        ResponseValidator validator = ResponseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        APIResponseImpl response = null;

        validator.validate(vh, context, key, response);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testResponseWithNoDescription() {
        ResponseValidator validator = ResponseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        APIResponseImpl response = new APIResponseImpl();

        validator.validate(vh, context, key, response);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"description\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testResponseWithEmptyDescription() {
        ResponseValidator validator = ResponseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        APIResponseImpl response = new APIResponseImpl();
        response.description("");

        validator.validate(vh, context, key, response);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"description\" field is missing or is set to an invalid value"));
    }
}
