/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;
import com.ibm.wsspi.channelfw.objectpool.CircularObjectPool;

/**
 * This class is an implementation of the IAsyncProvider interface. AsyncLibrary
 * provides the basic capabilities of the Async IO package at the operating system level.
 * AsyncLibrary uses standard operating system Files and Sockets to implement the features
 * of AsyncSocketChannels and AsyncFileChannels.
 * <p>
 * AsyncLibrary makes use of a native code library, with the default name "ibmaio"
 * (ibmaio.dll on Windows, libibmaio.so on Unix platforms, for example). The native
 * method calls into this library are all prefixed "aio_". The detailed requirements
 * for each of these methods are documented below.
 * <p>
 * The AsyncLibrary has a lifecycle which involves using the native methods as follows:
 * <ul>
 * <li>When AsyncLibrary is first loaded, it invokes the aio_init method to initialize
 * the native library. No other native calls are valid before aio_init is invoked.
 * The native library stays loaded until the Java application exits. When the Java application
 * exits, AsyncLibrary calls the aio_closeport method and the aio_shutdown method in
 * sequence. aio_closeport aims to close the IO Completion port (or equivalent native
 * object) on which threads wait for asynchronous notifications - any waiting threads
 * must be given the chance to exit the aio_getioev method that they have called.
 * aio_shutdown gives the native library the opportunity to dispose of any resources it
 * has used. No native calls are valid after aio_shutdown is invoked.
 * <li>When a new AsyncChannel is created (either a Socket or a File), the aio_prepare
 * method is invoked. This gives the native library the chance to prepare data structures
 * related to the new channel. No IO operations are valid on a channel until aio_prepare
 * is invoked. When an AsyncChannel is closed, aio_dispose is called, which allows
 * the native library to deallocate any resources associated with the channel. No IO
 * operations are valid on a channel after aio_dispose is called.
 * <li>IO operations are requested via the aio_read, aio_write and aio_multiIO methods.
 * aio_read and aio_write operate on data in a single data buffer. aio_multiIO
 * handles read and write operations on data held in an array of buffers.
 * <p>
 * IO operations may complete immediately - ie they are complete before the call to
 * aio_read (etc) returns. In this case, the data about the completed operation is
 * returned when the call returns.
 * <p>
 * Alternatively, the requested IO operations may not complete before the call to
 * aio_read (etc) returns. Such operations are said to be Pending - and will complete
 * at a later time, as signalled by the operating system. IO completion events are
 * signalled to the native library by mechanisms which vary depending on the facilities
 * provided by the operating system. It is the responsibility of the native library
 * to capture these events. The completion events are returned to the application by
 * (one or more) threads calling the aio_getioev method. Threads can block waiting for
 * completion events within the aio_getioev method. At most one completion event is
 * returned to the application when a thread returns from aio_getioev. No event is
 * returned if there is a timeout while waiting or if an error occurs.
 * <eul>
 */
public class AsyncLibrary implements IAsyncProvider {

    protected static final TraceComponent tc = Tr.register(AsyncLibrary.class,
                                                           TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                           TCPChannelMessageConstants.TCP_BUNDLE);

    // This is the name of the library containing the async primitives
    protected static final String LIBRARY_NAME = AsyncProperties.libraryName;

    // This is the maximum number of outstanding async calls we expect to have
    static final int MAX_IDENTIFIERS = AsyncProperties.maxIdentifiers;

    // Capabilities of the underlying native library
    static private int capabilities = 0;

    // Code for initializing and closing the natives library
    static private volatile IAsyncProvider instance = null;
    static LinkedList<Long> completionPorts = new LinkedList<Long>();

    static Object oneAtATime = new Object();

    static final int AIO_NOT_INITIALIZED = 0;
    static final int AIO_INITIALIZED = 1;
    static final int AIO_SHUTDOWN = 2;
    static volatile int aioInitialized = AIO_NOT_INITIALIZED;

