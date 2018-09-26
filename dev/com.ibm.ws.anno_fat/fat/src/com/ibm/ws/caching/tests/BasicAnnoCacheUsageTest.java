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
package com.ibm.ws.caching.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.jandex.JandexApplicationHelper;

import componenttest.topology.utils.FileUtils;

/**
   Test that the annotation cache is created, restart the server, and check that the cache is being used.
 */

public class BasicAnnoCacheUsageTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(BasicAnnoCacheUsageTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("annoFat_server");
    private static boolean needToStopServer = true;
    
    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        // TODO Auto-generated method stub
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        JandexApplicationHelper.addEarToServerApps(SHARED_SERVER.getLibertyServer(),
                                               "TestServlet40.ear", // earName
                                               true, // addEarResources
                                               "TestServlet40.war", // warName
                                               true, // addWarResources
                                               "TestServlet40.jar", // jarName
                                               true, // addJarResources
                                               "testservlet40.war.servlets", // packageNames
                                               "testservlet40.jar.servlets");

        installServerXml("jandexAppDefaultAppMgrDefault_server.xml");  // Default Jandex settings.  NOT using Jandex.
        
        SHARED_SERVER.startIfNotStarted();

        LOG.info("Setup : wait for message to indicate app has started");

        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation("TestServlet40");

        LOG.info("Setup : app has started, or so we believe");

    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (needToStopServer) {
            LOG.info("testCleanUp : stop server");
            SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W");
        } else {
            LOG.info("testCleanUp.  Not stopping server, since needToStopServer==false");
        }
    }

    /**
     * Copy the appropriate server.xml (server configuration)  for the test to be executed to
     * the root of the server directory.
     * 
     * @param sourceServerXml
     * @throws Exception
     */
    protected static void installServerXml(String sourceServerXml) throws Exception {

        final String serverRootDir = SHARED_SERVER.getLibertyServer().getServerRoot();
        final File serverXmlFile = new File(serverRootDir + "/server.xml");
        
        if (serverXmlFile.exists()) {
        	serverXmlFile.delete();
        }
        
        File serverConfigurationFile = new File(serverRootDir + "/serverConfigurations/" + sourceServerXml);
        FileUtils.copyFile(serverConfigurationFile, serverXmlFile); 
    }    
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void testAnnotationCacheCreatedAndIsActuallyUsed() throws Exception {
        
    	// Looking for a dir something like this: .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/annoCache
        File annoCacheDir = getAnnoCacheRoot();
        assertNotNull("Can't find 'annoCache' directory", annoCacheDir);
        assertTrue("annoCache directory does not exist.", annoCacheDir.exists());
        
        // Now with the application appended .../<SERVER_NAME>/workarea/org.eclipse.osgi/42/data/annoCache/APP_TestServlet40
        File applicationWorkArea = getAnnoCacheAppRoot();
        assertNotNull("Can't find application work area under " + applicationWorkArea, applicationWorkArea);
        assertTrue("annoCache directory does not exist.", applicationWorkArea.exists());    
        
        // Test if class.refs exists.  Assume cache created successfully if exists.
        File classRefsFile = findFile(applicationWorkArea, "class.refs");
        assertNotNull("Can't find class.refs.", classRefsFile);
        assertTrue("annoCache directory does not exist.", classRefsFile.exists());       
        
        // The app contains a servlet.  Verify we can access it.
        verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
        
        // Stop the server.
        SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W");
        
        // Remove the servlet from the cache, and restart the server.
        modifyCache();
        SHARED_SERVER.startIfNotStarted(false, false, false); 
        logAnnoTargetsFileModifiedTime();
        
        // If the server is using the annotation cache (as it is supposed to), then it should not be able to access the url,
        // because we removed it from the cache.  The server shouldn't be aware of the servlet.
        verifyBadUrl("/TestServlet40/SimpleTestServlet");
        
        // Stop the server here instead of in the cleanup method, because we need to allow the SRVE0190E
        // error message  to be in the logs. Meaning we were expecting the servlet access to fail.
        SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0014W", "SRVE0190E");
        needToStopServer = false;  // signal that server is already stopped
    }
    
    /**
     * Log the time that the anno.targets file was created under the WAR Module directory. (for debugging)
     * @throws Exception
     */
    public void logAnnoTargetsFileModifiedTime() throws Exception {
        File applicationWorkArea = getAnnoCacheAppRoot();
        String annoTargetsFileName = applicationWorkArea.getCanonicalPath() + "/MOD_%2FTestServlet40.war/seed/anno.targets";
        File annoTargetsFile = new File(annoTargetsFileName);
        LOG.info("File [ " + annoTargetsFile.getName() + " ] last modified [ " + convertTime(annoTargetsFile.lastModified()) + " ]");
    }
    
    
    public void modifyCache() throws Exception {
        LOG.info("In modifyApp()");

        File applicationWorkArea = getAnnoCacheAppRoot();
        
        String annoTargetsFileName = applicationWorkArea.getCanonicalPath() + "/MOD_%2FTestServlet40.war/seed/anno.targets";
        String tempFileName = annoTargetsFileName + ".temp";
        
        LOG.info("annoTargetsFileName=[ " + annoTargetsFileName + " ]");
        File annoTargetsFile = new File(annoTargetsFileName);
        BufferedReader targetsReader = new BufferedReader(new FileReader(annoTargetsFile)); 
        BufferedWriter tempFileWriter = null;
        FileWriter fw = null;
        try {

            fw = new FileWriter(tempFileName);
            LOG.info("new file name [ " + tempFileName + " ]");
            tempFileWriter = new BufferedWriter(fw);

            String line; 
            while ((line = targetsReader.readLine()) != null) {
               
            	// Skip line containing SimpleTestServlet and skip the line after that.
            	// This removes the class and webservlet annotation from cache.
                if (line.trim().equals("Class: testservlet40.jar.servlets.SimpleTestServlet")) {
                    LOG.info("skipping: " + line);
                    line = targetsReader.readLine();
                    LOG.info("skipping: " + line);
                } else {
                   LOG.info(line);
                   tempFileWriter.write(line + '\n');
                }
            } 

        } catch (IOException ioe) {
            LOG.info(ioe.getMessage());
            throw ioe;

        } finally {
            if (targetsReader != null) {
                targetsReader.close();
            }
            if (tempFileWriter != null) {
                tempFileWriter.close();
            }  
        }
        LOG.info("Calling rename");
        
        // 
        renameFile(new File(tempFileName), annoTargetsFile);
        
        LOG.info("RETURN File [ " + annoTargetsFile.getName() + " ] last modified [ " + convertTime(annoTargetsFile.lastModified()) + " ]");
       
        // File is renamed.  Cause a delay.  If file is rewritten, its time stamp should be later.

        //SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");

        //Date resultdate = new Date(System.currentTimeMillis());
        //System.out.println();
        //LOG.info("Sleeping [" + sdf.format(resultdate)  + "]" );
        //Thread.sleep(60000);
        //resultdate = new Date(System.currentTimeMillis());
        //LOG.info("Woke [ " +  sdf.format(resultdate)  + "]" );
        
    }
    
    public String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
        return format.format(date);
    }
    
    /**
     * rename oldFile to newFile
     * 
     * @param oldFile
     * @param newFile
     * @throws Exception
     */
    public void renameFile(File oldFile, File newFile) throws Exception {
    	if (newFile.exists()) {
    		newFile.delete();
    		if (newFile.exists()) {       
    			throw new IOException("Could not delete file [" + newFile.getAbsolutePath() + "]");
    		} else {
    			LOG.info("Deleted file [" + newFile.getName()  + "]");
    		}
    	} else {
    		LOG.info(newFile.getName() + " does not exist.  Suspicious, but were going to delete it anyway.");
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
    
    /*   Method to modify application  (renames file in JAR to  .class without expanding JAR)
    public void modifyApp() throws Exception{
        LOG.info("In modifyApp()");
        String serverRoot = SHARED_SERVER.getLibertyServer().getServerRoot(); 
        
        // !!! Modifying the app in the "expanded" dir doesn't help because the app is expanded from the EAR with 
        // every server restart.  So whatever changes we make, they are overwritten when the server restarts !!!
        // So to modify the app, you need to modify the EAR itself.  Which means expanding, modifying, and re-zipping.
        String jarPath = serverRoot + "/apps/expanded/TestServlet40.ear/TestServlet40.war/WEB-INF/lib/TestServlet40.jar";
        LOG.info("In modifyApp(): jarPath[ " + jarPath + " ]");
        Map<String, String> zip_properties = new HashMap<>();
        zip_properties.put("create", "false");
        URI zip_disk = URI.create("jar:file:" + jarPath);
        
        try (FileSystem zipfs = FileSystems.newFileSystem(zip_disk, zip_properties)) {
            LOG.info("About to access an entry from ZIP File");
           
            Path pathInZipFile  = zipfs.getPath("testservlet40/jar/servlets/SimpleTestServlet.hidden");
            LOG.info("got Path to: " + pathInZipFile);
          
            Path renamedZipEntry = zipfs.getPath("testservlet40/jar/servlets/SimpleTestServlet.class");
            LOG.info("got Path to: " + renamedZipEntry );
            
            LOG.info("About to rename an entry from [ " + pathInZipFile + "]" ); //.toUri() ); 
            
            Files.move( pathInZipFile, renamedZipEntry, StandardCopyOption.ATOMIC_MOVE);
            LOG.info("File successfully renamed");   
        } 
        
    }
    */
             
    public File getAnnoCacheAppRoot() {
        File annoCacheDir = getAnnoCacheRoot();
        File appDir = findDirectory(annoCacheDir, "APP_TestServlet40");
        LOG.info("getAppDirectory:       App Directory[" + appDir + "]");
        return appDir;
    }    
    
    public File getAnnoCacheRoot() {
        String osgiWorkAreaRoot = SHARED_SERVER.getLibertyServer().getOsgiWorkAreaRoot();
        LOG.info("getAnnoCacheDirectory: OSGI workarea[" + osgiWorkAreaRoot + "]");
        
        File annoCacheDir = findDirectory(new File(osgiWorkAreaRoot), "annoCache");
        LOG.info("getAnnoCacheDirectory: annoCache Dir[" + annoCacheDir + "]");
        return annoCacheDir;
    }
    
    public File findFile(File file, String searchFileName) {

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
    
    public File findDirectory(File file, String searchFileName) {
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

    /**
     * Request a simple servlet.
     *
     * @throws Exception
     */
    /*Test
    public void testSimpleServlet() throws Exception {

        this.verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
    }*/

    /**
     * Simple test to a servlet then read the header to ensure we are using
     * Servlet 4.0
     *
     * @throws Exception
     *             if something goes horribly wrong
     */
    /*Test
    public void testServletHeader() throws Exception {
        WebResponse response = this.verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // verify the X-Powered-By Response header
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/4.0", true, false);
    }
*/
    /**
     * Verifies that the ServletContext.getMajorVersion() returns 4 and
     * ServletContext.getMinorVersion() returns 0 for Servlet 4.0.
     *
     * @throws Exception
     */

    /*Test
    public void testServletContextMajorMinorVersion() throws Exception {
        this.verifyResponse("/TestServlet40/MyServlet?TestMajorMinorVersion=true", "majorVersion: 4");

        this.verifyResponse("/TestServlet40/MyServlet?TestMajorMinorVersion=true", "minorVersion: 0");
    } */
    
    // Returns a substring of the response body 
    // starting with beginText and 
    // ending with endText (if both beginText and endText are present)
    //
    // NOT USED IN ANY TEST 
    protected String parseResponse(WebResponse wr, String beginText, String endText) {
        String s;
        String body = wr.getResponseBody();
        int beginTextIndex = body.indexOf(beginText);
        if (beginTextIndex < 0)
            return "begin text, " + beginText + ", not found";
        int endTextIndex = body.indexOf(endText, beginTextIndex);
        if (endTextIndex < 0)
            return "end text, " + endText + ", not found";
        s = body.substring(beginTextIndex + beginText.length(), endTextIndex);
        return s;
    } 

}