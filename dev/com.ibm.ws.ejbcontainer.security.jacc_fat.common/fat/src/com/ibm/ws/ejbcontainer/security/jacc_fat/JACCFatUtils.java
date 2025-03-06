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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

/**
 * Some utility functions for JACC FATs.
 */
public class JACCFatUtils {

    /**
     * Install the jaccTestProvider-1.0 or jaccTestProvider-2.0 or jaccTestProvider-2.1 user feature and bundle into the Liberty server.
     *
     * @param myServer The server to install onto.
     * @throws Exception If the install failed.
     */
    public static void installJaccUserFeature(LibertyServer myServer) throws Exception {
        if (JakartaEEAction.isEE11OrLaterActive()) {
            myServer.installUserBundle("io.openliberty.security.authorization.jacc.testprovider_3.0");
            myServer.installUserFeature("jaccTestProvider-3.0");
        } else if (JakartaEEAction.isEE10Active()) {
            myServer.installUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_2.1");
            myServer.installUserFeature("jaccTestProvider-2.1");
        } else if (JakartaEEAction.isEE9Active()) {
            myServer.installUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_2.0");
            myServer.installUserFeature("jaccTestProvider-2.0");
        } else {
            myServer.installUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_1.0");
            myServer.installUserFeature("jaccTestProvider-1.0");
        }
    }

    /**
     * Uninstall the jaccTestProvider-1.0 or jaccTestProvider-2.0 user feature and bundle from the Liberty server.
     *
     * @param myServer The server to uninstall from.
     * @throws Exception If the uninstall failed.
     */
    public static void uninstallJaccUserFeature(LibertyServer myServer) throws Exception {
        if (JakartaEEAction.isEE11OrLaterActive()) {
            myServer.uninstallUserBundle("io.openliberty.security.authorization.jacc.testprovider_3.0");
            myServer.uninstallUserFeature("jaccTestProvider-3.0");
        } else if (JakartaEEAction.isEE10Active()) {
            myServer.uninstallUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_2.1");
            myServer.uninstallUserFeature("jaccTestProvider-2.1");
        } else if (JakartaEEAction.isEE9Active()) {
            myServer.uninstallUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_2.0");
            myServer.uninstallUserFeature("jaccTestProvider-2.0");
        } else {
            myServer.uninstallUserBundle("com.ibm.ws.security.authorization.jacc.testprovider_1.0");
            myServer.uninstallUserFeature("jaccTestProvider-1.0");
        }
    }

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/apps/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps     The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + app);
                JakartaEEAction.transformApp(someArchive);
            }
        }
    }
}
