/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.RequestBodyValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.parameters.RequestBodyImpl;

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
