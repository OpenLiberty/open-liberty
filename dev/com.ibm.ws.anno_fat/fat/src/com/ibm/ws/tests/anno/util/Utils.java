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
package com.ibm.ws.tests.anno.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.tests.anno.caching.util.AnnoCacheLocations;

import componenttest.topology.utils.FileUtils;

public class Utils {
    private static final Logger LOG = Logger.getLogger(Utils.class.getName());

    // Date formatting ...

    private static final Format DATE_FORMAT = new SimpleDateFormat("yyyy MM dd HH:mm:ss");

    public static String convertTime(long time){
        return DATE_FORMAT.format( new Date(time) );
    }

    public static void logFile(String title, File file) {
        String logText;

        if ( !file.exists() ) {
            logText = ( title + ": " +
                        "Path [ " + file.getPath() + " ] "+
                        "[ ** DOES NOT EXIST ** ]");

        } else if ( file.isDirectory() ) {
            logText = ( title + ": " +
                        "Path [ " + file.getPath() + " ] "+
                        "[ ** DIRECTORY ** ]");

        } else {
            logText = ( title + ": " +
                        "Path [ " + file.getPath() + " ] " +
                        "Size [ " + file.length() + " ] " +
                        "Last update [ " + convertTime(file.lastModified()) + " ]" );
        }

        LOG.info(logText);
    }

    // Simple server locations ...
    //
    // These use the default locations.

    public static String getInstalledAppPath(SharedServer sharedServer, String appName) {
        return sharedServer.getLibertyServer().getServerRoot() + "/apps/" + appName;
    }

    public static String getExpandedAppPath(SharedServer sharedServer, String appName) {
        return getExpandedAppsPath(sharedServer) + "/" + appName;
    } 

    public static String getExpandedAppsPath(SharedServer sharedServer) {
        return sharedServer.getLibertyServer().getServerRoot() + "/apps/expanded";
    }

    // Annotation caching utilities ...

    public static String getAnnoCacheParentPath(SharedServer sharedServer) throws IOException {
        return getAnnoCachePath(sharedServer) + "/..";
    }

    public static String getAnnoCachePath(SharedServer sharedServer) throws IOException {
        File annoCacheDir = getAnnoCacheDir(sharedServer);
        return annoCacheDir.getCanonicalPath();
    }

    public static File getAnnoCacheDir(SharedServer sharedServer) {
        String osgiWorkAreaRoot = sharedServer.getLibertyServer().getOsgiWorkAreaRoot();
        return findDirectory( new File(osgiWorkAreaRoot), AnnoCacheLocations.CACHE_NAME );
    }

    // File utilities ...

    
    // File primitives ...

