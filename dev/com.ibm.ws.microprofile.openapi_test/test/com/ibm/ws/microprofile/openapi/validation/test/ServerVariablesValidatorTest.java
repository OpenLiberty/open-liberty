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
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariablesImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ServerVariablesValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ServerVariablesValidatorTest {

    String key;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectServerVariables() {

        ServerVariablesValidator validator = ServerVariablesValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ServerVariablesImpl serverVariables = new ServerVariablesImpl();
        ServerVariableImpl variableOne = new ServerVariableImpl();
        ServerVariableImpl variableTwo = new ServerVariableImpl();
        ServerVariableImpl variableThree = new ServerVariableImpl();

        serverVariables.addServerVariable("one", variableOne);
        serverVariables.addServerVariable("two", variableTwo);
        serverVariables.addServerVariable("three", variableThree);

        validator.validate(vh, context, serverVariables);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullServerVariables() {

        ServerVariablesValidator validator = ServerVariablesValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ServerVariablesImpl serverVariables = null;

        validator.validate(vh, context, serverVariables);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testServerVariablesWithNullKey() {

        ServerVariablesValidator validator = ServerVariablesValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ServerVariablesImpl serverVariables = new ServerVariablesImpl();
        ServerVariableImpl variableOne = new ServerVariableImpl();
        ServerVariableImpl variableTwo = new ServerVariableImpl();
        ServerVariableImpl variableThree = new ServerVariableImpl();

        serverVariables.addServerVariable(null, variableOne);
        serverVariables.addServerVariable("two", variableTwo);
        serverVariables.addServerVariable("three", variableThree);

        validator.validate(vh, context, serverVariables);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("must not be empty or null"));
    }

    @Test
    public void testServerVariablesWithEmptyKey() {

        ServerVariablesValidator validator = ServerVariablesValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ServerVariablesImpl serverVariables = new ServerVariablesImpl();
        ServerVariableImpl variableOne = new ServerVariableImpl();
        ServerVariableImpl variableTwo = new ServerVariableImpl();
        ServerVariableImpl variableThree = new ServerVariableImpl();

        serverVariables.addServerVariable("", variableOne);
        serverVariables.addServerVariable("two", variableTwo);
        serverVariables.addServerVariable("three", variableThree);

        validator.validate(vh, context, serverVariables);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("must not be empty or null"));
    }

    @Test
    public void testServerVariablesWithNullValue() {

        ServerVariablesValidator validator = ServerVariablesValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ServerVariablesImpl serverVariables = new ServerVariablesImpl();
        ServerVariableImpl variableOne = new ServerVariableImpl();
        ServerVariableImpl variableTwo = new ServerVariableImpl();

        serverVariables.addServerVariable("one", variableOne);
        serverVariables.addServerVariable("two", variableTwo);
        serverVariables.addServerVariable("three", null);

        validator.validate(vh, context, serverVariables);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("must not be null"));
    }

}
