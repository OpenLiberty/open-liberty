/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspic11.fat;

import componenttest.topology.impl.LibertyServer;

/**
 * Some utility functions for JASPI FATs.
 */
public class JASPIFatUtils {

    /**
     * Install the jaspicUserTestFeature-3.0 user feature and bundle
     * into the Liberty server.
     *
     * @param myServer
     *            The server to install onto.
     * @throws Exception
     *             If the install failed.
     */
    public static void installJaspiUserFeature(LibertyServer myServer) throws Exception {
        myServer.installUserBundle("com.ibm.ws.security.jaspic.test_2.1");
        myServer.installUserFeature("jaspicUserTestFeature-3.0");
    }

    /**
     * Uninstall the jaspicUserTestFeature-3.0 user feature and bundle
     * from the Liberty server.
     *
     * @param myServer
     *            The server to uninstall from.
     * @throws Exception
     *             If the uninstall failed.
     */
    public static void uninstallJaspiUserFeature(LibertyServer myServer) throws Exception {
        myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test_2.1");
        myServer.uninstallUserFeature("jaspicUserTestFeature-3.0");
    }
}