    public static void transferExcept(
        File inputFile,
        String[] exceptLines,
        String[] enableOn,
        String[] disableOn,
        int[] regionSizes,
        File outputFile) throws IOException {

        LOG.info("transferExcept: ENTER");

        LOG.info("Input [ " + inputFile.getPath() + " ]");
        for ( int regionNo = 0; regionNo < exceptLines.length; regionNo++ ) {
            LOG.info("  Skip [ " + exceptLines[regionNo] + " ] Count [ " + regionSizes[regionNo] + " ]");
            LOG.info("  Enabled By [ " + enableOn[regionNo] + " ]");
            LOG.info("  Disabled By [ " + disableOn[regionNo] + " ]");
        }
        LOG.info("Output [ " + outputFile.getPath() + " ]");

        BufferedReader inputReader = null;
        BufferedWriter outputWriter = null;

        try {
            inputReader = new BufferedReader( new FileReader(inputFile) ); 
            outputWriter = new BufferedWriter( new FileWriter(outputFile) );

            // Patterns which don't have an enablement condition start enabled.

            boolean[] enabled = new boolean[exceptLines.length];
            for ( int regionNo = 0; regionNo < exceptLines.length; regionNo++ ) {
                enabled[regionNo] = ( enableOn[regionNo] == null );
            }

            String line; 
            while ( (line = inputReader.readLine()) != null ) {
                boolean ignore = false;

                String trimLine = line.trim();
                
                // For each pattern:
                //     If the pattern is enabled, either disable it,
                //     or apply it.
                //     If the pattern is disabled, possibly enable it.

                for ( int regionNo = 0; regionNo < exceptLines.length; regionNo++ ) {
                    if ( enabled[regionNo] ) {
                        if ( (disableOn[regionNo] != null) && trimLine.equals(disableOn[regionNo]) ) {
                            LOG.info("Disable [ " + regionNo + " ]");
                            enabled[regionNo] = false;

                        } else if ( trimLine.equals(exceptLines[regionNo]) ) {
                            LOG.info("Skip [ " + line + " ]");

                            int regionSize = regionSizes[regionNo];
                            for ( int ignoreNo = 1; ignoreNo < regionSize; ignoreNo++ ) {
                                LOG.info("Skip [ " + inputReader.readLine() + " ]");
                            }

                            ignore = true;
                            break;
                        }

                    } else {
                        if ( (enableOn[regionNo] != null) && trimLine.equals(enableOn[regionNo]) ) {
                            LOG.info("Enable [ " + regionNo + " ]");
                            enabled[regionNo] = true;
                        }
                    }
                }

                if ( !ignore ) {
                    LOG.info("Keep [ " + line + " ]");
                    outputWriter.write(line);
                    outputWriter.write("\n");
                }
            }

        } catch ( IOException e ) {
            LOG.info( e.getMessage() );
            throw e;

        } finally {
            if ( outputWriter != null ) {
                try {
                    outputWriter.close();
                } catch ( IOException e ) {
                    LOG.info( e.getMessage() );
                    throw e;
                }
            }

            if ( inputReader != null ) {
                try {
                    inputReader.close();
                } catch ( IOException e ) {
                    LOG.info( e.getMessage() );
                    throw e;
                }
            }
        }

        LOG.info("transferExcept: RETURN");
    }

    /**
     * Recursively search for a named simple file.
     *
     * Ignore any matches to directories: Only match simple files.
     *
     * @param root The root file which is to be tested.
     * @param name The name of the target file.
     *
     * @return The first simple file having the specified name null
     *     if no matching simple file is located.
     */
    public static File findFile(File root, String name) {
        if ( root.isDirectory() ) {
            File[] children = root.listFiles();
            for ( File child : children ) {
                File found = findFile(child, name);
                if ( found != null ) {
                    return found; // Located as a child.
                }
            }
            return null; // Not located as a child.  Do *NOT* test against the directory itself.

        } else {
            if ( root.getName().equals(name) ) {
                return root; // Located as the root.
            } else {
                return null; // The root is a simple file, but doesn't have the target name.
            }
        }
        
    }

    /**
     * Recursively search for a named directory file.
     *
     * Ignore any matches to simple files: Only match directories.
     *
     * @param root The root file which is to be tested.
     * @param name The name of the target file.
     *
     * @return The first directory having the specified name null
     *     if no matching directory is located.
     */
    public static File findDirectory(File root, String name) {
        if ( !root.isDirectory() ) {
            // Not found: Do not match against simple files, and
            // there are not children to search. 
            return null;
        }

        if ( root.getName().equals(name) ) {
            return root; // Located as the current root.
        }

        File[] children = root.listFiles();
        for ( File child : children ) {
            File found = findDirectory(child, name);
            if ( found != null ) {
                return found; // Located as a child.
            }
        }
        return null; // Not located as a child.
    }

    /**
     * Attempt to create a target location as a directory.
     * 
     * Fail if the target location already exists and is not a directory.
     * 
     * Do nothing if the target location already exists and is a directory.
     * 
     * Otherwise, attempt to create the target location and all parents as
     * directories.
     * 
     * @param dirPath The path to create as a directory.
     *
     * @throws IOException Thrown if the target location could not be
     *     created as a directory.
     */
    public static void mkDirs(String dirPath) throws IOException {
        LOG.info("mkDirs: " + dirPath);

        Path path = Paths.get(dirPath);
        if ( Files.exists(path) ) {
            if ( Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ) {
                LOG.info("mkdir: Already exists: " + dirPath);
                return;
            } else {
                throw new IOException("Already exists as a simple file " + dirPath);
            }
        }

        Files.createDirectories(path); // throws IOException

        if ( !Files.exists(path) ) {
            throw new IOException("Failed to create " + dirPath);
        }
    }

