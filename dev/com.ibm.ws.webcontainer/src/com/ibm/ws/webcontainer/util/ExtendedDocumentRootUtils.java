/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletContext;

import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.util.WSUtil;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

@SuppressWarnings("unchecked")
public class ExtendedDocumentRootUtils {

	private static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.util.ExtendedDocumentRootUtils";

    private static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    
    //private Vector searchPath = new Vector();
    private List<EDRPathEntry> searchPath = new ArrayList<EDRPathEntry>();
    
    private boolean useContentLength = false;
    
    private Container earContainer = null;

    private ExtDocRootFile extDocRootFile;
    
    public ExtDocRootFile getExtDocRootFile() {
		return extDocRootFile;
	}

	public ExtendedDocumentRootUtils(ServletContext ctxt, String extendedDocumentRoot) {
        if (extendedDocumentRoot != null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"ExtendedDocumentRootUtils", "extendedDocumentRoot --> " + extendedDocumentRoot);
            }
            /*
             * EDRs can contain entries relative to the ear, so we need to look for those inside of the ear's container
             */
            boolean containerWithoutEar = false;
            if(ctxt instanceof WebApp){
                Container moduleContainer = ((WebApp)ctxt).getModuleContainer();
                if(moduleContainer != null){
                    earContainer = moduleContainer.getEnclosingContainer();
                    if (earContainer==null) {
                        containerWithoutEar=true;
                    }
                }
            }
            createSearchPath(ctxt, extendedDocumentRoot, containerWithoutEar);
        }
    }

    public ExtendedDocumentRootUtils(String baseDir, String extendedDocumentRoot) {
        if (extendedDocumentRoot != null) {
            if (baseDir != null) {
                baseDir = baseDir.replace('\\', '/');
                if (baseDir.endsWith("/") == false) {
                    baseDir = baseDir + "/";
                }
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME,"ExtendedDocumentRootUtils", "baseDir --> ", baseDir);
                logger.logp(Level.FINE, CLASS_NAME,"ExtendedDocumentRootUtils", "extendedDocumentRoot --> " + extendedDocumentRoot);
            }
            createSearchPath(baseDir, extendedDocumentRoot, false);
        }

    }

    public boolean searchPathExists() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            String result = searchPath.isEmpty() == false ? "true" : "false";
            logger.logp(Level.FINE, CLASS_NAME,"searchPathExists", " ", result);
        }
        return (!searchPath.isEmpty());
    }

    private void createSearchPath(ServletContext ctx, String extendedDocumentRoot, boolean containerWithoutEar) {
        //If we have a container, we can't call getRealPath so we need to operate inside of it
        if(earContainer != null){
            createSearchPath("", extendedDocumentRoot, false);
            return;
        } else if (containerWithoutEar) {
            createSearchPath("", extendedDocumentRoot, containerWithoutEar);
            return;
        }
        String baseDir = ctx.getRealPath("/../");
        createSearchPath(baseDir, extendedDocumentRoot, false);
    }

    private void createSearchPath(String baseDir, String extendedDocumentRoot, boolean containerWithoutEar) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME,"createSearchPath", "baseDir --> " + baseDir);
            logger.logp(Level.FINE, CLASS_NAME,"createSearchPath", "extendedDocumentRoot --> " + extendedDocumentRoot);
        }

        StringTokenizer st = new StringTokenizer(extendedDocumentRoot, ",");

        while (st.hasMoreTokens()) {
            try {

                String currentSearchLocation = (st.nextToken().trim());
                if (currentSearchLocation != null) {

                    if ((isWindows && currentSearchLocation.indexOf(":") == 1) || (currentSearchLocation.startsWith("/"))) {
                        File f = new File(currentSearchLocation);
                        EDRPathEntry entry = new EDRPathEntry(f.toString(), false);
                        searchPath.add(entry);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"createSearchPath", "add to searchPath --> " + f.toString());

                    }
                    else {
                        //If we have a container, add a search path entry relative to that container
                        if (earContainer==null && containerWithoutEar) {
                            continue; //don't add relative paths when there isn't an ear.
                        }
                        if(earContainer != null){
                            EDRPathEntry entry = new EDRPathEntry(currentSearchLocation, true);
                            searchPath.add(entry);
                        } else {
                            File realPath = new File(baseDir + currentSearchLocation);
                            String canonicalPath = realPath.getCanonicalPath();
                            EDRPathEntry entry = new EDRPathEntry(canonicalPath, false);
                            searchPath.add(entry);
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME,"createSearchPath", "add to searchPath --> " + canonicalPath);
                        }

                    }
                }
            }
            catch (IOException io) {
                logger.logp(Level.SEVERE, CLASS_NAME,"createSearchPath", "exception.creating.search.path",io);
            }

        }

    }
    
    private String[] parseOnExtension(String input, String extension) {
        // To aid performance the extension MUST BE lower case, and all matching to the input will be done
        // in lower case.
        
        // This routine assumes a file separator of "/" is use, in that it assumes the sting passed in is a standard
        // java formatted (not OS dependent) full path name.
        // look for the "/" as part of the match so directory names like  a.war.xdir/abc is not thought to be a war.
        
        // do a fast check to get out of here quickly most of the time
        String inLower = input.toLowerCase();
        if (inLower.indexOf(extension) < 0) {
            return null;
        }
        
        String parser1 = "." + extension + "/";
        
        // if the extension exist in the string right before a directory delimiter, assume it is a war match
        if (inLower.indexOf(parser1) >= 0) {
        
            return input.split("/");

        } else {
            
            // check if string is ending in .extension, so no direcotry delimiter at the end
            String parser2 = "." + extension;
            int i = inLower.indexOf(parser2);
            if ( i >= 0) {
                int endIndex = inLower.length() - parser2.length();
                if (i == endIndex) { 
                    return input.split("/");
                }    
            }
            
            return null;
        }
    }

    public void handleExtendedDocumentRoots(String filename) throws FileNotFoundException, IOException {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "filename --> " + filename);
        boolean foundMatch = false;

        Iterator<EDRPathEntry> i = searchPath.iterator();

        search : while (i.hasNext()) {

            EDRPathEntry pathEntry = i.next();
            
            String _path = pathEntry.getPath();

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots","looking at entry:", _path);

            if(pathEntry.inContainer() && earContainer != null){
                Entry subContainerEntry = null;
                
                //If there is a war, we need to walk the path
                String[] sections = parseOnExtension(_path, "war");

                if (sections != null) {
                    Container currentContainer = earContainer;
                    Entry currentEntry = null;
                    for(int j = 0; j < sections.length; j++){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots","looking at section:", sections[j]);
                        
                        currentEntry = currentContainer.getEntry(sections[j]);

                        //Need to break out of outer loop here
                        if(currentEntry == null){
                            continue search;
                        }
                        try{
                            Container currentEntryContainer = currentEntry.adapt(Container.class);
                            currentContainer = currentEntryContainer;
                        } catch (UnableToAdaptException e){
                            //Current entry wasn't a container, EDRPath wasn't valid
                            continue search;
                        }
                    }
                    //If we get this far, the last container we walked into should be the subContainer
                    subContainerEntry = currentEntry;

                }
                else {

                    subContainerEntry = earContainer.getEntry(_path);
                }

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots","working with subContainerEntry of: ", subContainerEntry);

                if(subContainerEntry != null){
                    try{
                        Container subContainer = subContainerEntry.adapt(Container.class);
                        if(filename.startsWith("/")){
                            filename = filename.substring(1);
                        }
                        Entry potentialMatch = subContainer.getEntry(filename);
                        if(potentialMatch != null){
                            foundMatch = true;
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "found match in container --> " + potentialMatch);
                            this.extDocRootFile = new EntryResource(potentialMatch);
                            break search;
                        }
                    } catch (UnableToAdaptException e){
                        //no-op, keep looking for the file elsewhere
                    }
                }
            }
            else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots","Path entry not in container");

                String currDocumentRoot = pathEntry.getPath();
                File currFile = new File(currDocumentRoot);

                if (currFile.isDirectory()) {
                    File tmpFile = new File(currFile, filename);
                    if (tmpFile.exists()) {
                        foundMatch = true;
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "found match in directory --> " + tmpFile.toString());
                        handleCaseSensitivityCheck(tmpFile.toString(), filename);

                        this.extDocRootFile = new FileResource (tmpFile);

                        useContentLength = true;
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "useContentLength --> " + useContentLength);
                        break search;
                    }

                }
                else if (currFile.exists()) {
                    ZipFile zip = new ZipFile(currFile);
                    ZipEntry zEntry = zip.getEntry(filename.substring(1).replace('\\', '/'));
                    if (zEntry != null) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                            logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "found match in zip or jar file --> " + currFile.toString());
                        foundMatch=true;


                        String fullURL = "jar:" + currFile.toURI().toURL().toString()+"!"+filename.replace('\\', '/');
                        URL url=null;
                        try {
                            url = new URL(fullURL);
                        } catch (MalformedURLException e) {
                            logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "MalformedURLException for URL : " + fullURL);
                        }

                        this.extDocRootFile = new ZipFileResource (currFile, zEntry.getName(),url);


                        zip.close();

                        break search;
                    }

                    else {
                        zip.close();
                    }
                }
            }
        }
        if (!foundMatch) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"handleExtendedDocumentRoots", "unable to locate resource --> " + filename);
            throw new FileNotFoundException(filename);
        }

    }
    
    public InputStream getInputStream() /*throws FileNotFoundException, IOException */ {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) logger.logp(Level.FINE, CLASS_NAME,"getInputStream", "getInputStream for ExtendedDocumentRoot this -->" + this);
          try {
               return this.extDocRootFile.getIS();
          }

          catch (Exception e) {
               return null;
          }
    }

   /* Avoid this if at all possible in Liberty.
     public File getMatchedFile() {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) logger.logp(Level.FINE, CLASS_NAME,"getMatchedFile", "file --> [" +  extDocRootFile.getMatch() +"]");
          return  extDocRootFile.getMatch();
    }*/

    public long getLastModifiedMatchedFile() {
          if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) logger.logp(Level.FINE, CLASS_NAME,"getLastModifiedMatchedFile", "file --> [" +  (( extDocRootFile ==null)?0: extDocRootFile.getLastModified())+"]");
	  return(( extDocRootFile  ==null)?0: extDocRootFile.getLastModified());
    }

    public boolean useContentLength() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE, CLASS_NAME,"useContentLength", "length --> [" + useContentLength + "]");
        return useContentLength;
    }

    public ZipFile getMatchedZipFile() {
        if (this.extDocRootFile instanceof ZipFileResource) {
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            	logger.logp(Level.FINE, CLASS_NAME,"getMatchedZipFile", "is zip file");
             return ((ZipFileResource) this.extDocRootFile).getZipFile();
        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
        	logger.logp(Level.FINE, CLASS_NAME,"getMatchedZipFile", "not zip file");

        return null;
    }
    
    public ZipEntry getMatchedEntry() {
        if (this.extDocRootFile instanceof ZipFileResource) {
        	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            	logger.logp(Level.FINE, CLASS_NAME,"getMatchedEntry", "is zip file");
             return ((ZipFileResource) this.extDocRootFile).getZipEntry();
        } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
        	logger.logp(Level.FINE, CLASS_NAME,"getMatchedEntry", "not zip file");

        return null;
    }
    
    public Set<String> getResourcePaths(String filename) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.entering(CLASS_NAME,"getResourcePaths", filename);
        
    	Set paths = new HashSet<String> ();
    	
    	filename = WSUtil.resolveURI(filename.trim());
    	
        boolean isRootDirectory = filename.equals("/");
    	if (!isRootDirectory && filename.startsWith("/"))
            filename = filename.substring(1);
    	
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.logp(Level.FINE,CLASS_NAME,"getResourcePaths", "resolved fileName : " + filename);
   	
        Iterator<EDRPathEntry> it = searchPath.iterator();

        while (it.hasNext()) {
            EDRPathEntry edrEntry = it.next();

            if(edrEntry.inContainer() && earContainer != null){
                Entry subContainerEntry = earContainer.getEntry(edrEntry.getPath());
                if(subContainerEntry != null){
                    try {
                        Container subContainer = subContainerEntry.adapt(Container.class);
                        Container resourceContainer = null;
                        if(filename.equals("/")){
                            resourceContainer = subContainer;
                        } else {
                            Entry resourceEntry = subContainer.getEntry(filename);    
                            if(resourceEntry != null){
                                resourceContainer = resourceEntry.adapt(Container.class);
                            }
                        }
                        if (null != resourceContainer) {
                            Iterator<Entry> entryIt = resourceContainer.iterator();
                            while (entryIt.hasNext()) {

                                Entry entry = entryIt.next();
                                String path = entry.getPath();

                                Container directory = entry.adapt(Container.class);
                                boolean isDir = (entry.getSize() == 0 && directory != null);
                                if (isDir) {
                                    if (path.endsWith("/") == false) {
                                        path += "/";
                                    }
                                }

                                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                                    logger.logp(Level.FINE, CLASS_NAME, "getResourcePaths", "EDR {0}", edrEntry.toString());
                                    logger.logp(Level.FINE, CLASS_NAME, "getResourcePaths", "ENTRY {0} isDir {1}", new Object[]{entry.getPath(), isDir});
                                }

                                if (edrEntry.inContainer() && path.contains(edrEntry.getPath())) {
                                    path = path.substring(edrEntry.getPath().length() + 1);
                                }

                                paths.add(path);
                                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                                    logger.logp(Level.FINE, CLASS_NAME, "getResourcePaths", "added " + path);
                            }
                        }
                    } catch (UnableToAdaptException e) {
                        //no-op, continue looking for resource paths elsewhere. // add trace
                    }
                }
            }
            else{
                String currDocumentRoot = edrEntry.getPath();
                File currFile = new File(currDocumentRoot);

                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE,CLASS_NAME,"getResourcePaths", "check document root : " + currDocumentRoot);

                if (currFile.isDirectory()) {

                    // Don't search a directory with trailing file delimiter as it will return no hits
                    // even if there is a directory of the searchName
                    String searchName = filename;
                    while (searchName.endsWith("/")) {
                        searchName = searchName.substring(0,searchName.length()-1);
                    }


                    File tmpFile = null;
                    if (!isRootDirectory) {
                        tmpFile  = new File(currFile, searchName);
                    } else {
                        tmpFile  = currFile;
                    }	

                    // if the search file is a directory get a list of its contents. 
                    if (tmpFile.isDirectory()) {

                        try {

                            if (!isRootDirectory) {
                                handleCaseSensitivityCheck(tmpFile.toString(), searchName);
                            }    

                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "found match in directory --> " + tmpFile.toString());

                            // list all of the files and directories in the search directory
                            java.io.File[] fileList = tmpFile.listFiles();

                            if (fileList != null) {
                                for (int i = 0; i < fileList.length; i++) {
                                    String resourcePath = fileList[i].getPath();
                                    resourcePath = resourcePath.substring(currFile.toString().length());
                                    resourcePath = resourcePath.replace('\\', '/');

                                    // if the resource is a directory append a trailing file delimiter
                                    if (fileList[i].isDirectory()) {
                                        if (resourcePath.endsWith("/") == false) {
                                            resourcePath += "/";
                                        }
                                    }

                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                        logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "add path --> " + resourcePath);

                                    paths.add(resourcePath);
                                }
                            }
                        } catch (Exception e) {
                            // look in next directory
                        }                    
                    } 

                } else if (currFile.exists()) {

                    // search file was not a directory so assume it is a zip file            	
                    try {
                        ZipFile zip = new ZipFile(currFile);
                        ZipEntry zipEntry=null;
                        String rootEntry = null;

                        // take off any leading file delimiter and add a trailing file delimiter if needed
                        // the trailing slash makes sure we get a directory 
                        if (!isRootDirectory) {
                            if (!filename.endsWith("/"))
                                filename += "/";

                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "get Zip entry for  --> " + filename);

                            zipEntry = zip.getEntry(filename);

                            if (zipEntry!=null && zipEntry.isDirectory())
                                rootEntry = zipEntry.toString();

                        } else {
                            rootEntry = "";
                        }

                        if (rootEntry!=null) {

                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "found match in zip file --> " + currFile.toString());

                            Enumeration zipEntries = zip.entries(); //get all of the entries in the jar

                            while (zipEntries.hasMoreElements()){  //traverse all of the entries in the zip
                                ZipEntry currentZipEntry = ((ZipEntry)zipEntries.nextElement());
                                String currentEntry = currentZipEntry.toString();
                                // we only want files and directories directly under in the root (not sub-directories)
                                if (currentEntry.startsWith(rootEntry)) {

                                    String subEntry = currentEntry.substring(rootEntry.length()).replace('\\', '/');

                                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                        logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "check -->"  + subEntry);

                                    if (!subEntry.equals("")) {

                                        int slashIndex = subEntry.indexOf("/");

                                        if (slashIndex==-1 || slashIndex==subEntry.length()-1) {
                                            if (currentZipEntry.isDirectory() && !currentEntry.endsWith("/")) {
                                                currentEntry += "/";
                                            }
                                            if (!currentEntry.startsWith("/"))
                                                currentEntry = "/" + currentEntry;
                                            paths.add(currentEntry);
                                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                                                logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "add path --> " + currentEntry);
                                        }
                                    }
                                }    						
                            }
                        }    

                        zip.close();
                    } catch (Exception e) {
                        // ignore	
                    }
                } else if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"getResourcePaths", "EDR not not found --> " + currFile.toString());		
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.exiting(CLASS_NAME,"getResourcePaths", paths);
    	
    	return paths;
    }


    private void handleCaseSensitivityCheck(String path, String strippedPathInfo) throws FileNotFoundException, IOException {
        // 94578, "Case Sensitive Security Matching bug":  On Windows and as400 only, filename of
        //         requested file must exactly match case or we will throw FNF exception.
        if (com.ibm.ws.util.FileSystem.isCaseInsensitive) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                logger.logp(Level.FINE, CLASS_NAME,"handleCaseSensitivityCheck", "file system is case insensitive");
            File caseFile = new File(path);
            if (!com.ibm.ws.util.FileSystem.uriCaseCheck(caseFile, strippedPathInfo)) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"handleCaseSensitivityCheck", "failed for --> [" + path + "]");
                throw new FileNotFoundException(path);
            }
        }

    }
    
}
