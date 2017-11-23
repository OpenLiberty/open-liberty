/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.io.File;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyServer;

/**
 * Helper utilities for working with the ShrinkWrap APIs.
 */
public class ShrinkHelper {

    protected static final Class<?> c = ShrinkHelper.class;

    /**
     * Export an artifact to servers/$server.getName()/$path/$a.getName() under two directories:
     * autoFVT/publish/... and wlp/usr/...
     */
    public static void exportToServer(LibertyServer server, String path, Archive<?> a) throws Exception {
        String serverDir = "publish/servers/" + server.getServerName();
        exportArtifact(a, serverDir + '/' + path);
        server.copyFileToLibertyServerRoot(serverDir + "/" + path, path, a.getName());
    }

    /**
     * Export an artifact to clients/$client.getName()/$path/$a.getName() under two directories:
     * autoFVT/publish/... and wlp/usr/...
     */
    public static void exportToClient(LibertyClient client, String path, Archive<?> a) throws Exception {
        String clientDir = "publish/clients/" + client.getClientName();
        exportArtifact(a, clientDir + '/' + path);
        client.copyFileToLibertyClientRoot(clientDir + "/" + path, path, a.getName());
    }

    /**
     * Writes an application to a a file in the 'publish/servers/<server_name>/apps/' directory
     * with the file name returned by a.getName(), which should include the
     * file type extension (.ear, .war, .jar, .rar, etc)
     *
     * @param server The server to publish the application to
     * @param a The archive to export as a file
     */
    public static void exportAppToServer(LibertyServer server, Archive<?> a) throws Exception {
        exportToServer(server, "apps", a);
    }

    /**
     * Writes an Archive to a a file in the 'publish/servers/<server_name>' directory
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc)
     *
     * @param server The server to publish the application to
     * @param a The archive to export as a file
     * @throws Exception
     */
    public static void exportDropinAppToServer(LibertyServer server, Archive<?> a) throws Exception {
        exportToServer(server, "dropins", a);
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc)
     *
     * @param a The archive to export as a file
     * @param dest The target folder to export the archive to (i.e. publish/files/apps)
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest) {
        return exportArtifact(a, dest, true);
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc).
     *
     * @param a The archive to export as a file
     * @param dest The target folder to export the archive to (i.e. publish/files/apps)
     * @param printArchiveContents Whether or not to log the contents of the archive being exported
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest, boolean printArchiveContents) {
        Log.info(c, "exportArtifact", "Exporting shrinkwrap artifact: " + a.toString() + " to " + dest);
        File outputFile = new File(dest, a.getName());
        outputFile.getParentFile().mkdirs();
        a.as(ZipExporter.class).exportTo(outputFile, true);
        if (printArchiveContents)
            Log.info(ShrinkHelper.class, "exportArtifact", a.toString(true));
        return a;
    }

    /**
     * Recursively adds a folder and all of its contents to an archive.
     *
     * @param a The archive to add the files to
     * @param dir The directory which will be recursively added to the archive.
     */
    public static Archive<?> addDirectory(Archive<?> a, String dir) throws Exception {
        return addDirectory(a, dir, Filters.includeAll());
    }

    /**
     * Recursively adds a folder and all of its contents matching a filter to an archive.
     *
     * @param a The archive to add the files to
     * @param dir The directory which will be recursively added to the archive.
     * @param filter A filter indicating which files should be included in the archive
     */
    public static Archive<?> addDirectory(Archive<?> a, String dir, Filter<ArchivePath> filter) throws Exception {
        return a.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class).importDirectory(dir).as(GenericArchive.class),
                       "/", filter);
    }

    /**
     * Builds a WebArchive (WAR) with the default format, which assumes all resources are at:
     * 'test-applications/$appName/resources/`
     *
     * @param appName The name of the application. The '.war' file extension is assumed
     * @param packages A list of java packages to add to the application.
     * @return a WebArchive representing the application created
     */
    public static WebArchive buildDefaultApp(String appName, String... packages) throws Exception {
        String appArchiveName = appName.endsWith(".war") ? appName : appName + ".war";
        WebArchive app = ShrinkWrap.create(WebArchive.class, appArchiveName);
        for (String p : packages) {
            if (p.endsWith(".*"))
                app = app.addPackages(true, p.replace(".*", ""));
            else
                app = app.addPackages(false, p);
        }
        if (new File("test-applications/" + appName + "/resources/").exists())
            app = (WebArchive) addDirectory(app, "test-applications/" + appName + "/resources/");
        return app;
    }

    /**
     * Builds a JavaArchive (JAR) with the default format, which assumes all resources are at:
     * 'test-applications/$appName/resources/`
     *
     * @param name The name of the jar. The '.jar' file extension is assumed
     * @param packages A list of java packages to add to the application.
     * @return a JavaArchive representing the JAR created
     */
    public static JavaArchive buildJavaArchive(String name, String... packages) throws Exception {
        String archiveName = name.endsWith(".jar") ? name : name + ".jar";
        JavaArchive app = ShrinkWrap.create(JavaArchive.class, archiveName);
        for (String p : packages) {
            if (p.endsWith(".*"))
                app = app.addPackages(true, p.replace(".*", ""));
            else
                app = app.addPackages(false, p);
        }
        if (new File("test-applications/" + name + "/resources/").exists())
            app = (JavaArchive) addDirectory(app, "test-applications/" + name + "/resources/");
        return app;
    }

    /**
     * Invokes {@link #buildDefaultApp(String, String...)}
     * and then exports the resulting application to a Liberty server under the "dropins" directory
     *
     * @param server The server to export the application to
     * @param appname The name of the application
     * @param packages A list of java packages to add to the application.
     */
    public static WebArchive defaultDropinApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = buildDefaultApp(appName, packages);
        exportDropinAppToServer(server, app);
        String installedAppName = (appName.endsWith(".war") || appName.endsWith(".ear"))//
                        ? appName.substring(0, appName.length() - 4) : appName;
        server.addInstalledAppForValidation(installedAppName);
        return app;
    }

    /**
     * Invokes {@link #buildDefaultApp(String, String...)}
     * and then exports the resulting application to a Liberty server under the "apps" directory
     *
     * @param server The server to export the application to
     * @param appname The name of the application
     * @param packages A list of java packages to add to the application.
     */
    public static WebArchive defaultApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = buildDefaultApp(appName, packages);
        exportAppToServer(server, app);
        String installedAppName = (appName.endsWith(".war") || appName.endsWith(".ear"))//
                        ? appName.substring(0, appName.length() - 4) : appName;
        server.addInstalledAppForValidation(installedAppName);
        return app;
    }
}
