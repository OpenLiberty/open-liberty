package com.ibm.ws.anno.tests.caching;

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
 * 
 * Super class for all caching tests
 *
 */

public abstract class CachingTest extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(CachingTest.class.getName());
    
    public static final String EAR_FILE_NAME = "TestServlet40.ear";
    public static final String ANNO_CACHE_BACKUP_DIR = "annoBackup";


    // Depending on Test Classes to set sharedServer in an @BeforeClass method.
    // Otherwise, expect NPEs or sharedServer is set to server from previous test class.
    public static SharedServer sharedServer = null;
    public static LibertyServer libertyServer = null;

    @Override
    public SharedServer getSharedServer() {
        if (sharedServer == null) {
            sharedServer = new SharedServer("annoFat_server", false);
        }
        return sharedServer;
    }
    
    public static void setSharedServer() {
        sharedServer = new SharedServer("annoFat_server", false);
        libertyServer = sharedServer.getLibertyServer();
    }
    

    public static void addAppToServerAppsDir(Ear ear) throws Exception {
        LOG.info("Add TestServlet40 to the server if not already present.");

        try {
           FatHelper.addEarToServerApps(sharedServer.getLibertyServer(), ear);

        } catch (Exception e) {
            LOG.info("Caught exception from addEarToServerApps [" + e.getMessage() + "]");
            throw e;
        }
    }
    
    /**
     * Copy the app (ear file) from <serverRoot>/apps
     * to .../<annoCacheRoot>/<ANNO_CACHE_BACKUP_DIR>/apps/<earFileName>
     * 
     * @throws IOException
     */
    public static void backupApplicationFile() throws IOException {
        String annoCacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
        String backupPath = annoCacheParentPath + "/" + ANNO_CACHE_BACKUP_DIR + "/apps/";
        String backupFileName = backupPath + EAR_FILE_NAME;
        File backupFile = new File(backupFileName);
        mkDir(backupPath, DELETE_IF_EXISTS);
        
        String earPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + EAR_FILE_NAME;
        File sourceArchive = new File(earPath);

        LOG.info("Backing up file [ " + earPath + " ] to [ " + backupFileName + " ]");
        FileUtils.copyFile(sourceArchive, backupFile);
    }
    
