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

import org.junit.rules.ExternalResource;

import componenttest.topology.impl.LibertyServer;

/**
 * Automatically start a given LibertyServer before tests.
 */
class AutoStartRule extends ExternalResource {
    private final LibertyServer server;

    public AutoStartRule(final LibertyServer server) {
        this.server = server;
    }

    @Override
    protected void before() throws Throwable {
        server.startServer();
    }
}