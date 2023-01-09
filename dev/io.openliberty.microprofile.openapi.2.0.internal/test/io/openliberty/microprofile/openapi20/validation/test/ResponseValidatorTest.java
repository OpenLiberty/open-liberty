/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.ResponseValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;

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
