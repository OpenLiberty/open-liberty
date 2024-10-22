/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIModelOperations;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelOperationsImpl;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.SchemaValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestServiceCaller;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;

public class SchemaValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @BeforeClass
    public static void setup() throws Exception {
        // Manually provide required OSGi services
        Field f = SchemaValidator.class.getDeclaredField("MODEL_OPS");
        f.setAccessible(true);
        f.set(null, new TestServiceCaller<>(OpenAPIModelOperations.class, new OpenAPIModelOperationsImpl()));
    }

    @Test
    public void testSchemaCorrect() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();

        schema.setType(SchemaType.OBJECT);

        List<String> required = new ArrayList<>();
        required.addAll(Arrays.asList("message", "code"));
        schema.setRequired(required);

        Map<String, Schema> properties = new HashMap<>();
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
    public void testMultipleOfNotGreaterThanZero() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setMultipleOf(new BigDecimal(0));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero"));

        vh = new TestValidationHelper();

        schema = new SchemaImpl();
        schema.setMultipleOf(new BigDecimal(-3));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Schema Object must have the \"multipleOf\" property set to a number strictly greater than zero"));

        vh = new TestValidationHelper();

        schema = new SchemaImpl();
        schema.setMultipleOf(new BigDecimal(0.002));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(0, vh.getEventsSize());

        vh = new TestValidationHelper();

        schema = new SchemaImpl();
        schema.setMultipleOf(new BigDecimal(7));

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testStringSchemaWithInvalidProperties() {

        SchemaValidator validator = SchemaValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        SchemaImpl schema = new SchemaImpl();
        schema.setType(SchemaType.STRING);
        schema.setMinLength(-2);
        schema.setMaxLength(-1);

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
        schema.setMinLength(3);
        schema.setMaxLength(900);

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
        schema.setMinItems(-2);
        schema.setMaxItems(-1);

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
        schema.setMinProperties(-2);
        schema.setMaxProperties(-1);

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
        schema.setMinProperties(1);
        schema.setMaxProperties(53);

        validator.validate(vh, context, null, schema);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The \"minProperties\" property is not appropriate for the Schema Object of \"number\" type"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The \"maxProperties\" property is not appropriate for the Schema Object of \"number\" type"));
    }
}
