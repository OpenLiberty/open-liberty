/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

import componenttest.topology.impl.LibertyServer;

/**
 * Helper utilities for working with the ShrinkWrap APIs.
 */
public class ShrinkHelper {

    /**
     * Export an artifact to autoFVT/publish/servers/$server.getName()/$path/$a.getName()
     * and also copy it into the currently used wlp image at the same location
     */
    public static void exportToServer(LibertyServer server, String path, Archive<?> a) throws Exception {
        String serverDir = "publish/servers/" + server.getServerName();
        exportArtifact(a, serverDir + '/' + path);
        server.copyFileToLibertyServerRoot(serverDir + "/" + path, path, a.getName());
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
    public static void exportAppToServer(LibertyServer server, Archive<?> a) throws Exception {
        exportToServer(server, "apps", a);
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
        return exportArtifact(a, dest, printArchiveContents, false);
    }

    /**
     * Writes an Archive to a a file in the target destination
     * with the file name returned by a.getName(), which should include the
     * file type extension (ear, war, jar, rar, etc).
     *
     * @param a The archive to export as a file
     * @param dest The target folder to export the archive to (i.e. publish/files/apps)
     * @param printArchiveContents Whether or not to log the contents of the archive being exported
     * @param overWrite Wheather or not to overwrite an existing artifact
     */
    public static Archive<?> exportArtifact(Archive<?> a, String dest, boolean printArchiveContents, boolean overWrite) {
        Log.info(c, "exportArtifact", "Exporting shrinkwrap artifact: " + a.toString() + " to " + dest);
        File outputFile = new File(dest, a.getName());
        if (outputFile.exists() && overWrite) {
            return a;
        }
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
     * Shortcut for: <code>System.getProperty("user.dir")
     */
    public static String getCWD() {
        return System.getProperty("user.dir");
    }
}
