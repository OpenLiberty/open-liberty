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

import org.eclipse.microprofile.openapi.models.media.Encoding.Style;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.EncodingValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class EncodingValidatorTest {

    /*
     * Test Cases:
     * 1. Encoding object is null
     * 2. ContentType is set to an invalid value
     * 3. Style is set to an invalid value
     * 4. Style is set to 'form' and explode is set to 'false'
     * 5. Style is not set to 'form' and explode is set to 'true'
     * 6. Style is null and explode is true
     * 7. Positive test with correctly set Encoding
     */

    String key;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testEncodingIsNull() {

        EncodingImpl encodingNull = null;

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, encodingNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testContentTypeIsInvalid() {

        EncodingImpl contentTypeIsInvalid = new EncodingImpl();
        contentTypeIsInvalid.setContentType("invalidContenType");
        contentTypeIsInvalid.setStyle(Style.SPACE_DELIMITED);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, contentTypeIsInvalid);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testStyleIsFormExplodeFalse() {

        EncodingImpl styleIsFormExplodeFalse = new EncodingImpl();
        styleIsFormExplodeFalse.setStyle(Style.FORM);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, styleIsFormExplodeFalse);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testStyleOtherExplodeTrue() {

        EncodingImpl styleOtherExplodeTrue = new EncodingImpl();
        styleOtherExplodeTrue.setStyle(Style.DEEP_OBJECT);
        styleOtherExplodeTrue.setExplode(true);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, styleOtherExplodeTrue);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testStyleNullExplodeTrue() {

        EncodingImpl styleNullExplodeTrue = new EncodingImpl();
        styleNullExplodeTrue.setExplode(true);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, styleNullExplodeTrue);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());

    }

    public void testEncodingCorrect() {

        EncodingImpl encodingCorrect = new EncodingImpl();
        encodingCorrect.setContentType("application/octet-stream");
        encodingCorrect.setStyle(Style.PIPE_DELIMITED);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, encodingCorrect);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());

    }

}
