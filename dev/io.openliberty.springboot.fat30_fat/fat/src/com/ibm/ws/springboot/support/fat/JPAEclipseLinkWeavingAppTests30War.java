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

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.WebApplication;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JPAEclipseLinkWeavingAppTests30War extends JPAAppAbstractTests {

    @BeforeClass
    public static void modifyTestApp() {
        // modify the app to include hibernate in the WEB-INF/lib folder from the WEB-INF/lib-provided folder
        try {
            RemoteFile rFile = server.getFileFromLibertyServerRoot("apps/" + SPRING_BOOT_30_APP_DATA);
            File f = new File(rFile.getAbsolutePath());
            removePersistenceXML(f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String eclipseLinkAppName() {
        return "eclipseLink.weaving." + SPRING_BOOT_30_APP_DATA;
    }

    private static void removePersistenceXML(File f) throws IOException {
        try (JarFile jarFile = new JarFile(f);
                        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(new File(f.getParentFile(), eclipseLinkAppName())))) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry originalEntry = entries.nextElement();
                JarEntry newEntry = originalEntry;
                String entryName = originalEntry.getName();
                if (entryName.equals("WEB-INF/classes/META-INF/persistence.xml")) {
                    // Remove the default persistence.xml processed by Liberty
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

    @Override
    public void modifyAppConfiguration(WebApplication appConfig) {
        // Using Spring class EclipseLinkJpaVendorAdapter directly requires thrid-party API access
        ClassloaderElement classloader = new ClassloaderElement();
        classloader.setApiTypeVisibility("+third-party");
        appConfig.getClassloaders().add(classloader);
    }

    @Test
    public void testEclipseLinkWeavingJPAAppRunnerWar() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("COMMAND_LINE_RUNNER: SPRING DATA TEST: PASSED"));
    }

    @Test
    public void testEclipseLinkWeavingJPAWebContextWar() throws Exception {
        HttpUtils.findStringInUrl(server, "testName/testPersistence", "TESTED PERSISTENCE");
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("WEB_CONTEXT: SPRING DATA TEST: PASSED"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-6.0", "jdbc-4.2", "jndi-1.0", "componenttest-2.0", "persistence-3.1"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }

    @AfterClass
    public static void stopServerWithErrors() throws Exception {
        server.stopServer("CWWJP9991W", "WTRN0074E");
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        return Collections.singletonMap("test.persistence", "eclipselink.weaving");
    }

    @Override
    public String getApplication() {
        return eclipseLinkAppName();
    }
}
