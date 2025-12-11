/*******************************************************************************
 * Copyright (c) 2018,2025 IBM Corporation and others.
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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class MissingWebsocketFeatureTests40 extends AbstractSpringTests {
    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Test
    public void testMissingWebsocketFor40() throws Exception {
        assertNotNull("No error message CWWKC0278E was found for missing websocket feature",
                      server.waitForStringInLog("CWWKC0278E"));
        stopServer(true, "CWWKC0278E", "CWWKZ0002E");
    }

    @Test
    public void testMissingWebsocketWithSecurity() throws Exception {
        assertNotNull("No error message CWWKC0278E was found for missing websocket feature",
                      server.waitForStringInLog("CWWKC0278E"));
        stopServer(true, "CWWKC0278E", "CWWKZ0002E");
    }

    @Override
    public Set<String> getFeatures() {
        HashSet<String> features = new HashSet<>(3);
        features.add("springBoot-4.0");
        features.add("servlet-6.1");

        String methodName = testName.getMethodName();
        if ((methodName != null) && methodName.equals("testMissingWebsocketWithSecurity")) {
            features.add("appSecurity-6.0");
        }

        return features;
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_WEBSOCKET;
    }

}
