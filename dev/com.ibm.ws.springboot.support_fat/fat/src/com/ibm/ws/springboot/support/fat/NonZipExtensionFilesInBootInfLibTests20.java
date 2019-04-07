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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;

/**
 * The tests expects an exception to be thrown if the BOOT-INF library of the application
 * is not a valid zip regardless of the extension. And the server will start successfully
 * if all the BOOT-INF libraries are valid zip files regardless of the extension.
 */
@RunWith(FATRunner.class)
@Mode(FULL)
public class NonZipExtensionFilesInBootInfLibTests20 extends AbstractSpringTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Override
    public boolean expectApplicationSuccess() {
        String methodName = testName.getMethodName();
        if (methodName.contains("ServerStartSuccess")) {
            return true;
        }
        return false;
    }

    @Test
    public void expectServerStartFailureWhenANonZipLibraryExistsInAppArchive() throws Exception {
        // First stop the server so we can add a non zip library in the app archive
        server.stopServer(false);

        RemoteFile dropinsSpr = server.getFileFromLibertyServerRoot("dropins/" + SPRING_APP_TYPE);
        RemoteFile[] dropinApps = dropinsSpr.list(true);

        for (RemoteFile dropinApp : dropinApps) {
            if (dropinApp.isFile()) {
                File appFile = new File(dropinApp.getAbsolutePath());
                File tempFile = new File(dropinApp.getAbsolutePath() + ".tmp");
                if (!tempFile.exists()) {
                    Files.createFile(tempFile.toPath());
                }
                putANonZipEntry(appFile, tempFile);
                appFile.delete();
                tempFile.renameTo(appFile);
            }
        }

        server.startServer(true);

        server.setMarkToEndOfLog();

        stopServer(true, "CWWKZ0002E", "The entry BOOT-INF/lib/test.txt is not a valid zip.");

    }

    @Test
    public void expectServerStartSuccessWhenAZipLibraryWithNonZipExtensionExistsInAppArchive() throws Exception {
        // First stop the server so we can add a zip library with a non zip extension in the app archive
        server.stopServer(false);

        RemoteFile dropinsSpr = server.getFileFromLibertyServerRoot("dropins/" + SPRING_APP_TYPE);
        RemoteFile[] dropinApps = dropinsSpr.list(true);

        for (RemoteFile dropinApp : dropinApps) {
            if (dropinApp.isFile()) {
                File appFile = new File(dropinApp.getAbsolutePath());
                File tempFile = new File(dropinApp.getAbsolutePath() + ".tmp");
                if (!tempFile.exists()) {
                    Files.createFile(tempFile.toPath());
                }
                putAZipEntryWithNonZipExtension(appFile, tempFile);
                appFile.delete();
                tempFile.renameTo(appFile);
            }
        }

        server.startServer(true);

        server.setMarkToEndOfLog();

        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

    }

    /**
     * Put all entries of the app file into the temp file, in addition to new non zip
     * library entry.
     *
     * @param appFile
     * @param tempFile
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private void putANonZipEntry(File appFile, File tempFile) throws IOException {
        JarFile appJar = new JarFile(appFile);
        byte[] buffer = new byte[4096];
        int len;
        String newEntry = "BOOT-INF/lib/test.txt";
        HashSet<String> zipEntries = new HashSet<>();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempFile))) {
            for (Enumeration<JarEntry> entries = appJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (!entryName.equals(newEntry) && !zipEntries.contains(entryName)) {
                    zipEntries.add(entryName);
                    jos.putNextEntry(entry);
                    try (InputStream entryStream = appJar.getInputStream(entry)) {
                        while ((len = entryStream.read(buffer)) != -1) {
                            jos.write(buffer, 0, len);
                        }
                    }
                }
            }
            //Add a non zip entry BOOT-INF/lib/test.txt
            jos.putNextEntry(new ZipEntry(newEntry));
            try (ByteArrayInputStream bis = new ByteArrayInputStream(new byte[] { 't', 'e', 's', 't' })) {
                while ((len = bis.read(buffer)) != -1) {
                    jos.write(buffer, 0, len);
                }
            }
        }
    }

    /**
     * Put all the entries of the app file into the temp file, Change the file extension of one of the
     * library jar.
     *
     * @param appFile
     * @param tempFile
     * @throws IOException
     */
    @SuppressWarnings("resource")
    private void putAZipEntryWithNonZipExtension(File appFile, File tempFile) throws IOException {
        JarFile appJar = new JarFile(appFile);
        byte[] buffer = new byte[4096];
        int len;
        HashSet<String> zipEntries = new HashSet<>();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempFile))) {
            for (Enumeration<JarEntry> entries = appJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!zipEntries.contains(entryName)) {
                    zipEntries.add(entryName);
                    if (entryName.equals("BOOT-INF/lib/spring-boot-starter-web-2.0.0.RELEASE.jar")) {
                        //change the extension of the library jar
                        JarEntry entryWithDiffExt = new JarEntry("BOOT-INF/lib/" + entryName + ".xyz");
                        jos.putNextEntry(entryWithDiffExt);
                    } else {
                        jos.putNextEntry(entry);
                    }

                    try (InputStream entryStream = appJar.getInputStream(entry)) {
                        while ((len = entryStream.read(buffer)) != -1) {
                            jos.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }
}
