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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ConfigSpringBootApplicationWithArgsTests30 extends AbstractSpringTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        List<String> appArgs = appConfig.getApplicationArguments();
        appArgs.add("--myapp.arg1=value1");
        appArgs.add("--myapp.arg2=value2");
    }

    @Test
    public void testSpringAppArg1() throws Exception {
        HttpUtils.findStringInUrl(server, "/getAppProp?key=myapp.arg1", "value1");
    }

    @Test
    public void testSpringAppArg2() throws Exception {
        HttpUtils.findStringInUrl(server, "/getAppProp?key=myapp.arg2", "value2");
    }
}
