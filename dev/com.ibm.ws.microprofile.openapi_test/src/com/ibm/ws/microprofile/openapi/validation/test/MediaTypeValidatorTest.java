/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.openapi.validation.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.MediaTypeValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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
        Map<String, ExampleImpl> examples = new HashMap<String, ExampleImpl>();
        examples.put("example", new ExampleImpl());
        mediaType.setExample(examples);

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
