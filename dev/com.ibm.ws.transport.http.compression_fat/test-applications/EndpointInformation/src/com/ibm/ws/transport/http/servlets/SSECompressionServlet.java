/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.transport.http.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

/**
 * Servlet that simulates a Server-Sent Events (SSE) endpoint
 * specifically for testing compression behavior.
 * It sends multiple events, including one with a large payload
 * designed to trigger response compression.
 */
@WebServlet("/SSECompressionServlet")
public class SSECompressionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger Log = Logger.getLogger(SSECompressionServlet.class.getName());
    private static final long EVENT_DELAY_MS = 100;

    // Payload large enough to potentially trigger compression (> 2048 bytes)
    // Repeated message to easily create a large string.
    private static final String LARGE_PAYLOAD =
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. " +
        "This large event payload contains enough bytes for the autocompress feature to determine it is valid to compress. " +
        "There must be 2048 bytes minimum for compression to typically happen. This message will be repeated multiple times to ensure it exceeds the threshold. ";


    public SSECompressionServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set headers for SSE
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        // Disable caching for SSE
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // Get the PrintWriter to send events
        PrintWriter writer = response.getWriter();

        try {
            // Send a couple of initial small events
            sendSSEEvent(writer, "event1", "Initial small event data.");
            // Short delay to simulate time between events
            TimeUnit.MILLISECONDS.sleep(EVENT_DELAY_MS);

            sendSSEEvent(writer, "event2", "Another small event.");
            TimeUnit.MILLISECONDS.sleep(EVENT_DELAY_MS);

            // Send the large event designed to trigger compression
            Log.info("SSECompressionServlet: Sending large event (length: " + LARGE_PAYLOAD.length() + ")");
            sendSSEEvent(writer, "largeEvent", LARGE_PAYLOAD);
            Log.info("SSECompressionServlet: Large event sent and flushed.");
            TimeUnit.MILLISECONDS.sleep(EVENT_DELAY_MS);

            // Send a final event after the large one
            sendSSEEvent(writer, "finalEvent", "This event is the final event.");
            Log.info("SSECompressionServlet: Final event sent and flushed.");

        } catch (InterruptedException e) {
            // Restore interrupt status and handle exception
            Thread.currentThread().interrupt();
            Log.info("SSECompressionServlet: Thread interrupted during sleep.");
            throw new ServletException("SSE stream interrupted", e);
        } catch (IOException e) {
            // Handle potential IOExceptions during write/flush
             Log.info("SSECompressionServlet: IOException during SSE streaming: " + e.getMessage());
        } finally {
             Log.info("SSECompressionServlet: Closing writer.");
            if (writer != null) {
                 writer.flush();    // Not closing the writer here, container manages the response stream lifecycle.
            }
            Log.info("SSECompressionServlet: Request processing finished.");
        }
    }

    /**
     * Helper method to format and send a single SSE event.
     * Includes necessary line breaks and flushing.
     *
     * @param writer The PrintWriter for the response.
     * @param eventName The name of the event (optional, can be null).
     * @param data The data payload for the event.
     * @throws IOException If an I/O error occurs.
     */
    private void sendSSEEvent(PrintWriter writer, String eventName, String data) throws IOException {
        if (eventName != null && !eventName.isEmpty()) {
            writer.write("event: " + eventName + "\n");
        }
        // Handle multi-line data if necessary, splitting by newline
        String[] lines = data.split("\\r?\\n");
        for (String line : lines) {
             writer.write("data: " + line + "\n");
        }
        writer.write("\n"); // End of event marker
        writer.flush();
        Log.info("SSECompressionServlet: Flushed event: " + (eventName != null ? eventName : "message"));

        // Check for writer errors after flush
        if (writer.checkError()) {
            throw new IOException("Error occurred in PrintWriter after flush.");
        }
    }
}