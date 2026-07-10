/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mcpjava.server.Role;
import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.AudioContent;
import org.mcpjava.server.content.EmbeddedResource;
import org.mcpjava.server.content.ImageContent;
import org.mcpjava.server.content.ResourceLink;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.ToolResponse;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;

public class ToolContents {

    // Test data
    /** 1x1 red png */
    public static final byte[] TEST_IMAGE_DATA = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==");
    /** Minimal wav header */
    public static final byte[] TEST_AUDIO_DATA = Base64.getDecoder().decode("UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQAAAAA=");

    private static Jsonb jsonb = TestUtils.createJsonb();

    // Text Content
    @Test
    public void testTextConvenienceConstructor() {
        TextContent content = TextContent.of("Hello world");

        assertEquals("Hello world", content.text());
        assertThat(content.metadata().entrySet(), is(empty()));
        assertEquals(Optional.empty(), content.annotations());
    }

    @Test
    public void testTextContentWithNullMetaAndAnnotation() {
        TextContent content = TextContent.builder("Hello world")
                                         .build();

        assertEquals("Hello world", content.text());
        assertThat(content.metadata().entrySet(), is(empty()));
        assertEquals(Optional.empty(), content.annotations());
    }

    @Test
    public void testTextContentWithMetaAndAnnotation() {
        Map<String, Object> meta = Map.of("a", "b");
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .build();
        TextContent content = TextContent.builder("Hello world")
                                         .setMetadata(meta)
                                         .setAnnotations(annotations)
                                         .build();

        assertEquals("Hello world", content.text());
        assertEquals(meta, content.metadata());
        assertEquals(Optional.of(annotations), content.annotations());
    }

    @Test
    public void testTextContentPutMetadata() {
        var textContent = TextContent.builder("hello")
                                     .putMetadata("a", "b")
                                     .build();
        assertEquals("hello", textContent.text());
        assertThat(textContent.metadata(), Matchers.hasEntry("a", "b"));
    }

    @Test
    public void testTextContentSerialization() {
        var textContent = TextContent.builder("hello")
                                     .putMetadata("a", "b")
                                     .build();
        JSONAssert.assertEquals("""
                        {
                            "type": "text",
                            "text": "hello",
                            "_meta": {
                                "a": "b"
                            }
                        }
                        """,
                                jsonb.toJson(textContent),
                                JSONCompareMode.STRICT);
    }

    @Test(expected = NullPointerException.class)
    public void testTextContentNullTextThrowsException() {
        TextContent.of(null);
    }

    @Test(expected = NullPointerException.class)
    public void testTextContentNullWithMetaThrowsException() {
        Map<String, Object> meta = Map.of();
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .build();
        TextContent.builder(null)
                   .setAnnotations(annotations)
                   .setMetadata(meta)
                   .build();
    }

    // Audio Content
    @Test
    public void testAudioContentConvienienceConstructor() {
        AudioContent content = AudioContent.of(TEST_AUDIO_DATA, "audio/mpeg");

        assertEquals(TEST_AUDIO_DATA, content.data());
        assertEquals("audio/mpeg", content.mimeType());
        assertThat(content.metadata().entrySet(), is(empty()));
        assertEquals(Optional.empty(), content.annotations());
    }

    @Test
    public void testAudioContentWithNullMetaAndAnnotation() {
        AudioContent content = AudioContent.builder(TEST_AUDIO_DATA, "audio/mpeg")
                                           .build();

        assertEquals(TEST_AUDIO_DATA, content.data());
        assertEquals("audio/mpeg", content.mimeType());
        assertThat(content.metadata().entrySet(), is(empty()));
        assertEquals(Optional.empty(), content.annotations());
    }

    @Test
    public void testAudioContentWithMetaAndAnnotation() {
        Map<String, Object> meta = Map.of();
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .setPriority(0.5)
                                             .build();
        AudioContent content = AudioContent.builder(TEST_AUDIO_DATA, "audio/mpeg")
                                           .setMetadata(meta)
                                           .setAnnotations(annotations)
                                           .build();

        assertEquals(TEST_AUDIO_DATA, content.data());
        assertEquals("audio/mpeg", content.mimeType());
        assertEquals(meta, content.metadata());
        assertEquals(Optional.of(annotations), content.annotations());
    }

    @Test(expected = NullPointerException.class)
    public void testAudioContentNullDataThrowsException() {
        AudioContent.of(null, "audio/mpeg");
    }

    @Test(expected = NullPointerException.class)
    public void testAudioContentNullMimeThrowsException() {
        AudioContent.of(TEST_AUDIO_DATA, null);
    }

