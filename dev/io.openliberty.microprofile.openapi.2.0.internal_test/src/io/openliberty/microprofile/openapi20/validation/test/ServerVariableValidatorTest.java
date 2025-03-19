/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.ServerVariableValidator;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.servers.ServerVariableImpl;

/**
 *
 */
public class ServerVariableValidatorTest {

    protected OpenAPIImpl model = new OpenAPIImpl();
    protected Context context = new TestValidationContextHelper(model);

    protected TypeValidator<ServerVariable> getValidator() {
        return ServerVariableValidator.getInstance();
    }

    @Test
    public void testServerVariableTest() {
        TypeValidator<ServerVariable> validator = getValidator();
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
