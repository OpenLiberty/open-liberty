/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.ibm.ws.zos.channel.local.queuing.NativeServiceException;
import com.ibm.ws.zos.channel.local.queuing.NativeServiceResult;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * This class provides JNI methods for interacting with the native local comm code.
 * 
 * This guy is a singleton whose lifecycle is managed by LocalChannelProviderImpl.
 * 
 * This class also holds a reference to the SharedMemoryAttachmentManager, which 
 * provides methods to safely obtain and release access to the native shared memory
 * used by localcomm connections (to safely access that memory without the server
 * detaching from it). 
 * 
 */
public class NativeRequestHandler {

    /**
     * Reference to the local channel provider service.
     */
    private LocalChannelProviderImpl localChannelProvider = null;

    /**
     * Manages the attaching/detaching to/from the client's shared memory area.
     */
    private final SharedMemoryAttachmentManager sharedMemoryAttachmentManager = new SharedMemoryAttachmentManager(this);

    /**
     * This CTOR is for unit-test purposes only.
     */
    protected NativeRequestHandler() {}

    /**
     * Create a new native trace handler.
     * 
     * @param bufferHelper the buffer management object used to read main memory.
     */
    public NativeRequestHandler(LocalChannelProviderImpl localProvider) {
        localChannelProvider = localProvider;
        NativeMethodManager nativeMethodManager = localChannelProvider.nativeMethodManager;
        nativeMethodManager.registerNatives(NativeRequestHandler.class);
    }

    /**
     * Dip native and pull off a list of work elements from the black queue.
     * 
     * Note: this method will block until black queue work elements are available to process.
     * 
     * @param otherWorkToDo Set to true if the caller has other work that it can be doing, if there
     *                      are no local comm requests to read.  In that case, just return to the
     *                      caller instead of pausing to wait for local comm work.
     * 
     * @return A List of black queue work elements to be processed. The list may be empty or null.
     * 
     * @throws IOException
     */
    protected List<NativeWorkRequest> getWorkRequestElements(boolean otherWorkToDo) throws IOException {

        List<NativeWorkRequest> retMe = null;

        // Dip native for a pointer to a list of BBGZLWQEs (pass reference to processed list on 
        // subsequent call in order to release them in native code).
        long workRequestElements = ntv_getWorkRequestElements(otherWorkToDo);

        if (workRequestElements == -1) {
        	// Skip throw if just returning from a stop of the listener call (ntv_stopListeningForRequests)
        	if (LocalCommServiceResults.getLComServiceResult().getReasonCode() != LCOM_WRQ_WAITONWORK_RC_STOPLIS) {
        		throw new IOException(new NativeServiceException(LocalCommServiceResults.getLComServiceResult()));
        	}
        } else if (workRequestElements != 0) {

            // Create list of Work Requests from the native WorkRequestElements reference
            retMe = createNativeWorkRequests(workRequestElements);

            // Call native to release the list of NativeWorkRequests 
            int localRC = ntv_freeWorkRequestElements();
            if (localRC == -1) {
                throw new IOException(new NativeServiceException(LocalCommServiceResults.getLComServiceResult()));
            }
        } 
        
        return retMe;
    }

    /**
     * Create a list of NativeWorkRequests given a native reference to
     * a list of native LComWorkQueueElement's.
     * 
     * @param workRequestElements - A pointer to a native list of black queue work request elements.
     * 
     * @return A List of NativeWorkRequest objects
     */
    public List<NativeWorkRequest> createNativeWorkRequests(long workRequestElements) {

        LinkedList<NativeWorkRequest> requestList = new LinkedList<NativeWorkRequest>();

        // If we got a reference to a native list of WRQEs
        if (workRequestElements != 0) {
            NativeWorkRequest currentWRQE = null;
            for (long currentNativeWRQE_Ptr = workRequestElements; currentNativeWRQE_Ptr != 0; currentNativeWRQE_Ptr = currentWRQE.getNextWorkRequestPtr()) {
                ByteBuffer currentWRQE_bb = localChannelProvider.getDirectBufferHelper().getSlice(currentNativeWRQE_Ptr, NativeWorkRequest.MAX_NATIVE_SIZE);

                // Create a new NativeWorkRequest
                currentWRQE = new NativeWorkRequest(currentWRQE_bb);

                // Add new Work request to list
                requestList.addFirst(currentWRQE);
            }
        }

        return requestList;
    }

