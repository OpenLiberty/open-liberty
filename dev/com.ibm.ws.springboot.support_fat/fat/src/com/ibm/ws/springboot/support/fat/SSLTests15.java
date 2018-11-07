/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class SSLTests15 extends SSLCommonTests {
    @AfterClass
    public static void stopTestServer() throws Exception {
        if (!javaVersion.startsWith("1.")) {
            server.stopServer("CWWKC0265W");
        }
    }

    @Test
    public void testSSLSpringBootApplication15() throws Exception {
        testSSLApplication();
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "servlet-3.1", "transportSecurity-1.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

}