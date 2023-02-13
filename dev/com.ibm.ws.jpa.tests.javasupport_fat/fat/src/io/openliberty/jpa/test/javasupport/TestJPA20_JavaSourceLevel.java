/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.jpa.test.javasupport;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.common.apiservices.Bootstrap;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@MinimumJavaLevel(javaLevel = 7)
public class TestJPA20_JavaSourceLevel extends AbstractTestJavaSourceLevel {
    @Server("javaSupportServer_JPA20")
    public static LibertyServer server_jss_jpa20;

    @BeforeClass
    public static void setup() throws Exception {
        if (!isJPA20Available()) {
            return;
        }

        setupFATForJPA20(server_jss_jpa20, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (!isJPA20Available()) {
            return;
        }

        shutdownFAT(server_jss_jpa20);
    }

    private static boolean isJPA20Available() throws Exception {
        Bootstrap b = Bootstrap.getInstance();
        String installRoot = b.getValue("libertyInstallPath");
        File jpa20Feature = new File(installRoot + "/lib/features/com.ibm.websphere.appserver.jpa-2.0.mf");
        return jpa20Feature.exists();
    }

    @Test
    @MinimumJavaLevel(javaLevel = 7)
    public void testJPA20WithJava07() throws Exception {
        if (!isJPA20Available()) {
            return;
        }
        runTest(server_jss_jpa20, 7);
    }

}