    @Test(expected = NullPointerException.class)
    public void testAudioContentNullDataAndMimeWithMetaThrowsException() {
        Map<String, Object> meta = Map.of();
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .setPriority(0.5)
                                             .build();
        AudioContent.builder(null, null)
                    .setMetadata(meta)
                    .setAnnotations(annotations)
                    .build();
    }

    // Image Content
    @Test
    public void testImageContentConvienienceConstructor() {
        ImageContent content = ImageContent.of(TEST_IMAGE_DATA, "image/png");

        assertEquals(TEST_IMAGE_DATA, content.data());
        assertEquals("image/png", content.mimeType());
        assertEquals(emptyMap(), content.metadata());
        assertEquals(Optional.empty(), content.annotations());
    }

    @Test
    public void testImageContentWithNullMetaAndAnnotation() {
        ImageContent content = ImageContent.builder(TEST_IMAGE_DATA, "image/png")
                                           .build();

        assertEquals(TEST_IMAGE_DATA, content.data());
        assertEquals("image/png", content.mimeType());
        assertEquals(emptyMap(), content.metadata());
        assertEquals(Optional.empty(), content.annotations());
    }

    @Test
    public void testImageContentWithMetaAndAnnotation() {
        Map<String, Object> meta = Map.of();
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.ASSISTANT)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .setPriority(0.5)
                                             .build();
        ImageContent content = ImageContent.builder(TEST_IMAGE_DATA, "image/png")
                                           .setMetadata(meta)
                                           .setAnnotations(annotations)
                                           .build();

