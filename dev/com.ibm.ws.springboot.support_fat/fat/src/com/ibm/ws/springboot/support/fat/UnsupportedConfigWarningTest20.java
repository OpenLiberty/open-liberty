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
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;

import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class UnsupportedConfigWarningTest20 extends UnsupportedConfigWarningTestBase {
    @AfterClass
    public static void stopTestServer() throws Exception {
        server.stopServer("CWWKC0261W", "CWWKC0262W");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        List<String> appArgs = appConfig.getApplicationArguments();
        appArgs.add("--server.servlet.session.persistent=true");
        appArgs.add("--server.compression.enabled=true");
    }
}