    /**
     * Inform the native half of the channel that the new connection (contained in
     * the given NativeWorkRequest) was accepted.
     * 
     * This involves connecting to the client's shared memory area and notifying
     * the client thread that the connection was accepted.
     * 
     * @param connectWorkRequest - The NativeWorkRequest associated with the new connection.
     * @throws IOException
     */
    public void nativeConnectAccepted(NativeWorkRequest connectWorkRequest) throws IOException {

        // Obtain access to the client's shared memory area for this connection.
        // If we're the first one in, then we'll do the attach.
        // This access is for the connection as a whole. It's paired up with
        // a releaseAccess under close().
        sharedMemoryAttachmentManager.obtainAccessOrAttach(connectWorkRequest);

        // The access made above is for the connection as a whole.
        // This access is just for calling connectResponse.
        sharedMemoryAttachmentManager.obtainAccess(connectWorkRequest);
        try {
            safeConnectResponse(connectWorkRequest);
        } finally {
            sharedMemoryAttachmentManager.releaseAccess(connectWorkRequest);
        }
    }

    /**
     * Send connect response to client.
     * Tell the native code that the connection is basically complete.
     * 
     * @throws IOException
     */
    private void safeConnectResponse(NativeWorkRequest connectWorkRequest) throws IOException {

        int localRC = ntv_connectResponse(connectWorkRequest.getClientConnectionHandle().getBytes());

        if (localRC == -1) {
            throw new IOException(new NativeServiceException(LocalCommServiceResults.getLComServiceResult()));
        }
    }

    /**
     * Connect to the client's shared memory area.
     * Called by the SharedMemoryAttachmentManager.
     * 
     * @throws IOException
     */
    public void connectToClientsSharedMemory(byte[] clientConnHandle, byte[] requestSpecificParms) throws IOException {
        // Drive to native to connect to client's shared memory objects
        int localRC = ntv_connectToClientsSharedMemory(clientConnHandle, requestSpecificParms);

        if (localRC == -1) {
            throw new IOException(new NativeServiceException(LocalCommServiceResults.getLComServiceResult()));
        }
    }
    
    /**
     * Disconnect from the client's shared memory objects.
     * Called by the SharedMemoryAttachmentManager.
     * 
     * @throws IOException
     */
    public void disconnectFromClientsSharedMemory(byte[] clientConnHandle, byte[] requestSpecificParms) throws IOException {
        // Drive to native to disconnect from client's shared memory objects
        int localRC = ntv_disconnectFromClientsSharedMemory(clientConnHandle, requestSpecificParms);

        if (localRC == -1) {
            throw new IOException(new NativeServiceException(LocalCommServiceResults.getLComServiceResult()));
        }
    }

    /**
     * Attempt to read data for the given connection.
     * 
     * @param connectWorkRequest - contains all the info we need about the client connection.
     * 
     * @return A ByteBuffer containing the data. If no data was available and the request went async,
     *         then null is returned.
     * 
     * @throws IOException
     */
    public ByteBuffer read(NativeWorkRequest connectWorkRequest, boolean forceAsync) throws IOException {

        sharedMemoryAttachmentManager.obtainAccess(connectWorkRequest);
        try {
            return safeRead(connectWorkRequest.getClientConnectionHandle().getBytes(), forceAsync);
        } finally {
            sharedMemoryAttachmentManager.releaseAccess(connectWorkRequest);
        }
    }

    /**
     * Called from read after we've obtained access to the shared memory area.
     */
    private ByteBuffer safeRead(byte[] clientConnHandle, boolean forceAsync) throws IOException {

        NativeServiceResult nativeServiceResult = new NativeServiceResult();
        byte[] data = ntv_read(clientConnHandle, forceAsync, nativeServiceResult.getBytes());

        if (data != null) {
            return ByteBuffer.wrap(data);
        } else if (nativeServiceResult.getReturnCode() == 0) {
            // The request went async.  Return null.
            return null;
        } else {
            // Raise an exception.
            throw new IOException(new NativeServiceException(nativeServiceResult));
        }
    }

    /**
     * Write the given data to the the given connection.
     * 
     * @param clientConnHandle - the native connection handle.
     * @param data - the data to write.
     * 
     * @throws IOException
     */
    public void write(NativeWorkRequest connectWorkRequest, ByteBuffer data) throws IOException {

        sharedMemoryAttachmentManager.obtainAccess(connectWorkRequest);
        try {
            safeWrite(connectWorkRequest.getClientConnectionHandle().getBytes(), data);
        } finally {
            sharedMemoryAttachmentManager.releaseAccess(connectWorkRequest);
        }
    }

    /**
     * Called from write after we've obtained access to the shared memory area.
     */
    private void safeWrite(byte[] clientConnHandle, ByteBuffer data) throws IOException {

        NativeServiceResult nativeServiceResult = new NativeServiceResult();
        int localRC = ntv_write(clientConnHandle, data.array(), nativeServiceResult.getBytes());

        if (localRC != 0) {
            if (nativeServiceResult.getReturnCode() != 0) {
                throw new IOException(new NativeServiceException(nativeServiceResult));
            }
        }
    }

