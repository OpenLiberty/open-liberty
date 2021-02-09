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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.MediaTypeValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.examples.ExampleImpl;
import io.smallrye.openapi.api.models.media.EncodingImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;

/**
 *
 */
public class MediaTypeValidatorTest {

    private MediaTypeValidator validator;
    private TestValidationHelper validationHelper;
    private MediaTypeImpl mediaType;

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    /**
     * Setup a basic MediaType object for the tests.
     */
    @Before
    public void initialize() {
        validator = MediaTypeValidator.getInstance();
        validationHelper = new TestValidationHelper();
        mediaType = new MediaTypeImpl();
        // mediaType has schema, examples, example and encoding.
        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.OBJECT);
        schema.setTitle("Pet");
        schema.setDescription("Object representation of a pet");

        SchemaImpl name = new SchemaImpl();
        name.setTitle("name");
        name.setType(SchemaType.STRING);
        schema.addProperty("name", name);

        mediaType.setSchema(schema);
    }

    @Test
    public void testValidMediaTypeValidator() {

        EncodingImpl encoding = new EncodingImpl();
        encoding.setContentType("text/plain");
        Map<String, Encoding> encodingMap = new HashMap<String, Encoding>();
        encodingMap.put("name", encoding);

        mediaType.setEncoding(encodingMap);

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(0, validationHelper.getEventsSize());

    }

    @Test
    public void testInvalidEncodingMediaTypeValidator() {
        EncodingImpl encoding = new EncodingImpl();
        encoding.setContentType("text/plain");
        Map<String, Encoding> encodingMap = new HashMap<String, Encoding>();
        encodingMap.put("stuff", encoding);

        mediaType.setEncoding(encodingMap);

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testEmptySchemaMediaTypeValidator() {
        mediaType.setSchema(null);
        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(0, validationHelper.getEventsSize());
    }

    @Test
    public void testEmptySchemaInvalidEncodingMediaTypeValidator() {
        mediaType.setSchema(new SchemaImpl());

        EncodingImpl encoding = new EncodingImpl();
        encoding.setContentType("text/plain");
        Map<String, Encoding> encodingMap = new HashMap<String, Encoding>();
        encodingMap.put("stuff", encoding);

        mediaType.setEncoding(encodingMap);

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testNullSchemaInvalidEncodingMediaTypeValidator() {
        mediaType.setSchema(null);

        EncodingImpl encoding = new EncodingImpl();
        encoding.setContentType("text/plain");
        Map<String, Encoding> encodingMap = new HashMap<String, Encoding>();
        encodingMap.put("stuff", encoding);

        mediaType.setEncoding(encodingMap);

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testInvalidExampleMediaTypeValidator() {
        EncodingImpl encoding = new EncodingImpl();
        encoding.setContentType("text/plain");
        Map<String, Encoding> encodingMap = new HashMap<String, Encoding>();
        encodingMap.put("name", encoding);

        mediaType.setEncoding(encodingMap);
        mediaType.setExample("example");
        Map<String, Example> examples = new HashMap<String, Example>();
        examples.put("example", new ExampleImpl());
        mediaType.setExamples(examples);

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testValidExampleMediaTypeValidator() {
        mediaType.setExample("example");

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(0, validationHelper.getEventsSize());
    }

    @Test
    public void testValidExamplesMediaTypeValidator() {
        Map<String, ExampleImpl> examples = new HashMap<String, ExampleImpl>();
        examples.put("example", new ExampleImpl());

        mediaType.setExample(examples);

        validator.validate(validationHelper, context, mediaType);
        Assert.assertEquals(0, validationHelper.getEventsSize());
    }
}
