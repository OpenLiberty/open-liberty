/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.security.jaspic;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

public class JASPIFatUtils {

    /**
     * Install the user feature and bundle into the Liberty server.
     *
     * @param myServer The server to install onto.
     * @throws Exception If the install failed.
     */
    public static void installJaspiUserFeature(LibertyServer myServer) throws Exception {
        if (JakartaEEAction.isEE11Active()) {
            myServer.installUserBundle("com.ibm.ws.security.jaspic.test.jakarta");
            myServer.installUserFeature("jaspicUserTestFeature-3.1");
        } else if (JakartaEEAction.isEE10Active()) {
            myServer.installUserBundle("com.ibm.ws.security.jaspic.test.jakarta");
            myServer.installUserFeature("jaspicUserTestFeature-3.0");
        } else if (JakartaEEAction.isEE9Active()) {
            myServer.installUserBundle("com.ibm.ws.security.jaspic.test.jakarta");
            myServer.installUserFeature("jaspicUserTestFeature-2.0");
        } else {
            myServer.installUserBundle("com.ibm.ws.security.jaspic.test");
            myServer.installUserFeature("jaspicUserTestFeature-1.0");
        }
    }

    /**
     * Uninstall user feature and bundle from the Liberty server.
     *
     * @param myServer The server to uninstall from.
     * @throws Exception If the uninstall failed.
     */
    public static void uninstallJaspiUserFeature(LibertyServer myServer) throws Exception {
        if (JakartaEEAction.isEE11Active()) {
            myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test.jakarta");
            myServer.uninstallUserFeature("jaspicUserTestFeature-3.1");
        } else if (JakartaEEAction.isEE10Active()) {
            myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test.jakarta");
            myServer.uninstallUserFeature("jaspicUserTestFeature-3.0");
        } else if (JakartaEEAction.isEE9Active()) {
            myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test.jakarta");
            myServer.uninstallUserFeature("jaspicUserTestFeature-2.0");
        } else {
            myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test");
            myServer.uninstallUserFeature("jaspicUserTestFeature-1.0");
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