    public static void rename(File oldFile, File newFile) throws IOException {
        String oldPath = oldFile.getAbsolutePath();
        String newPath = newFile.getAbsolutePath();

        LOG.info("========================================");
        LOG.info("rename: Old: " + oldPath);
        LOG.info("rename: New: " + newPath);

        if ( newFile.exists() ) {
            LOG.info("rename: Delete new: " + newPath);

            newFile.delete();
            if ( newFile.exists() ) {
                throw new IOException("Failed to delete " + newPath);
            } else {
                LOG.info("rename: Deleted");
            }
        } else {
            LOG.info("rename: New does not exist: " + newPath);
        }

        String failureMsg;
        if ( !oldFile.renameTo(newFile) ) {
            failureMsg = "Failed to rename " + oldPath + " to " + newPath;
        } else if ( oldFile.exists() ) {
            failureMsg = "Old " + oldPath + " exists after rename to " + newPath;
        } else if ( !newFile.exists() ) {
            failureMsg = "New " + newPath + " does not exists after rename from " + oldPath;
        } else {
            failureMsg = null;
        }

        if ( failureMsg != null ) {
            LOG.info("rename: Failed: " + failureMsg);
            throw new IOException(failureMsg);
        } else {
            LOG.info("rename: Success");
        }

        LOG.info("========================================");
    }

    public static void unzip(File sourceArchive, String targetPath) throws IOException {
        String sourceAbsPath = sourceArchive.getAbsolutePath();

        File targetFile = new File(targetPath);
        String targetAbsPath = targetFile.getAbsolutePath();

        LOG.info("unzip: " + sourceAbsPath + " to " + targetAbsPath);

        InputStream inputStream = new FileInputStream(sourceArchive);
        ZipInputStream zipInputStream = null;

        long entriesWritten = 0L;

        try {
            zipInputStream = new ZipInputStream(inputStream);

            byte[] buffer = new byte[16 * 1024];

            ZipEntry zipEntry;
            while ( (zipEntry = zipInputStream.getNextEntry()) != null ) {
                String entryName = zipEntry.getName();

                if ( zipEntry.isDirectory() ) {
                    LOG.info("unzip: Skip directory entry: " + entryName);
                    continue;
                }

                LOG.info("unzip: Extract non-directory entry: " + entryName);

                long bytesWritten = 0L;

                File entryFile = new File(targetFile, entryName);
                OutputStream entryOutputStream = new FileOutputStream(entryFile);
                try {
                    int bytesRead;
                    while ( (bytesRead = zipInputStream.read(buffer)) != -1 ) {
                        entryOutputStream.write(buffer, 0, bytesRead);
                        bytesWritten += bytesRead;
                    }
                
                } finally {
                    entryOutputStream.close();
                }

                LOG.info("unzip: Extracted: " + entryName + ": bytes: " + bytesWritten);
                entriesWritten++;
            }

        } finally {
            if ( zipInputStream != null ) {
                zipInputStream.close();
            } else {
                inputStream.close();
            }

            LOG.info("unzip: " + sourceAbsPath + " to " + targetAbsPath + ": Entries: " + entriesWritten);
        }
    }

    public static void zip(File sourceFile, ZipOutputStream zipOutputStream) throws IOException {
        String sourceAbsPath = sourceFile.getAbsolutePath();
        LOG.info("zip: ENTER: " + sourceAbsPath);

        byte[] transferBuffer = new byte[32 * 1024];

        StringBuilder entryPath = new StringBuilder();
        int entriesAdded = zip(sourceFile, entryPath, zipOutputStream, transferBuffer);

        LOG.info("zip: RETURN: " + sourceAbsPath + ": Added entries: " + entriesAdded);
    }

