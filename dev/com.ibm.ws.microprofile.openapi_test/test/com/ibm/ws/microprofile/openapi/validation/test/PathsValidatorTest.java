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
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.PathsValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class PathsValidatorTest {

    String key;
    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectPaths() {

        PathsValidator validator = PathsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathsImpl paths = new PathsImpl();
        PathItemImpl pathItem = new PathItemImpl();
        paths.addPathItem("/test-path-item", pathItem);

        validator.validate(vh, context, paths);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullPaths() {

        PathsValidator validator = PathsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathsImpl paths = null;

        validator.validate(vh, context, paths);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullKeyInPaths() {

        PathsValidator validator = PathsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathsImpl paths = new PathsImpl();
        PathItemImpl pathItem = new PathItemImpl();
        paths.addPathItem(null, pathItem);

        validator.validate(vh, context, paths);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The map contains an invalid key. A map should not have empty or null keys"));
    }

    @Test
    public void testEmptyKeyInPaths() {

        PathsValidator validator = PathsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathsImpl paths = new PathsImpl();
        PathItemImpl pathItem = new PathItemImpl();
        paths.addPathItem("", pathItem);

        validator.validate(vh, context, paths);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The map contains an invalid key. A map should not have empty or null keys"));
    }

    @Test
    public void testNullValueInPaths() {

        PathsValidator validator = PathsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("/test-path-item", null);

        validator.validate(vh, context, paths);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The map specifies an invalid value for the \"/test-path-item\" key. A map should not have null values"));
    }

    @Test
    public void testInvalidKeyInPaths() {

        PathsValidator validator = PathsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathsImpl paths = new PathsImpl();
        PathItemImpl pathItem = new PathItemImpl();
        paths.addPathItem("test-path-item", pathItem);

        validator.validate(vh, context, paths);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("path value does not begin with a slash"));
    }

}
