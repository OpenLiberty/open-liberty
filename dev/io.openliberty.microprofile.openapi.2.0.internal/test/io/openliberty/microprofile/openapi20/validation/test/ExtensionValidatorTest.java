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
import io.openliberty.microprofile.openapi20.validation.ExtensionValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;

/**
 *
 */

public class ExtensionValidatorTest {
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testExtensionsValidatorCorrect() {
        ExtensionValidator validator = ExtensionValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        String extension = "x-testExtensionValue";
        validator.validate(vh, context, extension, null);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExtensionsValidatorNull() {
        ExtensionValidator validator = ExtensionValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        String extension = null;
        validator.validate(vh, context, extension, null);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExtensionsValidatorInvalidExtension() {
        ExtensionValidator validator = ExtensionValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        String extension = "invalidExtensionForTest";
        validator.validate(vh, context, extension, null);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("extension must begin with \"x-\""));
    }
}
