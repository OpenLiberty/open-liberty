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

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.SchemaValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;

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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Schema Object of \"array\" type must have \"items\" property defined"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Schema Object must not have both \"readOnly\" and \"writeOnly\" fields set to true"));
    }

    @Test
    public void testMultipleOfLessThanOne() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setMultipleOf(new BigDecimal(0));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"maxLength\" property of the Schema Object must be greater than or equal to zero"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The \"minLength\" property of the Schema Object must be greater than or equal to zero"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"maxLength\" property is not appropriate for the Schema Object of \"object\" type"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The \"minLength\" property is not appropriate for the Schema Object of \"object\" type"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"uniqueItems\" property is not appropriate for the Schema Object of \"string\" type"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"minItems\" property of the Schema Object must be greater than or equal to zero"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The \"maxItems\" property of the Schema Object must be greater than or equal to zero"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"minProperties\" property of the Schema Object must be greater than or equal to zero"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The \"maxProperties\" property of the Schema Object must be greater than or equal to zero"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"minProperties\" property is not appropriate for the Schema Object of \"number\" type"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The \"maxProperties\" property is not appropriate for the Schema Object of \"number\" type"));
    }
}
