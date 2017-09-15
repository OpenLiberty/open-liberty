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
package com.ibm.ws.sib.msgstore.impl;

import java.io.IOException;

import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

/**
 * This interface created so we can easily swap between the various implementations
 * of map in MessageStoreImpl.
 * This interface can be discarded when we decide which implementation to use.
 * @author DrPhill
 */
interface Map {
    /**
     * used in MessageStoreImpl.findById
     * @param key
     */
    public abstract AbstractItemLink get(final long key);
    /**
     * used in MessageStoreImpl.register
     * @param key
     * @param value
     */
    public abstract void put(final long key, final AbstractItemLink value);
    /**
     * used in MessageStoreImpl.unregister
     * @param key
     */
    public abstract AbstractItemLink remove(final long key);
    public abstract void clear();
    
    /**
     * @param writer
     */
    public abstract void xmlWriteOn(FormattedWriter writer) throws IOException ;
}