    protected static CircularObjectPool completionKeyPool;
    int compKeyPoolSize = AsyncProperties.COMPLETION_KEY_POOL_SIZE_DEFAULT;

    protected boolean doNativeIOCBInitAndTerm = true;

    /**
     * Method which cancels an in-process asynchronous IO operation
     * 
     * The native code should attempt to cancel the underlying IO operation, if possible.
     * Once this method has been called, the native code can assume that the Async IO Java
     * code is no longer interested in the result of the IO operation.
     * This code must return if the cancel was able to be done successfully or not.
     * If done successfully, then the caller can assume that the read/write buffers
     * for this cancelled operation will NOT be accessed by the TCP Channel until
     * another read/write is requested.
     * 
     * @param handle
     *            the handle (file descriptor) identifying the channel
     * @param identifier
     *            the identifier of the IO operation to cancel
     * @throws AsyncException
     * @return 0 - cancel was successful, 1 - cancel attempt failed
     */
    protected static native int aio_cancel2(long handle, long identifier) throws AsyncException;

    /**
     * Method which closes the resources associated with the native mechanisms used to wait for
     * asynchronous events to occur (IO Completion Ports, sys_epoll File Descriptors, etc). The
     * intent is to ensure that any threads waiting on these mechanisms are freed so that they can
     * terminate when the Async IO library is shutting down
     * 
     */
    protected static native void aio_closeport2(long completionPort) throws AsyncException;

    /**
     * Dispose the file handle when async operations complete (BEFORE closing File Handle)
     * 
     * If the native code has any local data associated with the file handle, it should free
     * the data. If the native code has wrappered the file descriptor in some way, it should
     * unwrapper the file descriptor and return the original file handle.
     * 
     * @param handle
     *            the handle (file descriptor) identifying the channel
     * 
     * @return
     *         the handle (file descriptor)
     */
    protected static native long aio_dispose(long handle) throws AsyncException;

    /**
     * Return the data for a completed asynchronous operation.
     * 
     * This call blocks until completion data is available for an operation or the timeout occurs.
     * Multiple threads can call aio_getioev at the same time. Only one thread returns
     * with the completion data for a particular IO operation. Which thread gets the data
     * for a particular IO operation is not defined.
     * <p>
     * The call is made with an address of a DirectByte Buffer which contains a long[4] completionData
     * array.
     * Upon successful dequeueing, the completionData array contains:
     * 
     * <pre>
     * ioev[0] - the channel identifier
     * ioev[1] - the call identifier
     * ioev[2] - the error code for a failed IO operation, or 0 if successful
     * ioev[3] - the number of bytes affected by a successful IO operation
     * </pre>
     * 
     * @param bufferAddress
     *            the (native) address of the buffer used to retrieve the event data
     * @param timeout
     *            the integer number of milliseconds to wait for an event.
     * @return <code>true</code> if the data is valid, <code>false</code> if a timeout occurred.
     * @throws AsyncException
     *             if the dequeueing failed for a reason other than a timeout.
     */
    protected static native boolean aio_getioev2(long bufferAddress, int timeout, long completionPort) throws AsyncException;

    protected static native int aio_getioev3(long[] b00, int size,
                                             int timeout, long completionPort) throws AsyncException;

    /**
     * Initialize the async natives library.
     * 
     * @param cacheSize
     *            size of the cache (max number of entries) for internal data structures allocated in
     *            a cache. This cache concerns data structures shared between the invoking threads and the threads
     *            handling asynchronous events returned by the operating systems.
     * @param throwableClazz
     *            an Exception class which must be throwable by the native code
     * @return An integer which contains a set of bits describing the capabilities of the native code
     *         for optional functions (eg multi IO operations for files and for sockets)
     * @throws AsyncException
     */
    protected static native int aio_init(int cacheSize, Class<?> throwableClazz) throws AsyncException;

