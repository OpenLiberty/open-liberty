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
package com.ibm.ws.logging.osgi.test.feature;

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
        RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "=/loggingTest",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/{sub}",
        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/{sub}/{sub2}"
    }
)
public class LoggingTestEndpoint implements RESTHandler {

    private static final TraceComponent tc = Tr.register(LoggingTestEndpoint.class);
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
        } else if (path.equals("/clear") || path.equals("/reset")) {
            handleClear(request, response);
        } else if (path.equals("/messages")) {
            handleMessages(request, response);
        } else if (path.equals("/old")) {
            handleOld(request, response);
        } else {
            // Default landing: show old UI / help page
            handleOld(request, response);
        }
    }

    /**
     * Action 1 (Text/CSV): Emits all provided messages from comma-separated input
     * at least once first, followed by random messages up to the requested count.
     */
    private void handleEmitText(RESTRequest request, RESTResponse response) throws IOException {
        String sMsgCount = request.getParameter("count");
        if (sMsgCount == null) {
            sMsgCount = request.getParameter("msgCount");
        }
        String rawMessages = request.getParameter("messages");

        // If not in query/form params, read from body
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
     * Action 1 (JSON): Emits all provided messages from JSON input at least once first,
     * followed by random messages up to the requested count.
     * Expected format: {"count": 100, "messages": ["ABCD1231I Hello", "CWWKF0011I Server started"]}
     */
    private void handleEmitJson(RESTRequest request, RESTResponse response) throws IOException {
        String body = readRequestBody(request);
        int count = parseJsonCount(body, 1);
        List<String> messageList = parseJsonMessages(body);

        if (messageList.isEmpty()) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"No messages found in JSON body. Expected format: {\\\"count\\\": 10, \\\"messages\\\": [\\\"MSG1\\\", \\\"MSG2\\\"]}\"}");
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
     * Action 2 (Verify): Validates that in-memory messages strictly match the configured routerThing pattern(s).
     */
    private void handleVerify(RESTRequest request, RESTResponse response) throws IOException {
        String routerThing = request.getParameter("routerThing");
        if (routerThing == null || routerThing.trim().isEmpty()) {
            routerThing = readRequestBody(request);
        }

        CapturedMessageStore.VerificationResult result = CapturedMessageStore.verifyAgainst(routerThing);

        response.setStatus(result.isPassed() ? 200 : 417); // 200 OK or 417 Expectation Failed
        response.setContentType("application/json");

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\": \"").append(result.isPassed() ? "PASSED" : "FAILED").append("\",");
        json.append("\"routerThing\": \"").append(escapeJson(result.getRouterThing() != null ? result.getRouterThing() : "")).append("\",");
        json.append("\"totalCaptured\": ").append(result.getTotalCaptured()).append(",");
        json.append("\"matchedCount\": ").append(result.getMatchedMessages().size()).append(",");
        json.append("\"unmatchedCount\": ").append(result.getUnmatchedMessages().size()).append(",");

        json.append("\"unmatchedMessages\": [");
        for (int i = 0; i < result.getUnmatchedMessages().size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(result.getUnmatchedMessages().get(i))).append("\"");
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
     * Reset / Clear in-memory captured messages.
     */
    private void handleClear(RESTRequest request, RESTResponse response) throws IOException {
        int previousCount = CapturedMessageStore.getCount();
        CapturedMessageStore.clear();

        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": \"SUCCESS\", \"clearedCount\": " + previousCount + "}");
    }

    /**
     * Retrieve current snapshot of captured messages.
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
     * Legacy interactive browser form under /old.
     */
    private void handleOld(RESTRequest request, RESTResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = new PrintWriter(response.getWriter());

        String sMsgCount = request.getParameter("msgCount");
        String rawMessages = request.getParameter("messages");
        String resultBanner = "";

        if (sMsgCount != null && rawMessages != null && !rawMessages.trim().isEmpty()) {
            int msgCount = parseCount(sMsgCount, 1);
            String[] messages = rawMessages.split("[,\\r\\n]+");
            List<String> validList = new ArrayList<String>();
            for (String m : messages) {
                if (!m.trim().isEmpty()) {
                    validList.add(m.trim());
                }
            }

            if (!validList.isEmpty()) {
                long elapsed = emitRandomMessages(validList, msgCount);
                resultBanner = "<div style='color:green; padding:10px; border:1px solid green; margin-bottom:15px;'>"
                        + "<b>Sent " + msgCount + " messages in " + elapsed + " ms! Captured in memory: "
                        + CapturedMessageStore.getCount() + "</b>"
                        + "</div>";
            }
        }

        out.println("<!DOCTYPE html><html><head><title>Logging Test Generator (Old UI)</title></head><body>");
        out.println("<h2>Message Router Test Generator (Legacy UI)</h2>");
        out.println(resultBanner);
        out.println("<p><b>Available Automated Endpoints:</b></p>");
        out.println("<ul>");
        out.println("  <li><code>POST /loggingTest/emit/text</code> - parameters: <code>count</code>, <code>messages</code> (comma separated)</li>");
        out.println("  <li><code>POST /loggingTest/emit/json</code> - JSON body: <code>{\"count\": 100, \"messages\": [\"ID1\", \"ID2\"]}</code></li>");
        out.println("  <li><code>GET/POST /loggingTest/verify</code> - parameter: <code>routerThing</code></li>");
        out.println("  <li><code>GET/POST /loggingTest/messages</code> - view captured in-memory messages</li>");
        out.println("  <li><code>GET/POST /loggingTest/clear</code> - reset/clear in-memory messages (Currently stored: " + CapturedMessageStore.getCount() + ")</li>");
        out.println("</ul>");
        out.println("<form method='POST'>");
        out.println("  <div>");
        out.println("    <label><b>Messages (comma or newline separated):</b></label><br/>");
        out.println("    <textarea name='messages' rows='6' cols='60'>"
                    + (rawMessages != null ? rawMessages : "ABCD1231I Hello World, ABCDEE045I Sample Message, BCDAEE410W Warning msg")
                    + "</textarea>");
        out.println("  </div><br/>");
        out.println("  <div>");
        out.println("    <label><b>Message Count:</b></label><br/>");
        out.println("    <input type='number' name='msgCount' value='" + (sMsgCount != null ? sMsgCount : "100") + "' min='1' />");
        out.println("  </div><br/>");
        out.println("  <input type='submit' value='Send Messages' style='padding:6px 16px; font-weight:bold;' />");
        out.println("</form>");
        out.println("</body></html>");
    }

    /**
     * Emits all provided messages at least once to ensure full coverage of all message ID types,
     * then continues emitting randomly selected messages from the list until the target count is reached.
     *
     * @param messages the list of message strings to emit
     * @param count the total number of messages to emit
     * @return elapsed time in milliseconds
     */
    private long emitRandomMessages(List<String> messages, int count) {
        long start = System.currentTimeMillis();
        // First emit each message at least once to ensure every message ID type is routed
        for (String msg : messages) {
            Tr.info(tc, msg);
        }
        // Then randomly emit messages for the remaining count
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

    /**
     * Lightweight JSON count extractor without requiring external JSON libraries.
     */
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
                    String numStr = json.substring(colonIdx + 1, endIdx).trim();
                    return Integer.parseInt(numStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultVal;
    }

    /**
     * Lightweight JSON messages array extractor.
     */
    private List<String> parseJsonMessages(String json) {
        List<String> list = new ArrayList<String>();
        if (json == null || json.isEmpty()) return list;

        int msgIdx = json.indexOf("\"messages\"");
        if (msgIdx == -1) msgIdx = json.indexOf("'messages'");
        if (msgIdx == -1) return list;

        int openBracket = json.indexOf('[', msgIdx);
        int closeBracket = json.indexOf(']', openBracket != -1 ? openBracket : msgIdx);
        if (openBracket != -1 && closeBracket != -1 && closeBracket > openBracket) {
            String arrayContent = json.substring(openBracket + 1, closeBracket);
            // Parse individual string items within quotes
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
                        if (!item.isEmpty()) {
                            list.add(item);
                        }
                    } else {
                        current.append(c);
                    }
                } else if (inQuotes) {
                    current.append(c);
                }
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
