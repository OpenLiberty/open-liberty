/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyClient;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Helper utilities for working with the ShrinkWrap APIs.
 */
public class ShrinkHelper {

    private static final Set<File> exportedArchives = new HashSet<>();

    public static void cleanAllExportedArchives() {
        for (File exportedArchive : exportedArchives) {
            Log.info(ShrinkHelper.class, "cleanAllExportedArchives", "Deleting archive at: " + exportedArchive.getAbsolutePath());
            exportedArchive.delete();
        }
        exportedArchives.clear();
    }

    public static enum DeployOptions {
        /**
         * Don't validate app startup
         */
        DISABLE_VALIDATION,
        /**
         * Overwrite the file if it already exists
         */
        OVERWRITE,
        /**
         * Deploy only to the liberty server/client
         * <p>
         * Default is to deploy to both the liberty server/client and the publish directory
         */
        SERVER_ONLY
    }

    protected static final Class<?> c = ShrinkHelper.class;

    /**
     * Export an artifact to servers/$server.getName()/$path/$a.getName() under two directories:
     * autoFVT/publish/... and wlp/usr/...
     */
    public static void exportToServer(LibertyServer server, String path, Archive<?> a, DeployOptions... options) throws Exception {
        String localLocation;
        if (serverOnly(options)) {
            localLocation = getTmpLocation(a);
        } else {
            localLocation = "publish/servers/" + server.getServerName() + "/" + path;
        }
        exportArtifact(a, localLocation, true, shouldOverwrite(options));
        server.copyFileToLibertyServerRoot(localLocation, path, a.getName());
    }

    /**
     * Export an artifact to clients/$client.getName()/$path/$a.getName() under two directories:
     * autoFVT/publish/... and wlp/usr/...
     */
    public static void exportToClient(LibertyClient client, String path, Archive<?> a, DeployOptions... options) throws Exception {
        String localLocation;
        if (serverOnly(options)) {
            localLocation = getTmpLocation(a);
        } else {
            localLocation = "publish/clients/" + client.getClientName() + "/" + path;
        }
        exportArtifact(a, localLocation, true, shouldOverwrite(options));
        client.copyFileToLibertyClientRoot(localLocation, path, a.getName());
    }

    /**
     * Export an artifact to wlp/usr/extension/lib and autoFVT/publish/extension/lib
     */
    public static void exportUserFeatureArchive(LibertyServer server, Archive<?> a, DeployOptions... options) throws Exception {
        String localLocation = "usr/extension/lib";
        exportArtifact(a, "lib/LibertyFATTestFiles", true, shouldOverwrite(options));
        server.copyFileToLibertyInstallRoot(localLocation, a.getName());
    }

