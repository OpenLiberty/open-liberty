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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.Test;

import io.openliberty.mcp.content.AudioContent;
import io.openliberty.mcp.content.Content.Annotations;
import io.openliberty.mcp.content.ImageContent;
import io.openliberty.mcp.content.Role;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.meta.MetaKey;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class ToolContents {

    // Text Content
    @Test
    public void testTextConvenienceConstructor() {
        TextContent content = new TextContent("Hello world");

        assertEquals("Hello world", content.text());
        assertNull(content._meta());
        assertNull(content.annotations());
        assertEquals(TextContent.Type.TEXT, content.type());
        assertSame(content, content.asText());
    }

    @Test
    public void testTextContentWithNullMetaAndAnnotation() {
        TextContent content = new TextContent("Hello world", null, null);

        assertEquals("Hello world", content.text());
        assertNull(content._meta());
        assertNull(content.annotations());
        assertEquals(TextContent.Type.TEXT, content.type());
        assertSame(content, content.asText());
    }

    @Test
    public void testTextContentWithMetaAndAnnotation() {
        Map<MetaKey, Object> meta = Map.of();
        Annotations annotations = new Annotations(Role.USER, ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                          .format(DateTimeFormatter.ISO_INSTANT),
                                                  0.5);
        TextContent content = new TextContent("Hello world", meta, annotations);

        assertEquals("Hello world", content.text());
        assertEquals(meta, content._meta());
        assertEquals(annotations, content.annotations());
        assertEquals(TextContent.Type.TEXT, content.type());
        assertSame(content, content.asText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTextContentNullTextThrowsException() {
        new TextContent(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTextContentNullWithMetaThrowsException() {
        Map<MetaKey, Object> meta = Map.of();
        Annotations annotations = new Annotations(Role.USER, ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                          .format(DateTimeFormatter.ISO_INSTANT),
                                                  0.5);
        new TextContent(null, meta, annotations);
    }

    // Audio Content
    @Test
    public void testAudioContentConvienienceConstructor() {
        AudioContent content = new AudioContent("base64-encoded-audio", "audio/mpeg");

        assertEquals("base64-encoded-audio", content.data());
        assertEquals("audio/mpeg", content.mimeType());
        assertNull(content._meta());
        assertNull(content.annotations());
        assertEquals(AudioContent.Type.AUDIO, content.type());
        assertSame(content, content.asAudio());
    }

    @Test
    public void testAudioContentWithNullMetaAndAnnotation() {
        AudioContent content = new AudioContent("base64-encoded-audio", "audio/mpeg", null, null);

        assertEquals("base64-encoded-audio", content.data());
        assertEquals("audio/mpeg", content.mimeType());
        assertNull(content._meta());
        assertNull(content.annotations());
        assertEquals(TextContent.Type.AUDIO, content.type());
        assertSame(content, content.asAudio());
    }

    @Test
    public void testAudioContentWithMetaAndAnnotation() {
        Map<MetaKey, Object> meta = Map.of();
        Annotations annotations = new Annotations(Role.USER, ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                          .format(DateTimeFormatter.ISO_INSTANT),
                                                  0.5);
        AudioContent content = new AudioContent("base64-encoded-audio", "audio/mpeg", meta, annotations);

        assertEquals("base64-encoded-audio", content.data());
        assertEquals("audio/mpeg", content.mimeType());
        assertEquals(meta, content._meta());
        assertEquals(annotations, content.annotations());
        assertEquals(TextContent.Type.AUDIO, content.type());
        assertSame(content, content.asAudio());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAudioContentNullDataThrowsException() {
        new AudioContent(null, "audio/mpeg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAudioContentNullMimeThrowsException() {
        new AudioContent("base64-encoded-audio", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAudioContentNullDataAndMimeWithMetaThrowsException() {
        Map<MetaKey, Object> meta = Map.of();
        Annotations annotations = new Annotations(Role.USER, ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                          .format(DateTimeFormatter.ISO_INSTANT),
                                                  0.5);
        new AudioContent(null, null, meta, annotations);
    }

    // Image Content
    @Test
    public void testImageContentConvienienceConstructor() {
        ImageContent content = new ImageContent("base64-encoded-image", "image/png");

        assertEquals("base64-encoded-image", content.data());
        assertEquals("image/png", content.mimeType());
        assertNull(content._meta());
        assertNull(content.annotations());
        assertEquals(AudioContent.Type.IMAGE, content.type());
        assertSame(content, content.asImage());
    }

    @Test
    public void testImageContentWithNullMetaAndAnnotation() {
        ImageContent content = new ImageContent("base64-encoded-image", "image/png", null, null);

        assertEquals("base64-encoded-image", content.data());
        assertEquals("image/png", content.mimeType());
        assertNull(content._meta());
        assertNull(content.annotations());
        assertEquals(AudioContent.Type.IMAGE, content.type());
        assertSame(content, content.asImage());
    }

    @Test
    public void testImageContentWithMetaAndAnnotation() {
        Map<MetaKey, Object> meta = Map.of();
        Annotations annotations = new Annotations(Role.ASSISTANT, ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                               .format(DateTimeFormatter.ISO_INSTANT),
                                                  0.5);
        ImageContent content = new ImageContent("base64-encoded-image", "image/png", meta, annotations);

        assertEquals("base64-encoded-image", content.data());
        assertEquals("image/png", content.mimeType());
        assertEquals(meta, content._meta());
        assertEquals(annotations, content.annotations());
        assertEquals(AudioContent.Type.IMAGE, content.type());
        assertSame(content, content.asImage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageContentNullDataThrowsException() {
        new ImageContent(null, "image/png");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageContentNullMimeThrowsException() {
        new ImageContent("base64-encoded-image", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testImageContentNullDataAndMimeWithMetaThrowsException() {
        Map<MetaKey, Object> meta = Map.of();
        Annotations annotations = new Annotations(Role.ASSISTANT, ZonedDateTime.of(2025, 8, 26, 8, 40, 0, 0, ZoneOffset.UTC)
                                                                               .format(DateTimeFormatter.ISO_INSTANT),
                                                  0.5);
        new ImageContent(null, null, meta, annotations);
    }

    // Role Enum Serialization Test
    @Test
    public void testRoleEnumSerialization() {
        Jsonb jsonb = JsonbBuilder.create();

        Role role = Role.ASSISTANT;
        String roleEnumJSON = jsonb.toJson(role);
        assertEquals("\"assistant\"", roleEnumJSON);

        Role assistantRole = jsonb.fromJson("\"assistant\"", Role.class);
        assertEquals(Role.ASSISTANT, assistantRole);
    }

}
