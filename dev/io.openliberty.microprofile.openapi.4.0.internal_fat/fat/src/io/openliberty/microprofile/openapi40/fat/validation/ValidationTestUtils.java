/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat.validation;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class ValidationTestUtils {

    /**
     * Assert that a message is found in the messages.log
     *
     * @param server the server to check
     * @param message the message to find
     */
    public static void assertMessage(LibertyServer server, String message) throws Exception {
        assertThat("Message not found: " + message,
                   server.findStringsInLogs(message),
                   not(empty()));
    }

    /**
     * Assert that a message is found a specific number of times in the messages.log
     *
     * @param server the server to check
     * @param count the number of times the message is expected
     * @param message the message to look for
     */
    public static void assertMessage(LibertyServer server, int count, String message) throws Exception {
        assertThat("Message not found " + count + " times: " + message,
                   server.findStringsInLogs(message),
                   hasSize(count));
    }

    /**
     * Assert that a message is not found in the messages log
     *
     * @param server the server to check
     * @param message the message to search for
     */
    public static void assertNoMessage(LibertyServer server, String message) throws Exception {
        assertThat("Unexpected message found: " + message,
                   server.findStringsInLogs(message),
                   is(empty()));
    }

}
