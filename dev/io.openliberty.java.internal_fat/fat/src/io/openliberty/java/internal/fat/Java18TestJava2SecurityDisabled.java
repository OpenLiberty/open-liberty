/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.java.internal.fat;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 18)
@MaximumJavaLevel(javaLevel = 18)
public class Java18TestJava2SecurityDisabled {
    @Server("java18-java2sec-disabled-server")
    public static LibertyServer server;

    @Test
    public void testJava2SecDisabledWithJava18() throws Exception {
        server.startServer();
        try {
            Assert.assertNotNull(server.waitForStringInLog("CWWKE0955E"));
        } finally {
            server.stopServer("CWWKE0955E");
        }
    }
}
