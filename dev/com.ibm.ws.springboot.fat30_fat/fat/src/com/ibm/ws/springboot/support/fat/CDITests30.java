/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class CDITests30 extends CommonWebServerTests {

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public Set<String> getFeatures() {
        Set<String> features = new HashSet<>(3);
        features.add("springBoot-3.0");
        features.add("servlet-6.0");
        features.add("cdi-2.1");
        return features;
    }

    @After
    public void stopTestServer() throws Exception {
        super.stopServer(true);
    }

    @Test
    public void testSpringBootApp30WithCDIFeatureEnabled() throws Exception {
        testBasicSpringBootApplication();
    }
}
