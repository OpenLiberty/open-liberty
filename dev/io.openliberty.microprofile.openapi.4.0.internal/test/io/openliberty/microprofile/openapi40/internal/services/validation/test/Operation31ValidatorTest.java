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

import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.successful;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.microprofile.openapi.models.Operation;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.validation.test.OperationValidatorTest;
import io.openliberty.microprofile.openapi40.internal.validation.Operation31Validator;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;

/**
 *
 */
public class Operation31ValidatorTest extends OperationValidatorTest {

    @Override
    protected TypeValidator<Operation> getValidator() {
        return Operation31Validator.getInstance();
    }

    @Override
    public void testOperationWithNoResponses() {
        TypeValidator<Operation> validator = getValidator();
        TestValidationHelper31 vh = new TestValidationHelper31();

        String pathNameOne = "/my-test-path-one/";

        PathItemImpl pathItemOne = new PathItemImpl();
        OperationImpl getPathItemOne = new OperationImpl();
        getPathItemOne.operationId("pathItemOneGetId");
        pathItemOne.setGET(getPathItemOne);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem(pathNameOne, pathItemOne);

        OpenAPIImpl model = new OpenAPIImpl();
        Context context = new TestValidationContextHelper(model);
        model.setPaths(paths);

        validator.validate(vh, context, null, getPathItemOne);
        // In 3.1, having no responses is valid
        assertThat(vh.getResult(), successful());
    }

}
