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
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ServerVariableValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ServerVariableValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testServerVariableTest() {
        ServerVariableValidator validator = ServerVariableValidator.getInstance();
        TestValidationHelper validationHelper = new TestValidationHelper();

        ServerVariableImpl serverVariable = new ServerVariableImpl();

        validator.validate(validationHelper, context, serverVariable);
        Assert.assertEquals(1, validationHelper.getEventsSize());

        validationHelper.resetResults();

        serverVariable.setDefaultValue("default");

        validator.validate(validationHelper, context, serverVariable);
        Assert.assertEquals(0, validationHelper.getEventsSize());

    }

}
