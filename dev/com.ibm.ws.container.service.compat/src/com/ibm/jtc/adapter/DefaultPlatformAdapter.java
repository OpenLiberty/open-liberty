/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jtc.adapter;

//Alex import static com.ibm.ffdc.Manager.Ffdc;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * Adapter class to give access to internals when running on a
 * Sun class library based JRE.
 */
public class DefaultPlatformAdapter implements IPlatformAdapter {
    private final static String CLASSNAME = DefaultPlatformAdapter.class.getName();
    private final static TraceComponent tc = Tr.register(CLASSNAME, "Runtime", "com.ibm.ws.runtime.runtime");

    private static Field svAddrField;
    private static Field svThreadLocalsField;

    static class StartPrivilegedThread2 implements PrivilegedAction {

        public StartPrivilegedThread2() {

        }

        public Object run() {
            try {
                Class<?> bufClass = Class.forName("java.nio.Buffer");
                svAddrField = bufClass.getDeclaredField("address");
                svAddrField.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    /**
     * Do as much reflection as we can in a static initializer
     * 
     * DirectByteBuffers have a field called "address" which holds
     * the physical address of the start of the buffer contents in
     * native memory. This static block obtains the value of the
     * address field through reflection.
     */
    static {
        StartPrivilegedThread2 privThread = new StartPrivilegedThread2();
        AccessController.doPrivileged(privThread);

        try {
            svThreadLocalsField = Thread.class.getDeclaredField("threadLocals");
            svThreadLocalsField.setAccessible(true);
        } catch (Throwable t) {
            // Alex Ffdc.log(t, DefaultPlatformAdapter.class, CLASSNAME, "59");
        }
    }

    class StartPrivilegedThread implements PrivilegedAction<Long> {

        SocketChannel ioSocket;

        StartPrivilegedThread(SocketChannel _ioSocket) {
            ioSocket = _ioSocket;
        }

        public Long run() {
            Long ret;
            try {
                Field fdValField = ioSocket.getClass().getDeclaredField("fdVal");
                fdValField.setAccessible(true);
                ret = Long.valueOf(fdValField.getInt(ioSocket));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return ret;
        }
    }

    /**
     * Get the native address for the specified DirectByteBuffer
     * 
     * @param byteBuffer
     *            Reference to a DirectByteBuffer
     * 
     * @return The native address of the specified DirectByteBuffer
     * 
     * @throws IllegalArgumentException
     *             If the specified buffer is not direct
     */
    public long getByteBufferAddress(ByteBuffer byteBuffer) {
        /*
         * This only works for DIRECT byte buffers. Direct ByteBuffers have a field
         * called "address" which
         * holds the physical address of the start of the buffer contents in native
         * memory.
         * This method obtains the value of the address field through reflection.
         */
        if (!byteBuffer.isDirect()) {
            throw new IllegalArgumentException("The specified byte buffer is not direct");
        }
        try {
            return svAddrField.getLong(byteBuffer);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    /**
     * Get the socket channel handle.
     * 
     * @param socketChannel
     *            The socket channel to get the handle from.
     * 
     * @return The handle for the specified socket channel
     */
    public long getSocketChannelHandle(SocketChannel socketChannel) {
        StartPrivilegedThread privThread = new StartPrivilegedThread(socketChannel);
        return AccessController.doPrivileged(privThread);
    }

    /**
     * Clean up the thread locals for the specified Thread.
     * 
     * @param thread
     *            The thread to clean up
     */
    public void cleanThreadLocals(Thread thread) {
        try {
            svThreadLocalsField.set(thread, null);
        } catch (IllegalAccessException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Unable to clear java.lang.ThreadLocals: ", e);
        }
    }
}