    /**
     * Zip files starting with a specified root file.
     * 
     * Add directory entries, except for the root file.
     * 
     * Ignore hidden files.
     * 
     * Answer the count of entries added, including simple file entries and directory entries.
     *
     * @param sourceFile The file to add to the zip file.
     * @param sourceRelativePath The relative path to the current file.
     * @param zipOutputStream The output stream which will receive the files.
     * @param transferBuffer Buffer used to transfer data into the zip file.
     *
     * @return The count of entries added to the zip file.
     *
     * @throws IOException
     */
    private static int zip(
        File sourceFile, StringBuilder sourceRelativePath,
        ZipOutputStream zipOutputStream,
        byte[] transferBuffer) throws IOException {

        String sourceName = sourceFile.getName();

        if ( sourceFile.isHidden() ) {
            LOG.info("zip: Ignore hidden: " + sourceName); 
            return 0;

        } else if ( sourceFile.isDirectory() ) {
            int entriesWritten = 0;

            if ( sourceRelativePath.length() != 0 ) {
                LOG.info("zip: Add entry for non-root directory: " + sourceName); 
                zipOutputStream.putNextEntry( new ZipEntry( sourceRelativePath.toString() ) );
                zipOutputStream.closeEntry();
                entriesWritten++;
            } else {
                LOG.info("zip: Do not add entry for root directory: " + sourceName);
            }

            int entryPathLength = sourceRelativePath.length();

            File[] sourceChildren = sourceFile.listFiles();
            for ( File sourceChild : sourceChildren ) {
                sourceRelativePath.append(sourceChild.getName());
                sourceRelativePath.append('/');

                entriesWritten += zip(sourceChild, sourceRelativePath, zipOutputStream, transferBuffer);

                sourceRelativePath.setLength(entryPathLength);
            }

            LOG.info("zip: Directory: " + sourceName + ": Wrote: " + entriesWritten);
            return entriesWritten;

        } else {
            LOG.info("zip: Add simple file: " + sourceName); 

            long totalWritten = 0L;

            InputStream sourceStream = new FileInputStream(sourceFile);

            try {
                ZipEntry zipEntry = new ZipEntry( sourceRelativePath.toString() );
                zipOutputStream.putNextEntry(zipEntry);

                int bytesRead;
                while ( (bytesRead = sourceStream.read(transferBuffer)) != -1 ) {
                    zipOutputStream.write(transferBuffer, 0, bytesRead);
                    totalWritten += bytesRead;
                }
            } finally {
                sourceStream.close();
            }

            LOG.info("zip: Simple file: " + sourceName + ": Wrote: " + totalWritten);
            return 1;
        }
    }

    // Application management ...

    /**
     * Replace the installed copy of an application with a collapsed copy of the
     * expanded application.
     * 
     * This is used to perform updates to installed applications: Files are updated
     * in the expanded application, then the installed application is replaced with
     * a collapsed copy of the updated expanded application.
     *
     * This method relies on default locations ("apps" and "apps/expanded") for
     * installed and expanded applications.
     *
     * @param sharedServer The server on which to perform the application replacement.
     * @param appName The name of the application which is to be replaced.
     *
     * @throws IOException Thrown if the replacement fails.
     */
    public static void replaceApplicationFromExpanded(SharedServer sharedServer, String appName) throws IOException {
        LOG.info("replaceApplicationFromExpanded: ENTER: " + appName);

        String expandedAppPath = Utils.getExpandedAppPath(sharedServer, appName);
        File expandedApp = new File(expandedAppPath);
        LOG.info("replaceApplicationFromExpanded: Expanded path: " + expandedApp.getAbsolutePath());

        String installedAppPath = getInstalledAppPath(sharedServer, appName);
        File installedApp = new File(installedAppPath);
        LOG.info("replaceApplicationFromExpanded: Installed path: " + installedApp.getAbsolutePath());

        File[] children = expandedApp.listFiles();
        for ( File child : children ) {
            String childName = child.getName();
            if ( !child.getName().toUpperCase().endsWith(".WAR") ) {
                continue;
            } else if ( !child.isDirectory() ) {
                continue;
            }

            LOG.info("replaceApplicationFromExpanded: WAR: " + childName);
            collapse(expandedAppPath, childName);
        }

        OutputStream outputStream = new FileOutputStream(installedApp);
        ZipOutputStream zipOutputStream = null;

        try {
            zipOutputStream = new ZipOutputStream(outputStream);
            zip(expandedApp, zipOutputStream);

        } finally {
            if ( zipOutputStream != null ) {
                zipOutputStream.close();
            } else {
                outputStream.close();
            }
        }
        
        LOG.info("replaceApplicationFromExpanded: RETURN: " + appName);
    }

