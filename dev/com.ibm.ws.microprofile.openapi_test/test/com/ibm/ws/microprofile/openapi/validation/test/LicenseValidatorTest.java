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
import com.ibm.ws.microprofile.openapi.impl.model.info.LicenseImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.LicenseValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class LicenseValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testNewLicenseObject() {
        LicenseValidator validator = LicenseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LicenseImpl license = new LicenseImpl();
        validator.validate(vh, context, license);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"name\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testNullLicense() {
        LicenseValidator validator = LicenseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LicenseImpl license = null;
        validator.validate(vh, context, license);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testLicenseWithInvalidUrl() {
        LicenseValidator validator = LicenseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LicenseImpl license = new LicenseImpl();
        license.setName("Apache 2.0");
        license.setUrl("notAValidURL");
        validator.validate(vh, context, license);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The License object must contain a valid URL"));
    }

    @Test
    public void testCorrectLicense() {
        LicenseValidator validator = LicenseValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LicenseImpl license = new LicenseImpl();
        license.setName("Apache 2.0");
        license.setUrl("http://myWebsite.com");

        validator.validate(vh, context, license);
        Assert.assertFalse(vh.hasEvents());
    }

}
