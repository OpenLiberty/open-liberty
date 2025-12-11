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
import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.successful;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.validation.test.OpenAPIValidatorTest;
import io.openliberty.microprofile.openapi40.internal.validation.OpenAPIDefinition31Validator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.info.InfoImpl;

public class OpenAPIDefinition31ValidatorTest extends OpenAPIValidatorTest {

    @Override
    protected TypeValidator<OpenAPI> getValidator() {
        return OpenAPIDefinition31Validator.getInstance();
    }

    @Test
    public void testComponentsOnly() {
        TypeValidator<OpenAPI> validator = getValidator();
        TestValidationHelper31 vh = new TestValidationHelper31();

        OpenAPIImpl openapi = new OpenAPIImpl();
        openapi.setOpenapi("3.1.0");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        openapi.setPaths(null);
        openapi.setComponents(new ComponentsImpl());
        openapi.setWebhooks(null);

        validator.validate(vh, context, openapi);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testWebhooksOnly() {
        TypeValidator<OpenAPI> validator = getValidator();
        TestValidationHelper31 vh = new TestValidationHelper31();

        OpenAPIImpl openapi = new OpenAPIImpl();
        openapi.setOpenapi("3.1.0");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        openapi.setPaths(null);
        openapi.setComponents(null);
        openapi.setWebhooks(Collections.emptyMap());

        validator.validate(vh, context, openapi);
        assertThat(vh.getResult(), successful());
    }

    @Override
    public void testNoPathsOpenAPI() {
        TypeValidator<OpenAPI> validator = getValidator();
        TestValidationHelper31 vh = new TestValidationHelper31();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("3.1.0");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        openapi.setPaths(null);

        validator.validate(vh, context, openapi);
        assertThat(vh.getResult(), hasError("The OpenAPI Object must contain at least one of the \"paths\", \"components\", or \"webhooks\" properties"));
    }

}
