/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.equinox.module.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.osgi.storage.bundlefile.BundleEntry;

import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class ModuleEntry extends BundleEntry {
    private final Entry entry;
    private final String path;

    /**
     * @param entry
     * @param path
     */
    public ModuleEntry(Entry entry, String path) {
        this.entry = entry;
        this.path = path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getInputStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        try {
            InputStream result = entry.adapt(InputStream.class);
            if (result == null) {
                throw new IOException("The entry did not supply an input stream: " + entry);
            }
            return result;
        } catch (UnableToAdaptException e) {
            throw new IOException("Unable to get input stream for entry: " + entry.getPath() + '/' + entry.getName(), e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getSize()
     */
    @Override
    public long getSize() {
        return entry.getSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getName()
     */
    @Override
    public String getName() {
        return path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getTime()
     */
    @Override
    public long getTime() {
        return entry.getLastModified();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getLocalURL()
     */
    @Override
    public URL getLocalURL() {
        //It is questionable if this is really needed.
        return entry.getResource();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry#getFileURL()
     */
    @Override
    public URL getFileURL() {
        //Probably don't need to do this
        throw new UnsupportedOperationException();
    }

}
