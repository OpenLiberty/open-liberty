/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.classloading;

import java.io.File;
import java.util.Map;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

public class TestUtils {

    static void setSysProps(LibertyServer targetServer, Map<String,String> props) throws Exception {
        Map<String,String> options = targetServer.getJvmOptionsAsMap();
        options.putAll(props);
        targetServer.setJvmOptions(options);
    }

    static void removeSysProps(LibertyServer targetServer, Map<String,String> props) throws Exception {
        Map<String,String> options = targetServer.getJvmOptionsAsMap();
        options.keySet().removeAll(props.keySet());
        targetServer.setJvmOptions(options);
    }

    /**
     * Build a Bell library (JAR) and export it to the "sharedLib" directory of the target server.
     *
     * @param String targetServer The server that will contain the exported archive.
     * @param String archiveName The name of the Bell library archive, including the file extension (e.g. "my.jar").
     * @param String[] classNames  The short names of classes to package.
     */
    static void buildAndExportBellLibrary(LibertyServer targetServer, String archiveName, String... classNames) throws Throwable {
        JavaArchive bellArchive = ShrinkHelper.buildJavaArchive(
                archiveName,
                new org.jboss.shrinkwrap.api.Filter<ArchivePath>() {
                    @Override public boolean include(ArchivePath ap) {
                        for (String cn : classNames)
                            if (ap.get().endsWith(cn + ".class")) return true;
                        return false;
                    }
                },
                "com.ibm.ws.test");
        ShrinkHelper.exportToServer(targetServer, "sharedLib", bellArchive);
    }

    static final boolean IS_DROPIN = true;

    /**
     * Build a web app archive (WAR) and export it to the target server.
     *
     * @param String targetServer The server that will contain the exported archive.
     * @param boolean isDropin When true export the WAR to the "dropins" directory; otherwise, the apps directory.
     * @param String archiveName The name of the WAR, including the file extension (e.g. "my.war").
     * @param String[] packageNames  List of package names to collect into WEB-INF/classes.
     */
    static WebArchive buildAndExportWebApp(
            LibertyServer targetServer,
            boolean isDropin,
            String archiveName,
            String... packageNames) throws Exception {

        WebArchive webApp = ShrinkWrap.create(WebArchive.class, archiveName);
        webApp.addPackages(true, packageNames);
        File webInf = new File("test-applications/" + archiveName + "/resources/WEB-INF");
        if (webInf.exists()) {
            for (File webInfElement : webInf.listFiles()) {
                webApp.addAsWebInfResource(webInfElement);
            }
        }
        String appFolder = isDropin ? "dropins" : "apps";
        ShrinkHelper.exportToServer(targetServer, appFolder, webApp);
        return webApp;
    }
}
