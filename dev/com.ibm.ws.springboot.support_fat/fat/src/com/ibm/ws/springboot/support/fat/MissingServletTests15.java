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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class MissingServletTests15 extends AbstractSpringTests {
    @AfterClass
    public static void stopTestServer() throws Exception {
        if (!javaVersion.startsWith("1.")) {
            server.stopServer("CWWKC0254E", "CWWKZ0002E", "CWWKC0265W");
        } else {
            server.stopServer("CWWKC0254E", "CWWKZ0002E");
        }
    }

    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Test
    public void testMissingServletFor15() throws Exception {
        assertNotNull("No error message was found for missing servlet feature ", server.waitForStringInLog("CWWKC0254E"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

}
