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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class ZipFileResource implements ExtDocRootFile{
	private static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.util.ZipFileResource";
	
	private String entry;
	private File jarFile;
	private URL url;
	public ZipFileResource (File jarFile, String entry){
		this.entry = entry;
		this.jarFile = jarFile;
	}
	
	public ZipFileResource (File jarFile, String entry, URL url){
		this.entry = entry;
		this.jarFile = jarFile;
		this.url = url;
	}
	
	public InputStream getIS() throws IOException {
		ZipFile zip = new ZipFile (jarFile);
		ZipEntry zEntry = zip.getEntry(entry);
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
			logger.logp(Level.FINE, CLASS_NAME,"getIS", "return ZipFileResource inputstream zip -->" + jarFile + " entry -->" + entry);
		return zip.getInputStream(zEntry);
	}
	
	public File  getMatch(){
		return jarFile;
	}
	
	public long getLastModified(){
	    return jarFile.lastModified();
	}
	
	public String getPath(){
	    return jarFile.getAbsolutePath();
	}

            public ZipFile getZipFile () {
                 try {
                      return new ZipFile (this.jarFile);
                 }

                 catch (Exception e) {
                      return null;
                 }
            }

            public ZipEntry getZipEntry () {
                 try {
                      return new ZipFile (this.jarFile).getEntry (this.entry);
                 }

                 catch (Exception e) {
                      return null;
                 }
            }
            
            public URL getURL() {
    			return url;
    		}
}