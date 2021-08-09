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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.model.tags.TagImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OpenAPIValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

public class OpenAPIValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectOpenAPI() {

        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("3.0.1");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("pathItemName", new PathItemImpl());
        openapi.setPaths(paths);

        TagImpl tagOne = new TagImpl();
        tagOne.setName("tagOne");
        TagImpl tagTwo = new TagImpl();
        tagTwo.setName("tagTwo");
        TagImpl tagThree = new TagImpl();
        tagThree.setName("tagThree");
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        openapi.setTags(tags);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullOpenAPI() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = null;
        validator.validate(vh, context, openapi);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNoOpenAPI() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi(null);

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("pathItemName", new PathItemImpl());
        openapi.setPaths(paths);

        TagImpl tagOne = new TagImpl();
        tagOne.setName("tagOne");
        TagImpl tagTwo = new TagImpl();
        tagTwo.setName("tagTwo");
        TagImpl tagThree = new TagImpl();
        tagThree.setName("tagThree");
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        openapi.setTags(tags);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"openapi\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testNoInfoOpenAPI() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("3.0.1");

        openapi.setInfo(null);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("pathItemName", new PathItemImpl());
        openapi.setPaths(paths);

        TagImpl tagOne = new TagImpl();
        tagOne.setName("tagOne");
        TagImpl tagTwo = new TagImpl();
        tagTwo.setName("tagTwo");
        TagImpl tagThree = new TagImpl();
        tagThree.setName("tagThree");
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        openapi.setTags(tags);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"info\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testNoPathsOpenAPI() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("3.0.1");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        openapi.setPaths(null);

        TagImpl tagOne = new TagImpl();
        tagOne.setName("tagOne");
        TagImpl tagTwo = new TagImpl();
        tagTwo.setName("tagTwo");
        TagImpl tagThree = new TagImpl();
        tagThree.setName("tagThree");
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        openapi.setTags(tags);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"paths\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testOpenAPIWithInvalidVersion() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("4.0.1");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("pathItemName", new PathItemImpl());
        openapi.setPaths(paths);

        TagImpl tagOne = new TagImpl();
        tagOne.setName("tagOne");
        TagImpl tagTwo = new TagImpl();
        tagTwo.setName("tagTwo");
        TagImpl tagThree = new TagImpl();
        tagThree.setName("tagThree");
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        openapi.setTags(tags);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The OpenAPI Object must contain a valid OpenAPI specification version."));
    }

    @Test
    public void testNullTagsOpenAPI() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("3.0.1");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("pathItemName", new PathItemImpl());
        openapi.setPaths(paths);

        openapi.setTags(null);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testOpenAPITagsNotUnique() {
        OpenAPIValidator validator = OpenAPIValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl openapi = new OpenAPIImpl();

        openapi.setOpenapi("3.0.1");

        InfoImpl info = new InfoImpl();
        info.title("Test OpenAPI model").version("2.3.0");
        openapi.setInfo(info);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem("pathItemName", new PathItemImpl());
        openapi.setPaths(paths);

        TagImpl tagOne = new TagImpl();
        tagOne.setName("tagOne");
        TagImpl tagTwo = new TagImpl();
        tagTwo.setName("tagOne");
        TagImpl tagThree = new TagImpl();
        tagThree.setName("tagThree");
        List<Tag> tags = new ArrayList<Tag>();
        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        openapi.setTags(tags);

        validator.validate(vh, context, openapi);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The OpenAPI Object must contain unique tag names."));
    }
}
