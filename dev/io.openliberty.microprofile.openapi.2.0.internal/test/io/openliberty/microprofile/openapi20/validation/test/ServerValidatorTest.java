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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.ServerValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.servers.ServerImpl;
import io.smallrye.openapi.api.models.servers.ServerVariableImpl;

/**
 *
 */
public class ServerValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    private ServerValidator validator;
    private TestValidationHelper validationHelper;
    private ServerImpl server;

    @Before
    public void initialize() {
        validator = ServerValidator.getInstance();
        validationHelper = new TestValidationHelper();
        server = new ServerImpl();
    }

    @Test
    public void testValidSimpleServerValidator() {
        server.setUrl("https://development.gigantic-server.com/v1");

        validator.validate(validationHelper, context, server);
        Assert.assertEquals(0, validationHelper.getEventsSize());
    }

    @Test
    public void testValidServerValidator() {
        server.setUrl("https://{username}.gigantic-server.com:{port}/{basePath}");
        
        ServerVariableImpl var1 = new ServerVariableImpl();
        var1.setDefaultValue("test_username");
        server.addVariable("username", var1);

        ServerVariableImpl var2 = new ServerVariableImpl();
        var2.setDefaultValue("9080");
        server.addVariable("port", var2);

        ServerVariableImpl var3 = new ServerVariableImpl();
        var3.setDefaultValue("v1");
        server.addVariable("basePath", var3);

        validator.validate(validationHelper, context, server);
        Assert.assertEquals(0, validationHelper.getEventsSize());

    }

    @Test
    public void testInvalidServerValidator() {
        server.setUrl("https://{username}.gigantic-server.com:{port}/{basePath}");

        ServerVariableImpl var1 = new ServerVariableImpl();
        var1.setDefaultValue("test_username");
        server.addVariable("username", var1);

        ServerVariableImpl var2 = new ServerVariableImpl();
        var2.setDefaultValue("9080");
        server.addVariable("port", var2);

        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());

    }

    @Test
    public void testValidSimpleUrlServerValidator() {
        server.setUrl("https://development.gigantic-server.com/v1");

        ServerVariableImpl var1 = new ServerVariableImpl();
        var1.setDefaultValue("test_username");
        server.addVariable("username", var1);

        ServerVariableImpl var2 = new ServerVariableImpl();
        var2.setDefaultValue("9080");
        server.addVariable("port", var2);

        ServerVariableImpl var3 = new ServerVariableImpl();
        var3.setDefaultValue("v1");
        server.addVariable("basePath", var3);

        validator.validate(validationHelper, context, server);
        Assert.assertEquals(0, validationHelper.getEventsSize());

    }

    @Test
    public void testInvalidURLNoCloseBracket() {
        server.setUrl("https://{development.gigantic-server.com/v1");
        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testInvalidURLUnorderedBrackets() {
        server.setUrl("https://}development{.gigantic-server.com/v1");
        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testInvalidURLEmptyVariableName() {
        server.setUrl("https://{}.gigantic-server.com/v1");
        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testURLInvalidVariableName() {
        server.setUrl("https://{user{name}.gigantic-server.com/v1");
        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());

        validationHelper.resetResults();

        server.setUrl("https://{user/name}.gigantic-server.com/v1");
        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

    @Test
    public void testInvalidURLExtraCloseBracket() {
        server.setUrl("https://test.gi}gantic-server.com/v1");
        validator.validate(validationHelper, context, server);
        Assert.assertEquals(1, validationHelper.getEventsSize());
    }

}
