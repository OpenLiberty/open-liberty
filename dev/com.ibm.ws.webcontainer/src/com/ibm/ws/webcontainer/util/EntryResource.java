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
package com.ibm.ws.webcontainer.util;

import java.io.IOException;
import java.io.InputStream;

import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class EntryResource implements ExtDocRootFile{

    Entry entry = null;
    
    public EntryResource(Entry contained){
        entry = contained;
    }

    public InputStream getIS() throws IOException {
        try {
            return entry.adapt(InputStream.class);
        } catch (UnableToAdaptException e) {
            return null;
        }
    }

    public long getLastModified() {
        return entry.getLastModified();
    }

    public String getPath() {
        return entry.getPath();
    }
    
    public Entry getEntry(){
        return entry;
    }

}
