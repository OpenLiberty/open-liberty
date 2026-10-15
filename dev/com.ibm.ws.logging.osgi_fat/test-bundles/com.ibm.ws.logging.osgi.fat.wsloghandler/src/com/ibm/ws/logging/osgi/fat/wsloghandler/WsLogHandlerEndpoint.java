/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.logging.osgi.fat.wsloghandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(
    service = { RESTHandler.class },
    configurationPolicy = ConfigurationPolicy.IGNORE,
    immediate = true,
    property = {
        "service.vendor=IBM",
        RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=/wsLogHandler",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/{sub}",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/{sub}/{sub2}"
    }
)
public class WsLogHandlerEndpoint implements RESTHandler {

    private static final TraceComponent tc = Tr.register(WsLogHandlerEndpoint.class);
    private static final Random RANDOM = new Random();

    @Override
    public void handleRequest(RESTRequest request, RESTResponse response) throws IOException {
        String path = request.getPath();
        if (path == null) {
            path = "";
        }
        path = path.trim();

        if (path.equals("/emit/text") || path.equals("/emit/csv")) {
            handleEmitText(request, response);
        } else if (path.equals("/emit/json")) {
            handleEmitJson(request, response);
        } else if (path.equals("/verify")) {
            handleVerify(request, response);
        } else if (path.equals("/verifyAbsent")) {
            handleVerifyAbsent(request, response);
        } else if (path.equals("/clear") || path.equals("/reset")) {
            handleClear(request, response);
        } else if (path.equals("/messages")) {
            handleMessages(request, response);
        } else {
            handleStatus(request, response);
        }
    }

