/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import javax.servlet.ServletContext;

import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

@SuppressWarnings("unchecked")
public class MetaInfResourcesFileUtils {

	protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.util.MetaInfResourcesFileUtils";

	private String searchRoot;
	private File[] libBinFiles;
	boolean recurseDirs;
	private ZipFileResource metaInfResFile;
	private File normalFile;
    
	public MetaInfResourcesFileUtils(String docRoot) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"MetaInfResourcesFileUtils", "docRoot --> " + docRoot);
        }
        
        recurseDirs = true;
        searchRoot = docRoot+"/WEB-INF/lib";
    	File rootDir = new File(searchRoot);
		libBinFiles = rootDir.listFiles();
    } 
	
    public MetaInfResourcesFileUtils(ServletContext ctxt) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"MetaInfResourcesFileUtils", "ctxt --> " + ctxt);
        }
 
        // get the list and populate it if possible
        List<String> libBinPathList = ((IServletContext)ctxt).getWebAppConfig().getLibBinPathList();

        if (libBinPathList==null||libBinPathList.isEmpty()) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"MetaInfResourcesFileUtils", "libBinPathList is empty");
             }
            
             recurseDirs = true;
            
        } else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"MetaInfResourcesFileUtils", "libBinPathList is NOT empty");
            }
        		
            recurseDirs = false;
            libBinFiles = new File[libBinPathList.size()];
            int i=0;
            for (String libBinPath:libBinPathList){
                libBinFiles[i] = new File(libBinPath);
                i++;
            }
        }
    }
    
    public void findInMetaInfResource(String filename){
    	findInMetaInfResource(libBinFiles,filename);
    }

	public void findInMetaInfResource(File[] searchPath, String filename){
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.entering(CLASS_NAME,"findInMetaInfResource", new Object [] {searchPath,filename});
        }
    	
        int fc = 0;
        normalFile=null;
        metaInfResFile=null;

        if (searchPath==null){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.exiting(CLASS_NAME,"findInMetaInfResource");
            }
        	return;
        }
        // for each file, output a table row
        while (fc < searchPath.length)
        {
        	File curFile = searchPath[fc];
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"findInMetaInfResource", "searching->"+curFile);
            }
        	
        	
            if (curFile.isDirectory())
            {
            	if (recurseDirs){
            		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME,"findInMetaInfResource", "recursing to find more jar files");
                    }
            		File[] subDirSearchPath = curFile.listFiles();
            		findInMetaInfResource(subDirSearchPath,filename);
            	} else {
            		// loose config, look in exploded META-INF/resources
            		try {
						String fullPath = curFile.getCanonicalPath()+"/META-INF/resources"+filename;
						if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
	                        logger.logp(Level.FINE, CLASS_NAME,"findInMetaInfResource", "look for file[{0}] in loose config", new Object [] {fullPath});
	                    }
						normalFile = new File(fullPath);
						if (!normalFile.exists()) {
							if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
		                        logger.logp(Level.FINE, CLASS_NAME,"findInMetaInfResource", "file does not exist");
		                    }
							normalFile=null;
						}
					} catch (IOException e) {
						Object[] args = { e };
			            logger.logp(Level.SEVERE, CLASS_NAME,"findInMetaInfResource", "Engine.Exception.[{0}]", args);
					}
            	}
            	
            } else {
            	String curFileName = curFile.getName();
            	if (curFileName.toLowerCase().endsWith(".jar")){
            		JarFile jarFile = null;
            		try {
						jarFile = new JarFile(curFile);
						String metaInfRelPath = "META-INF/resources"+filename;
						ZipEntry zipEntry = jarFile.getEntry(metaInfRelPath);
						if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
				            logger.logp(Level.FINE, CLASS_NAME,"findInMetaInfResource", "curFileName-->"+curFileName+", zipEntry --> " + zipEntry);
						if (zipEntry!=null){
							String fullURL = "jar:" + curFile.toURI().toURL().toString()+"!/"+metaInfRelPath;
							URL url=null;
							try {
								url = new URL(fullURL);
							} catch (MalformedURLException e) {
								Object[] args = { e };
					            logger.logp(Level.SEVERE, CLASS_NAME,"findInMetaInfResource", "Engine.Exception.[{0}]", args);
							}
							if (url!=null) {
								metaInfResFile = new ZipFileResource(curFile,metaInfRelPath,url);
								
							}
						}
						
					} catch (IOException e) {
						logException("findInMetaInfResource",filename, jarFile, e);
					} finally {
						if (jarFile!=null){
							try {
								jarFile.close();
							} catch (IOException e) {
								logException("findInMetaInfResource",filename, jarFile, e);
							}
						}
					}
            	}
            }
            if (normalFile!=null||metaInfResFile!=null	)
            	return;
        	fc++;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.exiting(CLASS_NAME,"findInMetaInfResource", new Object [] {normalFile,metaInfResFile});
        }
    }

	private void logException(String methodName, String filename, JarFile jarFile, IOException e) {
		String jarFileName = null;
		if (jarFile!=null)
			jarFileName = jarFile.getName();
		else
			jarFileName = "";
		Object[] args = { e ,jarFileName,filename};
		logger.logp(Level.SEVERE, CLASS_NAME,methodName, "ioexception.searching.jar.for.resource", args);
	}
    
   
    public Set<String> getResourcePaths(String filename){
    	Set setOfUrls = new HashSet<String> ();
    	return getResourcePaths(libBinFiles,"META-INF/resources"+filename,setOfUrls);
    }
    
    public Set<String> getResourcePaths(File[] searchPath,String filename, Set setOfUrls){
    	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "searchPath-->"+Arrays.toString(searchPath)+", filename --> " + filename);
		
    	
        int fc = 0;

        // for each file, output a table row
        if (searchPath==null){
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "unable to get file listing for directory");
        	return setOfUrls;
        }
        
        // Create search Strings for searching jars.  
        // Do early so we don't re-do it for every jar in the search path
		String jarSearchNameWithoutEndingSlash = filename;
		String jarSearchNameWithEndingSlash = filename;
    	if (jarSearchNameWithoutEndingSlash.endsWith("/")) {
    		jarSearchNameWithoutEndingSlash = jarSearchNameWithoutEndingSlash.substring(0,jarSearchNameWithoutEndingSlash.length()-1);
    	} else {
    		jarSearchNameWithEndingSlash += "/";
    	}
		
        while (fc < searchPath.length)
        {
        	File curFile = searchPath[fc];
        	
        	
            if (curFile.isDirectory())
            {
            	if (recurseDirs){
            		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                        logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "recursing to find more jar files");
                    }
            		File[] subDirSearchPath = curFile.listFiles();
            		getResourcePaths(subDirSearchPath,filename,setOfUrls);
            	} else {
            		// loose config, look in exploded META-INF/resources
            		try {
            			String basePath = curFile.getCanonicalPath();
            			String fullTestPath = basePath+"/"+filename;
						if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
	                        logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "looking for [{0}] in basePath[{1}] loose config", new Object [] {fullTestPath,basePath});
	                    }
						File tempDir = new File(fullTestPath);
						if (tempDir.exists()) {
							int fullRootLength = basePath.length()+19;
							if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
		                        logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "found dir in loose config");
		                    }
							File[] children = tempDir.listFiles();
							for (int i=0;i<children.length;i++){
								String pathToAdd = children[i].toString().substring(fullRootLength);
								if (File.separatorChar!='/')
									pathToAdd = pathToAdd.replace(File.separatorChar, '/');
								if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
			                        logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "loose config child[{0}], pathToAdd[{1}]", new Object [] {children[i],pathToAdd});
			                    }
	                                                        if (children[i].isDirectory()) {
	                                                            pathToAdd = pathToAdd + "/";
	                                                        }
								setOfUrls.add(pathToAdd);
							}
						}
					} catch (IOException e) {
						Object[] args = { e };
			            logger.logp(Level.SEVERE, CLASS_NAME,"getResourcePaths", "Engine.Exception.[{0}]", args);
					}
            	}
            } else {
            	
            	String curFileName = curFile.getName();
            	
            	
            	if (curFileName.toLowerCase().endsWith(".jar")){ //handle .JAR, .jAR, .JAr etc
            		JarFile jarFile = null;
            		try {
            			                    	
						jarFile = new JarFile(curFile);
						ZipEntry zipEntry=jarFile.getEntry(jarSearchNameWithoutEndingSlash);
						if (zipEntry==null) {
							// no use searching this jar, it doesn't contain the required directory
							if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
					            logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "curFileName-->"+curFileName+", no suitable entries for :" + jarSearchNameWithoutEndingSlash);
							// no use looking here
						} else {
													    
							Enumeration<JarEntry> entries = jarFile.entries(); //get all of the entries in the jar
						    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
				                logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "curFileName-->"+curFileName+", entries --> " + entries);
						    
						    String origName = null;
						    
						    while (entries.hasMoreElements()){  //traverse all of the entries in the jar
							    JarEntry currentEntry = entries.nextElement();
							    
							    origName = currentEntry.getName(); //jar entries don't start with /, so add it on for comparison

							    //if the length of parse name is not greater than file name, then its an exact match and we shouldn't return it
							    if (origName.length()>jarSearchNameWithEndingSlash.length() && origName.startsWith(jarSearchNameWithEndingSlash)){ 
								
								    // if there is a "/" the first one should be at the end of the filename so for example
								    // "subdir/" would meet this criteria whereas "subdir/fileA" would not.
							    	int slashIndex = origName.indexOf('/',jarSearchNameWithEndingSlash.length());
								    if (slashIndex==-1 || slashIndex == (origName.length()-1)) {
								    	
									    //remove off the META-INF/resources portion to match ServletContext.getResourcePaths javadocs
									    setOfUrls.add(origName.substring(18));
								    }
							    }    
							}
						}

            		} catch (IOException e) {
						logException("getResourcePaths",filename, jarFile, e);
					} finally {
						if (jarFile!=null){
							try {
								jarFile.close();
							} catch (IOException e) {
								logException("getResourcePaths",filename, jarFile, e);
							}
						}
					}
            	}
            }
        	fc++;
        }
        return setOfUrls;
    }

	public InputStream getInputStream() {
		try {
			if (metaInfResFile!=null)
				return metaInfResFile.getIS();
			else if (normalFile!=null)
				return new FileInputStream(normalFile);
		} catch (IOException e) {
			Object[] args = { e };
            logger.logp(Level.SEVERE, CLASS_NAME,"getInputStream", "Engine.Exception.[{0}]", args);
		}
		return null;
	}

	public File getMatchedStaticFile() {
		return normalFile;
	}

	public URL getURL() {
		try {
			if (metaInfResFile!=null)
				return metaInfResFile.getURL();
			else if (normalFile!=null)
				return normalFile.toURI().toURL();
		} catch (MalformedURLException e) {
			Object[] args = { e };
            logger.logp(Level.SEVERE, CLASS_NAME,"getURL", "Engine.Exception.[{0}]", args);
		}
		return null;
	}

	public ZipFileResource getMatchedZipFile() {
		return metaInfResFile;
	}

	public String getFilePath() {
		try {
			if (normalFile!=null)
				return normalFile.getCanonicalPath();
			else if (metaInfResFile!=null)
				return metaInfResFile.getMatch().getCanonicalPath();
		} catch (IOException e) {
			Object[] args = { e };
            logger.logp(Level.SEVERE, CLASS_NAME,"getFilePath", "Engine.Exception.[{0}]", args);
		}
		return null;
	}

    
}
