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

import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.ExternalDocumentationValidator;
import io.smallrye.openapi.api.models.ExternalDocumentationImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import junit.framework.Assert;

/**
 *
 */
public class ExternalDocumentationValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    ExternalDocumentationValidator validator = ExternalDocumentationValidator.getInstance();
    TestValidationHelper vh = new TestValidationHelper();

    @Test
    public void testExternalDocumentationRequiredUrlFieldNull() {

        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();

        vh.resetResults();
        externalDocs.setUrl(null);
        validator.validate(vh, context, externalDocs);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testExternalDocumentationInvalidUrl() {

        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();

        vh.resetResults();
        externalDocs.setUrl(":notAValidURL");
        validator.validate(vh, context, externalDocs);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testExternalDocumentationRelativeUrl() {

        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();

        vh.resetResults();
        externalDocs.setUrl("/../relativeURL");
        validator.validate(vh, context, externalDocs);
        Assert.assertEquals(0, vh.getEventsSize());

    }

    @Test
    public void testExternalDocumentationCorrect() {

        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();

        vh.resetResults();
        externalDocs.setUrl("http://myWebsite.com");
        validator.validate(vh, context, externalDocs);
        Assert.assertFalse(vh.hasEvents());

    }
}
