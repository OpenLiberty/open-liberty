/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

/**
 * Handles synchronization of tests with tools that need to run in parallel
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * &#64;Inject
 * ToolStatus toolStatus;
 *
 * @Tool
 * public String myTool(@ToolArg String latchName) {
 *     toolStatus.signalStarted(latchName);
 *     toolStatus.awaitShouldEnd(latchName);
 *     return "OK";
 * }
 * }
 * </pre>
 *
 * <pre>
 * {@code
 * &#64;Rule
 * public ToolStatus toolStatus = new ToolStatusClient(server, "myAppName");
 *
 * @Test
 * public String myTest() {
 *     String request = """
 *                     ...
 *                        "latchName": "MyLatch"
 *                     ...
 *                     """;
 *     Future<String> result = executor.submit(() -> client.callMCP(request));
 *     toolStatus.awaitStarted("MyLatch");
 *
 *     // Test something that is supposed to run while the first request is running
 *
 *     toolStatus.signalShouldEnd("MyLatch");
 *     assertEquals(expectedResult, result.get(10, SECONDS));
 * }
 * }
 * </pre>
 *
 */
public interface ToolStatus {

    /**
     * Signals that a tool has started and unblocks any thread waiting on this latch.
     *
     */
    void signalStarted(String latchName);

    /**
     * Waits for the tool to signal that it has started.
     *
     */
    void awaitStarted(String latchName);

    /**
     * Releases the latch so that a tool can complete.
     *
     */
    void signalShouldEnd(String latchName);

    /**
     * Blocks until the end latch with the given name is released by the test.
     * <p>
     * This is typically called by the tool to wait before completing execution,
     * allowing the test to control when the tool finishes.
     * </p>
     */
    void awaitShouldEnd(String latchName);

}