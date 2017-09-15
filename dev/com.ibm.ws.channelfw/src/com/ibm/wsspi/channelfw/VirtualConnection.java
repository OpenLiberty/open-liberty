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
package com.ibm.wsspi.channelfw;

import java.util.Map;

/**
 * This represents an inbound or outbound connection using the channel
 * framework. It basically is used to track the state for individual channels
 * in the chain associated with this connection.
 * 
 * @ibm-spi
 */
public interface VirtualConnection {

    /** The filechannel flag has not yet been set */
    int FILE_CHANNEL_CAPABLE_NOT_SET = 0;
    /** This VC does not support filechannel usage */
    int FILE_CHANNEL_CAPABLE_DISABLED = 1;
    /** This VC does support filechannel usage */
    int FILE_CHANNEL_CAPABLE_ENABLED = 2;

    /**
     * Destroys the virtual connection, freeing any resources allocated to it.
     */
    void destroy();

    /**
     * Get the state map associated with this virtual connection.
     * 
     * @return Map<Object,Object> to represent the state objects
     */
    Map<Object, Object> getStateMap();

    /**
     * Grant or deny permission to perform a Read request. This method will check
     * if the channels can process a new read request. If permission is granted,
     * then
     * the Read state of this channel-chain instance will be changed to Read
     * Pending.
     * The following reasons are some of the reasons a read permission request can
     * be denied:
     * 1) another read is pending or finishing, 2) a close is pending or
     * finishing.
     * If a read request is denied then appropriate action needs to be taken.
     * 
     * @return true if this channel-chain instance can process a new read request
     *         at
     *         this time, otherwise false.
     */
    boolean requestPermissionToRead();

    /**
     * Grant or deny permission to perform a Write request. This method will check
     * if the channels can process a new write request. If permission is granted,
     * then
     * the Write state of this channel-chain instance will be changed to Write
     * Pending.
     * The following reasons are some of the reasons a write permission request
     * can be denied:
     * 1) another write is pending or finishing, 2) a close is pending or
     * finishing.
     * If a write request is denied then appropriate action needs to be taken.
     * 
     * @return true if this channel-chain instance can process a new write request
     *         at
     *         this time, otherwise false.
     */
    boolean requestPermissionToWrite();

    /**
     * Grant or deny permission to close the connection.
     * Check if the channels can process a close, at this moment, based on the
     * state of
     * the this channel-chain instance.
     * If permission is granted, then the state of this
     * instance will be changed to Close Pending.
     * The following reasons are some of the reasons a close permission request
     * can be denied:
     * 1) A read request is pending or finishing and can not be closed out at this
     * time,
     * 2) A write request is pending or finishing and can not be closed out at
     * this time.
     * If the channels can not process
     * a close at this time, appropriate action needs to be taken.
     * 
     * @param waitForPermission
     *            Maximum time to wait for permission to be granted.
     * @return true if the the channel-chain can process a close request at this
     *         time,
     *         otherwise false.
     */
    boolean requestPermissionToClose(long waitForPermission);

    /**
     * Set the current state of the read to Read Done.
     * 
     */
    void setReadStateToDone();

    /**
     * Set the current state of the write to Write Done.
     * 
     */
    void setWriteStateToDone();

    /**
     * Check if input state tracking is being utilized for this connection.
     * Tracking will assumed to be off
     * no other "set" or "permission" methods have been called on this connection
     * If Tracking is not being used, then it is assumed that a
     * close of the connection will not be done while a read is outstanding. Not
     * using Tracking will increase performance by a small amount
     * 
     * @return true if this Tracking is being used, false if not.
     */
    boolean isInputStateTrackingOperational();

    /**
     * Return the object that is being used to synchronize access to state
     * tracking.
     * Code can synchronize on this object when work needs to be done without
     * another thread changing the object's state.
     * 
     * @return Object
     */
    Object getLockObject();

    /**
     * Grant or Deny permission for the Read request to be completed.
     * This routine should only be used by connector channels.
     * 
     * @return true if the channel can complete the read, else false
     */
    boolean requestPermissionToFinishRead();

