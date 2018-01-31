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
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.LicenseImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ContactValidator;
import com.ibm.ws.microprofile.openapi.impl.validation.InfoValidator;
import com.ibm.ws.microprofile.openapi.impl.validation.LicenseValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

public class InfoValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testLicenseValidator() {
        LicenseValidator validator = LicenseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LicenseImpl license = new LicenseImpl();//everything null
        validator.validate(vh, context, license);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();
        license.setUrl("notAValidURL");
        validator.validate(vh, context, license);
        Assert.assertEquals(2, vh.getEventsSize());

        vh.resetResults();
        license.setName("Apache 2.0");
        license.setUrl("http://myWebsite.com");
        validator.validate(vh, context, license);
        Assert.assertFalse(vh.hasEvents());
    }

    @Test
    public void testInfoValidator() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();//everything null
        validator.validate(vh, context, info);
        Assert.assertEquals(2, vh.getEventsSize());

        vh.resetResults();
        info.setTitle("test");
        validator.validate(vh, context, info);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();
        info.setVersion("1.0");
        validator.validate(vh, context, info);
        Assert.assertFalse(vh.hasEvents());

        vh.resetResults();
        info.setTermsOfService("notValidURL");
        validator.validate(vh, context, info);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();
        info.setTermsOfService("http://myWebsite.com");
        validator.validate(vh, context, info);
        Assert.assertFalse(vh.hasEvents());
    }

    @Test
    public void testContactValidator() {
        ContactValidator validator = ContactValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ContactImpl contact = new ContactImpl();//everything null
        validator.validate(vh, context, contact);
        Assert.assertFalse(vh.hasEvents());

        vh.resetResults();
        contact.setName("test");
        validator.validate(vh, context, contact);
        Assert.assertFalse(vh.hasEvents());

        vh.resetResults();
        contact.setUrl("notValidURL");
        validator.validate(vh, context, contact);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();
        contact.setUrl("http://myWebsite.com");
        validator.validate(vh, context, contact);
        Assert.assertFalse(vh.hasEvents());

        vh.resetResults();
        contact.setEmail("invalidEmail");
        validator.validate(vh, context, contact);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();
        contact.setEmail("myEmail@myCompany.com");
        validator.validate(vh, context, contact);
        Assert.assertFalse(vh.hasEvents());
    }
}
