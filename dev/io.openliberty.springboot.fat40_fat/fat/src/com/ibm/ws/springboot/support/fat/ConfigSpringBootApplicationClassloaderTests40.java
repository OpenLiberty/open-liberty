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

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;

@Mode(FULL)
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConfigSpringBootApplicationClassloaderTests40 extends AbstractSpringTests {

    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        configureClassloader(appConfig);
    }

    static enum ApiTypeVisibility {
        NONE {
            @Override
            public String toString() {
                return "";
            }
        },
        DEFAULT {
            @Override
            public String toString() {
                return "spec, ibm-api";
            }
        },
        DEFAULT_ADD_TP {
            @Override
            public String toString() {
                return "spec, ibm-api, third-party";
            }
        },
        DEFAULT_NO_IBMAPI {
            @Override
            public String toString() {
                return "spec, third-party";
            }
        }
    }

    void configureClassloader(SpringBootApplication appConfig) {
        List<ClassloaderElement> configuredLoaders = appConfig.getClassloaders();
        configuredLoaders.clear();

        if (testName.getMethodName().endsWith("DefaultVisibility")) {
            ClassloaderElement loader = new ClassloaderElement();
            loader.setApiTypeVisibility(ApiTypeVisibility.DEFAULT.toString());
            configuredLoaders.add(loader);
        } else if (testName.getMethodName().endsWith("DefaultNoIbmApiVisibility")) {
            ClassloaderElement loader = new ClassloaderElement();
            loader.setApiTypeVisibility(ApiTypeVisibility.DEFAULT_NO_IBMAPI.toString());
            configuredLoaders.add(loader);
        } else {
        }
    }

    @After
    public void stopServerAfterTest() {
        try {
            AbstractSpringTests.stopServer();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testSpringBootApplicationClassloaderDefaultVisibility() throws Exception {
        HttpUtils.findStringInUrl(server, "loadIbmApiClass", "SPRING BOOT, YOU GOT CLAZZ");
    }

    @Test
    public void testSpringBootApplicationClassloaderDefaultNoIbmApiVisibility() throws Exception {
        HttpUtils.findStringInUrl(server, "loadIbmApiClass", "SPRING BOOT, YOU GOT NO CLAZZ");
    }

}
