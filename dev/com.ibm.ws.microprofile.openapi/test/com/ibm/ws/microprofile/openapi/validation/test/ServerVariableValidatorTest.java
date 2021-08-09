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
