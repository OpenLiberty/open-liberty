/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.TagValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.smallrye.openapi.api.models.ExternalDocumentationImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.tags.TagImpl;

/**
 *
 */
public class TagValidatorTest {
    String key;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectTag() {

        TagValidator validator = TagValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        TagImpl tag = new TagImpl();
        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();
        tag.name("test-tag").description("This is a correctly set tag for testing").externalDocs(externalDocs);

        validator.validate(vh, context, key, tag);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullTag() {

        TagValidator validator = TagValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        TagImpl tag = null;

        validator.validate(vh, context, key, tag);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNoNameTag() {

        TagValidator validator = TagValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        TagImpl tag = new TagImpl();
        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();
        tag.description("This is a correctly set tag for testing").externalDocs(externalDocs);

        validator.validate(vh, context, key, tag);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"name\" field is missing or is set to an invalid value"));
    }

}
