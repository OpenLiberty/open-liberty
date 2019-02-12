package com.ibm.ws.anno.tests.caching;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.ws.anno.tests.util.Ear;
import com.ibm.ws.anno.tests.util.FatHelper;
import com.ibm.ws.anno.tests.util.Jar;
import com.ibm.ws.anno.tests.util.Utils;
import com.ibm.ws.anno.tests.util.War;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FileUtils;

/**
 * Super class for all caching tests.
 */
public abstract class CachingTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(CachingTest.class.getName());

    protected static void logBlock(String msg) {
        String blockChars = "";
        for ( int i = 0; i < msg.length() + 6; i++ ) {
            blockChars += "=";
        }

        LOG.info("");
        LOG.info(blockChars);
        LOG.info("=  " + msg + "  =");
        LOG.info(blockChars);
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

    public static SharedServer sharedServer = null;
    public static LibertyServer libertyServer = null;

    public static void setSharedServer() {
        sharedServer = new SharedServer(SERVER_NAME, false);
        libertyServer = sharedServer.getLibertyServer();
    }
    
    @Override
    public SharedServer getSharedServer() {
        if ( sharedServer == null ) {
            sharedServer = new SharedServer(SERVER_NAME, false);
        }
        return sharedServer;
    }

    protected static String getServerRoot() throws Exception {
        return sharedServer.getLibertyServer().getServerRoot();
    }
    
    public static String getAppsPath() throws Exception {
        return getServerRoot() + "/apps/";
    }

    public static String getExpandedAppsPath() throws Exception {
        return getAppsPath() + "expanded/";
    }

    public static File getInstalledAppDir() throws Exception {
        return new File( getAppsPath() );
    } 

    public static void addAppToServerAppsDir(Ear ear) throws Exception {
        LOG.info("Add application [" + ear + " ]");

        try {
           FatHelper.addEarToServerApps(sharedServer.getLibertyServer(), ear);

        } catch ( Exception e ) {
            LOG.info("Exception: " + e.getMessage());
            throw e;
        }
    }

    protected static void startServerClean() throws Exception {
        sharedServer.startIfNotStarted();
    }

    protected static void startServerDirty() throws Exception {
        sharedServer.startIfNotStarted(false, false, false);  
    }

    protected static void stopServer()  {    
        stopServer("CWWKZ0014W");
    }

    protected static void stopServer(String... expectedMessages) {
        LOG.info("stopServer ENTER");
        for ( String message : expectedMessages ) {
            LOG.info("stopServer : expecting [ " + message + " ]");
        }

        try {
            sharedServer.getLibertyServer().stopServer(expectedMessages);
        } catch ( Exception e ) {
            LOG.info("Caught Exception: " + e.getMessage());
        }

        LOG.info("stopServer RETURN");
    }

    protected static long waitForAppUpdateToBeNoticed() throws Exception {
        long lStartTime = System.nanoTime();
        libertyServer.waitForStringInLog("CWWKZ0003I:", libertyServer.getConsoleLogFile());
        long lEndTime = System.nanoTime();
        long elapsed = lEndTime - lStartTime;
        LOG.info("Server updated in milliseconds: " + elapsed / 1000000);

        libertyServer.setMarkToEndOfLog(libertyServer.getConsoleLogFile());

        return elapsed;
    }

    //

    public static void deleteApplicationFile() throws Exception { 
        delete( getApplicationFile() );
    }
    
    public static void deleteExpandedApplication() throws Exception {
        File appDir = getExpandedApplicationFile();
        String appAbsPath = appDir.getAbsolutePath();

        if ( !appDir.exists() ) {
            LOG.info("Skip delete; application directory [ " + appAbsPath + " ] does not exist.");
            return;
        }

        LOG.info("Delete application directory [ " + appAbsPath + " ]" );
        FileUtils.recursiveDelete(appDir);
    }
    
    /**
     * Expand the .../<server>/apps/<Application.ear> file 
     * to the apps/expanded/<Application.ear> directory 
     *    
     * If expanded application directory already exists, it is deleted before
     * the application expanded.  
     */
    public static void expandApplication() throws Exception {
        LOG.info("entry");

        String expandedAppPath = getServerRoot() + "/apps/expanded/" + getEarName() ;
        File expandedAppDir = new File(expandedAppPath);
        
        if ( expandedAppDir.exists() ) {
            deleteExpandedApplication();
        }
        
        // Then expand the application
        // Need to implement unzip

        // unzip(zippedEarPath, expandedAppPath);
    }    

    /**
     * Copy a "jvm.options" from the test server configuration folder to the server directory.
     *
     * @param sourceJvmOptions New options file.  If null, set the server to have no options.
     */
    protected static void installJvmOptions(String sourceJvmOptions) throws Exception {
        LOG.info("installJvmOptions [ " + sourceJvmOptions + " ]");

        String serverRootDir = getServerRoot();
        File serverJvmOptionsFile = new File(serverRootDir + "/jvm.options");

        if ( serverJvmOptionsFile.exists() ) {
            assertTrue("Unable to delete jvm.options file", serverJvmOptionsFile.delete());
            assertFalse("Unable to delete jvm.options file, still exists", serverJvmOptionsFile.exists());
        }

        if ( sourceJvmOptions != null ) {
            File sourceJvmOptionsFile = new File(serverRootDir + "/serverConfigurations/" + sourceJvmOptions);
            FileUtils.copyFile(sourceJvmOptionsFile, serverJvmOptionsFile); 
        }
    }

    /**
     * Copy a server.xml from the server configuration to the shared server.
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {
        LOG.info("installServerXml [ " + sourceServerXml + " ]");

        String serverRootDir = getServerRoot();
        File serverXmlFile = new File(serverRootDir + "/server.xml");

        if ( serverXmlFile.exists() ) {
            serverXmlFile.delete();
        }

        File sourceServerXmlFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(sourceServerXmlFile, serverXmlFile); 
    } 

    /**
     * Display the web.xml file in the logs
     */
    public static void displayWebXml() throws Exception {
        String webInfDirName = getServerRoot() + "/apps/expanded/" + getEarName() + "/TestServlet40.war/WEB-INF/";
        File webXmlFile = new File(webInfDirName + "web.xml");
        
        BufferedReader targetsReader = new BufferedReader(new FileReader(webXmlFile)); 
        try {
            String line; 
            LOG.info("web.xml:\n" + webInfDirName + "web.xml");
            while ((line = targetsReader.readLine()) != null) {
                LOG.info(line);
            } 

        } catch (IOException ioe) {
            LOG.info(ioe.getMessage());
            throw ioe;

        } finally {
            if (targetsReader != null) {
                targetsReader.close();
            } 
        }
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
                LOG.info("Ignoring [ " + dirPath + " ]");;
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
        String webInfLibDirName = sharedServer.getLibertyServer().getServerRoot() + "/apps/expanded/" + getEarName() + "/" + warName + "/WEB-INF/lib/";
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

    public static void replaceApplicationFileFromExpandedApp() throws Exception {
        String origPath = getApplicationPath();
        File origFile = new File(origPath);
        String origAbsPath = origFile.getAbsolutePath();

        String expandedPath = Utils.getExpandedAppPath(sharedServer, getEarName());
        File expandedFile = new File(expandedPath);
        String expandedAbsPath = expandedFile.getAbsolutePath();

        String cacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
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
    
    public static void replaceWebXmlInExpandedApp(String newWebXmlName) throws Exception {
        String webInfDirName = getExpandedApplicationPath() + "/TestServlet40.war/WEB-INF/";

        File origWebXml = new File(webInfDirName + "web.xml");
        File newWebXml = new File(webInfDirName + newWebXmlName);

        LOG.info("Copy [ " + newWebXml.getAbsolutePath() + " ] over [ " + origWebXml.getAbsolutePath() + " ]");
        FileUtils.copyFile(newWebXml, origWebXml);
    }
    
    public static void zzzzzzunzip(File sourceArchive, String destPath) throws IOException {
        String destDirPath = destPath;
        LOG.info("unzipping [ " + sourceArchive + " ] to [" + destDirPath + " ]" );
        
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceArchive));
        ZipEntry zipEntry = zis.getNextEntry();
               
        while(zipEntry != null) {
            
            String entryName = zipEntry.getName();
            LOG.info("entryName [ " + entryName + " ]" );
            
            File outFile = new File(destDirPath + entryName);
            FileOutputStream fos = new FileOutputStream(outFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }  
    
//    public static void unzip(String zipFileName, String destPath){
//
//        byte[] buffer = new byte[1024];
//
//        try {
//            LOG.info("Unzipping  [ " + zipFileName + " ]" );
//            
//            File destDir  = new File(destPath);
//            if ( !destDir.exists() ) {
//                mkDir(destPath);
//            }
//
//            ZipInputStream zis = new ZipInputStream( new FileInputStream(zipFileName) );
//            ZipEntry entry = zis.getNextEntry();
//
//            while ( entry != null ) {
//
//                String entryName = entry.getName();
//                File newFile = new File( destPath + File.separator + entryName );
//                
//                String parentPath = newFile.getParent();
//                
//                if (entryName.toUpperCase().endsWith(".WAR")) {
//                    
//                    unzip(destPath + File.separator + entryName);
//                
//                }                
//
//                LOG.info("unzipping entry [ " + newFile.getAbsoluteFile() + "]");;
//
//                //create all directories that don't exist
//                new File(newFile.getParent()).mkdirs();
//
//                FileOutputStream fos = new FileOutputStream(newFile);             
//
//                int len;
//                while ((len = zis.read(buffer)) > 0) {
//                    fos.write(buffer, 0, len);
//                }
//
//                fos.close();   
//                entry = zis.getNextEntry();
//            }
//
//            zis.closeEntry();
//            zis.close();
//
//        }catch(IOException ex){
//            ex.printStackTrace(); 
//        }
//    }    

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
}
