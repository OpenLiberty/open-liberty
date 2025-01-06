/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.DockerClientFactory;

import componenttest.security.utils.SSLUtils;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class KeyPairs {

    private static File privateKeyFile;
    private static File certificateFile;

    /**
     * @param server
     */
    public KeyPairs(LibertyServer server) {
        generateSSLStuff(server);
    }

    public static String certificateFilePath() {
        return certificateFile.getAbsolutePath();
    }

    public File getKey() {
        return privateKeyFile;
    }

    public File getCertificate() {
        return certificateFile;
    }

    private static boolean createdSSLStuff = false;

    public synchronized static void generateSSLStuff(LibertyServer server) {
        if (createdSSLStuff) {
            return;
        }

        try {
            KeyPair generatedKeyPair = SSLUtils.generateKeyPair();

            String dockerIP = DockerClientFactory.instance().dockerHostIpAddress();
            String dnName = "O=Evil Inc Test Certificate, CN=" + dockerIP + ", L=Toronto,C=CA";
            List<String> genericNameList = new ArrayList<String>();
            genericNameList.add(dockerIP);

            Certificate certificateObject = SSLUtils.selfSign(generatedKeyPair, dnName, genericNameList);
            String pathToPrivateKey = "";
            File tempDir = new File("./temporaryKeyPairs");
            //Key
            if (server == null) { //server hasn't been initialised yet. Use temporary directory
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                pathToPrivateKey = tempDir.getAbsolutePath() + "/private.key";
            } else {
                pathToPrivateKey = server.getServerSharedPath() + "/private.key";
            }
            privateKeyFile = new File(pathToPrivateKey);
            System.out.println(pathToPrivateKey);
            SSLUtils.exportPrivateKeyToFile(privateKeyFile, generatedKeyPair);
            //Certificate
            String pathToCertificate = "";
            if (server == null) { //server hasn't been initialised yet. Use temporary directory
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                pathToCertificate = tempDir.getAbsolutePath() + "/certificate.crt";
            } else {
                pathToCertificate = server.getServerSharedPath() + "/certificate.crt";
            }
            certificateFile = new File(pathToCertificate);
            SSLUtils.exportCertificateToFile(certificateFile, certificateObject);
            createdSSLStuff = true;
        } catch (Exception e) { //If we get an exception let the test fail and show the developer what went wrong
            throw new RuntimeException("Exception doing SSLStuff", e);
        }
    }

}
