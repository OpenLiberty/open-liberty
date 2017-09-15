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
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import componenttest.annotation.IgnoreTestNamesRule;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Run all tests with their method names using {@link FATServletClient}.
 * Deprecated: Use the {@link TestServlet} or {@link TestServlets} annotation instead
 */
@Deprecated
class RunFatClientUsingTestNamesRule implements TestRule {
    private final LibertyServer server;
    private final String path;

    public RunFatClientUsingTestNamesRule(final LibertyServer server, final String path) {
        this.server = server;
        this.path = path;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        if (description.getAnnotation(IgnoreTestNamesRule.class) == null) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final String testName = description.getMethodName();
                    FATServletClient.runTest(server, path, testName);
                }
            };
        } else {
            return base;
        }
    }
}