//    /**
//     * Copy the EXPANDED app (ear file) from <serverRoot>/apps/expanded
//     * to .../<annoCacheRoot>/<ANNO_CACHE_BACKUP_DIR>/apps/expandedEAR<earFileName>
//     * 
//     * Note that the origin "expanded" app directory can have expanded WARs within
//     * the app.  When copied to expandedEAR, we do not keep the WARs in expanded form.
//     * So only the EAR remains expanded.
//     * 
//     * @throws IOException
//     */
//    public static void copyExpandedEarToTemp() throws IOException {
//        
//        // Destination for expanded EAR in temp location
//        String annoCacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
//        String destinationPath = annoCacheParentPath + "/" + ANNO_CACHE_BACKUP_DIR + "/apps/expandedEAR/" + EAR_FILE_NAME;
//        File destination = new File(destinationPath);
//        mkDir(destinationPath);
//        
//        // Source of expanded EAR (which also contains expanded WAR)
//        String sourcePath = Utils.getExpandedAppPath(sharedServer, EAR_FILE_NAME);
//        File source = new File(sourcePath);
//        
//
//        
//        LOG.info("Source expandedAppPath [ " + sourcePath + " ]");
//        LOG.info("Destination expandedAppPath [ " + destinationPath + " ]");
//        
//        // If there are any unzipped WARs in the app, zip them.
//        File[] children = sourceDir.listFiles();
//        for (File child : children) {
//            
//            String childName = child.getName();
//            LOG.info("childName [" + childName + "]");
//            if (child.getName().toUpperCase().endsWith(".WAR") && child.isDirectory()) {
//                LOG.info("Found WAR in application [" + childName + "]");
//                
//                FileOutputStream fos = new FileOutputStream(destinationDir);
//                ZipOutputStream zos = new ZipOutputStream(fos);
//                
//                zip(sourceDir, "", zos);  // Note empty string instead of WAR path, because we want the relative path from the root of the WAR - Not the absolute path of the WAR.
//                zos.close();
//                fos.close();
//            }
//        }
//        
//        LOG.info("expandedAppPath [ " + sourcePath + " ]");
//        
//        // Replace the installedAppFile -- The application file under the <SERVER_NAME>/apps
//        File installedAppFile = getInstalledAppFile();
//        LOG.info("installedAppPath [ " + installedAppFile.getCanonicalPath() + " ]");
//        
//        FileOutputStream fos = new FileOutputStream(installedAppFile);
//        ZipOutputStream zos = new ZipOutputStream(fos);
//        
//        zip(sourceDir, "", zos);
//        zos.close();
//        fos.close();  
//    }
    

    
    public static void copyFolderWhileZippingWarDirs(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String files[] = source.list();

            for (String file : files)   {
                File srcFile = new File(source, file);
                File destFile = new File(destination, file);

                //  if it's a war zip it to the destination   
                if ( srcFile.getName().toUpperCase().endsWith(".WAR"))  {
                    LOG.info("Zipping WAR [ " + srcFile.getAbsolutePath() + " ] to [ " + destFile.getAbsolutePath());
                    FileOutputStream fos = new FileOutputStream(destFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);

                    zip(srcFile, "", zos);
                    zos.close();
                    fos.close();
                } else {
                    copyFolderWhileZippingWarDirs(srcFile, destFile);
                }
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(source);
                out = new FileOutputStream(destination);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

            } catch (Exception e) {
                try {
                    in.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                try {
                    out.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Delete the application file (ear file) from <serverRoot>/apps
     * @throws IOException
     */
    public static void deleteApplicationFile() throws IOException { 
        LOG.info("entry");
        String appPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + EAR_FILE_NAME;

        File appFile = new File(appPath);
        if (!!!appFile.exists()) {
            LOG.info("appication file [" + appPath + "] does not exist. No need to delete.");
            return;
        }
        
        LOG.info("Deleting appication file [" + appPath + "]");
        appFile.delete();
        if (appFile.exists()) {
            throw new IOException("Unable to delete [" + appFile.getName() + "]");
        }
        LOG.info("Deleted successfully.  Exit");
    }
    
    /**
     * Recursively delete the .../<server>/apps/expanded/<Application.EAR> directory.
     * @throws Exception
     */
    public static void deleteExpandedApplication() throws Exception {
        LOG.info("entry");
        String expandedAppPath = getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME ;
        File expandedAppDir = new File(expandedAppPath);
        
        if (!!!expandedAppDir.exists()) {
            LOG.info("expanded app dir [" + expandedAppDir + "] does not exist. No need to delete.");
            return;
        }
        
        if (expandedAppDir.exists()) {
            LOG.info("Deleting expanded app [ " + expandedAppPath + " ]" );
            FileUtils.recursiveDelete(expandedAppDir);
        }
        LOG.info("exit");
    }
    
    /**
     * Expand the .../<server>/apps/<Application.ear> file 
     *    to the apps/expanded/<Application.ear> directory 
     *    
     *  If expanded application directory already exists, it is deleted before
     *  the application expanded.  
     *    
     * @throws Exception
     */
    public static void expandApplication() throws Exception {
        LOG.info("entry");

        // First delete the existing expanded app
        String expandedAppPath = getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME ;
        
        File expandedAppDir = new File(expandedAppPath);
        
        if (expandedAppDir.exists()) {
            deleteExpandedApplication();
        }
        
        // Then expand the application
        // Need to implement unzip
        // unzip(zippedEarPath, expandedAppPath);
    }    

    public static Ear createApp() throws Exception {
        Jar jar1 = new Jar("TestServlet40.jar");
        jar1.addPackageName("testservlet40.jar.servlets");

        Jar jar2 = new Jar("TestServletA.jar");
        jar2.addPackageName("testservleta.jar.servlets");
        jar2.addPackageName("testservleta.jar.listeners");

        Jar jar3 = new Jar("TestServletB.jar");
        jar3.addPackageName("testservletb.jar.servlets");
        jar3.addPackageName("testservletb.jar.listeners");

        Jar jar4 = new Jar("TestServletC.jar");
        jar4.addPackageName("testservletc.jar.servlets");
        jar4.addPackageName("testservletc.jar.listeners");

        Jar jar5 = new Jar("TestServletD.jar");
        jar5.addPackageName("testservletd.jar.servlets");
        jar5.addPackageName("testservletd.jar.listeners");
        
        War war = new War("TestServlet40.war");
        war.addPackageName("testservlet40.war.servlets");
        war.addJar(jar1);
        war.addJar(jar2);
        war.addJar(jar3);
        war.addJar(jar4);
        war.addJar(jar5);

        Ear ear = new Ear(EAR_FILE_NAME);
        ear.addWar(war);

        return ear;
    }
    
    /**
     * Returns a File object for the application file under the "apps" directory under the .../server_root_dir/.
     * @return
     */
    public static File getInstalledAppFile() {
        String installedAppPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + EAR_FILE_NAME;

        LOG.info("installedAppPath [" + installedAppPath + "]");
        return new File(installedAppPath);
    } 
    
    /**
     * Returns the root directory of the Liberty server.
     * @return
     * @throws Exception
     */
    protected static String getServerRoot() throws Exception {
        return sharedServer.getLibertyServer().getServerRoot();
    }
    
    /**
     * Copy a server.xml from the server configuration to the shared server.
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {
        String serverRootDir = getServerRoot();
        File serverXmlFile = new File(serverRootDir + "/server.xml");

        if (serverXmlFile.exists()) {
            serverXmlFile.delete();
        }

        File serverConfigurationFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(serverConfigurationFile, serverXmlFile); 
    } 
    
    protected static void logBlock(String msg) {
        String blockChars = "";
        for (int i = 0; i < msg.length() + 6; i++) {
            blockChars += "=";
        }
        LOG.info("");
        LOG.info(blockChars);
        LOG.info("=  " + msg + "  =");
        LOG.info(blockChars);
    }
    
    /**
     * Display the web.xml file in the logs
     * @throws Exception
     */
    public static void displayWebXml() throws Exception {
        String webInfDirName = getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME + "/TestServlet40.war/WEB-INF/";
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
    
    /**
     * 
     * @param dirPath  - path to create
     * @throws IOException
     */
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
    
    public static void renameJarFileInApplication(String fileNameToRename, String newFileName) throws Exception {
        
        String webInfLibDirName = sharedServer.getLibertyServer().getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME + "/TestServlet40.war/WEB-INF/lib/";
        File fileToRename = new File(webInfLibDirName + fileNameToRename);
        File fileNewName = new File(webInfLibDirName + newFileName);
        boolean deleteWorked = fileToRename.renameTo(fileNewName);
        
        if ( !deleteWorked ) {
            throw new Exception("File [ " + fileNameToRename + " ] not removed.  File.delete() method returned false");
        } else if (fileToRename.exists()) {
            throw new Exception("File [ " + fileNameToRename + " ] not removed.  Still exists.");
        }

    }

    /**
     * rename oldFile to newFile
     * 
     * @param oldFile
     * @param newFile
     * @throws Exception
     */
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
    
    /**
     * 
     * @param sharedServer
     * @param appFileName
     * @throws IOException
     */
    public static void replaceApplicationFileFromExpandedApp() throws IOException {
        
        backupApplicationFile();
        deleteApplicationFile();
        
        String expandedAppPath = Utils.getExpandedAppPath(sharedServer, EAR_FILE_NAME);
        File expandedAppDir = new File(expandedAppPath);
        LOG.info("expandedAppPath [" + expandedAppPath + "]");
        
        // Destination for expanded EAR in temp location
        String annoCacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
        String tempPath = annoCacheParentPath + "/" + ANNO_CACHE_BACKUP_DIR + "/apps/expandedEAR/" + EAR_FILE_NAME;
        File tempDir = new File(tempPath);
        mkDir(tempPath, DELETE_IF_EXISTS);
        
        // In the zipped EAR, the WAR is also zipped.  In the expanded EAR, the WAR is also expanded.
        // Copy the expanded app to a temporary location, but zip the WAR directories.
        // The temp directory will hold an expanded EAR suitable for zipping.
        LOG.info("Copying expanded App [ "+ expandedAppPath + " ]");
        LOG.info("Copying to [ "+ tempPath + " ].  Zipping WAR dirs before copying.");
        copyFolderWhileZippingWarDirs(expandedAppDir, tempDir);
               
//        // If there are any unzipped WARs in the app, zip them.
//        File[] children = expandedAppDir.listFiles();
//        for (File child : children) {
//            
//            String childName = child.getName();
//            LOG.info("childName [" + childName + "]");
//            if (child.getName().toUpperCase().endsWith(".WAR") && child.isDirectory()) {
//                LOG.info("Found WAR in application [" + childName + "]");
//                zipWarInExpandedApp(sharedServer, expandedAppPath, childName);
//            }
//        }
//        
//        LOG.info("expandedAppPath [ " + expandedAppPath + " ]");
//        
        // Replace the installedAppFile ( The application file under the <SERVER_NAME>/apps )
        File installedAppFile = getInstalledAppFile();
        LOG.info("installedAppPath [ " + installedAppFile.getCanonicalPath() + " ]");
        
        FileOutputStream fos = new FileOutputStream(installedAppFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        
        LOG.info("Zipping tempDir [ " + tempDir.getAbsolutePath() + " ] to installedAppPath [ " + installedAppFile.getCanonicalPath() + " ]");
        zip(tempDir, "", zos);
        zos.close();
        fos.close();
    }
    
    public static void replaceWebXmlInExpandedApp(String newWebXmlName) throws IOException {
 
        String webInfDirName = sharedServer.getLibertyServer().getServerRoot() + "/apps/expanded/" + EAR_FILE_NAME + "/TestServlet40.war/WEB-INF/";
        File webXml = new File(webInfDirName + "web.xml");
        File newWebXml = new File(webInfDirName + newWebXmlName);
          
        // Copy new web.xml over old web.xml
        LOG.info("webInfDirName=[ " + webInfDirName + " ]");
        LOG.info("Replacing file [ " + webXml.getName() + " ] with [ " + newWebXml.getName() + " ]");
        FileUtils.copyFile(newWebXml, webXml);   
    } 
    
    protected static void startServerClean() throws Exception {
        LOG.info("starting server");
        sharedServer.startIfNotStarted();
        LOG.info("completed start server CLEAN step");
    }

    protected static void startServerDirty() throws Exception {
        LOG.info("starting server");
        sharedServer.startIfNotStarted(false, false, false);  
        LOG.info("completed start server DIRTY step");
    }

    
    protected static void stopServer()  {    
            stopServer("CWWKZ0014W");
    }

    protected static void stopServer(String... expectedMessages) {

        LOG.info("stopServer : stopping server");
        for ( String message : expectedMessages ) {
            LOG.info("stopServer : expecting [ " + message + " ]");
        }
        
        try {
            sharedServer.getLibertyServer().stopServer(expectedMessages);
            
        } catch (Exception e) {
            LOG.info("Caught Exception: [ " + e.getMessage() + " ]");
        }

        LOG.info("stopServer : stopped server");
    }
    
    protected static void waitForAppUpdateToBeNoticed() throws Exception {
        long lStartTime = System.nanoTime();
        libertyServer.waitForStringInLog("CWWKZ0003I:", libertyServer.getConsoleLogFile());

        long lEndTime = System.nanoTime();        
        long elapsed = lEndTime - lStartTime;
        LOG.info("Server updated in milliseconds: " + elapsed / 1000000);
        libertyServer.setMarkToEndOfLog(libertyServer.getConsoleLogFile());
    }
    
    public static void zzzzzzunzip(File sourceArchive, String destinationPath) throws IOException {

        String destinationDirPath = destinationPath;
        LOG.info("unzipping [ " + sourceArchive + " ] to [" + destinationDirPath + " ]" );
        
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceArchive));
        ZipEntry zipEntry = zis.getNextEntry();
               
        while(zipEntry != null) {
            
            String entryName = zipEntry.getName();
            LOG.info("entryName [ " + entryName + " ]" );
            
            File outFile = new File(destinationDirPath + entryName);
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
    
//    public static void unzip(String zipFileName, String destinationPath){
//
//        byte[] buffer = new byte[1024];
//
//        try {
//            LOG.info("Unzipping  [ " + zipFileName + " ]" );
//            
//            File destinationDir  = new File(destinationPath);
//            if ( !!!destinationDir.exists() ) {
//                mkDir(destinationPath);
//            }
//
//            ZipInputStream zis = new ZipInputStream( new FileInputStream(zipFileName) );
//            ZipEntry entry = zis.getNextEntry();
//
//            while ( entry != null ) {
//
//                String entryName = entry.getName();
//                File newFile = new File( destinationPath + File.separator + entryName );
//                
//                String parentPath = newFile.getParent();
//                
//                if (entryName.toUpperCase().endsWith(".WAR")) {
//                    
//                    unzip(destinationPath + File.separator + entryName);
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
    
    public static String getFileNameFromPath(String path) {
       int pos = path.lastIndexOf(File.separator);
       if ( pos != -1 ) {
           return path.substring(pos + 1);
       }
       return null;    
    }

    
   public static void zip(File fileToZip, String zippedFileName, ZipOutputStream zos) throws IOException {
        
        LOG.info("fileToZip [" + fileToZip.getAbsolutePath() + "] ZipToName [" + zippedFileName + " ]" );
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            String dirName = zippedFileName;
            if (!!!dirName.equals("")) {
                if (zippedFileName.endsWith("/")) {
                    LOG.info("putEntry [" + dirName +  " ]" );
                    zos.putNextEntry(new ZipEntry(dirName));
                    zos.closeEntry();
                } else {
                    dirName += "/";
                    zos.putNextEntry(new ZipEntry(dirName));
                    zos.closeEntry();  
                }
            }
            File[] children = fileToZip.listFiles();
            for (File child : children) {
                zip(child, dirName + child.getName(), zos);
            }
            return;
        }
        
        // If just a file (not a directory)
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(zippedFileName);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        fis.close();
    }
   
    
    /**
     * 
     * @param sharedServer
     * @param parentPath  - parent directory of WAR.
     * @param warName
     * @throws IOException
     */
    public static void zipWarInExpandedApp(SharedServer sharedServer, String parentPath, String warName) throws IOException {
        String warPath = parentPath + "/" + warName;
        String tempPath = warPath + ".temp";
        File warFile = new File(warPath);
        File tempFile = new File(tempPath);
        
        renameFile(warFile, tempFile);
        
        if ( warFile.exists()) {
            LOG.info("         !!!! RENAME DID NOT WORK !!!!!");
        }

        File tempDir = new File(tempPath);    // Zip this directory 
        File newWarFile = new File(warPath);  // into this file (the original file name)
        
        FileOutputStream fos = new FileOutputStream(newWarFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        
        zip(tempDir, "", zos);  // Note empty string instead of WAR path, because we want the relative path from the root of the WAR - Not the absolute path of the WAR.
        zos.close();
        fos.close();
        
        FileUtils.recursiveDelete(tempDir);
    }

}
