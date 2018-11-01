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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;

@Mode(FULL)
@RunWith(FATRunner.class)
public class JavaeeFeatureTests15 extends AbstractSpringTests {
    @AfterClass
    public static void stopTestServer() throws Exception {
        if (!javaVersion.startsWith("1.")) {
            server.stopServer("CWWKC0265W");
        }
    }

    /**
     * Export JavaEE feature API to basic Spring Boot application.
     */
    @Test
    public void testBasicAppEnableJavaee70() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "javaee-7.0"));
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        List<KeyStore> keystores = config.getKeyStores();
        keystores.clear();

        KeyStore keyStore = new KeyStore();
        keystores.add(keyStore);
        keyStore.setId("defaultKeyStore");
        keyStore.setPassword("yourPassword");
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

}
