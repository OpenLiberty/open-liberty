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

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.ContactImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ContactValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ContactValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);
    String key = null;

    @Test
    public void testCorrectContact() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = new ContactImpl();
        contact.setName("test_contact");
        contact.setEmail("mytestemail@gmail.com");
        contact.setUrl("http://test-url.com");

        validator.validate(vh, context, key, contact);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullCoontact() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = null;

        validator.validate(vh, context, key, contact);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testContactWithInvalidEmail() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = new ContactImpl();
        contact.setName("test_contact");
        contact.setEmail("mytestemail@gmail.");
        contact.setUrl("http://test-url.com");

        validator.validate(vh, context, key, contact);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Contact Object must contain a valid email address."));
    }

    @Test
    public void testContactWithInvalidUrl() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = new ContactImpl();
        contact.setName("test_contact");
        contact.setEmail("mytestemail@gmail.com");
        contact.setUrl("http/test-url.");

        validator.validate(vh, context, key, contact);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Contact Object must contain a valid URL."));
    }

    @Test
    public void testContactWithNullUrl() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = new ContactImpl();
        contact.setName("test_contact");
        contact.setEmail("mytestemail@gmail.com");
        contact.setUrl(null);

        validator.validate(vh, context, key, contact);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testContactWithNullEmail() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = new ContactImpl();
        contact.setName("test_contact");
        contact.setEmail(null);
        contact.setUrl("http://test-url.com");

        validator.validate(vh, context, key, contact);
        Assert.assertEquals(0, vh.getEventsSize());
    }
}
