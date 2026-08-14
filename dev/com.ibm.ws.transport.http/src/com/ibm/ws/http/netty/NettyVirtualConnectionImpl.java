/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.channelfw.ConnectionDescriptor;
import com.ibm.wsspi.channelfw.VirtualConnection;

public class NettyVirtualConnectionImpl implements VirtualConnection {
    private static final TraceComponent tc = Tr.register(NettyVirtualConnectionImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);
    private Map<Object, Object> stateStore = null;
    private boolean inetAddressingValid = false;
    private ConnectionDescriptor connDesc = null;

    //no-op using for complete callback.
    public static final NettyVirtualConnectionImpl SHARED_NETTY_CALLBACK_VC = new NettyVirtualConnectionImpl(true);

    protected NettyVirtualConnectionImpl() {
        this(false);
    }

    private NettyVirtualConnectionImpl(boolean sharedCallbackInstance) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor, this [" + this + "], sharedCallbackInstance [" + sharedCallbackInstance + "]");
        }
        //Isolate an empty shared request stateStore from normal operation.
        this.stateStore = sharedCallbackInstance ? Collections.emptyMap() : new HashMap<Object, Object>();
    }

    public static NettyVirtualConnectionImpl createVC() {
        return new NettyVirtualConnectionImpl();
    }

    @Override
    public void destroy() {
    }

    @Override
    public Map<Object, Object> getStateMap() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getStateMap, stateStore [" + this.stateStore + "]"); 
        }
        return stateStore;
    }

    @Override
    public boolean requestPermissionToRead() {
        return false;
    }

    @Override
    public boolean requestPermissionToWrite() {
        return false;
    }

    @Override
    public boolean requestPermissionToClose(long waitForPermission) {
        return false;
    }

    @Override
    public void setReadStateToDone() {
    }

    @Override
    public void setWriteStateToDone() {
    }

    @Override
    public boolean isInputStateTrackingOperational() {
        return false;
    }

    @Override
    public Object getLockObject() {
        return this;
    }

    @Override
    public boolean requestPermissionToFinishRead() {
        return false;
    }

    @Override
    public boolean requestPermissionToFinishWrite() {
        return false;
    }

    @Override
    public void setReadStatetoCloseAllowedNoSync() {
    }

    @Override
    public void setWriteStatetoCloseAllowedNoSync() {
    }

    @Override
    public boolean getCloseWaiting() {
        return false;
    }

    @Override
    public boolean isCloseWithReadOutstanding() {
        return false;
    }

    @Override
    public boolean isCloseWithWriteOutstanding() {
        return false;
    }

    @Override
    public void setInetAddressingValid(boolean _newValue) {
        this.inetAddressingValid = _newValue;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setInetAddressingValid, inetAddressingValid [" + this.inetAddressingValid + "]");
        }
    }

    @Override
    public boolean getInetAddressingValid() {
        return this.inetAddressingValid;
    }

    @Override
    public void setConnectionDescriptor(ConnectionDescriptor _newObject) {
        this.connDesc = _newObject;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setConnectionDescriptor, connDesc [" + this.connDesc + "]");
        }
    }

    @Override
    public ConnectionDescriptor getConnectionDescriptor() {
        return this.connDesc;
    }

    @Override
    public int attemptToSetFileChannelCapable(int value) {
        return 0;
    }

    @Override
    public int getFileChannelCapable() {
        return 0;
    }

    @Override
    public boolean isFileChannelCapable() {
        return false;
    }
}
