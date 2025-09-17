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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 17)
@RunWith(FATRunner.class)
public class UnsupportedConfigWarningTest30 extends UnsupportedConfigWarningTestBase {
    @AfterClass
    public static void stopTestServer() throws Exception {
        server.stopServer("CWWKC0261W", "CWWKC0262W");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        List<String> appArgs = appConfig.getApplicationArguments();
        appArgs.add("--server.servlet.session.persistent=true");
        appArgs.add("--server.compression.enabled=true");
    }
}
