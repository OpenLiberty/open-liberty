/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.iiop.yoko.helper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import org.apache.yoko.orb.OCI.IIOP.ExtendedConnectionHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactoryHelper;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 *
 */
public abstract class SocketFactoryHelper implements ExtendedConnectionHelper {

    private final TraceComponent tc;
    private static final Encoding CDR_1_2_ENCODING = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2);

    protected SocketFactoryHelper(TraceComponent tc) {
        this.tc = tc;
    }

    @FFDCIgnore(IOException.class)
    protected IOException openSocket(final int port, int backlog, InetAddress address, ServerSocket socket, boolean soReuseAddr) {
        SocketAddress socketAddress = new InetSocketAddress(address, port);

        //This code borrowed from TCPPort in channelFw:
        IOException bindError = null;
        if (!soReuseAddr) {
            //Forced re-use==false custom property
            try {
                attemptSocketBind(socket, socketAddress, false, backlog);
            } catch (IOException e) {
                // no FFDC
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Forced re-use==false bind attempt failed, ioe=" + e);
                }
                bindError = e;
            }
        } else {
            //re-use==true (default)
            // try the standard startup attempts
            try {
                attemptSocketBind(socket, socketAddress, false, backlog);
                //If we are not on Windows and the bind succeeded, we should set reuseAddr=true
                //for future binds.
                if (!isWindows()) {
                    socket.setReuseAddress(true);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ServerSocket reuse set to true to allow for later override");
                    }
                }
            } catch (IOException ioe) {
                // See if we got the error because the port is in waiting to be cleaned up.
                // If so, no one should be accepting connections on it, and open should fail.
                // If that's the case, we can set ReuseAddr to expedite the bind process.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ServerSocket bind failed on first attempt with IOException: " + ioe.getMessage());
                }
                bindError = ioe;
                try {
                    final String hostName = address == null ? "localhost" : address.getHostName();
                    final InetSocketAddress testAddr = AccessController.doPrivileged(new PrivilegedAction<InetSocketAddress>() {
                        @Override
                        public InetSocketAddress run() {
                            return new InetSocketAddress(hostName, port);
                        }
                    });
                    // PK40741 - test for localhost being resolvable before using it
                    if (!testAddr.isUnresolved()) {
                        SocketChannel testChannel = SocketChannel.open(testAddr);
                        // if we get here, socket opened successfully, which means
                        // someone is really listening
                        // so close connection and don't bother trying to bind again
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "attempt to connect to port to check listen status worked, someone else is using the port!");
                        }
                        testChannel.close();
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Test connection addr is unresolvable; " + testAddr);
                        }
                    }
                } catch (IOException testioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to connect to port to check listen status failed with IOException: " + testioe.getMessage());
                    }
                    try {
                        // open (or close) got IOException, retry with reuseAddr on
                        attemptSocketBind(socket, socketAddress, true, backlog);
                        bindError = null;

                    } catch (IOException newioe) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "ServerSocket bind failed on second attempt with IOException: " + newioe.getMessage());
                        }
                        bindError = newioe;
                    }
                }
            }
        }
        return bindError;
    }

    /**
     * Attempt a socket bind to the input address with the given re-use option
     * flag.
     *
     * @param address
     * @param reuseflag
     * @throws IOException
     */
    private void attemptSocketBind(ServerSocket serverSocket, SocketAddress address, boolean reuseflag, int backlog) throws IOException {
        serverSocket.setReuseAddress(reuseflag);
        serverSocket.bind(address, backlog);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerSocket bind worked, reuse=" + serverSocket.getReuseAddress());
        }
    }

    private static String osName;

    private static String getOSName() {
        if (osName == null) {
            osName = System.getProperty("os.name", "unknown");
        }
        return osName;
    }

    private static boolean isWindows() {
        String name = getOSName();
        return name.toLowerCase().startsWith("windows");
    }

    protected ORB orb;
    protected Codec codec;

    /**
     * Initialize the socket factory instance.
     *
     * @param orb The hosting ORB.
     * @param configName The initialization parameter passed to the socket factor.
     *            This contains the abstract name of our configurator,
     *            which we retrieve from a registry.
     */
    @FFDCIgnore({ UnknownEncoding.class, InvalidName.class })
    @Override
    public void init(ORB orb, String configName) {
        this.orb = orb;
        try {
            this.codec = CodecFactoryHelper.narrow(orb.resolve_initial_references("CodecFactory")).create_codec(CDR_1_2_ENCODING);
        } catch (UnknownEncoding e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            // e.printStackTrace();
        } catch (InvalidName e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            // e.printStackTrace();
        }
    }

    protected Socket createPlainSocket(String host, int port) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Creating plain endpoint to " + host + ":" + port);
        return new Socket(host, port);
    }

}
