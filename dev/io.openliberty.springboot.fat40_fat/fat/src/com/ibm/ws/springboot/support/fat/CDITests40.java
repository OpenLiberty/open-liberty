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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class CDITests40 extends CommonWebServerTests {

    @After
    public void stopTestServer() throws Exception {
        super.stopServer(true);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }

    @Override
    public Set<String> getFeatures() {
        Set<String> features = getWebFeatures();
        features.add("cdi-4.1");
        return features;
    }

    @Test
    public void testSpringBootApp40WithCDIFeatureEnabled() throws Exception {
        testBasicSpringBootApplication();
    }
}
