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

/**
 *
 */
public class EDRPathEntry {
    private String path = null;
    private boolean containerRelative = false;
    
    public EDRPathEntry(String filePath, boolean inContainer){
        path = filePath;
        containerRelative = inContainer;
    }
    
    public String getPath(){
        return path;
    }
    
    public boolean inContainer(){
        return containerRelative;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "EDRPathEntry [containerRelative=" + containerRelative + ", path=" + path + "]";
    }
}