    /**
     * Close down the native stuff associated with the given connection handle.
     * 
     * @param connectWorkRequest - the NativeWorkRequest associated with the connection.
     * 
     * @throws IOException
     */
    public void close(NativeWorkRequest connectWorkRequest) throws IOException {

    	sharedMemoryAttachmentManager.obtainAccess(connectWorkRequest);
    	try {
    		safeClose(connectWorkRequest);
    	} finally {
    		sharedMemoryAttachmentManager.releaseAccess(connectWorkRequest);

    		// This release is paired up with the initial obtainAccessOrAttach done
    		// during the connect.
    		// TODO: what if safeClose throws an exception?
    		sharedMemoryAttachmentManager.releaseAccess(connectWorkRequest);
    	}
    }

    /**
     * Called from close, after obtaining access to the client's shared memory.
     */
    private void safeClose(NativeWorkRequest connectWorkRequest) throws IOException {

        NativeServiceResult nativeServiceResult = new NativeServiceResult();

        int localRC = ntv_close(connectWorkRequest.getClientConnectionHandle().getBytes(),
                                nativeServiceResult.getBytes());
        if (localRC != 0) {
            if (nativeServiceResult.getReturnCode() != 0) {
                throw new IOException(new NativeServiceException(nativeServiceResult));
            }
        }
    }
    
    /**
     * Dump the native control blocks for the given connection.
     * Called during close when the conn is closing due to an exception.
     */
    public List<String> dumpNativeControlBlocks(NativeWorkRequest connectWorkRequest) throws IOException {
        return dumpNativeControlBlocks(connectWorkRequest, new IntrospectHelper(localChannelProvider));
    }
  
    /**
     * Dump the native control blocks for the given connection.
     * Called from:
     * 1) introspection 
     * 2) during close when the conn is closing due to an exception.
     * 
     * @param connectWorkRequest - The CONNECT NativeWorkRequest associated with the connection to be dumped
     * @param introspectHelper - Does the formatting of the native control blocks. 
     * 
     * @return List<String> - the formatted output (double-guttered).
     */
    protected List<String> dumpNativeControlBlocks(NativeWorkRequest connectWorkRequest, 
                                                   IntrospectHelper introspectHelper) throws IOException {
        sharedMemoryAttachmentManager.obtainAccess(connectWorkRequest);
        try {
            return safeDumpNativeControlBlocks(connectWorkRequest, introspectHelper);
        } finally {
            sharedMemoryAttachmentManager.releaseAccess(connectWorkRequest);
        }
    }
    
    /**
     * Dump the native control blocks for the given connection.
     */
    private List<String> safeDumpNativeControlBlocks(NativeWorkRequest connectWorkRequest, 
                                                     IntrospectHelper introspectHelper)  {
        return introspectHelper.dumpNativeControlBlocks(connectWorkRequest.getClientConnectionHandle());
    }
    
    /**
     * Dip native and reset the work request queue closure flags
     */
    protected void initWRQFlags() throws IOException {
        int localRC = ntv_initWRQFlags();
        if (localRC == -1) {
            throw new IOException(new NativeServiceException(LocalCommServiceResults.getLComServiceResult()));
        }
    }

    //
    // Native Methods
    //

    // Free current list of WorkRequests and Get a new list of request(s) to process
    public static final long LCOM_WRQ_WAITONWORK_RC_STOPLIS = 24;
    protected native long ntv_getWorkRequestElements(boolean otherWorkToDo);

    // Free current list of native WorkRequests
    protected native int ntv_freeWorkRequestElements();

    // Release Listener thread so it can end.
    protected native int ntv_stopListeningForRequests();

    // Drive connect results back to client
    protected native int ntv_connectResponse(byte[] clientConnHandle);

    // Connect to Client's shared memory objects
    protected native int ntv_connectToClientsSharedMemory(byte[] clientConnHandle, byte[] requestSpecificParms);
    
    // Disconnect from the Client's shared memory objects
    protected native int ntv_disconnectFromClientsSharedMemory(byte[] clientConnHandle, byte[] requestSpecificParms);

    // Read request for current data.
    protected native byte[] ntv_read(byte[] clientConnHandle, boolean forceAsync, byte[] nativeServiceResultBytes);

    // Send data across the connection
    protected native int ntv_write(byte[] clientConnHandle, byte[] sendBytes, byte[] nativeServiceResultBytes);

    protected native int ntv_close(byte[] clientConnHandle, byte[] bytes);

    // Reset work request queue closure flags
    protected native int ntv_initWRQFlags();

}
