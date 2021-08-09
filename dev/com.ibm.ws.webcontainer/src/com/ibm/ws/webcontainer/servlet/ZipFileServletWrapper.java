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
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor;
import com.ibm.ws.webcontainer.util.ZipFileResource;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;


public class ZipFileServletWrapper extends FileServletWrapper {
    private long fileSize = -1; // PM92967
	private ZipFileResource zipFileResource;
    
    public ZipFileServletWrapper(IServletContext parent, DefaultExtensionProcessor parentProcessor, ZipFileResource zipFileResource)
    {
        super(parent, parentProcessor);
        this.zipFileResource = zipFileResource;
        isZip=true;
    }

	public String getServletName()
    {
        return "Zip File wrapper";
    }

    public String getName()
    {
        return "Zip File wrapper";
    }

    protected InputStream getInputStream() throws IOException {
        return this.zipFileResource.getIS();
    }
    
    protected long getLastModified() {
    	long time = zipFileResource.getZipEntry().getTime();
        return time;
    }
    
    protected int getContentLength() {
        return getContentLength(true);
    }
    
    // PM92967, added method
    protected long getFileSize(boolean update) {
        if (fileSize == -1 || update) {
                fileSize = zipFileResource.getZipEntry().getSize();
        }
        
        return fileSize;
    }
    
	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.servlet.IServletWrapper#setParent(com.ibm.wsspi.webcontainer.servlet.IServletContext)
	 */
	public void setParent(IServletContext parent)
	{
		// nothing

	}
	// begin 268176    Welcome file wrappers are not checked for resource existence    WAS.webcontainer
	public boolean isAvailable (){
		return new File(zipFileResource.getZipFile().getName()).exists();
	}
	// end 268176    Welcome file wrappers are not checked for resource existence    WAS.webcontainer

	@Override
	protected RandomAccessFile getRandomAccessFile() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void handleRequest(ServletRequest req, ServletResponse res, WebAppDispatcherContext dispatchContext) throws Exception {
		try {
			super.handleRequest(req, res,dispatchContext);
		} finally {
	        if ( System.getSecurityManager() != null){
	        	try {
			         AccessController.doPrivileged(new PrivilegedExceptionAction<Object>()  {
			             public Object run() throws IOException {
			            	 zipFileResource.getZipFile().close();
			                 return null;
			             }
			        });
	        	} catch (PrivilegedActionException pae) {
	        		throw new IOException(pae.getMessage());
	        	}
		    } else {
			    zipFileResource.getZipFile().close();
		    }    
		}
	}
}
