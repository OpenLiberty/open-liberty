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
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.headers.Header.Style;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.HeaderValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

import junit.framework.Assert;

/**
 *
 */
public class HeaderValidatorTest {

    String key = null;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testHeaderIsNull() {

        HeaderImpl headerIsNull = null;

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, context, key, headerIsNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertFalse(vh.hasEvents());
    }

    @Test
    public void testExampleAndExamplesNotNull() {

        HeaderImpl exampleAndExamplesNotNull = new HeaderImpl();
        exampleAndExamplesNotNull.setExample("testExample");
        Map<String, Example> examples = new HashMap<String, Example>();
        ExampleImpl example = new ExampleImpl();
        example.setDescription("testExample");
        examples.put(key, example);
        exampleAndExamplesNotNull.setExamples(examples);
        SchemaImpl schema = new SchemaImpl();
        schema.setName("testSchema");
        exampleAndExamplesNotNull.setSchema(schema);

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, context, key, exampleAndExamplesNotNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testSchemaAndContentNull() {

        HeaderImpl schemaAndContentNull = new HeaderImpl();

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, context, key, schemaAndContentNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testSchemaAndContentNotNull() {

        HeaderImpl schemaAndContentNotNull = new HeaderImpl();

        SchemaImpl schema = new SchemaImpl();
        schema.setName("testSchema");
        schemaAndContentNotNull.setSchema(schema);

        ContentImpl content = new ContentImpl();
        MediaTypeImpl firstType = new MediaTypeImpl();
        content.put("first", firstType);
        schemaAndContentNotNull.setContent(content);

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, context, key, schemaAndContentNotNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testContentMapWithTwoEntries() {

        HeaderImpl contentMapWithTwoEntries = new HeaderImpl();

        ContentImpl content = new ContentImpl();
        MediaTypeImpl firstType = new MediaTypeImpl();
        MediaTypeImpl secondType = new MediaTypeImpl();
        content.put("first", firstType);
        content.put("second", secondType);
        contentMapWithTwoEntries.setContent(content);

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, context, key, contentMapWithTwoEntries);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testCorrectHeader() {

        HeaderImpl correctHeader = new HeaderImpl();

        Style style = Header.Style.SIMPLE;

        SchemaImpl schema = new SchemaImpl();
        schema.setName("testSchema");
        correctHeader.setSchema(schema);

        correctHeader.setStyle(style);

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, context, key, correctHeader);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());

    }

}