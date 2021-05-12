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
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.ServerVariableValidator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.servers.ServerVariableImpl;

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
