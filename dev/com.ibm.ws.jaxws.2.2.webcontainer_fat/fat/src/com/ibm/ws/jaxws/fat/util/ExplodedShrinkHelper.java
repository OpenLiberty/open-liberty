/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat.util;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class ExplodedShrinkHelper {
    public static WebArchive explodedApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packages);
        return (WebArchive) explodedArchiveToDestination(server, app, "apps");
    }

    public static WebArchive explodedDropinApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, packages);
        return (WebArchive) explodedArchiveToDestination(server, app, "dropins");
    }

    public static WebArchive explodedWarToDestination(LibertyServer server, String dest, String appName, String... packages) throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp(appName, packages);
        return (WebArchive) explodedArchiveToDestination(server, war, dest);
    }

    public static JavaArchive explodedJarToDestination(LibertyServer server, String dest, String appName, String... packages) throws Exception {
        JavaArchive jar = ShrinkHelper.buildJavaArchive(appName, packages);
        return (JavaArchive) explodedArchiveToDestination(server, jar, dest);
    }

    public static Archive<?> explodedArchiveToDestination(LibertyServer server, Archive<?> archive, String dest) throws Exception {
        String localLocation;
        Log.info(TestUtils.class, "explodedArchiveToDestination", "dest=" + dest);
        if (dest.isEmpty()) {
            localLocation = "publish/servers/" + server.getServerName();
            dest = server.getServerName();
        } else {
            localLocation = "publish/servers/" + server.getServerName() + "/" + dest;
        }
        Log.info(TestUtils.class, "explodedArchiveToDestination", "localLocation=" + localLocation);
        File outputFile = new File(localLocation);
        outputFile.mkdirs();
        archive.as(ExplodedExporter.class).exportExploded(outputFile, archive.getName());
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
        String path = file.getAbsolutePath().substring(file.getAbsolutePath().indexOf(dir));
        path = path.substring(0, path.lastIndexOf(file.getName()));
        Log.info(TestUtils.class, "copyFile", "path=" + path);
        server.copyFileToLibertyServerRoot(path, "../../" + file.getPath());
    }
}
