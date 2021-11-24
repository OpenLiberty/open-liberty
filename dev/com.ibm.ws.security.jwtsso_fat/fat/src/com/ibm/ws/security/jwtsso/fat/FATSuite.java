/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                FeatureOnlyTest.class,
                ConfigAttributeTests.class,
                CookieProcessingTests.class,
                ReplayCookieTests.class,
                CookieExpirationTests.class,
                BuilderTests.class,
                SigAlgTests.class,
//                EncryptionTests.class
})
public class FATSuite {

    public static boolean isJakartaEE9() {
        return RepeatTestFilter.isRepeatActionActive(JwtFatConstants.MPJWT_VERSION_20) || 
               RepeatTestFilter.isRepeatActionActive(JwtFatConstants.NO_MPJWT_EE9);
    }
    /**
     * JakartaEE9 transform a list of applications.
     *
     * @param myServer The server to transform the applications on.
     * @param apps     The names of the applications to transform. Should include the path from the server root directory.
     */
    public static void transformApps(LibertyServer myServer, String... apps) {
        if (isJakartaEE9()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }
}