        assertEquals(TEST_IMAGE_DATA, content.data());
        assertEquals("image/png", content.mimeType());
        assertEquals(emptyMap(), content.metadata());
        assertEquals(Optional.of(annotations), content.annotations());
    }

    @Test(expected = NullPointerException.class)
    public void testImageContentNullDataThrowsException() {
        ImageContent.of(null, "image/png");
    }

    @Test(expected = NullPointerException.class)
    public void testImageContentNullMimeThrowsException() {
        ImageContent.builder(TEST_IMAGE_DATA, null).build();
    }

    @Test(expected = NullPointerException.class)
    public void testImageContentNullDataAndMimeWithMetaThrowsException() {
        Map<String, Object> meta = Map.of();
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.ASSISTANT)
                                             .setLastModified(ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC).toInstant())
                                             .setPriority(0.5)
                                             .build();
        ImageContent.builder(null, null)
                    .setMetadata(meta)
                    .setAnnotations(annotations)
                    .build();
    }

    // Role Enum Serialization Test
    @Test
    public void testRoleEnumSerialization() {
        Jsonb jsonb = TestUtils.createJsonb();

        Role role = Role.ASSISTANT;
        String roleEnumJSON = jsonb.toJson(role);
        assertEquals("\"assistant\"", roleEnumJSON);

        Role assistantRole = jsonb.fromJson("\"assistant\"", Role.class);
        assertEquals(Role.ASSISTANT, assistantRole);
    }

    @Test
    public void testToolResponseSerialization() {
        var toolResponse = ToolResponse.builder()
                                       .addTextContent("OK")
                                       .build();

        JSONAssert.assertEquals("""
                        {
                          "content": [
                            {
                                "type": "text",
                                "text": "OK",
                            }
                          ],
                          "isError": false
                        }
                        """,
                                jsonb.toJson(toolResponse),
                                JSONCompareMode.STRICT);

    }

    // ResourceLink Content
    @Test
    public void testResourceLinkWithRequiredFieldsOnly() {
        ResourceLink link = ResourceLink.builder("my-doc", "file:///readme.md")
                                        .build();

        assertEquals("my-doc", link.name());
        assertEquals("file:///readme.md", link.uri());
        assertThat(link.metadata().entrySet(), is(empty()));
        assertEquals(Optional.empty(), link.annotations());
        assertEquals(Optional.empty(), link.description());
        assertEquals(Optional.empty(), link.mimeType());
    }

    @Test
    public void testResourceLinkWithAllFields() {
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.USER)
                                             .build();
        ResourceLink link = ResourceLink.builder("my-doc", "file:///readme.md")
                                        .setTitle("Read Me")
                                        .setDescription("A readme file")
                                        .setMimeType("text/plain")
                                        .setSize(1024L)
                                        .setAnnotations(annotations)
                                        .build();

        assertEquals("my-doc", link.name());
        assertEquals("file:///readme.md", link.uri());
        assertEquals("Read Me", link.title());
        assertEquals(Optional.of("A readme file"), link.description());
        assertEquals(Optional.of("text/plain"), link.mimeType());
        assertEquals(1024L, link.size().getAsLong());
        assertEquals(Optional.of(annotations), link.annotations());
    }

    @Test(expected = NullPointerException.class)
    public void testResourceLinkNullNameThrowsException() {
        ResourceLink.builder(null, "file:///readme.md").build();
    }

    @Test(expected = NullPointerException.class)
    public void testResourceLinkNullUriThrowsException() {
        ResourceLink.builder("my-doc", null).build();
    }

    @Test
    public void testResourceLinkSerialization() {
        ResourceLink link = ResourceLink.builder("my-doc", "file:///readme.md")
                                        .setTitle("Read Me")
                                        .setMimeType("text/plain")
                                        .setSize(1024L)
                                        .build();

        JSONAssert.assertEquals("""
                        {
                            "type": "resource_link",
                            "name": "my-doc",
                            "uri": "file:///readme.md",
                            "title": "Read Me",
                            "mimeType": "text/plain",
                            "size": 1024
                        }
                        """,
                                jsonb.toJson(link),
                                JSONCompareMode.STRICT);
    }

    @Test
    public void testResourceLinkSerializationMinimal() {
        ResourceLink link = ResourceLink.builder("readme", "file:///readme.md")
                                        .build();

        JSONAssert.assertEquals("""
                        {
                            "type": "resource_link",
                            "name": "readme",
                            "uri": "file:///readme.md"
                        }
                        """,
                                jsonb.toJson(link),
                                JSONCompareMode.STRICT);
    }

    // EmbeddedResource Content
    @Test
    public void testEmbeddedTextResourceWithRequiredFieldsOnly() {
        EmbeddedResource resource = EmbeddedResource.builder("Hello world", "file:///readme.md")
                                                    .build();

        assertThat(resource.metadata().entrySet(), is(empty()));
        assertEquals(Optional.empty(), resource.annotations());
    }

    @Test
    public void testEmbeddedTextResourceWithAllFields() {
        Annotations annotations = Annotations.builder()
                                             .setAudience(Role.ASSISTANT)
                                             .build();
        EmbeddedResource resource = EmbeddedResource.builder("Hello world", "file:///readme.md")
                                                    .setMimeType("text/plain")
                                                    .setAnnotations(annotations)
                                                    .build();

        assertEquals(Optional.of(annotations), resource.annotations());
    }

    @Test(expected = NullPointerException.class)
    public void testEmbeddedTextResourceNullTextThrowsException() {
        EmbeddedResource.builder((String) null, "file:///readme.md").build();
    }

    @Test(expected = NullPointerException.class)
    public void testEmbeddedTextResourceNullUriThrowsException() {
        EmbeddedResource.builder("Hello world", (String) null).build();
    }

    @Test(expected = NullPointerException.class)
    public void testEmbeddedBlobResourceNullDataThrowsException() {
        EmbeddedResource.builder((byte[]) null, "file:///image.png").build();
    }

    @Test(expected = NullPointerException.class)
    public void testEmbeddedBlobResourceNullUriThrowsException() {
        EmbeddedResource.builder(TEST_IMAGE_DATA, (String) null).build();
    }

    @Test
    public void testEmbeddedTextResourceSerialization() {
        EmbeddedResource resource = EmbeddedResource.builder("Hello world", "file:///readme.md")
                                                    .setMimeType("text/plain")
                                                    .build();

        JSONAssert.assertEquals("""
                        {
                            "type": "resource",
                            "resource": {
                                "uri": "file:///readme.md",
                                "text": "Hello world",
                                "mimeType": "text/plain"
                            }
                        }
                        """,
                                jsonb.toJson(resource),
                                JSONCompareMode.STRICT);
    }

    @Test
    public void testEmbeddedBlobResourceSerialization() {
        EmbeddedResource resource = EmbeddedResource.builder(TEST_IMAGE_DATA, "file:///image.png")
                                                    .setMimeType("image/png")
                                                    .build();

        String expectedBlob = Base64.getEncoder().encodeToString(TEST_IMAGE_DATA);
        JSONAssert.assertEquals("""
                        {
                            "type": "resource",
                            "resource": {
                                "uri": "file:///image.png",
                                "blob": "%s",
                                "mimeType": "image/png"
                            }
                        }
                        """.formatted(expectedBlob),
                                jsonb.toJson(resource),
                                JSONCompareMode.STRICT);
    }

}
