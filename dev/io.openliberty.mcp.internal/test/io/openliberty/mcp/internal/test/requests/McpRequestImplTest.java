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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mcpjava.server.ImplementationInfo;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.requests.ImplementationInfoImpl;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpRequestImpl;
import io.openliberty.mcp.internal.requests.RequestId;
import io.openliberty.mcp.internal.sessions.McpSession;
import io.openliberty.mcp.internal.sessions.McpSessionId;
import io.openliberty.mcp.internal.testutils.TestUtils;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class McpRequestImplTest {

    private static final String BETA_PROPERTY = "com.ibm.ws.beta.edition";
    private static String originalBetaValue;
    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        originalBetaValue = System.getProperty(BETA_PROPERTY);
        System.setProperty(BETA_PROPERTY, "true");
        jsonb = TestUtils.createJsonb();
    }

    @AfterClass
    public static void teardown() {
        if (originalBetaValue == null) {
            System.clearProperty(BETA_PROPERTY);
        } else {
            System.setProperty(BETA_PROPERTY, originalBetaValue);
        }
        jsonb = null;
    }

    @Test
    public void test2026_07_28_WithQualifiedMeta() throws Exception {
        McpTransport transport = createTransport(McpProtocolVersion.V_2026_07_28);
        McpRequest request = new McpRequest("2.0", new RequestId("req-1"), "tools/call", null);

        Map<String, Object> clientInfoMap = Map.of(
                                                   "name", "meta-client",
                                                   "title", "Meta Client Title",
                                                   "version", "3.0.0",
                                                   "description", "Description in meta",
                                                   "websiteUrl", "https://example.org");

        Map<String, Object> capsMap = Map.of("roots", Map.of("listChanged", true));

        Map<String, Object> meta = new HashMap<>();
        meta.put("io.modelcontextprotocol/clientInfo", clientInfoMap);
        meta.put("io.modelcontextprotocol/clientCapabilities", capsMap);
        meta.put("customProp", "customVal");

        McpRequestImpl reqImpl = new McpRequestImpl(request, transport, null, meta, jsonb);

        assertEquals("req-1", reqImpl.id());
        assertEquals("2026-07-28", reqImpl.protocolVersion());
        assertEquals(meta, reqImpl.metadata());
        assertFalse(reqImpl.sessionId().isPresent());

        ImplementationInfo info = reqImpl.clientInfo();
        assertNotNull(info);
        assertEquals("meta-client", info.name());
        assertEquals("Meta Client Title", info.title());
        assertEquals("3.0.0", info.version());
        assertEquals(Optional.of("Description in meta"), info.description());
        assertEquals(Optional.of("https://example.org"), info.websiteUrl());

        assertEquals(capsMap, reqImpl.rawClientCapabilities());
    }

    @Test
    public void test2026_07_28_WithShortMetaKeys() throws Exception {
        McpTransport transport = createTransport(McpProtocolVersion.V_2026_07_28);
        McpRequest request = new McpRequest("2.0", new RequestId(42), "tools/call", null);

        Map<String, Object> clientInfoMap = Map.of(
                                                   "name", "short-key-client",
                                                   "version", "1.5");

        Map<String, Object> capsMap = Map.of("sampling", Collections.emptyMap());

        Map<String, Object> meta = Map.of(
                                          "clientInfo", clientInfoMap,
                                          "capabilities", capsMap);

        McpRequestImpl reqImpl = new McpRequestImpl(request, transport, null, meta, jsonb);

        assertEquals(42, reqImpl.id());
        ImplementationInfo info = reqImpl.clientInfo();
        assertEquals("short-key-client", info.name());
        assertEquals("1.5", info.version());
        assertEquals(capsMap, reqImpl.rawClientCapabilities());
    }

    @Test
    public void test2026_07_28_WithoutMeta() throws Exception {
        McpTransport transport = createTransport(McpProtocolVersion.V_2026_07_28);
        McpRequest request = new McpRequest("2.0", new RequestId(1), "tools/call", null);

        McpRequestImpl reqImpl = new McpRequestImpl(request, transport, null, null, jsonb);

        assertEquals(ImplementationInfoImpl.UNKNOWN, reqImpl.clientInfo());
        assertEquals(Collections.emptyMap(), reqImpl.rawClientCapabilities());
    }

    @Test
    public void test2026_07_28_WithInvalidClientInfo() throws Exception {
        McpTransport transport = createTransport(McpProtocolVersion.V_2026_07_28);
        McpRequest request = new McpRequest("2.0", new RequestId(1), "tools/call", null);

        Map<String, Object> meta = Map.of("clientInfo", "not-a-map");

        McpRequestImpl reqImpl = new McpRequestImpl(request, transport, null, meta, jsonb);

        assertEquals(ImplementationInfoImpl.UNKNOWN, reqImpl.clientInfo());
    }

    @Test
    public void testOlderProtocolStatefulUsesSession() throws Exception {
        McpTransport transport = createTransport(McpProtocolVersion.V_2025_11_25);
        McpRequest request = new McpRequest("2.0", new RequestId(1), "tools/call", null);

        ImplementationInfo sessionInfo = new ImplementationInfoImpl(List.of(), "session-client", "Session Client", "1.0", null, null);
        Map<String, Object> sessionCaps = Map.of("sessionFeature", true);
        McpSession session = new McpSession(new McpSessionId("sess-abc"), null, null, sessionInfo, sessionCaps);

        // Even if request carries _meta, older protocol must read from session
        Map<String, Object> requestMeta = Map.of(
                                                 "clientInfo", Map.of("name", "ignored-request-client", "version", "9.9"),
                                                 "capabilities", Map.of("ignoredCaps", false));

        McpRequestImpl reqImpl = new McpRequestImpl(request, transport, session, requestMeta, jsonb);

        assertEquals("session-client", reqImpl.clientInfo().name());
        assertEquals("1.0", reqImpl.clientInfo().version());
        assertEquals(sessionCaps, reqImpl.rawClientCapabilities());
        assertTrue(reqImpl.sessionId().isPresent());
        assertEquals("sess-abc", reqImpl.sessionId().get());
    }

    @Test
    public void testOlderProtocolStateless() throws Exception {
        McpTransport transport = createTransport(McpProtocolVersion.V_2025_11_25);
        McpRequest request = new McpRequest("2.0", new RequestId(1), "tools/call", null);

        McpRequestImpl reqImpl = new McpRequestImpl(request, transport, null, null, jsonb);

        assertEquals(ImplementationInfoImpl.UNKNOWN, reqImpl.clientInfo());
        assertEquals(Collections.emptyMap(), reqImpl.rawClientCapabilities());
        assertFalse(reqImpl.sessionId().isPresent());
    }

    private static McpTransport createTransport(McpProtocolVersion version) throws Exception {
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                                                                             HttpServletRequest.class.getClassLoader(),
                                                                             new Class<?>[] { HttpServletRequest.class },
                                                                             (proxy, method, args) -> null);
        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(
                                                                               HttpServletResponse.class.getClassLoader(),
                                                                               new Class<?>[] { HttpServletResponse.class },
                                                                               (proxy, method, args) -> {
                                                                                   if ("getWriter".equals(method.getName())) {
                                                                                       return new PrintWriter(new StringWriter());
                                                                                   }
                                                                                   return null;
                                                                               });

        return new McpTransport(req, res, jsonb, 0) {
            @Override
            public McpProtocolVersion getProtocolVersion() {
                return version;
            }
        };
    }
}
