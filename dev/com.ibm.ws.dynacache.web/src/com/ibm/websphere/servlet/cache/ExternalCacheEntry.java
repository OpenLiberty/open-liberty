/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.cache;

import java.io.Serializable;
import java.util.Vector;

/**
 * This is a simple struct object that contains url, html and header
 * members for an external cache entry.
 * @ibm-api 
 */
public class ExternalCacheEntry implements Serializable {
    
    private static final long serialVersionUID = 1342185474L;
    
    /**
     * This is the host header as received in the request
     * @ibm-api 
     */
    public String host = null;

    /**
     * This is the uri part of the entry
     * @ibm-api 
     */
    public String uri = null;

    /**
     * This is the content (html) part of the entry
     * @ibm-api 
     */
    public byte[] content = null;

    /**
     * This hashtable contains the header fields needed for caching.
     * @ibm-api 
     */
    public Vector[] headerTable = null;
}