    private static String getTmpLocation(Archive<?> a) {
        String location = "publish/shrinkApps/" + a.getName() + "-" + System.nanoTime();

        File file = new File(location);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Failed to create tmp directory: " + location);
            }
        } else if (!file.isDirectory()) {
            throw new RuntimeException("Tmp directory already exists but is not a directory: " + location);
        }

        return location;
    }

    private static boolean shouldOverwrite(DeployOptions[] options) {
        return Arrays.asList(options).contains(DeployOptions.OVERWRITE);
    }

    private static boolean serverOnly(DeployOptions[] options) {
        return Arrays.asList(options).contains(DeployOptions.SERVER_ONLY);
    }

    private static boolean shouldValidate(DeployOptions[] options) {
        return !Arrays.asList(options).contains(DeployOptions.DISABLE_VALIDATION);
    }

    /**
     * Writes an application to a a file in the 'publish/servers/<server_name>/apps/' directory
     * with the file name returned by a.getName(), which should include the
     * file type extension (.ear, .war, .jar, .rar, etc)
     *
     * @param server  The server to publish the application to
     * @param a       The archive to export as a file
     * @param options The deployment options
     */
    public static void exportAppToServer(LibertyServer server, Archive<?> a, DeployOptions... options) throws Exception {
        exportToServer(server, "apps", a, options);

        if (shouldValidate(options)) {
            LibertyServerFactory.addAppsToVerificationList(a.getName(), server);
        }
    }

    /**
     * Writes an application to a a file in the 'publish/clients/<client_name>/apps/' directory
     * with the file name returned by a.getName(), which should include the
     * file type extension (.ear, .war, .jar, .rar, etc)
     *
     * @param client  The client to publish the application to
     * @param a       The archive to export as a file
     * @param options The deployment options
     */
    public static void exportAppToClient(LibertyClient client, Archive<?> a, DeployOptions... options) throws Exception {
        exportToClient(client, "apps", a, options);
    }

    /**
     * Writes an Archive to a a file in the 'publish/servers/<server_name>' directory
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc)
     *
     * The application is added to the list to be validated when the server starts. If the server is already started then this
     * method will wait for the application to be started.
     *
     *
     * @param  server    The server to publish the application to
     * @param  a         The archive to export as a file
     * @param  options   The deployment options
     * @throws Exception
     */
    public static void exportDropinAppToServer(LibertyServer server, Archive<?> a, DeployOptions... options) throws Exception {
        exportToServer(server, "dropins", a, options);

        String appName = a.getName();
        if (shouldValidate(options)) {
            String installedAppName = (appName.endsWith(".war") || appName.endsWith(".ear")) ? appName.substring(0, appName.length() - 4) : appName;
            server.addInstalledAppForValidation(installedAppName);
        }
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc)
     *
     * @param a    The archive to export as a file
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
     * @param a                    The archive to export as a file
     * @param dest                 The target folder to export the archive to (i.e. publish/files/apps)
     * @param printArchiveContents Whether or not to log the contents of the archive being exported
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest, boolean printArchiveContents) {
        return exportArtifact(a, dest, printArchiveContents, false);
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc).
     *
     * @param a                    The archive to export as a file
     * @param dest                 The target folder to export the archive to (i.e. publish/files/apps)
     * @param printArchiveContents Whether or not to log the contents of the archive being exported
     * @param overWrite            Wheather or not to overwrite an existing artifact
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest, boolean printArchiveContents, boolean overWrite) {
        return exportArtifact(a, dest, printArchiveContents, overWrite, false);
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc).
     *
     * @param a                    The archive to export as a file
     * @param dest                 The target folder to export the archive to (i.e. publish/files/apps)
     * @param printArchiveContents Whether or not to log the contents of the archive being exported
     * @param overWrite            Whether or not to overwrite an existing artifact
     * @param expand               Whether or not to explode the archive at the destination
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest, boolean printArchiveContents, boolean overWrite, boolean expand) {
        Log.info(c, "exportArtifact", "Exporting shrinkwrap artifact: " + a.toString() + " to " + dest);
        File outputFile = new File(dest, a.getName());
        exportedArchives.add(outputFile);
        if (outputFile.exists() && !overWrite) {
            Log.info(ShrinkHelper.class, "exportArtifact", "Not exporting artifact because it already exists at " + outputFile.getAbsolutePath());
            if (JakartaEE9Action.isActive()) {
                JakartaEE9Action.transformApp(outputFile.toPath());
            } else if (JakartaEE10Action.isActive()) {
                JakartaEE10Action.transformApp(outputFile.toPath());
            }
            return a;
        }
        outputFile.getParentFile().mkdirs();
        if (expand) {
            a.as(ExplodedExporter.class).exportExploded(outputFile.getParentFile(), outputFile.getName());
        } else {
            a.as(ZipExporter.class).exportTo(outputFile, true);
        }
        if (printArchiveContents)
            Log.info(ShrinkHelper.class, "exportArtifact", a.toString(true));
        if (JakartaEE9Action.isActive()) {
            JakartaEE9Action.transformApp(outputFile.toPath());
        } else if (JakartaEE10Action.isActive()) {
            JakartaEE10Action.transformApp(outputFile.toPath());
        }
        return a;
    }

    /**
     * Write out an asset as a file to the specified destination directory (relative to autoFVT)
     *
     * @param  name                 name of the file to create (must include extension)
     * @param  a                    asset to write (usually StringAsset)
     * @param  dest                 location (relative to autoFVT)
     * @param  printArchiveContents whether or not to log the contents of the archive being exported
     * @param  overWrite            whether or not to overwrite an existing file with the same name/dest
     * @return                      the Asset, a
     * @throws IOException
     */
    public static Asset exportStandaloneAsset(String name, Asset a, String dest, boolean printArchiveContents, boolean overWrite) throws IOException {
        File outputFile = new File(dest, name);
        if (outputFile.exists() && !overWrite) {
            Log.info(ShrinkHelper.class, "exportStandaloneAsset", "Not exporting asset because it already exists at " + outputFile.getAbsolutePath());
            return a;
        }
        StringBuilder sb = new StringBuilder();
        try (InputStream is = a.openStream(); FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] b = new byte[2048];
            int bytesRead;
            while ((bytesRead = is.read(b)) > 0) {
                sb.append(new String(b));
                fos.write(b);
            }
        }
        if (printArchiveContents) {
            Log.info(ShrinkHelper.class, "exportStandaloneAsset", name + ":" + System.lineSeparator() + sb.toString());
        }
        return a;
    }

    /**
     * Recursively adds a folder and all of its contents to an archive.
     *
     * @param a   The archive to add the files to
     * @param dir The directory which will be recursively added to the archive.
     */
    public static Archive<?> addDirectory(Archive<?> a, String dir) throws Exception {
        return addDirectory(a, dir, Filters.includeAll());
    }

    /**
     * Recursively adds a folder and all of its contents matching a filter to an archive.
     *
     * @param a      The archive to add the files to
     * @param dir    The directory which will be recursively added to the archive.
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
     * @param  appName  The name of the application. The '.war' file extension is assumed
     * @param  packages A list of java packages to add to the application.
     * @return          a WebArchive representing the application created
     */
    public static WebArchive buildDefaultApp(String appName, String... packages) throws Exception {
        return buildDefaultAppFromPath(appName, null, packages);
    }

    /**
     * Builds a WebArchive (WAR) with the default format, which assumes all resources are at:
     * '$appPath/test-applications/$appName/resources/`
     *
     * @param  appName  The name of the application. The '.war' file extension is assumed
     * @param  appPath  Absolute path where the appropriate test-applications directory exists.
     * @param  packages A list of java packages to add to the application.
     * @return          a WebArchive representing the application created
     */
    public static WebArchive buildDefaultAppFromPath(String appName, String appPath, String... packages) throws Exception {
        String appArchiveName = appName.endsWith(".war") ? appName : appName + ".war";
        WebArchive app = ShrinkWrap.create(WebArchive.class, appArchiveName);
        for (String p : packages) {
            if (p.endsWith(".*"))
                app = app.addPackages(true, p.replace(".*", ""));
            else
                app = app.addPackages(false, p);
        }
        String testAppResourcesDir = (appPath == null ? "" : appPath) + "test-applications/" + appName + "/resources/";
        if (new File(testAppResourcesDir).exists()) {
            app = (WebArchive) addDirectory(app, testAppResourcesDir);
        }
        return app;
    }

    /**
     * Builds a WebArchive (WAR) with the default format, does not add resources directory
     *
     * @param  appName  The name of the application. The '.war' file extension is assumed
     * @param  appPath  Absolute path where the appropriate test-applications directory exists.
     * @param  packages A list of java packages to add to the application.
     * @return          a WebArchive representing the application created
     */
    public static WebArchive buildDefaultAppFromPathNoResources(String appName, String appPath, String... packages) throws Exception {
        String appArchiveName = appName.endsWith(".war") ? appName : appName + ".war";
        WebArchive app = ShrinkWrap.create(WebArchive.class, appArchiveName);
        for (String p : packages) {
            if (p.endsWith(".*"))
                app = app.addPackages(true, p.replace(".*", ""));
            else
                app = app.addPackages(false, p);
        }
        return app;
    }

    /**
     * Builds a JavaArchive (JAR) with the default format, which assumes all resources are at:
     * 'test-applications/$appName/resources/`
     *
     * @param  name     The name of the jar. The '.jar' file extension is assumed
     * @param  packages A list of java packages to add to the application.
     * @return          a JavaArchive representing the JAR created
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
     * Builds a JavaArchive (JAR) with the default format, which assumes all resources are at:
     * 'test-applications/$appName/resources/`, and adds only classes accepted by the filter
     * in the specified package(s).
     *
     * @param  name     The name of the jar. The '.jar' file extension is assumed
     * @param  filter   A filter for classes of the specified package(s)
     * @param  packages A list of java packages to add to the application.
     * @return          a JavaArchive representing the JAR created
     */
    public static JavaArchive buildJavaArchive(String name, Filter<ArchivePath> filter, String... packages) throws Exception {
        String archiveName = name.endsWith(".jar") ? name : name + ".jar";
        JavaArchive app = ShrinkWrap.create(JavaArchive.class, archiveName);
        for (String p : packages) {
            if (p.endsWith(".*"))
                app = app.addPackages(true, filter, p.replace(".*", ""));
            else
                app = app.addPackages(false, filter, p);
        }
        if (new File("test-applications/" + name + "/resources/").exists())
            app = (JavaArchive) addDirectory(app, "test-applications/" + name + "/resources/");
        return app;
    }

    /**
     * Builds a JavaArchive (JAR) with the default format, does not add resources directory
     *
     * @param  name     The name of the jar. The '.jar' file extension is assumed
     * @param  packages A list of java packages to add to the application.
     * @return          a JavaArchive representing the JAR created
     */
    public static JavaArchive buildJavaArchiveNoResources(String name, String... packages) throws Exception {
        String archiveName = name.endsWith(".jar") ? name : name + ".jar";
        JavaArchive app = ShrinkWrap.create(JavaArchive.class, archiveName);
        for (String p : packages) {
            if (p.endsWith(".*"))
                app = app.addPackages(true, p.replace(".*", ""));
            else
                app = app.addPackages(false, p);
        }
        return app;
    }

    /**
     * Invokes {@link #buildDefaultApp(String, String...)}
     * and then exports the resulting application to a Liberty server under the "dropins" directory
     *
     * @param server   The server to export the application to
     * @param appname  The name of the application
     * @param packages A list of java packages to add to the application.
     */
    public static WebArchive defaultDropinApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = buildDefaultApp(appName, packages);
        exportDropinAppToServer(server, app);

        return app;
    }

    /**
     * Invokes {@link #buildDefaultApp(String, String...)}
     * and then exports the resulting application to a Liberty server under the "dropins" directory
     *
     * @param server        The server to export the application to
     * @param appname       The name of the application
     * @param deployOptions options to configure how the application is deployed
     * @param packages      A list of java packages to add to the application.
     */
    public static WebArchive defaultDropinApp(LibertyServer server, String appName, DeployOptions[] deployOptions, String... packages) throws Exception {
        WebArchive app = buildDefaultApp(appName, packages);
        exportDropinAppToServer(server, app, deployOptions);

        return app;
    }

    /**
     * Invokes {@link #buildDefaultApp(String, String...)}
     * and then exports the resulting application to a Liberty server under the "apps" directory
     *
     * @param server        The server to export the application to
     * @param appName       The name of the application
     * @param deployOptions options to configure how the application is deployed
     * @param packages      A list of java packages to add to the application.
     */
    public static WebArchive defaultApp(LibertyServer server, String appName, DeployOptions[] deployOptions, String... packages) throws Exception {
        WebArchive app = buildDefaultApp(appName, packages);
        exportAppToServer(server, app, deployOptions);
        return app;
    }

    /**
     * Invokes {@link #buildDefaultApp(String, String...)}
     * and then exports the resulting application to a Liberty server under the "apps" directory
     *
     * @param server   The server to export the application to
     * @param appName  The name of the application
     * @param packages A list of java packages to add to the application.
     */
    public static WebArchive defaultApp(LibertyServer server, String appName, String... packages) throws Exception {
        WebArchive app = defaultApp(server, appName, new DeployOptions[0], packages);
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

    /**
     * Builds a JAR file based on the the specified packages and test-applications/$userFeatureJarName
     * and exports it to autoFVT/extension/lib directory and the appropriate wlp/usr/extension/lib
     * directory on the server.
     */
    public static JavaArchive defaultUserFeatureArchive(LibertyServer server, String userFeatureJarName, String... packages) throws Exception {
        JavaArchive jar = buildJavaArchive(userFeatureJarName, packages);
        exportUserFeatureArchive(server, jar, DeployOptions.OVERWRITE);
        return jar;
    }

}
