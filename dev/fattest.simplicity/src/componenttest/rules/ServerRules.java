/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import componenttest.topology.impl.LibertyServer;

/**
 * JUnit rules for doing stuff with LibertyServers before and after tests e.g. starting and stopping.
 */
public final class ServerRules {

    /**
     * Use this rule to automatically start a given Liberty server before tests.
     * E.g. <code>@Rule public TestRule startRule = ServerRules.startAutomatically(myServer);</code>
     */
    public static TestRule startAutomatically(final LibertyServer server) {
        return new AutoStartRule(server);
    }

    /**
     * Use this rule to automatically stop a given Liberty server after tests.
     * E.g. <code>@Rule public TestRule stopRule = ServerRules.stopAutomatically(myServer);</code>
     */
    public static TestRule stopAutomatically(final LibertyServer server) {
        return new AutoStopRule(server);
    }

    /**
     * Use this rule to automatically start and stop a given Liberty server before and after tests.
     * E.g. <code>@Rule public TestRule startStopRule = ServerRules.startAndStopAutomatically(myServer);</code>
     */
    public static TestRule startAndStopAutomatically(final LibertyServer server) {
        return RuleChain.outerRule(new AutoStartRule(server)).around(new AutoStopRule(server));
    }

    // prevent instantiation of static utility class
    private ServerRules() {}

}