    /**
     * Find a message text for a native error code.
     * 
     * @param errorCode the numeric error code for which a text string is desired.
     * @param msg the output byte array that will contain the error code text, if
     *            text could be found for this error code.
     * @return the size of the text message for this error code. 0 if no message was found
     */
    protected static native int aio_getErrorString(int errorCode, byte[] msg) throws AsyncException;

    /**
     * Prepare the given file handle for use in async operations.
     * 
     * The native code may need to wrapper the original file handle so that additional data can be
     * attached to the file handle to enable the native code to work well. If this is done, the
     * wrappered version of the file handle is returned by this method and is used in all subsequent
     * method calls relating to this channel.
     * 
     * @param handle
     *            the original file descriptor for this channel
     * @return the file descriptor (wrappered if necessary)
     * @throws AsyncException
     */
    protected static native long aio_prepare2(long handle, long completionPort) throws AsyncException;

    protected static native long aio_newCompletionPort() throws AsyncException;

    /**
     * Perform an asynchronous multi read or write operation on a specified channel, where the data involved is handled in
     * an array of buffers. The intent is that the data is spread across a set of buffers. The operations start at the
     * beginning of the first buffer and extend sequentially acorss each of the buffers in turn.
     * 
     * The operation may complete immediately,
     * in which case the results are returned by this method. If the operation does not complete immediately, the
     * results are returned via the <code>aio_getioev</code> method when the operation does eventually complete.
     * 
     * @param iobufaddress the address of an array of data passed to the native code:
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
     * @param position - the byte position in the file for the read/write operation to start. Not used for socket IO operations
     * @param count - the number of buffers in the ioev array. Must be >0.
     * @param isRead - true if this is a read request, otherwise it is a write request
     * @param forceQueue - true if io should NOT be attempted before given to polling code
     * @param bytesRequested - minimum number of bytes requested to read. If '0', native code should
     *            attempt an immediate read and return results without handling off to polling code
     * @param useJITBuffer - true if native code should use jit buffer provided by getioev2 if no data is read
     *            prior to handing of to polling code
     * @return <code>true</code> if the operation completed immediately, <code>false</code> if the operation will
     *         complete asynchronously.
     * @throws AsyncException
     */
    protected static native boolean aio_multiIO3(long iobufaddress, long position, int count,
                                                 boolean isRead, boolean forceQueue, long bytesRequested, boolean useJITBuffer) throws AsyncException;

    /**
     * Shut down the async natives library, freeing up any outstanding resources.
     * The library cannot be used after this method is invoked.
     */
    protected static native void aio_shutdown() throws AsyncException;

    /**
     * Prepare a CompletionKey structure for use
     * 
     * @param address the address of the DirectByteBuffer of the completion key
     */
    protected static native void aio_initIOCB(long address) throws AsyncException;

    /**
     * Clean up a CompletionKey structure after use
     * 
     * @param address the address of the DirectByteBuffer of the completion key
     */
    protected static native void aio_termIOCB(long address) throws AsyncException;

