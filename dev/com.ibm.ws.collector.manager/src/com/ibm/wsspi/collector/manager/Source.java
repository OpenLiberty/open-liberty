/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.collector.manager;

/**
 * Source interface, a source managed by the collector manager framework should implement this interface.
 */
public interface Source {

    /**
     * Returns the name of the source. Source name will be used by the collector manager to identify
     * a source provider.
     * 
     * @return The name of the source.
     */
    String getSourceName();

    /**
     * Returns the location of the source. Source location along with source name will be used by the
     * collector manager to uniquely identify a source.
     * 
     * @return The location for this source.
     */
    String getLocation();

    /**
     * Source will publish events to this buffer manager.
     * Collector manager will use this method to assign a buffer instance to the source.
     * 
     * @param bufferMgr Buffer manager instance for the source.
     */
    void setBufferManager(BufferManager bufferMgr);

    /**
     * Collector manager will use this method to indicate that this buffer should no
     * longer be used.
     * 
     * @param bufferMgr Buffer manager instance for the source.
     */
    void unsetBufferManager(BufferManager bufferMgr);
}
