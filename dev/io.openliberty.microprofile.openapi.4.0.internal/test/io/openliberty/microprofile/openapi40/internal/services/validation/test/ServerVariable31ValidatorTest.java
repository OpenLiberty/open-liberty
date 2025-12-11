/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.validation.test;

import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.hasError;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.validation.test.ServerVariableValidatorTest;
import io.openliberty.microprofile.openapi40.internal.validation.ServerVariable31Validator;
import io.smallrye.openapi.api.models.servers.ServerVariableImpl;

public class ServerVariable31ValidatorTest extends ServerVariableValidatorTest {

    @Override
    protected TypeValidator<ServerVariable> getValidator() {
        return ServerVariable31Validator.getInstance();
    }

    @Test
    public void testEmptyEnum() {
        TypeValidator<ServerVariable> validator = getValidator();
        TestValidationHelper validationHelper = new TestValidationHelper();

        ServerVariableImpl serverVariable = new ServerVariableImpl();
        serverVariable.defaultValue("foo")
                      .enumeration(emptyList());

        validator.validate(validationHelper, context, serverVariable);
        assertThat(validationHelper.getResult(), hasError("The \"enum\" array in the Server Variable Object is empty"));
    }

    @Test
    public void testDefaultMissingFromEnum() {
        TypeValidator<ServerVariable> validator = getValidator();
        TestValidationHelper validationHelper = new TestValidationHelper();

        ServerVariableImpl serverVariable = new ServerVariableImpl();
        serverVariable.defaultValue("foo")
                      .enumeration(List.of("bar", "baz", "qux"));

        validator.validate(validationHelper, context, serverVariable);
        assertThat(validationHelper.getResult(), hasError("The \"foo\" value of the \"default\" property is not listed in the \"enum\" array"));
    }

}
