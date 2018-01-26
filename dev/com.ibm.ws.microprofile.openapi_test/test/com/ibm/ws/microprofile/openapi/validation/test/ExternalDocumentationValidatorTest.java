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

import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ExternalDocumentationValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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

        externalDocs.setUrl(null);
        validator.validate(vh, context, externalDocs);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testExternalDocumentationInvalidUrl() {

        ExternalDocumentationImpl externalDocs = new ExternalDocumentationImpl();

        vh.resetResults();
        externalDocs.setUrl("notAValidURL");
        validator.validate(vh, context, externalDocs);
        Assert.assertEquals(1, vh.getEventsSize());

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
