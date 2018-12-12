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
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.RequestBodyValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class RequestBodyValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testRequestBodyValidator() {
        RequestBodyValidator validator = RequestBodyValidator.getInstance();
        TestValidationHelper validationHelper = new TestValidationHelper();

        RequestBodyImpl requestBody = new RequestBodyImpl();

        validator.validate(validationHelper, context, requestBody);
        Assert.assertEquals(1, validationHelper.getEventsSize());

        validationHelper.resetResults();

        requestBody.setContent(new ContentImpl());

        validator.validate(validationHelper, context, requestBody);
        Assert.assertEquals(1, validationHelper.getEventsSize());

        validationHelper.resetResults();

        ContentImpl content = new ContentImpl();
        content.addMediaType("test", new MediaTypeImpl());
        requestBody.setContent(content);
        validator.validate(validationHelper, context, requestBody);
        Assert.assertEquals(0, validationHelper.getEventsSize());
    }
}
