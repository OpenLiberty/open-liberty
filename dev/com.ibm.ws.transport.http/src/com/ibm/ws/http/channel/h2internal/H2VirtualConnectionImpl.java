/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.Map;

import com.ibm.wsspi.channelfw.ConnectionDescriptor;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 *
 */
public class H2VirtualConnectionImpl implements VirtualConnection {

    // wrap the VirtualConnection object, override the Map with a wrapped Map

    VirtualConnection commonVC = null;
    H2Map h2Map = null;

    public H2VirtualConnectionImpl(VirtualConnection x) {
        commonVC = x;
        Map<Object, Object> m = commonVC.getStateMap();
        h2Map = new H2Map(m);
    }

    @Override
    public Map<Object, Object> getStateMap() {
        return h2Map;
    }

    @Override
    public void destroy() {
        commonVC.destroy();
    }

    @Override
    public boolean requestPermissionToRead() {
        return commonVC.requestPermissionToRead();
    }

    @Override
    public boolean requestPermissionToWrite() {
        return commonVC.requestPermissionToWrite();
    }

    @Override
    public boolean requestPermissionToClose(long waitForPermission) {
        return commonVC.requestPermissionToClose(waitForPermission);
    }

    @Override
    public void setReadStateToDone() {
        commonVC.setReadStateToDone();
    }

    @Override
    public void setWriteStateToDone() {
        commonVC.setWriteStateToDone();
    }

    @Override
    public boolean isInputStateTrackingOperational() {
        return commonVC.isInputStateTrackingOperational();
    }

    @Override
    public Object getLockObject() {
        return commonVC.getLockObject();
    }

    @Override
    public boolean requestPermissionToFinishRead() {
        return commonVC.requestPermissionToFinishRead();
    }

    @Override
    public boolean requestPermissionToFinishWrite() {
        return commonVC.requestPermissionToFinishWrite();
    }

    @Override
    public void setReadStatetoCloseAllowedNoSync() {
        commonVC.setReadStatetoCloseAllowedNoSync();
    }

    @Override
    public void setWriteStatetoCloseAllowedNoSync() {
        commonVC.setWriteStatetoCloseAllowedNoSync();
    }

    @Override
    public boolean getCloseWaiting() {
        return commonVC.getCloseWaiting();
    }

    @Override
    public boolean isCloseWithReadOutstanding() {
        return commonVC.isCloseWithReadOutstanding();
    }

    @Override
    public boolean isCloseWithWriteOutstanding() {
        return commonVC.isCloseWithWriteOutstanding();
    }

    @Override
    public void setInetAddressingValid(boolean _newValue) {
        commonVC.setInetAddressingValid(_newValue);
    }

    @Override
    public boolean getInetAddressingValid() {
        return commonVC.getInetAddressingValid();
    }

    @Override
    public void setConnectionDescriptor(ConnectionDescriptor _newObject) {
        commonVC.setConnectionDescriptor(_newObject);
    }

    @Override
    public ConnectionDescriptor getConnectionDescriptor() {
        return commonVC.getConnectionDescriptor();
    }

    @Override
    public int attemptToSetFileChannelCapable(int value) {
        return commonVC.attemptToSetFileChannelCapable(value);
    }

    @Override
    public int getFileChannelCapable() {
        return commonVC.getFileChannelCapable();
    }

    @Override
    public boolean isFileChannelCapable() {
        return commonVC.isFileChannelCapable();
    }

    public VirtualConnection getCommonVC() {
        return this.commonVC;
    }

}
