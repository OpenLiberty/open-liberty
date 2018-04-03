/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.test;

import java.util.ArrayList;

import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 *
 */
public class DummyHandler implements SynchronousHandler {

    private int numOfMessages = 0;
    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.TestHandler";
    public ArrayList<Object> messageHolder = new ArrayList<>();

    public DummyHandler() {}

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void synchronousWrite(Object event) {
        setNumOfMessages(getNumOfMessages() + 1);
        messageHolder.add(event);
    }

    @Override
    public void init(CollectorManager collectorManager) {}

    @Override
    public void setBufferManager(String sourceId, BufferManager bufferMgr) {}

    @Override
    public void unsetBufferManager(String sourceId, BufferManager bufferMgr) {}

    public int getNumOfMessages() {
        return numOfMessages;
    }

    public void setNumOfMessages(int numOfMessages) {
        this.numOfMessages = numOfMessages;
    }

}
