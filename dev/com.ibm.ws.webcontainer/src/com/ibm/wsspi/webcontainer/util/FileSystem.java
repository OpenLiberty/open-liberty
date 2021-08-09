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
package com.ibm.wsspi.webcontainer.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.webcontainer.WCCustomProperties; //PM10362
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.ws.util.WSUtil;
import com.ibm.ws.webcontainer.WebContainer;

/**
*
* @ibm-private-in-use
* 
*/

public class FileSystem {

	// 94578 Security Defect. 
	// Prior to this fix, WebSphere on Windows Servers was not checking case
	// sensitivity when serving JSP's or static html files.
	// If a file was secured, as long as the browser request used different 
	// capitalization and/or lowercase than the secured file, the file would be
	// served without being challenged.
	// Not a problem on UNIX type systems since the OS handles is case sensitive.

    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.wsspi.webcontainer.util.FileSystem";

    private static boolean tolerateSymLinks; 
	private static int symLinkCacheSize; 
    private static Map foundMatches; 
    
    private static final String MAP_VALUE = "";
    
	static {
		tolerateSymLinks = Boolean.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.toleratesymboliclinks")).booleanValue(); 
        if (tolerateSymLinks) {
    		symLinkCacheSize = Integer.valueOf(WebContainer.getWebContainerProperties().getProperty("com.ibm.ws.webcontainer.symboliclinkscachesize", "1000")).intValue();	
            if (symLinkCacheSize>0)
    		    foundMatches = Collections.synchronizedMap(new LRUCache(symLinkCacheSize));
            else foundMatches = null;
        }
    }
        
	public static boolean uriCaseCheck (File file, String matchString) throws java.io.IOException {
		return uriCaseCheck(file,matchString,true,true);
	}
	
	public static boolean uriCaseCheck (File file, String matchString, boolean checkWEBINF) throws java.io.IOException {
		return uriCaseCheck(file,matchString,checkWEBINF,true);
	}
		
	public static boolean uriCaseCheck (File file, String matchString, boolean checkWEBINF, boolean checkMETAINF) throws java.io.IOException {
		
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
            logger.entering(CLASS_NAME,"uriCaseCheck","file canpath="+file.getCanonicalPath()+", matchString="+matchString + ", checkWEBINF="+checkWEBINF);
		
		boolean result=false;
		
		
		// begin 154268 
		matchString = WSUtil.resolveURI(matchString);
		// (removed since it is changed to "//" inside of resolveURI 
		// matchString = matchString.replace ('/', '\\');	// change string from url format to Windows format
		// end 154268
	    // As per spec (servlet 2.4), deny access to WEB-INF

		String upperMatchString = new String(matchString.toUpperCase());
		
        if ( checkWEBINF && (upperMatchString.startsWith("/WEB-INF/") || upperMatchString.equals("/WEB-INF")) )
            result = false;
        else if ( checkMETAINF && upperMatchString.startsWith("/META-INF/") || upperMatchString.equals("/META-INF") )
        	result =  false;
        else {
		    matchString = matchString.replace ('/', File.separatorChar);
		    String canPath = file.getCanonicalPath();
            int matchStringLength = matchString.length();
	    //PM10362 Start
            if( WCCustomProperties.TOLERATE_LOCALE_MISMATCH_FOR_SERVING_FILES){ 
        	// Case: The System Locale encoding(used by JDK to get filename) is different then the encoding of the filename in request URI.            				
            	if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&& logger.isLoggable (Level.FINE)) 
                	logger.logp(Level.FINE, CLASS_NAME,"uriCaseCheck"," tolerateLocaleMismatchForServingFiles Custom property is set"); 
            	
        	    result = true;
            }//PM10362 End	
            else if (canPath.regionMatches(canPath.length() - matchStringLength, matchString, 0, matchStringLength)) {
                result=true;
            } else if (tolerateSymLinks) {
            	            	
    	        if (foundMatches != null && foundMatches.get(matchString) != null) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
                    	logger.logp(Level.FINE, CLASS_NAME,"uriCaseCheck"," : found in Cache");    	        	
    	        	result =true;
    	        } else {            	            	
            	    String absPath = file.getAbsolutePath();
        	        String appRootDir = absPath.substring(0,(absPath.length() - matchStringLength));
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
                        logger.logp(Level.FINE, CLASS_NAME,"uriCaseCheck","appRoot dir="+appRootDir);
        	        File appRoot = new File(appRootDir);
        	        if (walkPath(appRoot,matchString)) {
                        if (foundMatches != null) {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
                                logger.logp(Level.FINE, CLASS_NAME,"uriCaseCheck","add to Cache :" + matchString + ", Cache size: current = " +foundMatches.size()+ ", max = "+symLinkCacheSize);
                            foundMatches.put(matchString,MAP_VALUE);
                        }    
                        result = true;
        	        }    
        	    }  
            }
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
            logger.exiting(CLASS_NAME,"uriCaseCheck : result="+result);
		return result;
		
		
	} // end 94578 Security Defect.
	
    public static boolean walkPath(File appRoot, String relativePath) throws IOException {
        // Walk the path from the app root
        String[] relativePathElements = relativePath.replace(File.separatorChar, '/').split("/");
        File currentPath = new File(appRoot.getCanonicalPath());
        for (int i = 0; i < relativePathElements.length; i++) {        	
    		// Handle urls which start with a "/" in whoich case split  
    		if (!relativePathElements[i].equals("")) {
                String[] entries = currentPath.list(new StrictFileNameFilter(relativePathElements[i]));
                if (entries==null || entries.length != 1) {
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
                        logger.logp(Level.FINE, CLASS_NAME,"uriCaseCheck",relativePathElements[i] + " not found.");
                    return false;
                }
                currentPath = new File(currentPath, relativePathElements[i]);
    		}    
        }
        return true;
    }

    public final static class StrictFileNameFilter implements FilenameFilter {
        String fileName;

        StrictFileNameFilter(String fileName) {
            this.fileName = fileName;
        }

        public boolean accept(File file, String name) {
           return name.equals(fileName);
        }
    }
    
    public final static class LRUCache extends LinkedHashMap {
    	
    	private int _maxEntries;
    	
    	public LRUCache(int maxEntries){
    		super(maxEntries,(float)0.75,true);
    		_maxEntries = maxEntries;
    	}
    	
    	protected boolean removeEldestEntry(Map.Entry oldest){
    		return size() > _maxEntries;
    	}
    }
}


