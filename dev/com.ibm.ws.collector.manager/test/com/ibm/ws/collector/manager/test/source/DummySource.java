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
package com.ibm.ws.collector.manager.test.source;

import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;

/**
 *
 */
public class DummySource implements Source {

    @Override
    public String getSourceName() {
        return "dummysource";
    }

    @Override
    public String getLocation() {
        return "memory";
    }

    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        System.out.println("setBufferManager");
    }

    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        System.out.println("unsetBufferManager");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DummySource [getSourceName()=" + getSourceName() + ", getLocation()=" + getLocation() + "]";
    }
}