    /**
     * Initialize the provider.
     * 
     * @throws AsyncException
     */
    private static void initialize() throws AsyncException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initialize");
        }

        capabilities = aio_init(MAX_IDENTIFIERS, AsyncException.class);
        aioInitialized = AIO_INITIALIZED;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initialize: " + capabilities);
        }
    }

    /**
     * Shutdown the AIO library.
     */
    public static void shutdown() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "shutdown");
        }
        synchronized (oneAtATime) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "have lock"); // running with fixes (2)");
            }

            if (aioInitialized == AIO_INITIALIZED) {
                // mark the library as shutdown
                aioInitialized = AIO_SHUTDOWN;
                // Shut down sequence for the library...
                closeAllCompletionPorts();
                try {
                    aio_shutdown();
                } catch (AsyncException ae) {
                    // ignore shutdown errors
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "AsyncException occured while shutting down: " + ae.getMessage());
                    }
                }
            }
        } // end-sync
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "shutdown");
        }
    }

    /**
     * Get the singleton instance of the async library.
     * 
     * @return IasyncProvider
     */
    public static IAsyncProvider getInstance() {
        if (instance != null) {
            return instance;
        }
        try {
            return createInstance();
        } catch (AsyncException x) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error getting async provider instance, exception: " + x.getMessage());
            }
            FFDCFilter.processException(x, "com.ibm.io.async.AsyncLibrary", "331");
            return null;
        }
    }

    /**
     * Find or create the AIO provider.
     * 
     * @return IAsyncProvider
     * @throws AsyncException
     */
    public static synchronized IAsyncProvider createInstance() throws AsyncException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createInstance");
        }

        if (instance == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "instance is null.  Instantiating new AsyncLibrary ");
            }
            instance = new AsyncLibrary();
        }

        if ((aioInitialized == AIO_NOT_INITIALIZED) || (aioInitialized == AIO_SHUTDOWN)) {
            initialize();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "createInstance");
        }

        return instance;
    }

    /**
     * Default constructor.
     */
    protected AsyncLibrary() throws AsyncException {
        // Loading & initialization of native library
        AsyncException loadException = AccessController.doPrivileged(new PrivLoadLibrary());
        if (loadException != null) {
            throw loadException;
        }
        // native library loaded, try to initialize it
        initialize();

        // setup a pool for CompletionKeys
        if (AsyncProperties.sCompKeyPoolSize != null) {
            compKeyPoolSize = Integer.parseInt(AsyncProperties.sCompKeyPoolSize);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "CompKeyPoolSize is: " + compKeyPoolSize);
        }

        completionKeyPool = new CircularObjectPool(compKeyPoolSize);
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#hasCapability(int)
     */
    @Override
    public boolean hasCapability(int capability) {
        return (capabilities & capability) != 0;
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#cancel2(long, long)
     */
    @Override
    public int cancel2(long channelId, long callId) throws AsyncException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "cancel2: channel = " + channelId + " call id = " + callId);
        }
        int rc = 0;
        synchronized (oneAtATime) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "have lock");
            }
            if (aioInitialized == AIO_INITIALIZED) {
                try {
                    // Ask the native code to perform a cancel operation
                    rc = aio_cancel2(channelId, callId);
                } catch (Throwable t) {
                    if (aioInitialized != AIO_SHUTDOWN) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "caught throwable:" + t);
                        }
                        throw new AsyncException("Throwable caught from aio dll: aio_cancel2: " + t.getMessage());
                    }
                }
            }
        } // end-sync
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "cancel2: " + rc);
        }
        return rc;
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#dispose(long)
     */
    @Override
    public long dispose(long channelIdentifier) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "dispose", Long.valueOf(channelIdentifier));
        }
        long rc = 0L;
        synchronized (oneAtATime) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "have lock");
            }

            if (aioInitialized == AIO_INITIALIZED) {

                try {
                    rc = aio_dispose(channelIdentifier);
                } catch (AsyncException ae) {
                    // just log any errors
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error disposing channel: " + channelIdentifier + ", error: " + ae.getMessage());
                    }
                } catch (Throwable t) {
                    if (aioInitialized != AIO_SHUTDOWN) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "caught throwable:" + t);
                        }
                        throw new RuntimeException("Throwable caught from aio dll: aio_dispose: " + t.getMessage());
                    }
                }
            }
        } // end-sync
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "dispose");
        }
        return rc;
    }

    class PrivLoadLibrary implements PrivilegedAction<AsyncException> {
        /** Constructor */
        public PrivLoadLibrary() {
            // nothing to do
        }

        @Override
        public AsyncException run() {
            try {

                String os = System.getProperty("os.name").toLowerCase();

                if ((os.contains("sunos"))
                    || (os.contains("solaris"))
                    || (os.contains("hp-ux"))
                    || (os.startsWith("aix"))
                    || (os.startsWith("z/os"))
                    || (os.indexOf("os/390") > -1)
                    || (os.indexOf("linux") != -1)) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "on " + os + ", won't be calling Init/TermIOCB");
                    }
                    doNativeIOCBInitAndTerm = false;

                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "on " + os + ", will be calling Init/TermIOCB");
                    }
                    doNativeIOCBInitAndTerm = true;
                }

                if (os.equals("os/400")) {
                    System.load(LIBRARY_NAME);
                } else {
                    // Get the asyncIOHelper and pass it the loadLibrary request for this implementation class.
                    ChannelFrameworkImpl chfw = ChannelFrameworkImpl.getRef();
                    AsyncIOHelper asyncIOHelper = chfw.getAsyncIOHelper();
                    if (asyncIOHelper != null) {
                        asyncIOHelper.loadLibrary(AsyncLibrary.class, LIBRARY_NAME);
                    } else {
                        System.loadLibrary(LIBRARY_NAME);
                    }
                }
            } catch (Throwable t) {
                return new AsyncException("could not load native AIO support library. Exception " + t.getMessage());
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loaded native AIO library: " + LIBRARY_NAME);
            }
            return null;
        }
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#getCompletionData2(long, int, long)
     */
    @Override
    public boolean getCompletionData2(long bufferAddress, int timeout, long completionPort) throws AsyncException {
        boolean gotData = false;

        if (aioInitialized == AIO_INITIALIZED) {
            gotData = aio_getioev2(bufferAddress, timeout, completionPort);
            if (aioInitialized == AIO_SHUTDOWN) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getCompletionData2: Shutdown after callout");
                }
                gotData = false;
            }
        }

        return gotData;
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#getCompletionData3(long[], int, int, long)
     */
    @Override
    public int getCompletionData3(long[] iocbs, int size,
                                  int timeout, long completionPort)
                    throws AsyncException {
        int gotData = 0;

        if (aioInitialized == AIO_INITIALIZED) {
            gotData = aio_getioev3(iocbs, size, timeout, completionPort);
            if (aioInitialized == AIO_SHUTDOWN) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Shutdown after callout");
                }
                gotData = 0;
            }
        }

        return gotData;
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#prepare2(long, long)
     */
    @Override
    public long prepare2(long fd, long completionPort) throws AsyncException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "prepare2: fd = " + fd + " port = " + completionPort);
        }
        return aio_prepare2(fd, completionPort);
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#getNewCompletionPort()
     */
    @Override
    public synchronized long getNewCompletionPort() throws AsyncException {
        long port = aio_newCompletionPort();
        completionPorts.add(Long.valueOf(port));
        return port;
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#isCompletionPortValid(long)
     */
    @Override
    public synchronized boolean isCompletionPortValid(long completionPort) {
        for (Long port : completionPorts) {
            if (port.longValue() == completionPort) {
                return true;
            }
        }
        return false;
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#closeCompletionPort(long)
     */
    @Override
    public synchronized void closeCompletionPort(long completionPort) {
        final int listSize = completionPorts.size();
        for (int i = 0; i < listSize; i++) {
            if (completionPorts.get(i).longValue() == completionPort) {
                try {
                    aio_closeport2(completionPort);
                } catch (AsyncException ae) {
                    // just log any errors and continue
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error closing completion port: " + completionPort + ", error:" + ae.getMessage());
                    }
                }
                completionPorts.remove(i);
                break;
            }
        }
    }

    private static void closeAllCompletionPorts() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "closeAllCompletionPorts");
        }
        try {
            while (!completionPorts.isEmpty()) {
                Long completionPort = completionPorts.removeFirst();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Closing completion port: " + completionPort);
                }
                try {
                    aio_closeport2(completionPort.longValue());
                } catch (AsyncException ae) {
                    // just log any errors and continue
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Error closing completion port: " + completionPort + ", error:" + ae.getMessage());
                    }
                }
            }
        } catch (NoSuchElementException nsee) {
            // no more elements in list, so just exit
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "closeAllCompletionPorts");
        }
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#multiIO3(long, long, int, boolean, boolean, long, boolean)
     */
    @Override
    public boolean multiIO3(long iobufferAddress, long position, int count,
                            boolean isRead, boolean forceQueue,
                            long bytesRequested, boolean useJITBuffer) throws AsyncException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "multiIO3: pos=" + position + " count=" + count);
        }
        return aio_multiIO3(iobufferAddress, position, count, isRead, forceQueue, bytesRequested, useJITBuffer);
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#initializeIOCB(com.ibm.io.async.CompletionKey)
     */
    @Override
    public void initializeIOCB(CompletionKey theKey) throws AsyncException {

        // initIOCB will cross into native code, so do not call initIOCB
        // on unix platforms, since they don't do anything in this method
        if (doNativeIOCBInitAndTerm) {
            aio_initIOCB(theKey.getAddress());
        }
    }

    /*
     * @see com.ibm.io.async.IAsyncProvider#terminateIOCB(com.ibm.io.async.CompletionKey)
     */
    @Override
    public void terminateIOCB(CompletionKey theKey) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "terminateIOCB");
        }
        // termIOCB will cross into native code, so do not call termIOCB
        // on unix platforms, since they don't do anything in this method
        if (doNativeIOCBInitAndTerm && AIO_INITIALIZED == aioInitialized) {

            synchronized (oneAtATime) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "have lock");
                }
                if (aioInitialized == AIO_INITIALIZED) {

                    try {
                        aio_termIOCB(theKey.getAddress());
                    } catch (AsyncException ae) {
                        // just log the error and go on
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Error occured while terminating IOCB" + ae.getMessage());
                        }
                    } catch (Throwable t) {
                        if (aioInitialized != AIO_SHUTDOWN) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "caught throwable: " + t);
                            }
                            throw new RuntimeException("Throwable caught from aio dll: aio_termIOCB: " + t.getMessage());
                        }
                    }
                }
            } // end-sync
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "terminateIOCB");
        }
    }

    // cache error messages based on error codes
    static class ErrorMessageCache {
        ErrorMessageCache next;
        int errorCode;
        String message;

        ErrorMessageCache(int code, String msg) {
            this.errorCode = code;
            this.message = msg;
        }

        static final AtomicReference<ErrorMessageCache> cache = new AtomicReference<ErrorMessageCache>();

        protected static String get(int errorCode) {
            ErrorMessageCache head = cache.get(), entry;
            for (entry = head; entry != null; entry = entry.next) {
                // we can traverse the list without lock since the nodes are not removed
                if (entry.errorCode == errorCode) {
                    break;
                }
            }
            if (entry == null) {
                // not cached
                byte[] msg = new byte[255];
                int len = 0;
                String sRet = null, message;
                try {
                    len = aio_getErrorString(errorCode, msg);
                } catch (AsyncException ae) {
                    // getting error string failed, so use internal error string
                    sRet = "Unspecified error, error code: " + errorCode;
                }
                if (len != 0) {
                    sRet = new String(msg, 0, len);
                }

                if (sRet != null) {
                    message = "RC: " + errorCode + "  " + sRet;
                } else {
                    message = "Error Return Code: " + errorCode;
                }
                entry = new ErrorMessageCache(errorCode, message);

                // add a new cache entry (this could add a redundant entry)
                ErrorMessageCache head2;
                do {
                    head2 = cache.get();
                    entry.next = head2;
                } while (!cache.weakCompareAndSet(head2, entry));

            }
            return entry.message;
        }
    }

    /**
     * Get an IOException instance for the input description and native
     * AIO return coded.
     * 
     * @param desc
     * @param code
     * @return IOException
     */
    public static IOException getIOException(String desc, int code) {
        return new IOException(desc + ErrorMessageCache.get(code));
    }

}