    /**
     * Replace a web descriptor with an alternate descriptor.
     * 
     * Perform the replacement in the expanded copy of a target application,
     * that is, in the copy which is under "apps/expanded".
     *
     * The alternate descriptor must be specified in a location relative to
     * the folder containing the web descriptor.  Usually, that means placing
     * the alternate descriptor in the same folder as the web descriptor.
     * 
     * @param sharedServer The server in which to perform the replacement.
     * @param appName The name of the application in which to perform the
     *     replacement.
     * @param warName The name of the web module in which to perform the
     *     replacement.
     * @param altWebName The relative path of the alternate web descriptor.
     *
     * @throws Exception Thrown if the replacement fails.
     */
    public static void replaceWebXml(
        SharedServer sharedServer,
        String appName, String warName,
        String altWebName) throws Exception {

        LOG.info("replaceWebXml: ENTER: " + appName);

        String webInfDirName =
            sharedServer.getLibertyServer().getServerRoot() +
            "/apps/expanded/" +
            appName + "/" + warName + "/WEB-INF/";

        File webXml = new File(webInfDirName + "web.xml");
        File altWebXml = new File(webInfDirName + altWebName);

        LOG.info("replaceWebXml: Source: " + altWebXml.getAbsolutePath());
        LOG.info("replaceWebXml: Target: " + webXml.getAbsolutePath());

        FileUtils.copyFile(altWebXml, webXml);

        LOG.info("replaceWebXml: RETURN: " + appName);
    }

    /**
     * Collapse a directory.  Give the collapsed directory the same name
     * as the original directory.
     *
     * The directory is renamed, then the renamed directory is directly
     * onto the original location, then the renamed directory is deleted.
     *
     * @param parentPath The path to the parent of the target directory.
     * @param childName The name of the target directory.
     *
     * @throws IOException Thrown in case of a failure.
     */
    public static void collapse(String parentPath, String childName) throws IOException {
        String childPath = parentPath + '/' + childName;
        File childFile = new File(childPath);
        String childAbsPath = childFile.getAbsolutePath();

        LOG.info("collapse: ENTER: Path: " + childAbsPath);

        String stagingPath = childPath + ".temp";
        File stagingFile = new File(stagingPath);
        String stagingAbsPath = stagingFile.getAbsolutePath();
        LOG.info("collapse: Staging path: " + stagingAbsPath);

        LOG.info("collapse: Shift to staging");
        rename(childFile, stagingFile);

        LOG.info("collapse: Collapse from staging");

        OutputStream output = new FileOutputStream(childFile);
        ZipOutputStream zipOutput = null;

        try {
            zipOutput = new ZipOutputStream(output);

            zip(stagingFile, zipOutput);

        } finally {
            if ( zipOutput != null ) {
                zipOutput.close();
            } else {
                output.close();
            }
        }

        LOG.info("collapse: Delete staging:");
        FileUtils.recursiveDelete(stagingFile);

        LOG.info("collapse: RETURN: " + childAbsPath);
    }

