/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.suite;

import java.util.LinkedHashMap;
import java.util.Map;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTestAction;
import componenttest.topology.impl.LibertyServer;

/**
 * A {@link RepeatTestAction} which repeats a suite with the server running as a beta runtime.
 * <p>
 * The beta edition flag is read from the JVM option {@code com.ibm.ws.beta.edition}, and it only works from
 * {@code jvm.options} (not {@code bootstrap.properties}).
 * <p>
 * This action does not change the server itself. Getting a server from {@code LibertyServerFactory} deletes the
 * server directory and copies it again from {@code publish/servers}, so anything written before that point is
 * lost. Instead, this action is a marker, and {@link ServerLifecycleRule} calls {@link #applyTo(LibertyServer)}
 * after it has got the server and before it starts it, whenever {@link #isActive()} is true.
 * <p>
 * This means the action must be applied <em>outside</em> the {@link ServerLifecycleRule} so that it is active when
 * the server is started. See {@link McpMainServerSuite} for an example. There is nothing to undo afterwards because
 * the next time the server is fetched its directory is recreated without the option.
 * <p>
 * The non-beta repeat is just {@code RepeatTests.withoutModification()}. Tests which only make sense in one of
 * the two repeats can use {@code @SkipForRepeat} with {@link #ID} (skip on beta) or
 * {@code SkipForRepeat.NO_MODIFICATION} (skip on non-beta). Tests which need to know which repeat they
 * are in can call {@link #isActive()}.
 */
public class BetaRepeatAction implements RepeatTestAction {

    /** The id of this action, for use with {@code @SkipForRepeat} */
    public static final String ID = "MCP_BETA";

    private static final String BETA_OPTION = "-Dcom.ibm.ws.beta.edition";

    /**
     * Returns whether the tests are currently running in the beta repeat.
     *
     * @return {@code true} if running as a beta runtime
     */
    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID);
    }

    /**
     * Adds the beta edition JVM option to the server's {@code jvm.options}, keeping any options already there.
     * <p>
     * This must be called after the server has been fetched from {@code LibertyServerFactory} and before it is started.
     *
     * @param server the server which is about to be started
     * @throws Exception if the options cannot be read or written
     */
    public static void applyTo(LibertyServer server) throws Exception {
        Map<String, String> options = new LinkedHashMap<>(server.getJvmOptionsAsMap());
        options.put(BETA_OPTION, "true");
        server.setJvmOptions(options);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() {}

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String toString() {
        return "Beta runtime (" + BETA_OPTION + "=true)";
    }
}
