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

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OpenAPIValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

public class OpenAPIValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testOpenAPIValidator() {

        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();
        openapi.setOpenapi(null); //overwrite the default value of "3.0.0"
        validator.validate(vh, context, openapi);
        Assert.assertEquals(3, vh.getEventsSize());
        //TODO - test the actual events

        vh.resetResults();
        openapi.setOpenapi("4.0"); //invalid version
        validator.validate(vh, context, openapi);
        Assert.assertEquals(3, vh.getEventsSize());

        vh.resetResults();
        openapi.setOpenapi("3.0");
        openapi.setInfo(new InfoImpl());
        openapi.setPaths(new PathsImpl().addPathItem("/", new PathItemImpl()));
        validator.validate(vh, context, openapi);
        Assert.assertFalse(vh.hasEvents());
    }
}