    /**
     * Copy a file from the applications folder ("apps") to a backup folder
     * placed as a peer to the annotation caching folder.
     *
     * Create the backup folder if necessary.
     *
     * @param sharedServer The server containing the application which is to be
     *     copied.
     * @param earName The name of the EAR which is to be copied.
     *
     * @throws IOException Thrown if the EAR could not be copied.
     */
    public static void backupApplication(SharedServer sharedServer, String earName) throws IOException {
        LOG.info("backupApplication: ENTER: EAR: " + earName);

        String earPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + earName;
        File sourceEar = new File(earPath);

        String annoCacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
        String backupPath = annoCacheParentPath + "/annoCacheBackup/apps/";

        String backupEarName = backupPath + earName;
        File backupEar = new File(backupEarName);

        LOG.info("backupApplication: Source: " + sourceEar.getAbsolutePath());
        LOG.info("backupApplication: Target: " + backupEar.getAbsolutePath());

        mkDirs(backupPath);

        FileUtils.copyFile(sourceEar, backupEar);

        LOG.info("backupApplication: RETURN: EAR: " + earName);
    }

    /**
     * Delete an application.
     * 
     * The application is located in the server applications folder, "apps".
     * 
     * @param sharedServer The server from which to delete the application.
     * @param earName The name of the application which is to be deleted.
     * 
     * @throws IOException Thrown if the application cannot be deleted.
     */
    public static void deleteApplication(SharedServer sharedServer, String earName) throws IOException { 
        String earPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + earName;
        File earFile = new File(earPath);
        LOG.info("deleteApplication: " + earFile.getAbsolutePath());

        earFile.delete();

        if ( earFile.exists() ) {
            throw new IOException("Failed to delete " + earFile.getAbsolutePath());
        }
    }

    //

    public static void installServerXml(SharedServer sharedServer, String sourceName) throws Exception {
        LOG.info("installServerXml ENTER: " + sourceName);

        String sourcePath = "publish/servers/annoFat_server/config/" + sourceName;
        File sourceFile = new File(sourcePath);

        String destPath = sharedServer.getLibertyServer().getServerRoot() + "/server.xml";
        File destFile = new File(destPath);

        if ( destFile.exists() ) {
            destFile.delete();
        }

        FileUtils.copyFile(sourceFile, destFile);
        
        LOG.info("installServerXml RETURN: " + sourceName);
    }

    public static void installJvmOptions(SharedServer sharedServer, String sourceName) throws Exception {
        LOG.info("installJvmOptions ENTER: " + sourceName);

        String sourcePath = "publish/servers/annoFat_server/config/" + sourceName;
        File sourceFile = new File(sourcePath);

        String destPath = sharedServer.getLibertyServer().getServerRoot() + "/jvm.options";
        File destFile = new File(destPath);

        if ( destFile.exists() ) {
            destFile.delete();
        }

        FileUtils.copyFile(sourceFile, destFile);

        LOG.info("installJvmOptions RETURN: " + sourceName);
    }

    public static void display(SharedServer sharedServer, String description, String path) throws Exception {
        String fullPath = sharedServer.getLibertyServer().getServerRoot() + "/" + path;
        File targetFile = new File(fullPath);

        display(description, targetFile);
    }

    public static void display(String description, File file) throws Exception {
        logFile(description, file);

        if ( !file.exists() || !file.isFile() ) {
            return;
        }

        logLines(file);
    }

    public static void logLines(File targetFile) throws Exception {
        Reader reader = new FileReader(targetFile);
        BufferedReader targetsReader = null;
        try {
            targetsReader = new BufferedReader(reader);

            String line; 
            while ( (line = targetsReader.readLine()) != null ) {
                LOG.info(line);
            } 

        } finally {
            if ( targetsReader != null ) {
                targetsReader.close();
            } else {
                reader.close();
            }
        }
    }
    

    public static void replaceOnServer(
        SharedServer sharedServer,
        String commonPath,
        String targetName, String replacementName) throws Exception {

        LOG.info("replaceOnServer: ENTER");

        String commonServerPath = sharedServer.getLibertyServer().getServerRoot() + commonPath;

        File targetPath = new File(commonServerPath + targetName);
        File replacementPath = new File(commonServerPath + replacementName);

        LOG.info("replaceOnServer: Target: " + targetPath.getAbsolutePath() +
                " replacement: " + replacementPath.getAbsolutePath());

        FileUtils.copyFile(replacementPath, targetPath);

        LOG.info("replaceOnServer: RETURN");
    }
}
