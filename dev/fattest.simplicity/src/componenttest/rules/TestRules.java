/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules;

import org.junit.rules.TestRule;

import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Some helpful JUnit rules.
 * Deprecated: Use the {@link TestServlet} or {@link TestServlets} annotation instead
 */
@Deprecated
public class TestRules {

    public interface PathGetter {
        TestRule onPath(String path);

        ServletGetter usingApp(String appName);
    }

    public interface ServletGetter {
        TestRule andServlet(String servletName);
    }

    /**
     * Run all tests with their method names using {@link FATServletClient}.
     * <p>
     * Examples: <br>
     * <code>@Rule public TestRule runAll = TestRules.runAllUsingTestNames(server).onPath("appName/servletName");</code> <br>
     * <code>@Rule public TestRule runAll = TestRules.runAllUsingTestNames(server).usingApp("appName").andServlet("servletName");</code>
     */
    public static PathGetter runAllUsingTestNames(final LibertyServer server) {
        return new PathGetter() {
            @Override
            public TestRule onPath(final String path) {
                return new RunFatClientUsingTestNamesRule(server, path);
            }

            @Override
            public ServletGetter usingApp(final String appName) {
                return new ServletGetter() {

                    @Override
                    public TestRule andServlet(final String servletName) {
                        final String path = appName + "/" + servletName;
                        return new RunFatClientUsingTestNamesRule(server, path);
                    }
                };
            }
        };
    }

    // prevent instantiation of static utility class
    private TestRules() {}
}
