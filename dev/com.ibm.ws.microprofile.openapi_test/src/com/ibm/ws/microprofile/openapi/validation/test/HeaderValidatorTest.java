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

import org.eclipse.microprofile.openapi.models.headers.Header.Style;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.impl.validation.HeaderValidator;

/**
 *
 */
public class HeaderValidatorTest {

    /*
     * Test Cases
     * 1. Header object is null
     * 2. Both example and examples are not null
     * 3. Both schema and content are null
     * 4. Both schema and content are not null
     * 5. Content map contains more than one entry
     * 6. Style is not null but is not 'simple'
     * 7. Positive test with a correctly set header
     */

    String key;

    @Test
    public void testHeaderIsNull() {

        HeaderImpl headerIsNull = null;

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, null, key, headerIsNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testExampleAndExamplesNotNull() {

        HeaderImpl exampleAndExamplesNotNull = new HeaderImpl();
        exampleAndExamplesNotNull.setExample("testExample");
        Map<String, ExampleImpl> examples = new HashMap<String, ExampleImpl>();
        ExampleImpl example = new ExampleImpl();
        example.setDescription("testExample");
        examples.put(key, example);
        exampleAndExamplesNotNull.setExample(examples);
        SchemaImpl schema = new SchemaImpl();
        schema.setName("testSchema");
        exampleAndExamplesNotNull.setSchema(schema);

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, null, key, exampleAndExamplesNotNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testSchemaAndContentNull() {

        HeaderImpl schemaAndContentNull = new HeaderImpl();

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, null, key, schemaAndContentNull);

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
        validator.validate(vh, null, key, schemaAndContentNotNull);

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
        validator.validate(vh, null, key, contentMapWithTwoEntries);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testCorrectHeader() {

        HeaderImpl correctHeader = new HeaderImpl();

        Style style = Style.SIMPLE;

        SchemaImpl schema = new SchemaImpl();
        schema.setName("testSchema");
        correctHeader.setSchema(schema);

        correctHeader.setStyle(style);

        TestValidationHelper vh = new TestValidationHelper();
        HeaderValidator validator = HeaderValidator.getInstance();
        validator.validate(vh, null, key, correctHeader);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());

    }

}
