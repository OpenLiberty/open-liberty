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
import com.ibm.ws.microprofile.openapi.impl.validation.ExtensionValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("must start with \"x-\""));
    }
}
