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
import com.ibm.ws.microprofile.openapi.impl.model.media.DiscriminatorImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.DiscriminatorValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class DiscriminatorValidatorTest {
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testDiscriminatorValidatorCorrect() {
        DiscriminatorValidator validator = DiscriminatorValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        DiscriminatorImpl discriminator = new DiscriminatorImpl();
        discriminator.addMapping("name", "value");
        discriminator.setPropertyName("testPropertyName");
        validator.validate(vh, context, null, discriminator);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullDiscriminator() {
        DiscriminatorValidator validator = DiscriminatorValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        DiscriminatorImpl discriminator = null;
        validator.validate(vh, context, null, discriminator);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullPropertyName() {
        DiscriminatorValidator validator = DiscriminatorValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        DiscriminatorImpl discriminator = new DiscriminatorImpl();
        discriminator.setPropertyName(null);
        discriminator.addMapping("name", "value");
        validator.validate(vh, context, null, discriminator);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testEmptyPropertyName() {
        DiscriminatorValidator validator = DiscriminatorValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        DiscriminatorImpl discriminator = new DiscriminatorImpl();
        discriminator.setPropertyName("");
        discriminator.addMapping("name", "value");
        validator.validate(vh, context, null, discriminator);
        Assert.assertEquals(1, vh.getEventsSize());
    }
}
