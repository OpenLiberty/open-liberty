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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

class FileResource implements ExtDocRootFile{
	private static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.util");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.util.FileResource";
	
	private File file;
	protected FileResource ( File file){
		this.file = file;
	}
	public InputStream getIS() throws IOException{
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) 
			logger.logp(Level.FINE, CLASS_NAME,"getIS", "return FileResource inputstream file -->" + file);
		return new FileInputStream (file);
	}
	
	public long getLastModified(){
	    return file.lastModified();
	}
	
	public String getPath(){
	    return file.getAbsolutePath();
	}
	public File  getMatch(){
		return file;
	}

}