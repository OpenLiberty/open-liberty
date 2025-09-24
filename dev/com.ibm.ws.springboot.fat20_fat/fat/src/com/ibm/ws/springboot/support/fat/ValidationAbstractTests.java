/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public abstract class ValidationAbstractTests extends AbstractSpringTests {

    @BeforeClass
    public static void modifyTestApp() {
        try {
            RemoteFile rFile = server.getFileFromLibertyServerRoot("apps/" + SPRING_BOOT_20_APP_VALIDATION);
            File f = new File(rFile.getAbsolutePath());
            removeLibProvided(f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void removeLibProvided(File f) throws IOException {
        try (JarFile jarFile = new JarFile(f);
                        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(new File(f.getParentFile(), getModifiedApplicationName())))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry originalEntry = entries.nextElement();
                JarEntry newEntry = originalEntry;
                String entryName = originalEntry.getName();
                if (entryName.startsWith("WEB-INF/lib-provided/")) {
                    // remove all content of lib-provided
                    continue;
                }
                jarOutputStream.putNextEntry(newEntry);
                byte[] buffer = new byte[1024];
                int bytesRead;
                try (InputStream entryIn = jarFile.getInputStream(originalEntry)) {
                    while ((bytesRead = entryIn.read(buffer)) != -1) {
                        jarOutputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
        }
    }

    private static String getModifiedApplicationName() {
        return "provided.removed." + SPRING_BOOT_20_APP_VALIDATION;
    }

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public String getApplication() {
        return getModifiedApplicationName();
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }
}
