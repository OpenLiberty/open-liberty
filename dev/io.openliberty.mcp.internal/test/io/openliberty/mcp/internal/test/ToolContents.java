/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import org.junit.Test;
import org.mcpjava.server.Role;
import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.AudioContent;
import org.mcpjava.server.content.ImageContent;
import org.mcpjava.server.content.TextContent;

import io.openliberty.mcp.internal.content.RoleAdapter;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

public class ToolContents {

    // Test data
    /** 1x1 red png */
    public static final byte[] TEST_IMAGE_DATA = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==");
    /** Minimal wav header */
    public static final byte[] TEST_AUDIO_DATA = Base64.getDecoder().decode("UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQAAAAA=");

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
        Map<String, Object> meta = Map.of();
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
        JsonbConfig config = new JsonbConfig().withAdapters(new RoleAdapter());
        Jsonb jsonb = JsonbBuilder.create(config);

        Role role = Role.ASSISTANT;
        String roleEnumJSON = jsonb.toJson(role);
        assertEquals("\"assistant\"", roleEnumJSON);

        Role assistantRole = jsonb.fromJson("\"assistant\"", Role.class);
        assertEquals(Role.ASSISTANT, assistantRole);
    }

}
