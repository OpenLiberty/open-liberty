/*******************************************************************************
 * Copyright (c) 1997, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.Iterator;

/**
 * @author asisin
 * 
 */
public class URIMapper {

    protected URIMatcher matcher;

    /**
     * This constructor will create a matcher thats optimized for speed
     * but will not scale very well for very large data sets.
     * 
     */
    public URIMapper() {
        matcher = new URIMatcher(false);
    }

    /**
     * Constructor with capability of contolling the scability vs speed
     * characteristics of the matcher.
     * 
     * @param scalable Set to true for scalable matcher at the expense of speed
     */
    public URIMapper(boolean scalable) {
        matcher = new URIMatcher(scalable);
    }

    /**
     * PM06111 Add new contructor
     * Constructor with capability of contolling the scability vs speed
     * characteristics of the matcher.
     * 
     * @param scalable Set to true for scalable matcher at the expense of speed
     * @param useStringKeys Set to true to indes paths using their string value and not hashCode
     */
    public URIMapper(boolean scalable, boolean useStringKeys) {
        matcher = new URIMatcher(scalable, useStringKeys);
    }

    /**
     * @see com.ibm.ws.core.RequestMapper#addMapping(String, Object)
     */
    public void addMapping(String path, Object target) throws Exception {
        matcher.put(path, target);
    }

    public Object replaceMapping(String path, Object target) throws Exception {
        return matcher.replace(path, target);
    }

    /**
     * @see com.ibm.ws.core.RequestMapper#removeMapping(String)
     */
    public void removeMapping(String path) {
        matcher.remove(path);
    }

    /**
     * @see com.ibm.ws.core.RequestMapper#targetMappings()
     */
    public Iterator targetMappings() {
        return matcher.iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.core.RequestMapper#exists(java.lang.String)
     */
    public boolean exists(String path) {
        return matcher.exists(path);
    }

}
