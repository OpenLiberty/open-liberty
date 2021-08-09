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
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;

/**
 *
 */
public class DummyHandler implements Handler {

    CollectorManager collectorManager;

    @Override
    public String getHandlerName() {
        return "dummyhandler";
    }

    @Override
    public void init(CollectorManager collectorManager) {
        this.collectorManager = collectorManager;
    }

    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {
        System.out.println("setBufferManager: " + sourceId);
    }

    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {
        System.out.println("unsetBufferManager: " + sourceId);
    }

    @Override
    public String toString() {
        return "DummyHandler [getHandlerName()=" + getHandlerName() + "]";
    }
}