    /**
     * Grant or Deny permission for the Write request to be completed.
     * This routine should only be used by connector channels.
     * 
     * @return true if the channel can complete the write, else false
     */
    boolean requestPermissionToFinishWrite();

    /**
     * Set the current state of the read to Close Allowed. No synchronization and
     * no state error checking will be done.
     * It is assumed that a call to getLockObject will be called to synchronize
     * this routine. The reason that may need to be done is to change the
     * current state while also completing other functions which are tied to the
     * state change.
     * This routine should only be used by connector channels.
     * 
     */
    void setReadStatetoCloseAllowedNoSync();

    /**
     * Set the current state of the write to Close Allowed. No synchronization and
     * no state error checking will be done.
     * It is assumed that a call to getLockObject will be called to synchronize
     * this routine. The reason that may need to be done is to change the
     * current state while also completing other functions which are tied to the
     * state change.
     * This routine should only be used by connector channels.
     * 
     */
    void setWriteStatetoCloseAllowedNoSync();

    /**
     * A check to see if a close has been requested with a timeout, and
     * is now waiting for the state to change to a close-able state.
     * This routine should only be used by connector channels.
     * 
     * @return true if a close has been requested with a timeout, but is
     *         in a wait state, otherwise return false.
     */
    boolean getCloseWaiting();

    /**
     * A check to see if a read was outstanding when close permission was granted
     * 
     * @return true if a read was outstanding
     */
    boolean isCloseWithReadOutstanding();

    /**
     * A check to see if a write was outstanding when close permission was granted
     * 
     * @return true if a write was outstanding
     */
    boolean isCloseWithWriteOutstanding();

    /**
     * set if InetAddressing is available for this implementation of the Channel
     * Framework.
     * InetAddressing is defined as the ability to use java InetAddress
     * objects for connection information calls, like the TCP Channel's
     * connection link method of:
     * public InetAddress getLocalAddress()
     * 
     * Some implementations of the Channel Framework, mainly some Z/OS
     * implementations,
     * do not support containing the connection information in the InetAddress
     * class.
     * For those OSes, the ConnectionDescriptor class of the VirtualConnection
     * will
     * need to be used to obtain host names and addresses.
     * 
     * @param _newValue
     *            true if InetAddressing is supported, false if not.
     */
    void setInetAddressingValid(boolean _newValue);

    /**
     * get a value telling if InetAddressing is supported by this instance of the
     * Channel Framework.
     * InetAddressing is defined as the ability to use java InetAddress
     * objects for connection information calls, like the TCP Channel's
     * connection link method of:
     * public InetAddress getLocalAddress()
     * 
     * Some implementations of the Channel Framework, mainly some Z/OS
     * implementations,
     * do not support containing the connection information in the InetAddress
     * class.
     * For those OSes, the ConnectionDescriptor class of the VirtualConnection
     * will
     * need to be used to obtain host names and addresses.
     * 
     * @return true if InetAddressing is supported, false if not.
     * 
     */
    boolean getInetAddressingValid();

    /**
     * Associate a ConnectionDescriptor object with this Virtual Connection
     * instance.
     * 
     * @param _newObject
     *            the ConnectionDescriptor object for this Vitual Connection.
     */
    void setConnectionDescriptor(ConnectionDescriptor _newObject);

    /**
     * Get the ConnectionDescriptor object for this Virtual Connection instance.
     * 
     * @return ConnectionDescriptor
     */
    ConnectionDescriptor getConnectionDescriptor();

    /**
     * Set the FileChannel Capable state if the state transition is valid
     * 
     * @param value
     *            the desired new state of the FileChannel Capable setting
     * @return the FileChannel Capable state after the set attempt
     */
    int attemptToSetFileChannelCapable(int value);

    /**
     * Get the current FileChannel Capable state.
     * 
     * @return the current FileChannel Capable state
     */
    int getFileChannelCapable();

    /**
     * Query whether this connection is currently set to explicitly allow the
     * FileChannel writing path. This is the same as comparing
     * getFileChannelCapable()
     * against the ENABLED value.
     * 
     * @return boolean
     */
    boolean isFileChannelCapable();
}
