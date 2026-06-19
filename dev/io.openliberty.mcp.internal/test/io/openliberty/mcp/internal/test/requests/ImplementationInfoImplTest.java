/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.requests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mcpjava.server.Icon;
import org.mcpjava.server.Icon.Theme;
import org.mcpjava.server.ImplementationInfo;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.internal.requests.IconImpl;
import io.openliberty.mcp.internal.requests.ImplementationInfoImpl;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

public class ImplementationInfoImplTest {

    private static final String TEST_ICON_URL = "https://example.org/icon";

    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        JsonbConfig config = new JsonbConfig();
        config.withAdapters(new IconImpl.Adapter(),
                            new ImplementationInfoImpl.Adapter());
        jsonb = JsonbBuilder.create(config);
    }

    @AfterClass
    public static void teardown() {
        jsonb = null;
    }

    @Test
    public void testImplementationInfoSerialization() throws JSONException {
        List<Icon> icons = List.of(createIcon(32, 32, Theme.LIGHT),
                                   createIcon(32, 32, Theme.DARK));
        ImplementationInfo info = new ImplementationInfoImpl(icons,
                                                             "example-client",
                                                             "Example Client",
                                                             "1.0",
                                                             "An example client",
                                                             "http://example.com");

        JSONObject infoJson = new JSONObject().put("icons", List.of(iconJson(32, 32, Theme.LIGHT),
                                                                    iconJson(32, 32, Theme.DARK)))
                                              .put("name", "example-client")
                                              .put("title", "Example Client")
                                              .put("version", "1.0")
                                              .put("description", "An example client")
                                              .put("websiteUrl", "http://example.com");

        JSONAssert.assertEquals(infoJson, new JSONObject(jsonb.toJson(info)), JSONCompareMode.STRICT);
    }

    @Test
    public void testImplementationInfoDeserialization() throws JSONException {
        JSONObject infoJson = new JSONObject().put("icons", List.of(iconJson(32, 32, Theme.LIGHT),
                                                                    iconJson(32, 32, Theme.DARK)))
                                              .put("name", "example-client")
                                              .put("title", "Example Client")
                                              .put("version", "1.0")
                                              .put("description", "An example client")
                                              .put("websiteUrl", "http://example.com");

        ImplementationInfo info = jsonb.fromJson(infoJson.toString(), ImplementationInfoImpl.class);

        assertEquals("example-client", info.name());
        assertEquals("Example Client", info.title());
        assertEquals("1.0", info.version());
        assertEquals("An example client", info.description().get());
        assertEquals("http://example.com", info.websiteUrl().get());
        assertEquals(List.of(createIcon(32, 32, Theme.LIGHT), createIcon(32, 32, Theme.DARK)), info.icons());
    }

    public Icon createIcon(int width, int height, Theme theme) {
        return Icon.builder(TEST_ICON_URL + "/" + width + "x" + height + "/" + theme.name().toLowerCase())
                   .addSize(width, height)
                   .setTheme(theme)
                   .setMimeType("image/png")
                   .build();
    }

    public JSONObject iconJson(int width, int height, Theme theme) throws JSONException {
        return new JSONObject().put("src", TEST_ICON_URL + "/" + width + "x" + height + "/" + theme.name().toLowerCase())
                               .put("mimeType", "image/png")
                               .put("sizes", List.of(width + "x" + height))
                               .put("theme", theme.name().toLowerCase());
    }
}
