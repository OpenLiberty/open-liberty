/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.microprofile.openapi.fat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.microprofile.openapi.fat.annotations.AnnotationProcessingTest;
import com.ibm.ws.microprofile.openapi.fat.config.OpenAPIConfigQuickTest;
import com.ibm.ws.microprofile.openapi.fat.config.OpenAPIConfigTest;
import com.ibm.ws.microprofile.openapi.fat.filter.FilterConfigTest;
import com.ibm.ws.microprofile.openapi.validation.fat.ValidationSuite;

import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.security.utils.SSLUtils;

@RunWith(Suite.class)
@SuiteClasses({
    AnnotationProcessingTest.class,
    ApplicationProcessorServletTest.class,
    ApplicationProcessorTest.class,
    ContentTypeTest.class,
    OpenAPIConfigQuickTest.class,
    OpenAPIConfigTest.class,
    ValidationSuite.class,
    FilterConfigTest.class,
    ProxySupportTest.class,
    EndpointAvailabilityTest.class,
    UICustomizationTest.class,
    OpenAPICorsTest.class
})

public class FATSuite {
    public static RepeatTests defaultRepeat(String serverName) {
        return MicroProfileActions.repeat(serverName,
            MicroProfileActions.MP71_EE10, // mpOpenAPI-4.1 + EE10 , LITE
            MicroProfileActions.MP71_EE11, // mpOpenAPI-4.1 + EE11 , FULL
            MicroProfileActions.MP70_EE10, // mpOpenAPI-4.0 + EE10 , FULL
            MicroProfileActions.MP70_EE11, // mpOpenAPI-4.0 + EE11 , FULL
            MicroProfileActions.MP61, // mpOpenAPI-3.1, FULL
            MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
            MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
            MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
            MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL
    }

    public static RepeatTests repeatPre40(String serverName) {
        return MicroProfileActions.repeat(serverName,
            MicroProfileActions.MP61, // mpOpenAPI-3.1, LITE
            MicroProfileActions.MP50, // mpOpenAPI-3.0, FULL
            MicroProfileActions.MP41, // mpOpenAPI-2.0, FULL
            MicroProfileActions.MP33, // mpOpenAPI-1.1, FULL
            MicroProfileActions.MP22);// mpOpenAPI-1.0, FULL
    }

    /**
     * A limited set of repeats for slow tests where the code is common
     * @param serverName the liberty server name
     * @return a repeat rule
     */
    public static RepeatTests repeatLimited(String serverName) {
        return MicroProfileActions.repeat(serverName, MicroProfileActions.MP70_EE10,
            MicroProfileActions.MP33);
    }

    static {
        /*
         * Set property to allow the use of the 'Origin' header in CORS tests This
         * property is read one time only when the 'HttpURLConnection' class is first
         * instantiated and cannot be changed. Setting this property before the tests
         * run ensures that the value is not defaulted to 'False' when non-CORS tests
         * run
         */
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    /**
     * Pre-generate a keystore for every server to avoid liberty generating one on every server start
     * @throws Exception if something goes wrong
     */
    @BeforeClass
    public static void createKeystores() throws Exception {
        long startTime = System.nanoTime();
        KeyPair key = SSLUtils.generateKeyPair();
        Certificate cert = SSLUtils.selfSign(key, "cn=localhost", Arrays.asList("localhost"));

        Path serversDir = Paths.get("publish", "servers");
        try (DirectoryStream<Path> servers = Files.newDirectoryStream(serversDir)) {
            for (Path server : servers) {
                Path keyDir = server.resolve("resources/security");
                Files.createDirectories(keyDir);
                outputKey(key, cert, keyDir.resolve("key.p12"), "password".toCharArray());
            }
        }
        Log.info(FATSuite.class, "createKeystores",
            "Created keystores in " + Duration.ofNanos(System.nanoTime() - startTime));
    }

    private static void outputKey(KeyPair key,
                                  Certificate cert,
                                  Path path,
                                  char[] password)
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null); // Initialize empty keystore
        ks.setKeyEntry("key", key.getPrivate(), password, new Certificate[] {
            cert
        });
        try (OutputStream os = Files.newOutputStream(path)) {
            ks.store(os, password);
        }
    }
}
