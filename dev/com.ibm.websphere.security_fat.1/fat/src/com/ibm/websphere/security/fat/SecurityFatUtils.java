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

package com.ibm.websphere.security.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Some utility functions for FATs.
 */
public class SecurityFatUtils {

    /**
     * JakartaEE9 transform a list of applications. The applications are the simple app names and they must exist at '<server>/dropins/<appname>'.
     *
     * @param myServer The server to transform the applications on.
     * @param apps The simple names of the applications to transform.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEE9Action.isActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }
}
