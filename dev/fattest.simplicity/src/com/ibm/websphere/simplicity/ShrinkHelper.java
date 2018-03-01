/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
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
import java.util.List;
import java.util.Arrays;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
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
     * If no ExportOptions.OVERWRITE is not provided this method will default to not overwriting the archive.
     */
    public static void exportToServer(LibertyServer server, String path, Archive<?> a, ExportOptions... exportOptions) throws Exception {

        List<ExportOptions> listExportOptions = Arrays.asList(exportOptions);

        if (listExportOptions.contains(ExportOptions.NO_OVERWRITE) && listExportOptions.contains(ExportOptions.OVERWRITE)){
            throw new IllegalArgumentException("Mutually exclusive export options were provided");
        }

        String serverDir = "publish/servers/" + server.getServerName();
        String localFilePath = serverDir + '/' + path;
        File localFile = new File(localFilePath, a.getName());
       
        exportArtifact(a, localFilePath, exportOptions);

        if (listExportOptions.contains(ExportOptions.OVERWRITE) || ! server.fileExistsInLibertyServerRoot(path + '/' + a.getName())) { 
            Log.info(ShrinkHelper.class, "exportToServer", "Copying file " + a.getName() + " to liberty server");
            server.copyFileToLibertyServerRoot(localFilePath, path, a.getName());
        } else {
            Log.info(ShrinkHelper.class, "exportToServer", "Not copying file " + a.getName() + " to liberty server because it already exists at the destination.");
        }
    }

    /**
     * Export an artifact to clients/$client.getName()/$path/$a.getName() under two directories:
     * autoFVT/publish/... and wlp/usr/...
     *
     * If no ExportOptions.OVERWRITE is not provided this method will default to not overwriting the archive.
     */
    public static void exportToClient(LibertyClient client, String path, Archive<?> a, ExportOptions... exportOptions) throws Exception {

        List<ExportOptions> listExportOptions = Arrays.asList(exportOptions);

        if (listExportOptions.contains(ExportOptions.NO_OVERWRITE) && listExportOptions.contains(ExportOptions.OVERWRITE)){
            throw new IllegalArgumentException("Mutually exclusive export options were provided");
        }

        String clientDir = "publish/clients/" + client.getClientName();
        String localFilePath = clientDir + '/' + path;
        File localFile = new File(localFilePath, a.getName());

        exportArtifact(a, localFilePath, exportOptions);

        if (listExportOptions.contains(ExportOptions.OVERWRITE) || ! client.fileExistsInLibertyClientRoot(path + '/' + a.getName())) { 
            Log.info(ShrinkHelper.class, "exportToClient", "Copying file " + a.getName() + " to liberty client");
            client.copyFileToLibertyClientRoot(localFilePath, path, a.getName());
        } else {
            Log.info(ShrinkHelper.class, "exportToClient", "Not copying file " + a.getName() + " to liberty client because it already exists at the destination.");
        }
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
     * Writes an application to a a file in the 'publish/clients/<client_name>/apps/' directory
     * with the file name returned by a.getName(), which should include the
     * file type extension (.ear, .war, .jar, .rar, etc)
     *
     * @param client The client to publish the application to
     * @param a The archive to export as a file
     */
    public static void exportAppToClient(LibertyClient client, Archive<?> a) throws Exception {
        exportToClient(client, "apps", a);
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
            return exportArtifact(a, dest, printArchiveContents, ExportOptions.NO_OVERWRITE);
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc).
     *
     * @param a The archive to export as a file
     * @param dest The target folder to export the archive to (i.e. publish/files/apps)
     * @param printArchiveContents Whether or not to log the contents of the archive being exported
     * @param ExportOptions options to customise the methods behaviour. Currently the two options are ExportOptions.NO_OVERWRITE and ExportOptions.OVERWRITE. The default is ExportOptions.NO_OVERWRITE
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest, boolean printArchiveContents, ExportOptions... exportOptions) {
        Log.info(c, "exportArtifact", "Exporting shrinkwrap artifact: " + a.toString() + " to " + dest);

        List<ExportOptions> listExportOptions = Arrays.asList(exportOptions);

        if (listExportOptions.contains(ExportOptions.NO_OVERWRITE) && listExportOptions.contains(ExportOptions.OVERWRITE)){
            throw new IllegalArgumentException("Mutually exclusive export options were provided");
        }

        File outputFile = new File(dest, a.getName());
        if (outputFile.exists() && ! listExportOptions.contains(ExportOptions.OVERWRITE)) {
            Log.info(ShrinkHelper.class, "exportArtifact", "Not exporting artifact because it already exists at " + outputFile.getAbsolutePath());
            return a;
        }
        outputFile.getParentFile().mkdirs();
        a.as(ZipExporter.class).exportTo(outputFile, true);
        if (printArchiveContents) {
            Log.info(ShrinkHelper.class, "exportArtifact", a.toString(true));
        }
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

    public static ResourceAdapterArchive buildDefaultRar(String rarName, String... packages) throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, rarName + ".rar")
                        .addAsLibrary(ShrinkHelper.buildJavaArchive(rarName, packages));
        ShrinkHelper.addDirectory(rar, "test-resourceadapters/" + rarName + "/resources/");
        return rar;
    }

    public static ResourceAdapterArchive defaultRar(LibertyServer server, String rarName, String... packages) throws Exception {
        ResourceAdapterArchive rar = buildDefaultRar(rarName, packages);
        ShrinkHelper.exportToServer(server, "connectors", rar);
        return rar;
    }

    public enum ExportOptions {
        NO_OVERWRITE, OVERWRITE;
    }
}
