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

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.validation.test.PathItemValidatorTest;
import io.openliberty.microprofile.openapi40.internal.validation.PathItem31Validator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;

public class PathItem31ValidatorTest extends PathItemValidatorTest {

    @Override
    protected TypeValidator<PathItem> getValidator() {
        return PathItem31Validator.getInstance();
    }

    /**
     * Internal refs are valid in path items in 3.1
     */
    @Override
    @Test
    public void testInternalRefInPathItem() {
        String key = "{username}";

        TypeValidator<PathItem> validator = getValidator();
        TestValidationHelper31 vh = new TestValidationHelper31();

        // Need a model with a PathItem to reference
        PathItemImpl sharedPathItem = new PathItemImpl();
        sharedPathItem.GET(new OperationImpl().description("testOp"));

        Components components = new ComponentsImpl();
        components.addPathItem("sharedPathItem", sharedPathItem);

        OpenAPI model = new OpenAPIImpl().components(components);

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.ref("#/components/pathItems/sharedPathItem");

        Context context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, pathItem);
        assertThat(vh.getResult(), successful());
    }
}
