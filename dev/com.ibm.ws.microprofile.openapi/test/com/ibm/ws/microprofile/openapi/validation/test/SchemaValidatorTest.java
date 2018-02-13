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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.SchemaValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class SchemaValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testSchemaCorrect() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();

        schema.setName("ErrorModel");
        schema.setType(SchemaType.OBJECT);

        List<String> required = new ArrayList<String>();
        required.addAll(Arrays.asList("message", "code"));
        schema.setRequired(required);

        Map<String, Schema> properties = new HashMap<String, Schema>();
        SchemaImpl messageSchema = new SchemaImpl();
        messageSchema.setType(SchemaType.STRING);
        SchemaImpl codeSchema = new SchemaImpl();
        codeSchema.setType(SchemaType.INTEGER);
        codeSchema.setMinimum(new BigDecimal(100));
        codeSchema.setMaximum(new BigDecimal(600));
        properties.put("message", messageSchema);
        properties.put("code", codeSchema);
        schema.setProperties(properties);

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullSchema() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = null;

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testArraySchemaWithNoItems() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.ARRAY);

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testReadOnlyAndWriteOnlyTrue() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.STRING);
        schema.setReadOnly(true);
        schema.setWriteOnly(true);

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testMultipleOfLessThanOne() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setMultipleOf(new BigDecimal(0));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testStringSchemaWithInvalidProperties() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.STRING);
        schema.setMinLength(new Integer(-2));
        schema.setMaxLength(new Integer(-1));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(2, vh.getEventsSize());
    }

    @Test
    public void testInvalidTypeSchema() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.OBJECT);
        schema.setMinLength(new Integer(3));
        schema.setMaxLength(new Integer(900));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(2, vh.getEventsSize());
    }

    @Test
    public void testStringSchemaWithUniqueItems() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.STRING);
        schema.setUniqueItems(true);

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testArraySchemaWithInvalidProperties() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.ARRAY);
        schema.setItems(new SchemaImpl());
        schema.setMinItems(new Integer(-2));
        schema.setMaxItems(new Integer(-1));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(2, vh.getEventsSize());
    }

    @Test
    public void testObjectSchemaWithInvalidProperties() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.OBJECT);
        schema.setMinProperties(new Integer(-2));
        schema.setMaxProperties(new Integer(-1));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(2, vh.getEventsSize());
    }

    @Test
    public void testNumberSchemaWithInvalidProperties() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.NUMBER);
        schema.setMinProperties(new Integer(1));
        schema.setMaxProperties(new Integer(53));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(2, vh.getEventsSize());
    }
}
