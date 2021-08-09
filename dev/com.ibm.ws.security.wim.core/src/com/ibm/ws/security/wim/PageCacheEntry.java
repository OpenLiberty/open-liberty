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
package com.ibm.ws.security.wim;

import com.ibm.wsspi.security.wim.model.Root;

public class PageCacheEntry {

    private int totalSize;
    private Root root = null;

    /**
     * Constructor for PageCacheEntry.
     */
    public PageCacheEntry() {
        super();
    }

    /**
     * Constructs the PageCacheEntry with the provided input parameters
     * 
     * @param totalSize the total size of a paging search results
     * @param dg a datagraph contains the to be cached entity
     * 
     */
    public PageCacheEntry(int totalSize, Root rootDO) {
        super();
        this.totalSize = totalSize;
        root = rootDO;
    }

    /**
     * Returns the total size of a paging search
     * 
     * @return the total size of a paging search
     */
    public int getTotalSize() {
        return this.totalSize;
    }

    /**
     * Returns the paged DataGraph object
     * 
     * @return the paged DataGraph object
     */
    public Root getDataObject() {
        return root;
    }

    /**
     * Sets the total size of a paging search
     * 
     * @param totalSize the total size of a paging search
     */
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * Sets the list of entities which will be stored in the paging cache
     * 
     * @param entities a list of entities
     */
    public void setDataObject(Root rootDO) {
        root = rootDO;
    }
}
