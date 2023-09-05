/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.ibm.wsspi.collector.manager.BufferManager;

public class BufferManagerEMQHelper {

    protected volatile static boolean EMQRemovedFlag = false;
    private static final int EMQ_TIMER = 60 * 5 * 1000; //5 Minute timer
    protected static List<BufferManager> bufferManagerList= Collections.synchronizedList(new ArrayList<BufferManager>());

    public BufferManagerEMQHelper() {
    }

    public static void removeEMQByTimer(){
        new java.util.Timer(true).schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        BufferManagerEMQHelper.removeEMQTrigger();
                    }
                },
                BufferManagerEMQHelper.EMQ_TIMER);
    }
    
    public synchronized static void removeEMQTrigger(){
            BufferManagerEMQHelper.EMQRemovedFlag=true;
            for(BufferManager i: BufferManagerEMQHelper.bufferManagerList) {
                ((BufferManagerImpl) i).removeEMQ();
            }
    }
    
    public synchronized static boolean getEMQRemovedFlag() {
        return EMQRemovedFlag;
    }

    public synchronized static void addBufferManagerList(BufferManager bufferManager) {
        bufferManagerList.add(bufferManager);
    }
}
