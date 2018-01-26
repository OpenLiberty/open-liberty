/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.openapi.validation.test;

import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.EncodingValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

import junit.framework.Assert;

/**
 *
 */
public class EncodingValidatorTest {

    String key = null;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testEncodingIsNull() {

        EncodingImpl encodingNull = null;

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, encodingNull);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());

    }

    @Test
    public void testStyleIsFormExplodeFalse() {

        EncodingImpl styleIsFormExplodeFalse = new EncodingImpl();
        styleIsFormExplodeFalse.setStyle(Encoding.Style.FORM);
        styleIsFormExplodeFalse.setExplode(false);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, styleIsFormExplodeFalse);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testStyleOtherExplodeTrue() {

        EncodingImpl styleOtherExplodeTrue = new EncodingImpl();
        styleOtherExplodeTrue.setStyle(Encoding.Style.DEEP_OBJECT);
        styleOtherExplodeTrue.setExplode(true);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, styleOtherExplodeTrue);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(1, vh.getEventsSize());
    }

    public void testEncodingCorrect() {

        EncodingImpl encodingCorrect = new EncodingImpl();
        encodingCorrect.setContentType("application/octet-stream");
        encodingCorrect.setStyle(Encoding.Style.PIPE_DELIMITED);

        TestValidationHelper vh = new TestValidationHelper();
        EncodingValidator validator = EncodingValidator.getInstance();
        validator.validate(vh, context, key, encodingCorrect);

        //Check for number of events only to keep assert statement independent of error message
        Assert.assertEquals(0, vh.getEventsSize());

    }

}