    /**
     * POST /wsLogHandler/emit/text — emits random messages from CSV input.
     */
    private void handleEmitText(RESTRequest request, RESTResponse response) throws IOException {
        String sMsgCount = request.getParameter("count");
        if (sMsgCount == null) {
            sMsgCount = request.getParameter("msgCount");
        }
        String rawMessages = request.getParameter("messages");
        if (rawMessages == null || rawMessages.trim().isEmpty()) {
            rawMessages = readRequestBody(request);
        }

        int count = parseCount(sMsgCount, 1);
        List<String> messageList = parseCsvMessages(rawMessages);

        if (messageList.isEmpty()) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"No messages provided. Use 'messages' parameter or request body.\"}");
            return;
        }

        long elapsed = emitRandomMessages(messageList, count);

        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": \"SUCCESS\", \"emittedCount\": " + count
                + ", \"uniqueMessages\": " + messageList.size()
                + ", \"elapsedMs\": " + elapsed + "}");
    }

    /**
     * POST /wsLogHandler/emit/json — emits random messages from JSON input.
     * Expected: {"count": N, "messages": ["ID1 text", "ID2 text"]}
     */
    private void handleEmitJson(RESTRequest request, RESTResponse response) throws IOException {
        String body = readRequestBody(request);
        int count = parseJsonCount(body, 1);
        List<String> messageList = parseJsonMessages(body);

        if (messageList.isEmpty()) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"No messages found in JSON body.\"}");
            return;
        }

        long elapsed = emitRandomMessages(messageList, count);

        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": \"SUCCESS\", \"emittedCount\": " + count
                + ", \"uniqueMessages\": " + messageList.size()
                + ", \"elapsedMs\": " + elapsed + "}");
    }

    /**
     * GET/POST /wsLogHandler/verify — validates captured messages against routerThing patterns.
     * HTTP 200 on PASSED, HTTP 417 on FAILED.
     */
    private void handleVerify(RESTRequest request, RESTResponse response) throws IOException {
        String routerThing = request.getParameter("routerThing");
        if (routerThing == null || routerThing.trim().isEmpty()) {
            routerThing = readRequestBody(request);
        }

        CapturedMessageStore.VerificationResult result = CapturedMessageStore.verifyAgainst(routerThing);

        response.setStatus(result.isPassed() ? 200 : 417);
        response.setContentType("application/json");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\": \"").append(result.isPassed() ? "PASSED" : "FAILED").append("\",");
        json.append("\"routerThing\": \"").append(escapeJson(result.getRouterThing() != null ? result.getRouterThing() : "")).append("\",");
        json.append("\"totalCaptured\": ").append(result.getTotalCaptured()).append(",");
        json.append("\"matchedCount\": ").append(result.getMatchedMessages().size()).append(",");
        json.append("\"unmatchedCount\": ").append(result.getUnmatchedMessages().size()).append(",");
        json.append("\"missingCount\": ").append(result.getMissingPatterns().size()).append(",");

        json.append("\"unmatchedMessages\": [");
        for (int i = 0; i < result.getUnmatchedMessages().size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(result.getUnmatchedMessages().get(i))).append("\"");
        }
        json.append("],");

        json.append("\"missingPatterns\": [");
        for (int i = 0; i < result.getMissingPatterns().size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(result.getMissingPatterns().get(i))).append("\"");
        }
        json.append("],");

        json.append("\"matchedMessages\": [");
        for (int i = 0; i < result.getMatchedMessages().size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(result.getMatchedMessages().get(i))).append("\"");
        }
        json.append("]");
        json.append("}");

        response.getWriter().write(json.toString());
    }

    /**
     * GET/POST /wsLogHandler/verifyAbsent — asserts that none of the given patterns
     * appear in the captured message store.
     * HTTP 200 (PASSED) when no captured message matches any supplied pattern.
     * HTTP 417 (FAILED) when at least one captured message matches.
     */
    private void handleVerifyAbsent(RESTRequest request, RESTResponse response) throws IOException {
        String patterns = request.getParameter("patterns");
        if (patterns == null || patterns.trim().isEmpty()) {
            patterns = readRequestBody(request);
        }

        CapturedMessageStore.AbsenceResult result = CapturedMessageStore.verifyAbsent(patterns);

        response.setStatus(result.isPassed() ? 200 : 417);
        response.setContentType("application/json");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\": \"").append(result.isPassed() ? "PASSED" : "FAILED").append("\",");
        json.append("\"patterns\": \"").append(escapeJson(result.getPatterns() != null ? result.getPatterns() : "")).append("\",");
        json.append("\"totalCaptured\": ").append(result.getTotalCaptured()).append(",");
        json.append("\"unexpectedlyMatchedCount\": ").append(result.getUnexpectedlyMatched().size()).append(",");
        json.append("\"unexpectedlyMatched\": [");
        List<String> unexpected = result.getUnexpectedlyMatched();
        for (int i = 0; i < unexpected.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(unexpected.get(i))).append("\"");
        }
        json.append("]");
        json.append("}");

        response.getWriter().write(json.toString());
    }

    /**
     * GET/POST /wsLogHandler/clear — resets the in-memory captured message store.
     */
    private void handleClear(RESTRequest request, RESTResponse response) throws IOException {
        int previousCount = CapturedMessageStore.getCount();
        CapturedMessageStore.clear();

        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": \"SUCCESS\", \"clearedCount\": " + previousCount + "}");
    }

    /**
     * GET /wsLogHandler/messages — returns a snapshot of all captured messages.
     */
    private void handleMessages(RESTRequest request, RESTResponse response) throws IOException {
        List<CapturedMessageStore.CapturedEntry> list = CapturedMessageStore.getEntries();

        response.setStatus(200);
        response.setContentType("application/json");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"totalCaptured\": ").append(list.size()).append(",");
        json.append("\"messages\": [");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(",");
            CapturedMessageStore.CapturedEntry entry = list.get(i);
            json.append("{");
            json.append("\"messageId\": \"").append(escapeJson(entry.getMessageId())).append("\",");
            json.append("\"formattedMessage\": \"").append(escapeJson(entry.getFormattedMessage())).append("\",");
            json.append("\"hidden\": ").append(entry.isMessageHidden()).append(",");
            json.append("\"timestamp\": ").append(entry.getTimestamp());
            json.append("}");
        }
        json.append("]");
        json.append("}");
        response.getWriter().write(json.toString());
    }

    /**
     * Default: returns a simple status page listing available endpoints.
     */
    private void handleStatus(RESTRequest request, RESTResponse response) throws IOException {
        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": \"OK\", \"capturedCount\": " + CapturedMessageStore.getCount()
                + ", \"endpoints\": [\"POST /wsLogHandler/emit/text\","
                + "\"POST /wsLogHandler/emit/json\","
                + "\"GET /wsLogHandler/verify?routerThing=...\","
                + "\"GET /wsLogHandler/verifyAbsent?patterns=...\","
                + "\"GET /wsLogHandler/messages\","
                + "\"GET /wsLogHandler/clear\"]}");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private long emitRandomMessages(List<String> messages, int count) {
        long start = System.currentTimeMillis();
        for (String msg : messages) {
            Tr.info(tc, msg);
        }
        for (int i = messages.size(); i < count; i++) {
            String chosenMsg = messages.get(RANDOM.nextInt(messages.size()));
            Tr.info(tc, chosenMsg);
        }
        return System.currentTimeMillis() - start;
    }

    private int parseCount(String sVal, int defaultVal) {
        if (sVal == null) return defaultVal;
        try {
            int v = Integer.parseInt(sVal.trim());
            return v > 0 ? v : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private List<String> parseCsvMessages(String text) {
        List<String> list = new ArrayList<String>();
        if (text == null) return list;
        String[] tokens = text.split("[,\\r\\n]+");
        for (String t : tokens) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private String readRequestBody(RESTRequest request) throws IOException {
        Reader reader = request.getInput();
        if (reader == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(reader)) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private int parseJsonCount(String json, int defaultVal) {
        if (json == null || json.isEmpty()) return defaultVal;
        int countIdx = json.indexOf("\"count\"");
        if (countIdx == -1) countIdx = json.indexOf("'count'");
        if (countIdx != -1) {
            int colonIdx = json.indexOf(':', countIdx);
            if (colonIdx != -1) {
                int endIdx = colonIdx + 1;
                while (endIdx < json.length() && (Character.isDigit(json.charAt(endIdx)) || Character.isWhitespace(json.charAt(endIdx)))) {
                    endIdx++;
                }
                try {
                    return Integer.parseInt(json.substring(colonIdx + 1, endIdx).trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return defaultVal;
    }

    private List<String> parseJsonMessages(String json) {
        List<String> list = new ArrayList<String>();
        if (json == null || json.isEmpty()) return list;

        int msgIdx = json.indexOf("\"messages\"");
        if (msgIdx == -1) msgIdx = json.indexOf("'messages'");
        if (msgIdx == -1) return list;

        int openBracket  = json.indexOf('[', msgIdx);
        int closeBracket = json.indexOf(']', openBracket != -1 ? openBracket : msgIdx);
        if (openBracket == -1 || closeBracket == -1 || closeBracket <= openBracket) return list;

        String arrayContent = json.substring(openBracket + 1, closeBracket);
        boolean inQuotes = false;
        char quoteChar = '"';
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if ((c == '"' || c == '\'') && (i == 0 || arrayContent.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                    current.setLength(0);
                } else if (c == quoteChar) {
                    inQuotes = false;
                    String item = current.toString().trim();
                    if (!item.isEmpty()) list.add(item);
                } else {
                    current.append(c);
                }
            } else if (inQuotes) {
                current.append(c);
            }
        }
        return list;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
