/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.test.merge.parts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.junit.Assert;
import org.junit.Test;

public class TagsMergeTest {

    @Test
    public void testMergingTags() {

        OpenAPI primaryOpenAPI;

        OpenAPI doc1 = OASFactory.createOpenAPI();
        doc1.addTag(OASFactory.createTag().name("common"));
        doc1.addTag(OASFactory.createTag().name("dog"));
        doc1.addTag(OASFactory.createTag().name("cat"));

        primaryOpenAPI = TestUtil.merge(doc1);

        Assert.assertNotNull("Tags is null", primaryOpenAPI.getTags());
        Assert.assertEquals(3, primaryOpenAPI.getTags().size());
        validateTags(primaryOpenAPI.getTags(), "common", "dog", "cat");

        OpenAPI doc2 = OASFactory.createOpenAPI();
        doc2.addTag(OASFactory.createTag().name("common"));
        doc2.addTag(OASFactory.createTag().name("users"));
        doc2.addTag(OASFactory.createTag().name("events"));

        primaryOpenAPI = TestUtil.merge(doc1, doc2);

        Assert.assertNotNull("Tags is null", primaryOpenAPI.getTags());
        Assert.assertEquals(5, primaryOpenAPI.getTags().size());
        validateTags(primaryOpenAPI.getTags(), "common", "dog", "cat", "users", "events");

        OpenAPI doc3 = OASFactory.createOpenAPI();
        doc3.addTag(OASFactory.createTag().name("common"));
        doc3.addTag(OASFactory.createTag().name("feed"));
        doc3.addTag(OASFactory.createTag().name("status"));

        primaryOpenAPI = TestUtil.merge(doc1, doc2, doc3);

        Assert.assertNotNull("Tags is null", primaryOpenAPI.getTags());
        Assert.assertEquals(7, primaryOpenAPI.getTags().size());
        validateTags(primaryOpenAPI.getTags(), "common", "dog", "cat", "users", "events", "feed", "status");

        primaryOpenAPI = TestUtil.merge(doc1, doc3);
        Assert.assertNotNull("Tags is null", primaryOpenAPI.getTags());
        Assert.assertEquals(5, primaryOpenAPI.getTags().size());
        validateTags(primaryOpenAPI.getTags(), "common", "dog", "cat", "feed", "status");

        primaryOpenAPI = TestUtil.merge(doc3);
        Assert.assertNotNull("Tags is null", primaryOpenAPI.getTags());
        Assert.assertEquals(3, primaryOpenAPI.getTags().size());
        validateTags(primaryOpenAPI.getTags(), "common", "feed", "status");

        primaryOpenAPI = TestUtil.merge();
        Assert.assertNull("Tags is not null", primaryOpenAPI.getTags());
    }

    private void validateTags(List<Tag> tags, String... expected) {
        Set<String> expectedTagNames = new HashSet<>(Arrays.asList(expected));
        Set<String> currentTagNames = tags.stream().map(Tag::getName).collect(Collectors.toSet());
        Assert.assertEquals(expectedTagNames, currentTagNames);
    }

}
