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
package com.ibm.io.async;

import java.io.IOException;

/**
 * This interface is implemented by subsystems that provide async functionality, such as an async socket provider, async file system
 * provider, or (for testing) a fake asynch provider
 */
public interface IAsyncProvider {

    public interface AsyncIOHelper {
        boolean enableAsyncIO();

        void loadLibrary(Class<? extends IAsyncProvider> providerClass, String libraryName);
    }

    /* Definitions for potential provider capabilities. */

    /**
     * Flag indicating that the provider supports scatter/gather directly on sockets.
     */
    int CAP_MULTI_SOCKET = 0x00000001;

    /**
     * Flag indicating that the provider supports scatter/gather directly on files.
     */
    int CAP_MULTI_FILE = 0x00000002;

    /**
     * Flag indicating that the provider supports jit buffers on reads.
     */
    int CAP_JIT_BUFFERS = 0x00000004;

    /**
     * Flag indicating that the provider supports jit buffers on reads.
     */
    int CAP_BATCH_IO = 0x00000008;

    /**
     * Indicates if the native library is capable of a specific set of functions
     * 
     * <pre>
     * static int CAP_MULTI_SOCKET = 0x00000001;
     * static int CAP_MULTI_FILE = 0x00000002;
     * static int CAP_JIT_BUFFERS = 0x00000004;
     * </pre>
     * 
     * @param capability
     *            the provider capability to test
     * @return true if the provider does support the given capabilty, and false otherwise.
     */
    boolean hasCapability(int capability);

    /**
     * Prepare the given file handle for use in async operations. The native code may need to wrap the original file handle so that
     * additional data can be attached to the file handle to enable the native code to work well. If this is done, the wrappered version of
     * the file handle is returned by this method and is used in all subsequent method calls relating to this channel.
     * 
     * @param fileDescriptor
     *            the original file descriptor for this channel
     * @param completionPort
     * @return the handle (wrappered if necessary)
     * @throws IOException
     *             if a problem occurs preapring the file handle.
     */
    long prepare2(long fileDescriptor, long completionPort) throws IOException;

    /**
     * Complete the lifecycle on the given channel identifier by disposing of resources associated with the asynchronous provision for the
     * given channel identifier. Once resources are disposed they are invalid for use in future asynchronous calls.
     * 
     * @param channelIdentifier
     *            the identifier for the inderlying file descriptor
     * @return the file descriptor that was associated with the identifier.
     */
    long dispose(long channelIdentifier);

    /**
     * Perform an asynchronous multi read or write operation on a specified channel, where the data involved is handled in
     * an array of buffers. The intent is that the data is spread across a set of buffers. The operations start at the
     * beginning of the first buffer and extend sequentially acorss each of the buffers in turn.
     * 
     * The operation may complete immediately,
     * in which case the results are returned by this method. If the operation does not complete immediately, the
     * results are returned via the <code>aio_getioev</code> method when the operation does eventually complete.
     * 
     * @param iobufferaddress the address of an array of data passed to the native code:
     *            <pre>
     *            [0] - the channel identifier
     *            [1] - the call identifier
     *            [4] - native data structure address
     *            [5] - address of start of first buffer
     *            ioev[6] - length of first buffer
     *            ioev[7...] - addresses & lengths of second and subsequent buffers
     *            </pre>
     *            Contains the following data if the operation completes immediately:
     *            <pre>
     *            [0] - the channel identifier
     *            [1] - the call identifier
     *            [2] - the error code for a failed IO operation, or 0 if successful
     *            [3] - the number of bytes affected by a successful IO operation
     *            </pre>
     * 
     * @param position
     *            the byte position in the file for the read/write operation to start. Not used for socket IO operations
     * @param count
     *            the number of buffers in the ioev array. Must be >0.
     * @param isRead
     * @param forceQueue
     * @param bytesRequested
     * @param useJITBuffer
     * @return <code>true</code> if the operation completed immediately, <code>false</code> if the operation will
     *         complete asynchronously.
     * @throws AsyncException
     */
    boolean multiIO3(long iobufferaddress, long position, int count, boolean isRead, boolean forceQueue, long bytesRequested, boolean useJITBuffer)
                    throws AsyncException;

    /**
     * Method which cancels an in-process asynchronous IO operation The native code should attempt to cancel the underlying IO operation, if
     * possible. Once this method has been called, the native code can assume that the Async IO Java code is no longer interested in the
     * result of the IO operation.
     * 
     * @param channelId - the identifier of the channel whose IO operation to cancel
     * @param callId - the identifier of the call to be canceled
     * 
     * @throws IOException
     *             if the cancel cannot be completed.
     * @return 0 - cancel was successfull, 1 - cancel attempt failed
     */
    int cancel2(long channelId, long callId) throws IOException;

    /**
     * Return the data for a completed asynchronous operation. This call blocks until completion data is available for an operation or the
     * timeout occurs. Multiple threads can call aio_getioev at the same time. Only one thread returns with the completion data for a
     * particular IO operation. Which thread gets the data for a particular IO operation is not defined.
     * <p>
     * The call is made with an empty long[4]completionData array passed as a DirectByteBuffer address.
     * Upon successful dequeuing, the completionData array contains:
     * 
     * <pre>
     * ioev[0] - the channel identifier
     * ioev[1] - the call identifier
     * ioev[2] - the error code for a failed IO operation, or 0 if successful
     * ioev[3] - the number of bytes affected by a successful IO operation
     * </pre>
     * 
     * @param bufferAddress
     * @param timeout
     * @param completionPort
     * @return <code>true</code> if the data is valid, <code>false</code> if a timeout occurred.
     * @throws AsyncException
     *             if the dequeueing failed for a reason other than a timeout.
     */
    boolean getCompletionData2(long bufferAddress, int timeout, long completionPort) throws AsyncException;

    /**
     * This get IO event call will request a batch of events from the native
     * library. In all other respects, it is identical to the single event
     * polling api.
     * 
     * @param iocbs
     * @param size
     * @param timeout
     * @param completionPort
     * @return int - number of events returned
     * @throws AsyncException
     * @see IAsyncProvider#getCompletionData2(long, int, long)
     */
    int getCompletionData3(long[] iocbs, int size,
                                   int timeout, long completionPort) throws AsyncException;

    /**
     * Initializes a given CompletionKey - allows the native code to initialize any
     * data structure it needs to attach to an IOCB before it is first used
     * 
     * @param theKey the key to initialize
     * @throws AsyncException
     */
    void initializeIOCB(CompletionKey theKey) throws AsyncException;

    /**
     * Terminates the use of a given CompletionKey - allows the native code to deallocate any
     * data structure it attached to the IOCB with <code>initializeIOCB</code>
     * 
     * @param theKey the key to terminate
     */
    void terminateIOCB(CompletionKey theKey);

    /**
     * Creates a new native completion port or polling object to be used for grouping.
     * prepare2 and getCompletionData3 requests.
     * 
     * @return the handle to the completion port.
     * @throws AsyncException
     */
    long getNewCompletionPort() throws AsyncException;

    /**
     * Closes a native completion port or polling object.
     * 
     * @param completionPort
     */
    void closeCompletionPort(long completionPort);

    /**
     * Checks if the Completion Port is still valid for use
     * 
     * @param completionPort
     * @return true if still in use (in the list), otherwise false
     */
    boolean isCompletionPortValid(long completionPort);

}