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
import io.openliberty.microprofile.openapi20.validation.ExampleValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.examples.ExampleImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;

/**
 *
 */
public class ExampleValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);
    String key = null;

    @Test
    public void testCorrectExample() {
        ExampleValidator validator = ExampleValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ExampleImpl example = new ExampleImpl();
        example.setDescription("This is a test example");
        example.setSummary("Example for validator testing purposes");
        example.setExternalValue("testExternalValue");

        validator.validate(vh, context, key, example);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullExample() {
        ExampleValidator validator = ExampleValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ExampleImpl example = null;

        validator.validate(vh, context, key, example);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExampleWithValueAndExternalValueSet() {
        ExampleValidator validator = ExampleValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ExampleImpl example = new ExampleImpl();
        example.setDescription("This is a test example");
        example.setSummary("Example for validator testing purposes");
        example.setExternalValue("testExternalValue");
        example.setValue(new SchemaImpl());

        validator.validate(vh, context, key, example);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("specifies both \"value\" and \"externalValue\" fields."));
    }

    @Test
    public void testExampleWithNullExternalValue() {
        ExampleValidator validator = ExampleValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ExampleImpl example = new ExampleImpl();
        example.setDescription("This is a test example");
        example.setSummary("Example for validator testing purposes");
        example.setExternalValue(null);

        validator.validate(vh, context, key, example);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExampleNullValue() {
        ExampleValidator validator = ExampleValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ExampleImpl example = new ExampleImpl();
        example.setDescription("This is a test example");
        example.setSummary("Example for validator testing purposes");
        example.setExternalValue("testExternalValue");
        example.setValue(null);

        validator.validate(vh, context, key, example);
        Assert.assertEquals(0, vh.getEventsSize());
    }

}
