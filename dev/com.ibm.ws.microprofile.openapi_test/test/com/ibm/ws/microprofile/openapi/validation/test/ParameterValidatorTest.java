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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ParameterValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ParameterValidatorTest {

    String key = null;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectQueryParameter() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = new ParameterImpl();
        ExampleImpl example = new ExampleImpl();
        example.summary("This is a test param example").externalValue("http://example.com/queryparam");
        SchemaImpl schema = new SchemaImpl();
        schema.type(SchemaType.OBJECT);
        parameter.name("test-param").in(In.QUERY).example(example).schema(schema);

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testCorrectHeaderParameter() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = new ParameterImpl();
        ContentImpl content = new ContentImpl();
        MediaTypeImpl mediaType = new MediaTypeImpl();
        content.addMediaType("param-content", mediaType);
        parameter.name("Accept").in(In.HEADER).content(content);

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullParameter() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = null;

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testQueryParameterWithSchemaAndContent() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = new ParameterImpl();
        ExampleImpl example = new ExampleImpl();
        example.summary("This is a test param example").externalValue("http://example.com/queryparam");
        SchemaImpl schema = new SchemaImpl();
        schema.type(SchemaType.OBJECT);
        ContentImpl content = new ContentImpl();
        MediaTypeImpl mediaType = new MediaTypeImpl();
        content.addMediaType("param-content", mediaType);
        parameter.name("test-param").in(In.QUERY).example(example).schema(schema).content(content);

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Parameter object must not contain a schema property and a content property"));
    }

    @Test
    public void testQueryParameterWithOutSchemaOrContent() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = new ParameterImpl();
        ExampleImpl example = new ExampleImpl();
        example.summary("This is a test param example").externalValue("http://example.com/queryparam");
        parameter.name("test-param").in(In.QUERY).example(example);

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Parameter object does not contain a schema property or a content property"));
    }

    @Test
    public void testQueryParameterWithExampleAndExamples() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = new ParameterImpl();
        ExampleImpl example = new ExampleImpl();
        example.summary("This is a test param example").externalValue("http://example.com/queryparam");
        Map<String, Example> examples = new HashMap<String, Example>();
        examples.put("example-name", example);
        SchemaImpl schema = new SchemaImpl();
        schema.type(SchemaType.OBJECT);
        parameter.name("test-param").in(In.QUERY).example(example).schema(schema).examples(examples);

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Parameter object specifies both an example object and an examples object"));
    }

    @Test
    public void testHeaderParameterWithMoreThanOneContentEntry() {
        ParameterValidator validator = ParameterValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ParameterImpl parameter = new ParameterImpl();
        ContentImpl content = new ContentImpl();
        MediaTypeImpl mediaTypeOne = new MediaTypeImpl();
        MediaTypeImpl mediaTypeTwo = new MediaTypeImpl();
        content.addMediaType("param-content-one", mediaTypeOne);
        content.addMediaType("param-content-two", mediaTypeTwo);
        parameter.name("Accept").in(In.HEADER).content(content);

        validator.validate(vh, context, key, parameter);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Parameter object must contain only one entry"));
    }

}
