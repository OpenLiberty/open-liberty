/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;

import componenttest.annotation.MaximumJavaLevel;

/**
 *
 */
@MaximumJavaLevel(javaLevel = 8)
public class ErrorPage15Test extends ErrorPageBaseTest {

    @AfterClass
    public static void stopTestServer() throws Exception {
        if (!javaVersion.startsWith("1.")) {
            server.stopServer("Exception: Thrown on purpose for FAT test", "SRVE0777E: Exception thrown by application class", "CWWKC0265W");
        } else {
            server.stopServer("Exception: Thrown on purpose for FAT test", "SRVE0777E: Exception thrown by application class");
        }
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

}
