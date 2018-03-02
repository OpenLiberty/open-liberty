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
package com.ibm.ws.microprofile.impl.parser.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.tags.TagImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class TagParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);
    List<Tag> tags = new ArrayList<Tag>();

    @Before
    public void testForNullResult() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        TagImpl tagOne = new TagImpl();
        tagOne.name("Airlines").description("airlines app");
        TagImpl tagTwo = new TagImpl();
        tagTwo.name("airline").description("all the airlines methods");
        TagImpl tagThree = new TagImpl();
        tagThree.name("availability").description("all the availibility methods");
        TagImpl tagFour = new TagImpl();
        tagFour.name("bookings").description("all the bookings methods");
        TagImpl tagFive = new TagImpl();
        tagFive.name("reviews").description("all the review methods");

        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        tags.add(tagFour);
        tags.add(tagFive);
    }

    @Test
    public void testOpenAPITags() {
        assertNotNull(result.getOpenAPI().getTags());
        for (Tag tag : result.getOpenAPI().getTags()) {
            assertNotNull(tag);
        }

        for (Tag tag : result.getOpenAPI().getTags()) {
            assertTrue(tags.contains(tag));
        }
    }

    @Test
    public void testOperationTags() {
        PathItemImpl path = new PathItemImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(1).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(2).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(3).getName()));

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getTags());
        assertTrue(path.getPOST().getTags().contains(tags.get(3).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(3).getName()));

        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getTags());
        assertTrue(path.getPUT().getTags().contains(tags.get(3).getName()));

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getTags());
        assertTrue(path.getDELETE().getTags().contains(tags.get(3).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getTags());
        assertTrue(path.getPOST().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getTags());
        assertTrue(path.getDELETE().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}/{airlines}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));
    }

}
