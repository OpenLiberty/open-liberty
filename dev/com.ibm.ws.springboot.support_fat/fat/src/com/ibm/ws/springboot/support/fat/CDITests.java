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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(FULL)
public class CDITests extends CommonWebServerTests {

    private static final String TEST_APP_20_WITH_CDI = "testSpringBootApp20WithCDIFeatureEnabled";

    @Override
    public String getApplication() {
        String methodName = testName.getMethodName();
        if (TEST_APP_20_WITH_CDI.equals(methodName)) {
            return SPRING_BOOT_20_APP_BASE;
        }
        Assert.fail("Unknown test.");
        return null;
    }

    @Override
    public Set<String> getFeatures() {
        String methodName = testName.getMethodName();
        Set<String> features = new HashSet<>(Arrays.asList("servlet-3.1", "cdi-1.2"));
        if (TEST_APP_20_WITH_CDI.equals(methodName)) {
            features.add("springBoot-2.0");
        } else {
            Assert.fail("Unknown test.");
        }
        return features;
    }

    @After
    public void stopTestServer() throws Exception {
        super.stopServer(true);
    }

    @Test
    public void testSpringBootApp20WithCDIFeatureEnabled() throws Exception {
        testBasicSpringBootApplication();
    }
}
