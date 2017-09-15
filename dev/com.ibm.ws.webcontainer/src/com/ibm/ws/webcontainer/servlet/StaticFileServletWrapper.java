/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;


public class StaticFileServletWrapper extends FileServletWrapper {
    private File file;
    RandomAccessFile raf = null;
    private long fileSize = -1; // PM92967
    
    public StaticFileServletWrapper(IServletContext parent, DefaultExtensionProcessor parentProcessor, File file)
    {
        super(parent, parentProcessor);
        this.file = file;
    }
    
    public String getServletName()
    {
        return "Static File wrapper";
    }
    
    public String getName()
    {
        return "Static File wrapper";
    }
    
    protected InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }
    
    protected RandomAccessFile getRandomAccessFile() throws IOException {
    	raf= new RandomAccessFile(file,"rw");
    	return raf;
    }
    
    protected long getLastModified() {
        return file.lastModified();
    }
    
    // PM92967, added method
    protected long getFileSize(boolean update) {
        if (fileSize == -1 || update) {
                fileSize = file.length();
        }
        
        return fileSize;
    }
	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletWrapper#setParent(com.ibm.wsspi.webcontainer.servlet.IServletContext)
	 */
	public void setParent(IServletContext parent)
	{
		// do nothing

	}
	
	// begin 268176    Welcome file wrappers are not checked for resource existence    WAS.webcontainer
	public boolean isAvailable (){
		return this.file.exists();
	}
	// end 268176    Welcome file wrappers are not checked for resource existence    WAS.webcontainer




}
