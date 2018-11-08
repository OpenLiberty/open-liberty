/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.tests.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.ws.anno.tests.caching.AnnoCacheLocations;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.topology.utils.FileUtils;

public class Utils {
	private static final Logger LOG = Logger.getLogger(Utils.class.getName());
	
    public static void unzip(File sourceArchive, String destinationPath) throws IOException {

    	String destinationDirPath = destinationPath;
    	LOG.info("unzipping [ " + sourceArchive + " ] to [" + destinationDirPath + " ]" );
    	
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceArchive));
        ZipEntry zipEntry = zis.getNextEntry();
        
        while(zipEntry != null){
        	
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
    
    public static void zip(File fileToZip, String zippedFileName, ZipOutputStream zos) throws IOException {
    	
    	LOG.info("fileToZip [" + fileToZip.getName() + "] fileToZipName [" + zippedFileName + " ]" );
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
        			LOG.info("fileToZip [" + fileToZip.getName() + "] fileToZipName [" + zippedFileName + " ]" );
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
    
    public static void replaceApplicationFileFromExpandedApp(SharedServer sharedServer, String appFileName) throws IOException {
        String expandedAppPath = Utils.getExpandedAppPath(sharedServer, appFileName);
        File expandedAppDir = new File(expandedAppPath);
        
        // If there are any unzipped WARs in the app, zip them.
        File[] children = expandedAppDir.listFiles();
        for (File child : children) {
        	String childName = child.getName();
        	if (child.getName().toUpperCase().endsWith(".WAR") && child.isDirectory()) {
        		LOG.info("Found WAR in application [" + childName + "]");
        		zipWarInExpandedApp(sharedServer, expandedAppPath, childName);
        	}
        }
        
        LOG.info("M expandedAppPath [ " + expandedAppPath + " ]");
        
        // Replace the installedAppFile -- The application file under the <SERVER_NAME>/apps
        File installedAppFile = getInstalledAppFile(sharedServer, appFileName);
        LOG.info("M installedAppPath [ " + installedAppFile.getCanonicalPath() + " ]");
        
        FileOutputStream fos = new FileOutputStream(installedAppFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        
        zip(expandedAppDir, "", zos);
        zos.close();
        fos.close();
    }
    
    public static void replaceWebXml(SharedServer sharedServer, String appFileName, String newWebXmlName) throws Exception {
        LOG.info("In modifyApp()");

        //String webInfDirName = SHARED_SERVER.getLibertyServer().getServerRoot() + "/apps/expanded/" + APP_NAME + "TestServlet40.war/WEB-INF";
        String webInfDirName = sharedServer.getLibertyServer().getServerRoot() + "/apps/expanded/" + appFileName + "/TestServlet40.war/WEB-INF/";
        File webXml = new File(webInfDirName + "web.xml");
        File newWebXml = new File(webInfDirName + newWebXmlName);
          
    	// Copy new web.xml over old web.xml
        LOG.info("webInfDirName=[ " + webInfDirName + " ]");
    	LOG.info("Replacing file [ " + webXml.getName() + " ] with [ " + newWebXml.getName() + " ]");
    	FileUtils.copyFile(newWebXml, webXml);   
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

        File tempDir = new File(tempPath);    // Zip this directory 
        File newWarFile = new File(warPath);  // into this file (the original file name)
        
        FileOutputStream fos = new FileOutputStream(newWarFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        
        zip(tempDir, "", zos);  // Note empty string instead of WAR path, because we want the relative path from the root of the WAR - Not the absolute path of the WAR.
        zos.close();
        fos.close();
        
        FileUtils.recursiveDelete(tempDir);
    }

    /**
     * Copy the app (ear file) from <serverRoot>/apps
     * to .../<annoCacheRoot>/annoCacheBackup/apps/<earFileName>
     * 
     * @param sharedServer
     * @param earFileName
     * @throws IOException
     */
    public static void backupApplicationFile(SharedServer sharedServer, String earFileName) throws IOException {
        String annoCacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
        String backupPath = annoCacheParentPath + "/annoCacheBackup/apps/";
        String backupFileName = backupPath + earFileName;
        mkDir(backupPath);
        
        String earPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + earFileName;
        File sourceArchive = new File(earPath);
        File backupFile = new File(backupFileName);
        
        FileUtils.copyFile(sourceArchive, backupFile);
    }
    
    /**
     * Delete the application file (ear file) from <serverRoot>/apps
     * @param sharedServer
     * @param earFileName
     * @throws IOException
     */
    public static void deleteApplicationFile(SharedServer sharedServer, String earFileName) throws IOException { 
        
        String appPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + earFileName;
        File appFile = new File(appPath);
        appFile.delete();
        if (appFile.exists()) {
        	throw new IOException("Unable to delete [" + appFile.getName() + "]");
        }
    }
    
    
    
    
    public static String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
        return format.format(date);
    }
    
    protected static void expandAppIntoOsgiWorkArea(SharedServer sharedServer, String earFileName) throws IOException {
        String annoCacheParentPath = Utils.getAnnoCacheParentPath(sharedServer);
        String tempExpandedEarPath = annoCacheParentPath + "/annoCacheTempExpandedApps/" + earFileName;
        LOG.info("tempExpandedEarPath [ " + tempExpandedEarPath + " ]");
        mkDir(tempExpandedEarPath);

        String earPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + earFileName;
        File sourceArchive = new File(earPath);
        
        Utils.unzip(sourceArchive, tempExpandedEarPath);
    }
    
    
    protected static void mkDir(String dirPath) throws IOException {
    	LOG.info("Making dir [" + dirPath + "]");
    	Path path = Paths.get(dirPath);
    	if (!Files.exists(path)) {
    		LOG.info("Creating directories");
            try {
                Files.createDirectories(path);
                if (!Files.exists(path) ) {
                	throw new IOException("Create directories failed");
                
                }
            } catch (IOException e) {
                LOG.info("Exception: [ " + e.getMessage() + "]");
                e.printStackTrace();
                throw e;
            }
        } else {
        	LOG.info("Path already exists!");
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
     * Retrieves the directory path immediately above the "anno" directory
     * in the OSGI work area.
     * @return
     */
    public static String getAnnoCacheParentPath(SharedServer sharedServer) throws IOException {
    	String annoCacheParentPath = getAnnoCachePath(sharedServer) + "/..";
    	LOG.info("annoCacheParentDir [" + annoCacheParentPath  + "]");
    	return annoCacheParentPath;
    }
    
    /**
     * Returns a File object for the application file under the "apps" directory under the .../server_root_dir/.
     * @return
     */
    public static File getInstalledAppFile(SharedServer sharedServer, String appFileName) {
    	String installedAppPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/" + appFileName;

    	LOG.info("installedAppPath [" + installedAppPath + "]");
        return new File(installedAppPath);
    }   
    
    /**
     * Retrieves the application directory under the <serverRoot>/apps/expanded directory.
     * @return
     */
    public static String getExpandedAppPath(SharedServer sharedServer, String appFileName) {
    	String expandedAppPath = getExpandedAppsPath(sharedServer) + "/" + appFileName;
    	LOG.info("expandedAppPath [" + expandedAppPath + "]");
        return expandedAppPath;
    } 
    
    /**
     * Retrieves "apps" directory path under the server root directory.
     * For example:  <serverRoot>/apps/expanded
     * 
     * @return
     */
    public static String getExpandedAppsPath(SharedServer sharedServer) {
    	String expandedAppsPath = sharedServer.getLibertyServer().getServerRoot() + "/apps/expanded";
    	LOG.info("expandedAppsPath [" + expandedAppsPath + "]");
        return expandedAppsPath;
    }     
    
    /**
     * Retrieves the directory of the application found in the "anno" directory
     * in the OSGI work area.
     * @return
     */
    public static File getAnnoCacheAppDir(SharedServer sharedServer) {
        File annoCacheDir = getAnnoCacheDir(sharedServer);
        File appDir = findDirectory(annoCacheDir, "APP_TestServlet40");
        LOG.info("AnnoCache App Directory[" + appDir + "]");
        return appDir;
    }    
    
    /**
     * Retrieves the root directory of the "anno" directory
     * in the OSGI work area.
     * @return
     */    
    public static File getAnnoCacheDir(SharedServer sharedServer) {
        String osgiWorkAreaRoot = sharedServer.getLibertyServer().getOsgiWorkAreaRoot();
        LOG.info("OSGI workarea[" + osgiWorkAreaRoot + "]");
        
        File annoCacheDir = findDirectory(new File(osgiWorkAreaRoot), AnnoCacheLocations.CACHE_NAME);
        LOG.info("annoCache dir[" + annoCacheDir + "]");
        return annoCacheDir;
    }
    
    public static String getAnnoCachePath(SharedServer sharedServer) throws IOException {
        String osgiWorkAreaRoot = sharedServer.getLibertyServer().getOsgiWorkAreaRoot();
        LOG.info("OSGI workarea[" + osgiWorkAreaRoot + "]");
        
        File annoCacheDir = findDirectory(new File(osgiWorkAreaRoot), AnnoCacheLocations.CACHE_NAME);
        LOG.info("annoCache Dir[" + annoCacheDir + "]");
        return annoCacheDir.getCanonicalPath();
    }
    
    public static File findFile(File file, String searchFileName) {
        if (file.isDirectory()) {
            File[] arr = file.listFiles();
            for (File f : arr) {
                File found = findFile(f, searchFileName);
                if (found != null)
                    return found;
            }
        } else {
            if (file.getName().equals(searchFileName) ) {
                return file;
            }
        }
        return null;
    }
    
    public static File findDirectory(File file, String searchFileName) {
        if (file.isDirectory()) {
            if (file.getName().equals(searchFileName) ) {
                return file;
            }
            File[] arr = file.listFiles();
            for (File f : arr) {
                File found = findDirectory(f, searchFileName);
                if (found != null)
                    return found;
            }
        } else {
            // File ignored.  We are looking for a directory.
        }
        
        return null;
    }

 
}
