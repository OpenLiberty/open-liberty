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

import java.net.SocketException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class SSLMutualAuthTests20 extends SSLCommonTests {

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0", "transportSecurity-1.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @After
    public void stopTestServer() throws Exception {
        String methodName = testName.getMethodName();

        if ("testClientAuthNeedWithoutClientSideKeyStoreFor20".equals(methodName)) {
            super.stopServer(true, "CWWKO0801E");
        } else {
            super.stopServer(true);
        }
    }

    /* Passes when application property server.ssl.client-auth=NEED and if client side keystore and truststore are provided for authentication. */
    @Test
    public void testClientAuthNeedWithClientSideKeyStoreFor20() throws Exception {
        testSSLApplication();
    }

    /*
     * Fails when application property server.ssl.client-auth=NEED and if client side keystore and truststore are not provided for authentication.
     * This scenario throws a Socket Exception
     */
    @Test
    public void testClientAuthNeedWithoutClientSideKeyStoreFor20() throws Exception {
        try {
            testSSLApplication();
            Assert.fail("The connection should not succeed");
        } catch (SocketException e) {
            // we get different exceptions; this is from Oracle VM
        } catch (SSLException e) {
            // we get different exceptions; this is from IBM VM
        }
    }

    /* Passes when application property server.ssl.client-auth=WANT and even if client side keystore and truststore are not provided for authentication. */
    @Test
    public void testClientAuthWantWithoutClientSideKeyStoreFor20() throws Exception {
        testSSLApplication();
    }
}
