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
package com.ibm.ws.tests.anno.caching;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.tests.anno.caching.util.AnnoCacheLocations;
import com.ibm.ws.tests.anno.util.AppPackagingHelper;
import com.ibm.ws.tests.anno.util.Ear;
import com.ibm.ws.tests.anno.util.Jar;
import com.ibm.ws.tests.anno.util.Utils;
import com.ibm.ws.tests.anno.util.War;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FileUtils;

/**
 * Super class for all caching tests.
 */
public abstract class AnnoCachingTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(AnnoCachingTest.class.getName());

    protected static void info(String msg) {
        LOG.info(msg);
    }

    protected static void logBlock(String msg) {
        int msgLen = msg.length();
        StringBuilder blockChars = new StringBuilder(msgLen + 6);
        for ( int charNo = 0; charNo < msgLen + 6; charNo++ ) {
            blockChars.append("=");
        }
        String banner = blockChars.toString();

        LOG.info("");
        LOG.info(banner);
        LOG.info("=  " + msg + "  =");
        LOG.info(banner);
    }

    protected static long nsToMs(long ns) {
        return TimeUnit.NANOSECONDS.toMillis(ns);
    }

    protected static String formatNs(long ns) {
        return format( nsToMs(ns) );
    }

    protected static String format(long value) {
        return String.format("%12d", Long.valueOf(value));
    }
    
    protected static String format(float value) {
        return String.format("%12.2f", Float.valueOf(value));
    }

    // Test application ...

    public static String earFileName = null;

    public static String getEarName() throws Exception {
        if ( earFileName == null ) {
            throw new Exception("EAR file name not initialized");
        }
        return earFileName;
    }

    public static void setEarName(String earName) {
        earFileName = earName;
    }

    public static String getApplicationPath() throws Exception {
        return getAppsPath() + getEarName();
    }

    public static File getApplicationFile() throws Exception {
        return new File( getApplicationPath() );
    }

    public static File getInstalledAppFile() throws Exception {
        return getApplicationFile();
    } 

    public static String getExpandedApplicationPath() throws Exception {
        return getExpandedAppsPath() + getEarName();
    }

    public static File getExpandedApplicationFile() throws Exception {
        return new File( getExpandedApplicationPath() );
    }

    public static Ear createApp() throws Exception {
        // This jar put the servlets and listeners in the same package.
        Jar jar1 = new Jar("TestServlet40.jar");
        jar1.addPackageName("testservlet40.jar.servlets");
        jar1.addPackageName("testservlet40.jar.util");

        Jar jar2 = new Jar("TestServletA.jar");
        jar2.addPackageName("testservleta.jar.servlets");
        jar2.addPackageName("testservleta.jar.listeners");
        jar2.addPackageName("testservleta.jar.util");

        Jar jar3 = new Jar("TestServletB.jar");
        jar3.addPackageName("testservletb.jar.servlets");
        jar3.addPackageName("testservletb.jar.listeners");
        jar3.addPackageName("testservletb.jar.util");

        Jar jar4 = new Jar("TestServletC.jar");
        jar4.addPackageName("testservletc.jar.servlets");
        jar4.addPackageName("testservletc.jar.listeners");
        jar4.addPackageName("testservletc.jar.util");

        Jar jar5 = new Jar("TestServletD.jar");
        jar5.addPackageName("testservletd.jar.servlets");
        jar5.addPackageName("testservletd.jar.listeners");
        jar5.addPackageName("testservletd.jar.util");

        // Classes put everything in a single package.
        War war = new War("TestServlet40.war");
        war.addPackageName("testservlet40.war.servlets");

        war.addJar(jar1);
        war.addJar(jar2);
        war.addJar(jar3);
        war.addJar(jar4);
        war.addJar(jar5);

        Ear ear = new Ear(getEarName());
        ear.addWar(war);

        return ear;
    }

    // Shared server ...

    // Depending on Test Classes to set sharedServer in an @BeforeClass method.
    // Otherwise, expect NPEs or sharedServer is set to server from previous test class.

    public static final String SERVER_NAME = "annoFat_server";

    public static SharedServer SHARED_SERVER;

    public static void setSharedServer() {
        SHARED_SERVER = new SharedServer(SERVER_NAME, false);
    }

    @Override
    public SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    public static SharedServer getServer() {
        return SHARED_SERVER;
    }

    public static String getServerName() {
        return SHARED_SERVER.getServerName();
    }

    public static LibertyServer getLibertyServer() {
        return getServer().getLibertyServer();
    }

    protected static String getServerRoot() throws Exception {
        return getLibertyServer().getServerRoot();
    }
    
    protected static String getTempDir() throws Exception {
        return getLibertyServer().getServerRoot() + "/temp/annoTest";
    }
    
    public static String getAppsPath() throws Exception {
        return getServerRoot() + "/apps/";
    }

    public static String getExpandedAppsPath() throws Exception {
        return getAppsPath() + "expanded/";
    }

    public static File getInstalledAppsDir() throws Exception {
        return new File( getAppsPath() );
    } 

    public static void addToAppsDir(Ear ear) throws Exception {
        LOG.info("addToAppsDir: " + ear);
        try {
           AppPackagingHelper.addEarToServerApps( getLibertyServer(), ear );
        } catch ( Exception e ) {
            LOG.info("Exception: " + e.getMessage());
            throw e;
        }
    }


    public enum ServerStartType { 
        DO_SCRUB, DO_NOT_SCRUB;
    }

    protected static void startServer(ServerStartType startType) throws Exception {
        LOG.info("startServer: ENTER: " + startType);

        getServer().startIfNotStarted(false,   // Don't preScrub 
                                      (startType == ServerStartType.DO_SCRUB),
                                      false);  // Don't validate apps have started

        LOG.info("startServer: RETURN");
    }

    //

    protected static void startServerScrub() throws Exception {
        LOG.info("startServerScrub: ENTER");

        getServer().startIfNotStarted();

        LOG.info("startServerScrub: RETURN");
    }

    protected static void startServer() throws Exception {
        LOG.info("startServer: ENTER");

        getServer().startIfNotStarted(false, false, false);

        LOG.info("startServer: RETURN");
    }

    protected static void stopServer()  {
        stopServer("CWWKZ0014W");
    }

    protected static void stopServer(String... expectedMessages) {
        LOG.info("stopServer: ENTER");
        for ( String message : expectedMessages ) {
            LOG.info("stopServer: expecting [ " + message + " ]");
        }

        try {
            getLibertyServer().stopServer(expectedMessages);
        } catch ( Exception e ) {
            LOG.info("Exception: " + e.getMessage());
        }

        LOG.info("stopServer RETURN");
    }

    protected static long waitForAppUpdate() throws Exception {
        LOG.info("waitForAppUpdate: ENTER");

        LibertyServer libertyServer = getLibertyServer();
        RemoteFile consoleLog = libertyServer.getConsoleLogFile();

        long lStartTime = System.nanoTime();
        libertyServer.waitForStringInLog("CWWKZ0003I:", consoleLog);
        long lEndTime = System.nanoTime();
        long elapsed = lEndTime - lStartTime;
        LOG.info("waitForAppUpdate: Elapsed (ms): " + elapsed / 1000000);

        libertyServer.setMarkToEndOfLog(consoleLog);

        LOG.info("waitForAppUpdate: RETURN");
        return elapsed;
    }

    protected static long waitForConsole(String message) throws Exception {
        LOG.info("waitForConsole: ENTER [ " + message + " ]");

        LibertyServer libertyServer = getLibertyServer();
        RemoteFile consoleLog = libertyServer.getConsoleLogFile();

        long lStartTime = System.nanoTime();
        libertyServer.waitForStringInLog(message, consoleLog);
        long lEndTime = System.nanoTime();
        long elapsed = lEndTime - lStartTime;
        LOG.info("waitForConsole: Elapsed (ms): " + elapsed / 1000000);

        LOG.info("waitForConsole: RETURN");
        return elapsed;
    }

    //

    public static void deleteApplication() throws Exception {
        File appFile = getApplicationFile();
        String appAbsPath = appFile.getAbsolutePath();
        if ( !appFile.exists() ) {
            LOG.info("deleteApplication: Application does not exist: " + appAbsPath);
        } else {
            LOG.info("deleteApplication: Application: " + appAbsPath);
            delete(appFile);
        }
    }

    public static void deleteExpandedApplication() throws Exception {
        File appFile = getExpandedApplicationFile();
        String appAbsPath = appFile.getAbsolutePath();
        if ( !appFile.exists() ) {
            LOG.info("deleteExpandedApplication: Expanded application does not exist: " + appAbsPath);
        } else {
            LOG.info("deleteExpandedApplication: Expanded application: " + appAbsPath);
            FileUtils.recursiveDelete(appFile);
        }
    }

    /**
     * Copy a "jvm.options" from the test server configuration folder to the server directory.
     *
     * @param sourceJvmOptions New options file.  If null, set the server to have no options.
     */
    protected static void installJvmOptions(String sourceJvmOptions) throws Exception {
        LOG.info("installJvmOptions: ENTER: " + sourceJvmOptions);

        String destRootPath = getServerRoot();
        String destPath = destRootPath + "/jvm.options";
        File destFile = new File(destPath);

        if ( destFile.exists() ) {
            assertTrue("Failed to delete jvm.options file", destFile.delete());
            assertFalse("File jvm.options still exists", destFile.exists());
        }

        String sourcePath = "publish/servers/annoFat_server/config/" + sourceJvmOptions;
        File sourceFile = new File(sourcePath);

        FileUtils.copyFile(sourceFile, destFile);

        Utils.display("JVM options", destFile);

        LOG.info("installJvmOptions: RETURN: " + sourceJvmOptions);
    }

    /**
     * Copy a server.xml from the server configuration to the shared server.
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {
        LOG.info("installServerXml: ENTER: " + sourceServerXml);

        String destRootPath = getServerRoot();
        String destPath = destRootPath + "/server.xml";
        File destFile = new File(destPath);

        if ( destFile.exists() ) {
            assertTrue("Failed to delete server.xml file", destFile.delete());
            assertFalse("File server.xml still exists", destFile.exists());
        }

        String sourcePath = "publish/servers/annoFat_server/config/" + sourceServerXml;
        File sourceFile = new File(sourcePath);
        FileUtils.copyFile(sourceFile, destFile);

        LOG.info("installServerXml: RETURN: " + sourceServerXml);
    } 

    public static void useAlternateExpandedWebXml(String sourceName) throws Exception {
        LOG.info("useAlternateExpandedWebXml: ENTER: " + sourceName);

        String webDirPath = getExpandedApplicationPath() + "/TestServlet40.war/WEB-INF";
        File webInfDir = new File(webDirPath);

        File sourceFile = new File(webInfDir, sourceName);
        String sourcePath = sourceFile.getAbsolutePath();
        File destFile = new File(webInfDir, "web.xml");
        String destPath = destFile.getAbsolutePath();

        LOG.info("useAlternateExpandedWebXml: Source: " + sourcePath);
        LOG.info("useAlternateExpandedWebXml: Destination: " + destPath);

        FileUtils.copyFile(sourceFile, destFile);

        Utils.display("TestServlet40.war descriptor", destFile);

        LOG.info("useAlternateExpandedWebXml: RETURN: " + sourceName);
    }
  
	/**
	 * Copies one entry in a zip to another entry in the same zip.
	 * Like copyFile, but inside a zip.
	 * 
	 * @param sourceEntry location of entry to copy
	 * @param destinationEntry location of entry to replace in zip (full path from root of zip)
	 * @param zipLocationOnDisk  location on disk of zip file.
	 */
    public static void copyEntryWithinZip(String sourceEntry, 
    		                          String destinationEntry,
    		                          String zipLocationOnDisk) throws IOException {

    	LOG.info("sourceEntry: " + sourceEntry);
    	LOG.info("destinationEntry:  " + destinationEntry);
    	LOG.info("zipLocationOnDisk:  " + zipLocationOnDisk);

    	Path zipFilePath = Paths.get(zipLocationOnDisk);
    	try( FileSystem fs = FileSystems.newFileSystem(zipFilePath, null) ){
    	  	Path sourcePath = fs.getPath(sourceEntry);
    		Path destPath = fs.getPath(destinationEntry);
    		
    		Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
    	}
    }
    
	/**
	 * Renames one entry in a zip.  Actually copies the entry then deletes the original.
	 * 
	 * @param sourceEntry location of entry to copy
	 * @param destinationEntry location of entry to replace in zip (full path from root of zip)
	 * @param zipLocationOnDisk  location on disk of zip file.
	 */
    public static void renameEntryInZip(String sourceEntry, 
    		                            String destinationEntry,
    		                            String zipLocationOnDisk) throws IOException {

    	LOG.info("sourceEntry: " + sourceEntry);
    	LOG.info("destinationEntry:  " + destinationEntry);
    	LOG.info("zipLocationOnDisk:  " + zipLocationOnDisk); 

    	Path zipFilePath = Paths.get(zipLocationOnDisk);
    	try( FileSystem fs = FileSystems.newFileSystem(zipFilePath, null) ){
    	  	Path sourcePath = fs.getPath(sourceEntry);
    		Path destPath = fs.getPath(destinationEntry);
    		
    		Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
    		Files.delete(sourcePath);
    		
    	}
    }

    public static void displayWebXmlFromExpandedApp(String altWebXml) throws Exception {
        String webDirPath = getExpandedApplicationPath() + "/TestServlet40.war/WEB-INF";
        File webDir = new File(webDirPath);
        File webFile;
        
        if (altWebXml != null) {
        	webFile = new File(webDir, "web.xml");
        } else {
        	webFile = new File(webDir, altWebXml);
        }
        
        Utils.display("TestServlet40.war descriptor", webFile);
    }
    
    protected static final boolean DELETE_IF_EXISTS = true;
    protected static final boolean SKIP_IF_EXISTS = false;
    
    protected static void mkDir(String dirPath, boolean deleteIfAlreadyExists) throws IOException {
        LOG.info("ENTER. [" + dirPath + "] deleteIfAlreadyExists [ " + deleteIfAlreadyExists + " ]");
        Path path = Paths.get(dirPath);
       
        if (Files.exists(path)) {
            LOG.info("Already exists [ " + dirPath + " ]");
            if (deleteIfAlreadyExists) {
                if (Files.isDirectory(path)) {
                    LOG.info("Is Directory, deleting [ " + dirPath + " ]");
                    FileUtils.recursiveDelete(path.toFile());
                } else {
                    LOG.info("Is File, deleting [ " + dirPath + " ]");
                    Files.delete(path);
                }
            }  else {
                LOG.info("Ignoring [ " + dirPath + " ]");
            }
        }
        
        if (!Files.exists(path)) {
           
            try {
                
                Files.createDirectories(path);
                
                if (!Files.exists(path) ) {
                    throw new IOException("Create directories failed");
                
                }
                LOG.info("Directory created.");
            } catch (IOException e) {
                LOG.info("Exception: [ " + e.getMessage() + "]");
                e.printStackTrace();
                throw e;
            }
        } 
    }
    
    public static void renameJarFileInApplication(String warName, String fileNameToRename, String newFileName) throws Exception {
        String webInfLibDirName = getLibertyServer().getServerRoot() + "/apps/expanded/" + getEarName() + "/" + warName + "/WEB-INF/lib/";
        File fileToRename = new File(webInfLibDirName + fileNameToRename);
        File fileNewName = new File(webInfLibDirName + newFileName);
        boolean renameWorked = fileToRename.renameTo(fileNewName);
        
        if (renameWorked && fileToRename.exists()) {
            renameWorked = false;
        }

        // Maybe, the rename failed because the server still had the file
        // locked.  Give the server time to unlock the file, then try again.

        if (!renameWorked) {
            standardDelay();
            renameWorked = fileToRename.renameTo(fileNewName);
        }

        if (!renameWorked) {
            throw new Exception("File [ " + fileNameToRename + " ] not removed.  File.delete() method returned false");
        } else if (fileToRename.exists()) {
            throw new Exception("File [ " + fileNameToRename + " ] not removed.  Still exists.");
        }
    }

    public static void renameFile(File oldFile, File newFile) throws IOException {
        LOG.info("Attempt to rename [" + oldFile.getName() + "] to [" + newFile.getName() + "]");

        if (newFile.exists()) {
            LOG.info(newFile.getName() + " already exists.  Going to delete it.");
            newFile.delete();
            if (newFile.exists()) {       
                throw new IOException("Could not delete file [" + newFile.getAbsolutePath() + "]");
            } else {
                LOG.info("Deleted file [" + newFile.getName()  + "]");
            }
        } else {
            LOG.info(newFile.getName() + " does not exist.");
        }

        // Make a backup copy (for debugging)
        // String backupFileName =oldFile.getAbsolutePath() + ".backup";
        // LOG.info("backing up file [ " + oldFile.getName() + " ] to \n  [ " + backupFileName + " ]");
        // FileUtils.copyFile(oldFile, new File(backupFileName));

        if ( oldFile.renameTo(newFile) ) {
            LOG.info("Successfully renamed [" + oldFile.getName() + "] to [" + newFile.getName() + "]");
        } else {
            String message = "Could not rename file [" + oldFile.getAbsolutePath() + "] to [" + newFile.getName() + "]";
            LOG.info(message);
            throw new IOException(message);
        }
    }
    
    public static final String ANNO_CACHE_BACKUP_DIR = "annoBackup";

    public static void unexpandApp() throws Exception {
        String origPath = getApplicationPath();
        File origFile = new File(origPath);
        String origAbsPath = origFile.getAbsolutePath();

        String expandedPath = Utils.getExpandedAppPath( getServer(), getEarName() );
        File expandedFile = new File(expandedPath);
        String expandedAbsPath = expandedFile.getAbsolutePath();

        String cacheParentPath = Utils.getAnnoCacheParentPath( getServer() );
        String backupRootPath = cacheParentPath + "/" + ANNO_CACHE_BACKUP_DIR + "/backup/";
        String updatedRootPath = cacheParentPath + "/" + ANNO_CACHE_BACKUP_DIR + "/updated/";

        String backupPath = backupRootPath + getEarName();
        File backupFile = new File(backupPath);
        String backupAbsPath = backupFile.getAbsolutePath();

        String updatedPath = updatedRootPath + getEarName();
        File updatedFile  = new File(updatedPath);
        String updatedAbsPath = updatedFile.getAbsolutePath();

        LOG.info("appOriginal [ " + origAbsPath + " ]");
        LOG.info("appBackup   [ " + backupAbsPath + " ]");
        LOG.info("appExpanded [ " + expandedAbsPath + " ]");
        LOG.info("appUpdated  [ " + updatedAbsPath + " ]");

        LOG.info("Prepare updated folder [ " + updatedRootPath + " ]");
        mkDir(updatedRootPath, DELETE_IF_EXISTS);

        LOG.info("Collapse updated application [ " + expandedAbsPath + " ] to [ " + updatedAbsPath + " ]");
        zip(expandedFile, updatedFile);

        LOG.info("Prepare backup folder [ " + backupRootPath + " ]");
        mkDir(backupRootPath, DELETE_IF_EXISTS);

        LOG.info("Move original application [ " + origAbsPath + " ] to [ " + backupAbsPath + " ]");
        origFile.renameTo(backupFile);

        LOG.info("Move updated application [ " + updatedAbsPath + " ] to [ " + origAbsPath + " ]");
        updatedFile.renameTo(origFile);
    }
    
    /**
     * Replaces the application ear under the serverRoot/apps directory from an expanded (unzipped) application.
     * 
     * @param expandedEarPath  location of expanded EAR to be zipped
     * @throws Exception
     */
    public static void replaceEarFromExpandedEar(String expandedEarPath, String earPath) throws Exception {
    	String methodName = "replaceEarInAppsDir";
    	LOG.info(methodName + ": ENTER: " + expandedEarPath + ", " + earPath) ;

    	// Zip to a temporary zip first.  
    	String tempEarAbsPath = ( new File(getTempDir())).getAbsolutePath() + "/" + getEarName();
    	File tempEarFile = new File(tempEarAbsPath);   
       	LOG.info(methodName + ": Zipping updated application [ " + expandedEarPath + " ] to [ " + tempEarAbsPath + "]");
    	zip(new File(expandedEarPath), tempEarFile);

    	// Delete the ear which will be replaced
    	File earFile = new File(earPath);
    	String earAbsPath = earFile.getAbsolutePath();
    	LOG.info(methodName + ": Deleteing original application [ " + earAbsPath + " ]");
    	earFile.delete();

    	// Replace Ear from temp
    	LOG.info(methodName + ": Move updated application [ " + tempEarAbsPath + " ] to [ " + earAbsPath + " ]");
    	boolean succeeded = tempEarFile.renameTo(earFile);
    	if (!succeeded) {
    		throw new IOException();
    	}
    	LOG.info(methodName + ": EXIT ");
    }

    public static void unzip(File sourceArchive, String destPath) throws IOException {
        String destDirPath = destPath;
        File destDir = new File(destDirPath);
        
        LOG.info("unzip [ " + sourceArchive + " ] to [" + destDirPath + " ]" );
        
        if (sourceArchive.isDirectory()) {
        	LOG.info("sourceArchive is a directory - will copy directory" );
        	FileUtils.copyDirectory(sourceArchive, new File(destPath));
        	return;
        }
        
        byte[] buffer = new byte[32768];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceArchive));
        ZipEntry zipEntry = zis.getNextEntry();
               
        while(zipEntry != null) {

        	String entryName = zipEntry.getName();
        	LOG.info("entryName [ " + entryName + " ]" );
        	File outFile = new File(destDir + File.separator + entryName);

        	//check zip Slip vulnerability
        	String destDirCanonicalPath = destDir.getCanonicalPath();
        	String outFileCanonicalPath = outFile.getCanonicalPath();
        	if (!outFileCanonicalPath.startsWith(destDirCanonicalPath + File.separator)) {
        		throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        	}

        	if (zipEntry.isDirectory()) {
        		outFile.mkdirs();

        	} else {
        		new File(outFile.getParent()).mkdirs();
        		FileOutputStream fos = new FileOutputStream(outFile);
        		
            	LOG.info("Unzipping to " + outFile.getAbsolutePath());
            	
        		int len;
        		while ((len = zis.read(buffer)) > 0) {
        			fos.write(buffer, 0, len);
        		}
        		fos.close();
        	}
        	
        	zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }  
       
    public static void expandEarToTemp(String earPath, String tempExpandedEarPath, boolean deleteIfExists) throws Exception {
        mkDir(tempExpandedEarPath, deleteIfExists);
        
        File earFile = new File(earPath);
        
        unzip(earFile, tempExpandedEarPath);
    }
    
    public static void makeBackupCopyOfEar(String earPath, String backupEarPath) throws IOException {
    	
        File earFile = new File(earPath);
        File backupEarFile = new File(backupEarPath);
        if (backupEarFile.exists()) {
        	backupEarFile.delete();
        }
        FileUtils.copyFile(earFile, backupEarFile);
    }    

    public static void zipWarInExpandedApp(SharedServer sharedServer, String parentPath, String warName) throws IOException {
        String warPath = parentPath + "/" + warName;
        String tempWarPath = warPath + ".temp";

        File warFile = new File(warPath);
        File tempWarFile = new File(tempWarPath);
        
        renameFile(warFile, tempWarFile);

        if ( warFile.exists()) {
            LOG.info("         !!!! RENAME DID NOT WORK !!!!!");
        }

        zip(tempWarFile, warFile);
        
        FileUtils.recursiveDelete(tempWarFile);
    }

    // Zip primitives ...

    public static void zip(File source, File target) throws IOException {
        LOG.info("Zip source [ " + source.getAbsolutePath() + " ] into [ " + target.getAbsolutePath() + " ]");

        FileOutputStream fos = null;
        ZipOutputStream zos = null;

        try {
            fos = new FileOutputStream(target);
            zos = new ZipOutputStream(fos);

            // Note the empty string parameter.
            // Store entries using their relative path from the root.

            zip(source, "", zos);

        } finally {
            if ( zos != null ) {
                try {
                    zos.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
            if ( fos != null ) {
                try {
                    fos.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static final byte[] transferBuffer = new byte[16 * 1024];

    public static void zip(File source, String targetName, ZipOutputStream zos) throws IOException {
        LOG.info("fileToZip Source [ " + source.getAbsolutePath() + " ] Target [" + targetName + " ]" );

        if ( source.isHidden() ) {
            LOG.info("fileToZip skipped: Source is hidden");
            return;
        }

        if ( source.isDirectory() ) {
            if ( !targetName.equals("") ) {
                if ( !targetName.endsWith("/") ) {
                    targetName += "/";
                }

                LOG.info("putEntry [" + targetName +  " ]" );
                zos.putNextEntry(new ZipEntry(targetName));
                zos.closeEntry();
            }

            File[] children = source.listFiles();
            for ( File child : children ) {
                zip(child, targetName + child.getName(), zos);
            }

        } else {
            FileInputStream fis = new FileInputStream(source);
            try {
                LOG.info("putEntry [" + targetName +  " ]" );
                ZipEntry zipEntry = new ZipEntry(targetName);
                zos.putNextEntry(zipEntry);
                int length;
                while ( (length = fis.read(transferBuffer)) >= 0 ) {
                    zos.write(transferBuffer, 0, length);
                }
                zos.closeEntry();
            } finally {
                fis.close();
            }
        }
    }

    // File primitives ...

    public static void listFiles(File source) {
        listFiles(source, IS_ROOT, "* ", "- ");
    }

    public static final boolean IS_ROOT = true;
    public static final boolean IS_NOT_ROOT = false;

    public static void listFiles(File source, boolean isRoot, String dirIndent, String fileIndent) {
        String sourceName = ( IS_ROOT ? source.getAbsolutePath() : source.getName() );

        if ( source.isDirectory() ) {
            LOG.info(dirIndent + sourceName);

            String nextDirIndent = dirIndent + "  ";
            String nextFileIndent = fileIndent + "  ";

            File[] files = source.listFiles();
            for ( File f : files ) {
                listFiles(f, IS_NOT_ROOT, nextDirIndent, nextFileIndent);
            }

        } else {
            LOG.info(fileIndent + sourceName);
        }
    }

    public static void copyFile(String sourcePath, String destPath) throws IOException {
        File sourceFile = new File(sourcePath);
        String sourceAbsPath = sourceFile.getAbsolutePath();

        File destFile = new File(destPath);
        String destAbsPath = destFile.getAbsolutePath();

        if ( destFile.isDirectory() ) {
            destFile = new File( destPath, sourceFile.getName() );
        }

        LOG.info("Copy [ "  + sourceAbsPath + " ] to [ " + destAbsPath + " ]");
        FileUtils.copyFile(sourceFile, destFile);
    }

    public static void copyFolder(String sourcePath, String destPath) throws IOException {
        File sourceFile = new File(sourcePath);
        String sourceAbsPath = sourceFile.getAbsolutePath();

        File destFile = new File(destPath);
        String destAbsPath = destFile.getAbsolutePath();

        LOG.info("Copy [ "  + sourceAbsPath + " ] to [ " + destAbsPath + " ]");
        
        if ( !sourceFile.exists() ) {
            throw new IOException("Source [ " + sourceAbsPath + " ] does not exist.");
        } else if ( !sourceFile.isDirectory() ) {
            throw new IOException("Source [ " + sourceAbsPath + " ] is not a directory");
        } else if ( destFile.exists() && !destFile.isDirectory() ) {
            throw new IOException("Destination [ " + destAbsPath + " ] is not a directory");
        }
        
        copyFolder(sourceFile, destFile, DO_NOT_COLLAPSE_WARS);
    }  
    
    public static void copyFolder(File source, File dest) throws IOException {
        copyFolder(source, dest, DO_NOT_COLLAPSE_WARS);
    }

    //

    public static final boolean DO_COLLAPSE_WARS = true;
    public static final boolean DO_NOT_COLLAPSE_WARS = false;

    public static void copyFolder(File source, File dest, boolean collapseWars) throws IOException {
        String sourcePath = source.getAbsolutePath();
        String destPath = dest.getAbsolutePath();

        LOG.info("Copy [ "  + sourcePath + " ] to [ " + destPath + " ]");
        LOG.info("Collapse WARs [ "  + collapseWars + " ]");

        if ( source.isDirectory() ) {
            if ( !dest.exists() ) {
                dest.mkdirs();
            }

            String files[] = source.list();
            for ( String file : files )   {
                File srcFile = new File(source, file);
                File destFile = new File(dest, file);

                if ( collapseWars && isWar(srcFile) ) {
                    zip(srcFile, destFile);
                } else {
                    copyFolder(srcFile, destFile, collapseWars);
                }
            }

        } else {
            transfer(source, dest);
        }
    }

    public static long transfer(File source, File dest) throws IOException {
        long total = 0L;

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);

            int length;
            while ((length = in.read(transferBuffer)) > 0) {
                out.write(transferBuffer, 0, length);
                total += length;
            }

        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                } 
            }
        }

        return total;
    }

    //

    public static String getFileNameFromPath(String path) {
       int pos = path.lastIndexOf(File.separator);
       if ( pos != -1 ) {
           return path.substring(pos + 1);
       }
       return null;    
    }

    public static boolean isWar(File file) {
        return file.getName().toUpperCase().endsWith(".WAR");
    }

    //

    private static void delete(File file) throws IOException {
        String path = file.getAbsolutePath();

        if ( !file.exists() ) {
            LOG.info("Skip delete; [ " + path + " ] does not exist");
            return;
        }

        LOG.info("Delete [ " + path + " ]");
        file.delete();
        if ( file.exists() ) {
            LOG.info("Failed first deletion of [ " + path + " ]; sleeping 400 ms");
            standardDelay();

            LOG.info("Delete [ " + path + " ] (second try)");
            file.delete();
            if ( file.exists() ) {
                throw new IOException("Failed to delete [ " + path + " ]");
            } else {
                LOG.info("Deleted [ " + path + " ] (second try)");
            }
        } else {
            LOG.info("Deleted [ " + path + " ]");
        }
    }

    private static void standardDelay() {
        try {
            Thread.sleep(400); // Give the server extra time to release the EAR.
        } catch ( InterruptedException e ) {
            // Ignore
        }
    }

    //

    public File getAnnoCacheRoot() {
        String osgiWorkAreaRoot = getSharedServer().getLibertyServer().getOsgiWorkAreaRoot();
        LOG.info("getAnnoCacheRoot: OSGI workarea: " + osgiWorkAreaRoot);

        File annoCacheDir = Utils.findDirectory( new File(osgiWorkAreaRoot), AnnoCacheLocations.CACHE_NAME );
        if ( annoCacheDir == null ) {
            LOG.info("getAnnoCacheRoot: Not found: " + AnnoCacheLocations.CACHE_NAME);
        } else {
            LOG.info("getAnnoCacheRoot: " + annoCacheDir.getAbsolutePath());
        }

        return annoCacheDir;
    }

    public File getAnnoCacheAppRoot(String earName) {
        File annoCacheDir = getAnnoCacheRoot();
        if ( annoCacheDir == null ) {
            return null;
        }

        File appDir = Utils.findDirectory(
            annoCacheDir,
            AnnoCacheLocations.APP_PREFIX + earName + AnnoCacheLocations.APP_SUFFIX);

        if ( appDir == null ) {
            LOG.info("getAnnoCacheAppRoot: Not found: " + earName);
        } else {
            LOG.info("getAnnoCacheAppRoot: " + appDir.getAbsolutePath());
        }
        return appDir;
    }
}
