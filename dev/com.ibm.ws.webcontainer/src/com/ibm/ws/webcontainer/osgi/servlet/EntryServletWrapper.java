/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import com.ibm.ws.webcontainer.extension.DefaultExtensionProcessor;
import com.ibm.ws.webcontainer.servlet.FileServletWrapper;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *
 */
public class EntryServletWrapper extends FileServletWrapper {

    private Entry entry;
    private long fileSize = -1; // PM92967
    
    public EntryServletWrapper(IServletContext parent, DefaultExtensionProcessor parentProcessor, Entry entry)
    {
        super(parent, parentProcessor);
        this.entry = entry;
    }
    
    public String getServletName()
    {
        return "Entry wrapper";
    }
    
    public String getName()
    {
        return "Entry wrapper";
    }
    
    protected InputStream getInputStream() throws IOException {
        try {
            return entry.adapt(InputStream.class);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }
    }
    
    protected RandomAccessFile getRandomAccessFile() throws IOException {
        return null;
    }
    
    protected long getLastModified() {
        return entry.getLastModified();
    }
    
    // PM92967, added method
    protected long getFileSize(boolean update) {
        if (fileSize == -1 || update) {
                fileSize = entry.getSize();
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
        
        public boolean isAvailable (){
                return this.entry.getSize() != 0;
        }
}
