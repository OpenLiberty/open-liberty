/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ConnectionDescriptor;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Base VirtualConnection Implementation. This implements the common functions
 * of the
 * virtual connection shared by the inbound and outbound specific virtual
 * connections.
 */
public class VirtualConnectionImpl implements VirtualConnection {

    private static final TraceComponent tc = Tr.register(VirtualConnectionImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    private static long ONE_MILLISECOND_IN_NANOSECONDS = 1000000L;

    /**
     * state map for virtual connection
     */
    private Map<Object, Object> stateStore = null;

    /**
     * Constructor for VirtualConnectionImpl
     */
    protected VirtualConnectionImpl() {
        // Nothing needed here at this time.
    }

    /**
     * re-initialize this VirtualConnection
     * 
     */
    protected void init() {
        // CONN_RUNTIME: set the top channel in the VC outbound
        this.stateStore = new HashMap<Object, Object>();
        this.fileChannelCapable = FILE_CHANNEL_CAPABLE_NOT_SET;
    }

    /*
     * @see com.ibm.ws.channelfw.chains.VirtualConnection#destroy()
     */
    @Override
    public void destroy() {
        // Nothing needed here at this time.
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnection#getStateMap()
     */
    @Override
    public Map<Object, Object> getStateMap() {
        // CONN_RUNTIME: get stateMap from VC
        return this.stateStore;
    }

    // State Values
    private static int READ_PENDING = 0x1001;
    private static int READ_WAITING = 0x1002;
    private static int READ_FINISHING = 0x1004;
    private static int WRITE_PENDING = 0x1010;
    private static int WRITE_WAITING = 0x1020;
    private static int WRITE_FINISHING = 0x1040;
    private static int CLOSE_PENDING = 0x1100;

    private static int IN_USE_MASK = 0x1000;
    private static int IN_USE_MASK_CLEAR_OUT = ~IN_USE_MASK;

    private static int READ_PENDING_CLEAR_OUT = ~(READ_FINISHING | READ_WAITING) | IN_USE_MASK;
    private static int READ_FINISHING_CLEAR_OUT = ~(READ_PENDING | READ_WAITING) | IN_USE_MASK;
    private static int READ_WAITING_CLEAR_OUT = ~(READ_PENDING | READ_FINISHING) | IN_USE_MASK;
    private static int READ_DONE_CLEAR_OUT = ~(READ_PENDING | READ_FINISHING | READ_WAITING) | IN_USE_MASK;

    private static int WRITE_PENDING_CLEAR_OUT = ~(WRITE_FINISHING | WRITE_WAITING) | IN_USE_MASK;
    private static int WRITE_FINISHING_CLEAR_OUT = ~(WRITE_PENDING | WRITE_WAITING) | IN_USE_MASK;
    private static int WRITE_WAITING_CLEAR_OUT = ~(WRITE_PENDING | WRITE_FINISHING) | IN_USE_MASK;
    private static int WRITE_DONE_CLEAR_OUT = ~(WRITE_PENDING | WRITE_FINISHING | WRITE_WAITING) | IN_USE_MASK;

    private static int CLOSE_NOT_ALLOWED_MASK = (READ_PENDING | READ_FINISHING | WRITE_PENDING | WRITE_FINISHING | CLOSE_PENDING) & IN_USE_MASK_CLEAR_OUT;

    private static int FINISH_NOT_ALLOWED_MASK = CLOSE_PENDING & IN_USE_MASK_CLEAR_OUT;

    private static int READ_NOT_ALLOWED_MASK = (CLOSE_PENDING | READ_PENDING | READ_WAITING | READ_FINISHING) & IN_USE_MASK_CLEAR_OUT;

    private static int WRITE_NOT_ALLOWED_MASK = (CLOSE_PENDING | WRITE_PENDING | WRITE_WAITING | WRITE_FINISHING) & IN_USE_MASK_CLEAR_OUT;

    private static int READ_OUTSTANDING = (READ_PENDING | READ_WAITING | READ_FINISHING) & IN_USE_MASK_CLEAR_OUT;
    private static int WRITE_OUTSTANDING = (WRITE_PENDING | WRITE_WAITING | WRITE_FINISHING) & IN_USE_MASK_CLEAR_OUT;

    // Close waiting flag
    private boolean closeWaiting = false;

    private int currentState = 0x0000;

    private boolean readOutWithClosePending = false;
    private boolean writeOutWithClosePending = false;

    private boolean inetAddressingValid = true;
    private ConnectionDescriptor connDesc = null;

    private int fileChannelCapable = FILE_CHANNEL_CAPABLE_NOT_SET;

    /*
     * @see VirtualConnection#requestPermissionToRead()
     */
    @Override
    public boolean requestPermissionToRead() {
        boolean rc = false;
        synchronized (this) {
            if ((currentState & READ_NOT_ALLOWED_MASK) == 0) {
                currentState = (currentState | READ_PENDING) & READ_PENDING_CLEAR_OUT;
                rc = true;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestPermissionToRead returning " + rc);
        }
        return rc;
    }

    /*
     * @see VirtualConnection#requestPermissionToWrite()
     */
    @Override
    public boolean requestPermissionToWrite() {
        boolean rc = false;
        synchronized (this) {
            if ((currentState & WRITE_NOT_ALLOWED_MASK) == 0) {
                currentState = (currentState | WRITE_PENDING) & WRITE_PENDING_CLEAR_OUT;
                rc = true;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestPermissionToWrite returning " + rc);
        }
        return rc;
    }

    /*
     * @see VirtualConnection#isCloseWithReadOutstanding()
     */
    @Override
    public boolean isCloseWithReadOutstanding() {
        if (currentState == CLOSE_PENDING) {
            return this.readOutWithClosePending;
        }
        return false;
    }

    /*
     * @see VirtualConnection#isCloseWithWriteOutstanding()
     */
    @Override
    public boolean isCloseWithWriteOutstanding() {
        if (currentState == CLOSE_PENDING) {
            return this.writeOutWithClosePending;
        }
        return false;
    }

    /*
     * @see VirtualConnection#getCloseWaiting()
     */
    @Override
    public boolean getCloseWaiting() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getCloseWaiting returning: " + this.closeWaiting);
        }
        return this.closeWaiting;
    }

    /*
     * @see VirtualConnection#requestPermissionToClose()
     */
    @Override
    public boolean requestPermissionToClose(long waitForPermission) {
        synchronized (this) {
            if ((currentState & CLOSE_NOT_ALLOWED_MASK) == 0) {

                if ((currentState & READ_OUTSTANDING) != 0) {
                    readOutWithClosePending = true;
                } else {
                    readOutWithClosePending = false;
                }
                if ((currentState & WRITE_OUTSTANDING) != 0) {
                    writeOutWithClosePending = true;
                } else {
                    writeOutWithClosePending = false;
                }

                currentState = CLOSE_PENDING;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "requestPermissionToClose returning true");
                }
                return true;
            }

            if (waitForPermission <= 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "requestPermissionToClose returning false");
                }
                return false;
            }

