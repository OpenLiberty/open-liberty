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
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.internal.requests.IconImpl;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

/**
 *
 */
public class IconImplTest {

    /**  */
    private static final String TEST_ICON_URL = "https://example.org/icon";

    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        JsonbConfig config = new JsonbConfig();
        config.withAdapters(new IconImpl.Adapter());
        jsonb = JsonbBuilder.create(config);
    }

    @AfterClass
    public static void teardown() {
        jsonb = null;
    }

    @Test
    public void testIconSerialization() throws JsonbException, JSONException {
        Icon icon = Icon.builder(TEST_ICON_URL)
                        .addSize(32, 32)
                        .addSize(48, 48)
                        .setMimeType("image/png")
                        .setTheme(Theme.LIGHT)
                        .build();

        JSONObject iconJson = new JSONObject().put("src", TEST_ICON_URL)
                                              .put("mimeType", "image/png")
                                              .put("sizes", List.of("32x32", "48x48"))
                                              .put("theme", "light");

        JSONAssert.assertEquals(iconJson, new JSONObject(jsonb.toJson(icon)), JSONCompareMode.STRICT);
    }

    @Test
    public void testIconDeserialization() throws JsonbException, JSONException {
        JSONObject iconJson = new JSONObject().put("src", TEST_ICON_URL)
                                              .put("mimeType", "image/png")
                                              .put("sizes", List.of("32x32", "48x48"))
                                              .put("theme", "light");

        Icon icon = jsonb.fromJson(iconJson.toString(), Icon.class);

        assertEquals(TEST_ICON_URL, icon.src());
        assertEquals("image/png", icon.mimeType().get());
        assertEquals(List.of("32x32", "48x48"), icon.sizes());
        assertEquals(Theme.LIGHT, icon.theme().get());

    }
}
