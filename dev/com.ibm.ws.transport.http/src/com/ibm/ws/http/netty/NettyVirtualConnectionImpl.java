/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wsspi.channelfw.ConnectionDescriptor;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 *
 */
public class NettyVirtualConnectionImpl implements VirtualConnection {

    private Map<Object, Object> stateStore = null;

    protected NettyVirtualConnectionImpl() {
    }

    public void init() {
        this.stateStore = new HashMap<Object, Object>();
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<Object, Object> getStateMap() {
        // TODO Auto-generated method stub
        return stateStore;
    }

    @Override
    public boolean requestPermissionToRead() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean requestPermissionToWrite() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean requestPermissionToClose(long waitForPermission) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setReadStateToDone() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setWriteStateToDone() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isInputStateTrackingOperational() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object getLockObject() {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public boolean requestPermissionToFinishRead() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean requestPermissionToFinishWrite() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setReadStatetoCloseAllowedNoSync() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setWriteStatetoCloseAllowedNoSync() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getCloseWaiting() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCloseWithReadOutstanding() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCloseWithWriteOutstanding() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setInetAddressingValid(boolean _newValue) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getInetAddressingValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setConnectionDescriptor(ConnectionDescriptor _newObject) {
        // TODO Auto-generated method stub

    }

    @Override
    public ConnectionDescriptor getConnectionDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int attemptToSetFileChannelCapable(int value) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getFileChannelCapable() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isFileChannelCapable() {
        // TODO Auto-generated method stub
        return false;
    }

}
