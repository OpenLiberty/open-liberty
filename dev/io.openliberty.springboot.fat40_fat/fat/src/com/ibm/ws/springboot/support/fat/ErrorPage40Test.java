/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ErrorPage40Test extends ErrorPageBaseTest {

    @AfterClass
    public static void stopTestServer() throws Exception {
        server.stopServer("Exception: Thrown on purpose for FAT test", "SRVE0777E: Exception thrown by application class");
    }

    /**
     * Override: Web applications use springboot and servlet.
     *
     * @return The features provisioned in the test server. This
     *         implementation always answers "springBoot-4.0" and
     *         "servlet-6.1`".
     */
    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }
}
