/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.auth.data.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.rules.TestName;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Some utility functions for FATs.
 */
public class AuthDataFatUtils {

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/apps/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEE9Action.isActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "apps" + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }

    /**
     * Get the normalized test name. This will remove the test repeat pattern from the end of the test name.
     *
     * @param testName The test name.
     * @return The normalized test name.
     */
    public static String normalizeTestName(TestName testName) {
        return testName.getMethodName().replace("_EE9_FEATURES", "");
    }
}
