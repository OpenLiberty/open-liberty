/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.metaKeyApp;

import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.ToolResponse;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/metaKeyValidationTestServlet")
public class MetaKeyValidationTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    // putMetadata — valid keys accepted
    @Test
    public void testPutValidReverseDnsKey() throws Exception {
        assertPutValidMetaKeyAccepted("com.example/myKey", "hello");
    }

    @Test
    public void testPutValidNameOnlyKey() throws Exception {
        assertPutValidMetaKeyAccepted("plainName", "world");
    }

    @Test
    public void testPutValidEmptyName() throws Exception {
        // Empty name segment is allowed by spec
        assertPutValidMetaKeyAccepted("com.example/", "value");
    }

    @Test
    public void testPutValidKeyWithHyphenAndUnderscore() throws Exception {
        assertPutValidMetaKeyAccepted("com.example/my-key_v1.0", "value");
    }

    @Test
    public void testPutNameStartingWithHyphenIsRejected() throws Exception {
        assertPutInvalidMetaKeyRejected("-badName");
    }

    @Test
    public void testPutNameEndingWithDotIsRejected() throws Exception {
        assertPutInvalidMetaKeyRejected("badName.");
    }

    @Test
    public void testPutLabelStartingWithDigitIsRejected() throws Exception {
        assertPutInvalidMetaKeyRejected("1com.example/key");
    }

    @Test
    public void testPutEmptyPrefixIsRejected() throws Exception {
        assertPutInvalidMetaKeyRejected("/key");
    }

    @Test
    public void testPutEmptyLabelFromConsecutiveDotsIsRejected() throws Exception {
        assertPutInvalidMetaKeyRejected("com..example/key");
    }

    // setMetadata — valid map accepted
    @Test
    public void testSetInvalidMetadataMapIsRejected() throws Exception {
        Map<String, Object> meta = Map.of("com&example/key", "value1",
                                          "plainName", "value2");
        try {
            ToolResponse.builder()
                        .setMetadata(meta)
                        .addTextContent("OK")
                        .build();
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    // setMetadata — invalid key in map rejected
    @Test
    public void testSetValidMetadataMap() throws Exception {
        Map<String, Object> meta = Map.of("com.example/key", "value1",
                                          "plainName", "value2");
        ToolResponse.builder()
                    .setMetadata(meta)
                    .addTextContent("OK")
                    .build();
    }

    private void assertPutValidMetaKeyAccepted(String key, String value) {
        ToolResponse.builder()
                    .addContent(TextContent.of("OK"))
                    .putMetadata(key, value)
                    .build();
    }

    private void assertPutInvalidMetaKeyRejected(String key) {
        try {
            ToolResponse.builder()
                        .addContent(TextContent.of("OK"))
                        .putMetadata(key, "foo")
                        .build();
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
