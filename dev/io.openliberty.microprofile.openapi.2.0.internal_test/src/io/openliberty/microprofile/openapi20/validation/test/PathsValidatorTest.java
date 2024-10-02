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
import io.openliberty.microprofile.openapi20.internal.validation.PathsValidator;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;

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