            long waitTillTime = (System.nanoTime() / ONE_MILLISECOND_IN_NANOSECONDS) + waitForPermission; //Measured in ms
            long waitTime = waitForPermission;
            while (true) {
                // wait for a state change that will allow closing
                try {
                    closeWaiting = true;
                    this.wait(waitTime);
                    closeWaiting = false;

                    // See if Permission can be granted
                    if ((currentState & CLOSE_NOT_ALLOWED_MASK) == 0) {

                        if ((currentState & READ_OUTSTANDING) != 0) {
                            readOutWithClosePending = true;
                        } else {
                            readOutWithClosePending = false;
                        }
                        if ((currentState & WRITE_OUTSTANDING) != 0) {
                            writeOutWithClosePending = true;
                        } else {
                            writeOutWithClosePending = false;
                        }
                        currentState = CLOSE_PENDING;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "requestPermissionToClose returning(2) true");
                        }
                        return true;
                    }
                    waitTime = (waitTillTime - (System.nanoTime() / ONE_MILLISECOND_IN_NANOSECONDS));
                    if (waitTime <= 0) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "requestPermissionToClose returning(2) false");
                        }
                        return false;
                    }
                    continue;
                } catch (InterruptedException x) {
                    // See if Permission can be granted
                    if ((currentState & CLOSE_NOT_ALLOWED_MASK) == 0) {

                        if ((currentState & READ_OUTSTANDING) != 0) {
                            readOutWithClosePending = true;
                        } else {
                            readOutWithClosePending = false;
                        }
                        if ((currentState & WRITE_OUTSTANDING) != 0) {
                            writeOutWithClosePending = true;
                        } else {
                            writeOutWithClosePending = false;
                        }
                        currentState = CLOSE_PENDING;

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "requestPermissionToClose returning(3) true");
                        }
                        return true;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "requestPermissionToClose returning(3) false");
                    }
                    return false;
                }
            } // end-while
        } // end-sync
    }

    /*
     * @see VirtualConnection#setReadStateToDone()
     */
    @Override
    public void setReadStateToDone() {
        synchronized (this) {
            currentState = currentState & READ_DONE_CLEAR_OUT;
            if (closeWaiting) {
                this.notify();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setReadStateToDone");
            }
        }
    }

    /*
     * @see VirtualConnection#setWriteStateToDone()
     */
    @Override
    public void setWriteStateToDone() {
        synchronized (this) {
            currentState = currentState & WRITE_DONE_CLEAR_OUT;
            if (closeWaiting) {
                this.notify();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setWriteStateToDone");
            }
        }
    }

    /*
     * @see VirtualConnection#isInputStateTrackingOperational()
     */
    @Override
    public boolean isInputStateTrackingOperational() {
        synchronized (this) {
            return (this.currentState != 0);
        }
    }

    /**
     * @see VirtualConnection#getLockObject()
     */
    @Override
    public Object getLockObject() {
        return this;
    }

    /*
     * @see VirtualConnection#requestPermissionToFinishRead()
     */
    @Override
    public boolean requestPermissionToFinishRead() {
        boolean rc = true;
        synchronized (this) {
            if ((currentState & FINISH_NOT_ALLOWED_MASK) != 0) {
                rc = false;
            } else {
                currentState = (currentState | READ_FINISHING) & READ_FINISHING_CLEAR_OUT;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestPermissionToFinishRead returning " + rc);
        }
        return rc;
    }

    /*
     * @see VirtualConnection#requestPermissionToFinishWrite()
     */
    @Override
    public boolean requestPermissionToFinishWrite() {
        boolean rc = true;
        synchronized (this) {
            if ((currentState & FINISH_NOT_ALLOWED_MASK) != 0) {
                rc = false;
            } else {
                currentState = (currentState | WRITE_FINISHING) & WRITE_FINISHING_CLEAR_OUT;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "requestPermissionToFinishWrite returning " + rc);
        }
        return rc;
    }

    /*
     * @see VirtualConnection#setReadStatetoCloseAllowedNoSync()
     */
    @Override
    public void setReadStatetoCloseAllowedNoSync() {
        currentState = (currentState | READ_WAITING) & READ_WAITING_CLEAR_OUT;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setReadStatetoCloseAllowedNoSync");
        }
    }

    /*
     * @see VirtualConnection#setWriteStatetoCloseAllowedNoSync()
     */
    @Override
    public void setWriteStatetoCloseAllowedNoSync() {
        currentState = (currentState | WRITE_WAITING) & WRITE_WAITING_CLEAR_OUT;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setWriteStatetoCloseAllowedNoSync");
        }
    }

    /*
     * @see VirtualConnection#setInetAddressingValid(boolean)
     */
    @Override
    public void setInetAddressingValid(boolean _newValue) {
        this.inetAddressingValid = _newValue;
    }

    /*
     * @see VirtualConnection#getInetAddressingValid()
     */
    @Override
    public boolean getInetAddressingValid() {
        return this.inetAddressingValid;
    }

    /*
     * @see VirtualConnection#setConnectionDescriptor(ConnectionDescriptor)
     */
    @Override
    public void setConnectionDescriptor(ConnectionDescriptor _newObject) {
        this.connDesc = _newObject;
    }

    /*
     * @see VirtualConnection#getConnectionDescriptor()
     */
    @Override
    public ConnectionDescriptor getConnectionDescriptor() {
        return this.connDesc;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.VirtualConnection#attemptToSetFileChannelCapable
     * (int)
     */
    @Override
    public int attemptToSetFileChannelCapable(int value) {
        // Can't go from Disabled to enabled.
        // Can't go from disabled/enabled to NOT_SET
        if (value == FILE_CHANNEL_CAPABLE_DISABLED) {
            fileChannelCapable = FILE_CHANNEL_CAPABLE_DISABLED;
        } else if (value == FILE_CHANNEL_CAPABLE_ENABLED) {
            if (fileChannelCapable == FILE_CHANNEL_CAPABLE_NOT_SET) {
                fileChannelCapable = FILE_CHANNEL_CAPABLE_ENABLED;
            }
        }

        // let the user know what the current value is, since it might not be
        // what the caller attempted to set it to.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "set file channel capable: " + this.fileChannelCapable);
        }
        return this.fileChannelCapable;
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnection#getFileChannelCapable()
     */
    @Override
    public int getFileChannelCapable() {
        return this.fileChannelCapable;
    }

    /*
     * @see com.ibm.wsspi.channelfw.VirtualConnection#isFileChannelCapable()
     */
    @Override
    public boolean isFileChannelCapable() {
        return this.fileChannelCapable == VirtualConnection.FILE_CHANNEL_CAPABLE_ENABLED;
    }
}