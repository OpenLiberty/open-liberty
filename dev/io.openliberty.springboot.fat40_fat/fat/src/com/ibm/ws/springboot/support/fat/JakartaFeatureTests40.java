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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.config.ORB;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JakartaFeatureTests40 extends AbstractSpringTests {

    /**
     * Export JavaEE feature API to basic Spring Boot application.
     */
    @Test
    public void testBasicAppEnableJavaee11() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-4.0", "jakartaee-11.0"));
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        ORB orb = config.getOrb();
        orb.setId("defaultOrb");
        orb.setOrbSSLInitTimeout("45");

        List<KeyStore> keystores = config.getKeyStores();
        keystores.clear();

        KeyStore keyStore = new KeyStore();
        keystores.add(keyStore);
        keyStore.setId("defaultKeyStore");
        keyStore.setPassword("yourPassword");
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }
}
