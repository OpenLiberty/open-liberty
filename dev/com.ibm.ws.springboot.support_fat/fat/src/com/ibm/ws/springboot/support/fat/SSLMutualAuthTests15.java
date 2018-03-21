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
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class SSLMutualAuthTests15 extends SSLCommonTests {

    @Test
    public void testSSLMutualAuthSpringBootApplication15() throws Exception {
        testSSLApplication();
    }

    @Override
    public String getKeyStorePath() {
        try {
            RemoteFile ksRemoteFile = server.getFileFromLibertyServerRoot("client-keystore.jks");
            return ksRemoteFile.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalStateException("Key Store file not found", e);
        }
    }

    @Override
    public String getKeyStorePassword() {
        return "secret";
    }

    @Override
    public String getTrustStorePath() {
        try {
            RemoteFile tsRemoteFile = server.getFileFromLibertyServerRoot("client-truststore.jks");
            return tsRemoteFile.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalStateException("Trust Store file not found", e);
        }
    }

    @Override
    public String getTrustStorePassword() {
        return "secret";
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "servlet-3.1", "ssl-1.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

}
