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
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.InfoValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

public class InfoValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testNewInfoObject() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();//everything null
        validator.validate(vh, context, info);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"version\" field is missing or is set to an invalid value"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("Required \"title\" field is missing or is set to an invalid value"));

    }

    @Test
    public void testNullInfo() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = null;
        validator.validate(vh, context, info);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testInfoWithTitleOnly() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();//everything null
        info.setTitle("test");
        validator.validate(vh, context, info);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"version\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testInfoWithVersionOnly() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();//everything null
        info.setVersion("1.0");
        validator.validate(vh, context, info);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"title\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testInfoWithInvalidUrlForTermsOfService() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();
        info.setTitle("test");
        info.setVersion("1.0");
        info.setTermsOfService("notValidURL");
        validator.validate(vh, context, info);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Info Object must contain a valid URL"));
    }

    @Test
    public void testInfoWithNullTermsOfService() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();
        info.setTitle("test");
        info.setVersion("1.0");
        info.setTermsOfService(null);

        validator.validate(vh, context, info);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testCorrectInfo() {

        InfoValidator validator = InfoValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        InfoImpl info = new InfoImpl();
        info.setTitle("test");
        info.setVersion("1.0");
        info.setTermsOfService("http://myWebsite.com");

        validator.validate(vh, context, info);
        Assert.assertFalse(vh.hasEvents());
    }
}
