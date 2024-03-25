/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;

public class ExplodedShrinkHelper {
    private static final Logger LOG = Logger.getLogger(ExplodedShrinkHelper.class.getName());

    public static WebArchive explodedApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packages);
        return (WebArchive) explodedArchiveToDestination(server, app, "apps");
    }

    public static EnterpriseArchive explodedEarApp(LibertyServer server, WebArchive warArchive, String warName,
                                                   String earName, boolean addEarResources, String... packages) throws Exception {
        String methodName = "explodedEarApp";
        LOG.info(methodName + ": Create ear: " + earName);
        EnterpriseArchive earArchive = ShrinkWrap.create(EnterpriseArchive.class, earName + ".ear");

        LOG.info(methodName + ": Add war: " + warName);
        earArchive.addAsModule(warArchive);

//		if (addEarResources) {
//			LOG.info(methodName + ": Add EAR descriptor: " + earName);
//			String appDescriptorPath = "test-applications/" + earName + "/resources/META-INF/application.xml";
//			earArchive.addAsManifestResource(new File(appDescriptorPath));
//		}
//
//		String permissionsPath = "test-applications/" + earName + "/resources/META-INF/permissions.xml";
//		File permissionsFile = new File(permissionsPath);
//		if (permissionsFile.exists()) {
//			LOG.info(methodName + ": Add EAR permissions: " + earName);
//			earArchive.addAsManifestResource(permissionsFile);
//		}

        LOG.info(methodName + ": Export EAR to server: " + earName);
        // ShrinkHelper.exportToServer(server, toServerDir, earArchive);

        return (EnterpriseArchive) explodedArchiveToDestination(server, earArchive, "apps");
    }

    public static WebArchive explodedDropinApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packages);
        return (WebArchive) explodedArchiveToDestination(server, app, "dropins");
    }

    public static WebArchive explodedWarToDestination(LibertyServer server, String dest, String appName,
                                                      String... packages) throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(appName, packages);
        return (WebArchive) explodedArchiveToDestination(server, war, dest);
    }

    public static JavaArchive explodedJarToDestination(LibertyServer server, String dest, String appName,
                                                       String... packages) throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchive(appName, packages);
        return (JavaArchive) explodedArchiveToDestination(server, jar, dest);
    }

    public static Archive<?> explodedArchiveToDestination(LibertyServer server, Archive<?> archive, String dest) throws Exception {
        String localLocation;
        LOG.info("explodedArchiveToDestination dest=" + dest);
        if (dest.isEmpty()) {
            localLocation = "publish/servers/" + server.getServerName();
            dest = server.getServerName();
        } else {
            localLocation = "publish/servers/" + server.getServerName() + "/" + dest;
        }
        LOG.info("explodedArchiveToDestination localLocation=" + localLocation);
        File outputFile = new File(localLocation);
        outputFile.mkdirs();
        File explodedFile = archive.as(ExplodedExporter.class).exportExploded(outputFile, archive.getName());
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(explodedFile.toPath());
        }
        copyFileToDirectory(server, outputFile, dest);
        return archive;
    }

    public static void copyFileToDirectory(LibertyServer server, File file, String dir) throws Exception {
        if (file.isDirectory()) {
            copyFile(server, file, dir);
            for (File f : file.listFiles()) {
                copyFileToDirectory(server, f, dir);
            }
        } else {
            copyFile(server, file, dir);
        }
    }

    private static void copyFile(LibertyServer server, File file, String dir) throws Exception {

        // String dir literal can't be found on Windows platform since it contains different File.separator
        if (server.getMachine().getOSVersion().toLowerCase().contains("windows")) {
            dir = dir.replaceAll("/", "\\\\");

        }

        String path = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(dir));
        path = path.substring(0, path.lastIndexOf(file.getName()));
        LOG.info("copyFile path=" + path);
        server.copyFileToLibertyServerRoot(path, "../../" + file.getPath());
    }
}
