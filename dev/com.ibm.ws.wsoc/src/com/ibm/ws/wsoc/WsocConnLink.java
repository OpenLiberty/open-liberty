/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.SendHandler;
import javax.websocket.Session;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.wsoc.MessageWriter.WRITE_TYPE;
import com.ibm.ws.wsoc.external.SessionExt;
import com.ibm.ws.wsoc.injection.InjectionProvider;
import com.ibm.ws.wsoc.injection.InjectionProvider12;
import com.ibm.ws.wsoc.injection.InjectionThings;
import com.ibm.ws.wsoc.link.LinkRead;
import com.ibm.ws.wsoc.link.LinkWrite;
import com.ibm.ws.wsoc.link.LinkWriteExt10;
import com.ibm.ws.wsoc.link.LinkWriteFactory;
import com.ibm.ws.wsoc.servercontainer.ServerContainerExt;
import com.ibm.ws.wsoc.servercontainer.ServletContainerFactory;
import com.ibm.ws.wsoc.servercontainer.v10.ServerContainerImplFactory10;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

public class WsocConnLink {

    private static final TraceComponent tc = Tr.register(WsocConnLink.class);

    @Trivial
    public static enum LINK_STATUS {
        IO_OK, IO_NOT_OK, LOCAL_CLOSING, CLOSED
    };

    @Trivial
    public static enum READ_LINK_STATUS {
        OK_TO_READ, BEFORE_READ_ON_WIRE, READ_ON_WIRE, CLOSE_REQUESTED, CLOSE_REQUESTED_FROM_IDLE_TIMEOUT, ON_READ_THREAD, READ_NOT_OK
    };

    @Trivial
    public static enum WRITE_LINK_STATUS {
        OK_TO_WRITE, WRITING, WRITING_PONG, WRITE_NOT_OK
    };

    @Trivial
    public static enum CLOSE_FRAME_STATE {
        NOT_SET, ANTICIPATING, RECEIVED
    }

    @Trivial
    public static enum RETURN_STATUS {
        OK, READ_IN_PROGRESS, WRITE_IN_PROGRESS, IO_NOT_OK, CLOSE
    };

    @Trivial
    public static enum OK_TO_READ {
        NOT_OK, OK_NO_TIMEOUT, OK_CLOSE_FRAME_TIMEOUT
    }

    public LINK_STATUS linkStatus = LINK_STATUS.IO_NOT_OK;
    public READ_LINK_STATUS readLinkStatus = READ_LINK_STATUS.READ_NOT_OK;
    public WRITE_LINK_STATUS writeLinkStatus = WRITE_LINK_STATUS.WRITE_NOT_OK;

    private CLOSE_FRAME_STATE closeFrameState = CLOSE_FRAME_STATE.NOT_SET;

    private final int closeFrameReadTimeout = 30000; // 30 seconds to wait for a close frame seems reasonable
    private final int WAIT_ON_WRITE_TO_CLOSE = 5500; // watchdog timer on waiting to write a close frame once a close has been initiated.
    private final int WAIT_ON_READ_TO_CLOSE = 8500; // watchdog timer on waiting to cancel a read once a close has been initiated.

    public Object linkSync = new Object() {
    };
    public boolean readNotifyTriggered = false;
    public boolean writeNotifyTriggered = false;

    private Endpoint appEndPoint = null;
    private SessionExt wsocSession = null;

    private VirtualConnection vConnection = null;
    private TCPConnectionContext tcpConnection = null;
    private ConnectionLink deviceConnLink = null;
    private TCPReadRequestContext tcpReadContext = null;

    private WsocReadCallback wrc = null;

    private LinkRead linkRead = null;
    private LinkWrite linkWrite = null;

    private EndpointManager endpointManager = null;
    private ParametersOfInterest things = null;

    boolean readWrite = false;

    private final Object SyncReadPushPop = new Object();
    private int readPush = 0;
    private final Object SyncWritePushPop = new Object();
    private int writePush = 0;

    WsByteBuffer writeBufferToRelease = null;

    public static enum DATA_TYPE {
        BINARY, TEXT, PING, PONG, CLOSE, UNKNOWN
    };

    private static final int READ_BUFFER_SIZE = 8192;

    public WsocConnLink() {

    }

    public void initialize(Endpoint ep,
                           EndpointConfig epc,
                           SessionExt ses,
                           TransportConnectionAccess access,
                           boolean clientSide) {

        appEndPoint = ep;
        wsocSession = ses;
        tcpConnection = access.getTCPConnectionContext();
        deviceConnLink = access.getDeviceConnLink();
        vConnection = access.getVirtualConnection();

        TCPWriteRequestContext tcpWriteContext = tcpConnection.getWriteInterface();
        tcpReadContext = tcpConnection.getReadInterface();


        linkWrite = WebSocketVersionServiceManager.getLinkWriteFactory().getLinkWrite();
        linkRead = new LinkRead();

        wrc = new WsocReadCallback();
        wrc.setConnLinkCallback(this);

        if (clientSide) {
            linkWrite.initialize(tcpWriteContext, epc, this, true);
            linkRead.initialize(tcpReadContext, epc, ep, this, false);
        } else {
            linkWrite.initialize(tcpWriteContext, epc, this, false);
            linkRead.initialize(tcpReadContext, epc, ep, this, true);

        }
    }

    public void setParametersOfInterest(ParametersOfInterest value) {
        things = value;
    }

    public ParametersOfInterest getParametersOfInterest() {
        return things;
    }

    public WsocWriteCallback getWriteCallback() {
        WsocWriteCallback wwc = new WsocWriteCallback();
        wwc.setConnLinkCallback(this);
        return (wwc);
    }

    public WsocReadCallback getReadCallback() {
        WsocReadCallback wrc = new WsocReadCallback();
        wrc.setConnLinkCallback(this);
        return (wrc);
    }

    public OK_TO_READ okToStartRead() {

        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "link status: " + linkStatus + " read link status is: " + readLinkStatus + " closeFrameState: " + closeFrameState);
            }

            // check the most common cases, and leave quickly if possible
            if ((linkStatus == LINK_STATUS.IO_OK)
                && (readLinkStatus == READ_LINK_STATUS.OK_TO_READ || readLinkStatus == READ_LINK_STATUS.ON_READ_THREAD)) {

                readLinkStatus = READ_LINK_STATUS.BEFORE_READ_ON_WIRE;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }

                if (closeFrameState == CLOSE_FRAME_STATE.ANTICIPATING) {
                    return OK_TO_READ.OK_CLOSE_FRAME_TIMEOUT;
                } else {
                    return OK_TO_READ.OK_NO_TIMEOUT;
                }

            }

            // ok, now check the less common cases, mostly due to reading and closing at the same time
            if (((readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED)
                 && (linkStatus == LINK_STATUS.LOCAL_CLOSING || closeFrameState == CLOSE_FRAME_STATE.ANTICIPATING))

                || ((readLinkStatus == READ_LINK_STATUS.OK_TO_READ || readLinkStatus == READ_LINK_STATUS.ON_READ_THREAD)
                    && linkStatus == LINK_STATUS.LOCAL_CLOSING && closeFrameState == CLOSE_FRAME_STATE.ANTICIPATING)

                || (readLinkStatus == READ_LINK_STATUS.ON_READ_THREAD && linkStatus == LINK_STATUS.LOCAL_CLOSING
                    && closeFrameState == CLOSE_FRAME_STATE.NOT_SET)) {

                readLinkStatus = READ_LINK_STATUS.BEFORE_READ_ON_WIRE;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }
                return OK_TO_READ.OK_CLOSE_FRAME_TIMEOUT;
            }

            return OK_TO_READ.NOT_OK;
        }
    }

    @FFDCIgnore(InterruptedException.class)
    public RETURN_STATUS okToWrite(boolean okToWait, boolean calledFromClose, boolean calledFromPong) {
        synchronized (linkSync) {

            while (true) {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "okToWrite entry WsocConnLink: " + this.hashCode() + " linkStatus: " + linkStatus + " writeLinkStatus: " + writeLinkStatus);
                }

                if (calledFromClose) {

                    if (((linkStatus == LINK_STATUS.LOCAL_CLOSING)) && (writeLinkStatus == WRITE_LINK_STATUS.OK_TO_WRITE)) {
                        if (calledFromPong) {
                            writeLinkStatus = WRITE_LINK_STATUS.WRITING_PONG;
                        } else {
                            writeLinkStatus = WRITE_LINK_STATUS.WRITING;
                        }
                        return RETURN_STATUS.OK;
                    }
                    if (linkStatus == LINK_STATUS.IO_NOT_OK) {
                        return RETURN_STATUS.IO_NOT_OK;
                    }
                } else {

                    if ((linkStatus == LINK_STATUS.IO_OK) && (writeLinkStatus == WRITE_LINK_STATUS.OK_TO_WRITE)) {
                        if (calledFromPong) {
                            writeLinkStatus = WRITE_LINK_STATUS.WRITING_PONG;
                        } else {
                            writeLinkStatus = WRITE_LINK_STATUS.WRITING;
                        }
                        return RETURN_STATUS.OK;
                    }

                    if ((linkStatus == LINK_STATUS.IO_NOT_OK) || (linkStatus == LINK_STATUS.LOCAL_CLOSING)) {
                        return RETURN_STATUS.IO_NOT_OK;
                    }
                }

                if ((okToWait) || (writeLinkStatus == WRITE_LINK_STATUS.WRITING_PONG)) {
                    try {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "writeSync.wait()");
                        }
                        writeNotifyTriggered = false;

                        if (calledFromClose) {
                            // one shot wait, if we can't get control after 5 seconds, then give up and force the close down
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "okToWrite WsocConnLink: " + this.hashCode() + " linkSync.wait(...)");
                            }
                            linkSync.wait(WAIT_ON_WRITE_TO_CLOSE);
                            if ((linkStatus == LINK_STATUS.LOCAL_CLOSING) || (linkStatus == LINK_STATUS.IO_OK)) {
                                // write of close frame will likely fail, but ensure that we will try to close the device link
                                return RETURN_STATUS.OK;
                            } else {
                                // just leave, close of connection has already occurred from the other side and this thread didn't detect it
                                // link will have been closed as the close from the other side has already been processed.
                                return RETURN_STATUS.IO_NOT_OK;
                            }

                        } else {
                            while (writeNotifyTriggered == false) {
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "okToWrite WsocConnLink: " + this.hashCode() + " linkSync.wait()");
                                }
                                linkSync.wait();
                            }
                        }
                    } catch (InterruptedException e) {
                        // do NOT allow instrumented FFDC to be used here
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "unexpected InterruptedException");
                        }
                        if (calledFromClose) {
                            return RETURN_STATUS.OK;
                        }
                    }

                } else {
                    if (writeLinkStatus == WRITE_LINK_STATUS.WRITING) {
                        return RETURN_STATUS.WRITE_IN_PROGRESS;
                    }
                    // should not get here
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "unexpected link status");
                    }
                    return RETURN_STATUS.IO_NOT_OK;
                }

            }
        }
    }

    @FFDCIgnore(InterruptedException.class)
    public void waitToClose() {

        synchronized (linkSync) {

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "waitToClose entry WsocConnLink: " + this.hashCode() + " linkStatus: " + linkStatus + " writeLinkStatus: " + writeLinkStatus);
            }

            while (true) {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "linkStatus: " + linkStatus + "  writeLinkStatus: " + writeLinkStatus);
                }

                if ((writeLinkStatus != WRITE_LINK_STATUS.WRITING) && (writeLinkStatus != WRITE_LINK_STATUS.WRITING_PONG)) {
                    return;
                }

                try {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "linkSync.wait()");
                    }
                    writeNotifyTriggered = false;
                    while (writeNotifyTriggered == false) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "waitToClose WsocConnLink: " + this.hashCode() + " linkSync.wait() ");
                        }
                        linkSync.wait();
                    }
                } catch (InterruptedException e) {
                    // do NOT allow instrumented FFDC to be used here
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "unexpected InterruptedException");
                    }
                }
            }
        }
    }

    public READ_LINK_STATUS signalReadComplete() {
        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
            }
            if ((readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED) || (readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED_FROM_IDLE_TIMEOUT)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "linkSync.notify()");
                }
                if (readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED_FROM_IDLE_TIMEOUT) {
                    readLinkStatus = READ_LINK_STATUS.OK_TO_READ;
                }

                readNotifyTriggered = true;
                linkSync.notifyAll();
            }

            if ((readLinkStatus != READ_LINK_STATUS.CLOSE_REQUESTED)) {
                readLinkStatus = READ_LINK_STATUS.OK_TO_READ;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }
            }
            return readLinkStatus;
        }
    }

    public boolean processReadErrorComplete(IOException ioe) {
        // return true if new read is needed.

        // need a couple of flags so we can move method calls to outside the sync block
        boolean closeFromHere = false;
        String s = null;

        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "processReadErrorComplete: readLinkStatus: " + readLinkStatus + " closeFrameState: " + closeFrameState);
            }

            // If this is a timeout when we were waiting for the close frame, then just shut things down
            if (closeFrameState == CLOSE_FRAME_STATE.ANTICIPATING) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "processReadErrorComplete :closeframe: closing connection on timeout on reading for a close frame");
                }
                linkStatus = LINK_STATUS.CLOSED;
                closeFrameState = CLOSE_FRAME_STATE.RECEIVED;
                deviceConnLink.close(vConnection, null);
                return false;
            }

            if ((ioe instanceof SocketTimeoutException)
                && (readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED || readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED_FROM_IDLE_TIMEOUT)) {
                // the NO_TIMEOUT/Current read should now be cancelled, and a close frame has been sent or will be sent,
                // so signal we want a timeout read for the close frame

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "processReadErrorComplete :closeframe: received timeout.  notify waiting threads");
                }

                closeFrameState = CLOSE_FRAME_STATE.ANTICIPATING;

                // if it was "from idle", now we are just like a normal close
                readLinkStatus = READ_LINK_STATUS.CLOSE_REQUESTED;

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "linkSync.notify()");
                }
                readNotifyTriggered = true;
                linkSync.notifyAll();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "processReadErrorComplete :closeframe: reading for a close frame");
                }
                return true;

            } else {

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "linkSync.notify() from unexpected error");
                }
                readNotifyTriggered = true;
                linkSync.notifyAll();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "linkStatus: " + getLinkStatus() + " readLinkStatus: " + readLinkStatus);
                }

                // unexpected close of this connection. Close on this thread if we are not already closing.
                // Change read status for possible attempt to read
                // in a close frame later (which may just be a fool's errand after getting an unexpected read error)
                if ((getLinkStatus() != LINK_STATUS.CLOSED) && (getLinkStatus() != LINK_STATUS.LOCAL_CLOSING)) {

                    if (ioe != null) {
                        s = ioe.getMessage();
                    }
                    closeFromHere = true;
                }
            }
        }

        // close out of the sync block
        if (closeFromHere) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "processReadErrorComplete :closeframe: got and unexpected error, so send a close frame and close the connection");
            }
            callOnClose(s, CloseReason.CloseCodes.UNEXPECTED_CONDITION);
        }

        return false;
    }

    public void signalNotReading() {
        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
            }

            if ((readLinkStatus != READ_LINK_STATUS.CLOSE_REQUESTED)) {
                readLinkStatus = READ_LINK_STATUS.OK_TO_READ;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }
            }
        }
    }

    public void signalNotWriting() {
        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "signalNotWriting WsocConnLink: " + this.hashCode());
            }
            writeLinkStatus = WRITE_LINK_STATUS.OK_TO_WRITE;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "signalNotWriting WsocConnLink: " + this.hashCode() + " linkSync.notifyAll()");
            }
            writeNotifyTriggered = true;
            linkSync.notifyAll();
        }
    }

    public boolean signalLocalClose() {
        synchronized (linkSync) {
            LINK_STATUS x = getLinkStatus();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "signalLocalClose WsocConnLink: " + this.hashCode() + " entry linkStatus: " + x);
            }

            if ((x == LINK_STATUS.CLOSED) || (x == LINK_STATUS.LOCAL_CLOSING) || (x == LINK_STATUS.IO_NOT_OK)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Close has been called while a previous close/destroy is in progress. Link Status: " + getLinkStatus());
                }
                return true;
            }

            linkStatus = LINK_STATUS.LOCAL_CLOSING;
            return false;
        }
    }

    public boolean signalClose() {
        // return true if this was closed/closing/"io not ok" coming in, else return false
        // set status to close either way.

        boolean rc = false;
        synchronized (linkSync) {

            LINK_STATUS x = getLinkStatus();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "signalClosed WsocConnLink: " + this.hashCode() + " entry linkStatus: " + x);
            }

            if ((x == LINK_STATUS.CLOSED) || (x == LINK_STATUS.LOCAL_CLOSING) || (x == LINK_STATUS.IO_NOT_OK)) {
                rc = true;
            }

            linkStatus = LINK_STATUS.CLOSED;
        }
        return rc;
    }

    public void setLinkStatusesToOK() {
        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "setLinkStatusesToOK WsocConnLink: " + this.hashCode());
            }

            linkStatus = LINK_STATUS.IO_OK;
            readLinkStatus = READ_LINK_STATUS.OK_TO_READ;
            writeLinkStatus = WRITE_LINK_STATUS.OK_TO_WRITE;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
            }
        }
    }

    public void setLinkStatusToNotOK() {
        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "setLinkStatusToNotOK WsocConnLink: " + this.hashCode());
            }
            linkStatus = LINK_STATUS.IO_NOT_OK;
            readLinkStatus = READ_LINK_STATUS.READ_NOT_OK;
            writeLinkStatus = WRITE_LINK_STATUS.WRITE_NOT_OK;
        }
    }

    public LINK_STATUS getLinkStatus() {
        synchronized (linkSync) {
            return linkStatus;
        }
    }

    public void setReadLinkStatus(READ_LINK_STATUS newStatus) {
        synchronized (linkSync) {
            readLinkStatus = newStatus;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
            }
        }
    }

    public CLOSE_FRAME_STATE getCloseFrameState() {
        return closeFrameState;
    }

    public boolean anticipatingCloseFrame() {
        if (closeFrameState == CLOSE_FRAME_STATE.ANTICIPATING) {
            return true;
        } else {
            return false;
        }
    }

    public void setReadLinkStatusAndCloseFrameState(READ_LINK_STATUS newStatus, CLOSE_FRAME_STATE newState) {
        synchronized (linkSync) {
            readLinkStatus = newStatus;
            closeFrameState = newState;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "readLinkStatus: " + readLinkStatus + " closeFrameState: " + closeFrameState);
            }
        }
    }

    @FFDCIgnore(IOException.class)
    public void close(CloseReason cr, boolean needToCloseSession, boolean cleanupRead) {
        //signal to close, return if already closing
        boolean closedAlready = false;
        closedAlready = signalClose();

        try {
            // tell the user app that a close is going to happen
            if (!closedAlready) {
                appEndPoint.onClose(wsocSession, cr);
            }
        } finally {

            if (needToCloseSession) {
                if (wsocSession != null) {
                    wsocSession.getSessionImpl().close(cr, false);
                }
            }

            // once the user's onClose has been called, we can now remove the session from the list
            // notify the endpoint manager that this session should no longer be considered open
            removeSession();

            if ((cleanupRead == true) && (linkRead != null)) {
                linkRead.resetReader();
            }

            if (!closedAlready) {
                // write a close frame to the client
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "close reason phrase is: " + cr.getReasonPhrase());
                }

                int closeValue = cr.getCloseCode().getCode();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "close reason code is: " + closeValue);
                }

                //close frame data needs to be 125 UTF-8 encoded bytes. First 2 bytes are for close code and next 123 bytes are for reason phrase
                byte[] reasonBytes = cr.getReasonPhrase().getBytes(Utils.UTF8_CHARSET);
                //at this point of execution close reason is already trimmed off to <=123 UTF-8 encoded byte length. Hence no need to check for it again here.
                int len = reasonBytes.length;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "close reason len is: " + len);
                }
                byte[] closeData = new byte[len + 2];
                closeData[0] = (byte) ((closeValue >> 8) & (0x000000FF));
                closeData[1] = (byte) ((closeValue & 0x000000FF));
                System.arraycopy(reasonBytes, 0, closeData, 2, len);
                try {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "close :closeframe: sending close frame. readLinkStatus: " + readLinkStatus);
                    }
                    linkWrite.writeBuffer(getBufferManager().wrap(closeData), OpcodeType.CONNECTION_CLOSE, WRITE_TYPE.SYNC, null, 0);
                } catch (IOException x) {
                    //just log the message in FFDC and trace. Nothing else can be done at this point since onClose() is already called
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught IOException: " + x.getMessage());
                    }
                    // allow instrumented FFDC to be used here
                }
            }

            //close down the link
            
            deviceConnLink.close(vConnection, null);
        }
    }

    private void removeSession() {
        // notify the endpoint manager that this session should no longer be considered open
        if (endpointManager != null) {
            endpointManager.removeSession(appEndPoint, wsocSession);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Endpoint manager was unexpectedly null");
            }
        }
    }

    public void incomingCloseConnection(CloseReason cr) {

        if (getLinkStatus() == LINK_STATUS.LOCAL_CLOSING) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Client has responded to outgoing close request, closing connection");
            }
            waitToClose();
            signalClose();
            deviceConnLink.close(vConnection, null);
        } else {
            waitToClose();
            close(cr, true, false);
        }

    }

    CloseReason idleTimeoutCloseReason = null;

    @FFDCIgnore(InterruptedException.class)
    public boolean finishReadBeforeIdleClose() {
        // Return value denotes if connection should be closed

        synchronized (linkSync) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
            }
            if (readLinkStatus == READ_LINK_STATUS.READ_ON_WIRE) {
                readLinkStatus = READ_LINK_STATUS.CLOSE_REQUESTED_FROM_IDLE_TIMEOUT;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }

                readNotifyTriggered = false;
                linkRead.cancelRead();

                try {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "linkSync.wait(). waiting on read to clear.");
                    }
                    while (readNotifyTriggered == false) {
                        linkSync.wait(WAIT_ON_READ_TO_CLOSE);
                    }
                    if (readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED_FROM_IDLE_TIMEOUT || closeFrameState == CLOSE_FRAME_STATE.ANTICIPATING) {
                        // cancelling the outstanding (idle) read appears to have worked
                        return true;
                    } else {
                        // data was apparently read before we could cancel.
                        return false;
                    }
                } catch (InterruptedException e) {
                    // do NOT allow instrumented FFDC to be used here
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "InterruptedException during close waiting for callback complete or error");
                    }
                }
            } else if (readLinkStatus == READ_LINK_STATUS.ON_READ_THREAD) {
                // read thread could be "idle" up in user code, so we need to close this from here now.  TCK considers threads stuck up in user code as idle.
                synchronized (linkSync) {
                    // assume the Read is stuck up in user code, so no read outstanding.  so write the close frame and be ready to receive the close response.
                    closeFrameState = CLOSE_FRAME_STATE.ANTICIPATING;
                }

                return true;
            }

        }
        return false;
    }

    @FFDCIgnore(InterruptedException.class)
    public void finishReadBeforeClose(SessionIdleTimeout sit) {

        if (sit != null) {
            // clean up resources with the SessionIdleTimeout
            sit.cleanup();
        }

        // cut off close loops, for example the users onClose call close.
        if (checkIfClosingAlready() || linkStatus == LINK_STATUS.IO_NOT_OK) {
            return;
        }

        int loopCounter = 0;
        int loopMsecWait = 100;

        // wait for 3 seconds for read thread to get back to a safe closable state.  Otherwise we need to just close down the connection/session rather than
        // hang waiting for the read thread to do it's thing.
        int loopMax = 3000 / loopMsecWait;

        boolean done = false;
        while ((!done) && (loopCounter < loopMax)) {
            loopCounter++;
            synchronized (linkSync) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }

                //check if the same thread which is executing the read/invoking onMessage() has called session.close(). If yes, no need to wait()
                //because onMessage() is already called on this thread.
                if (readLinkStatus == READ_LINK_STATUS.ON_READ_THREAD) {
                    readLinkStatus = READ_LINK_STATUS.CLOSE_REQUESTED;
                    closeFrameState = CLOSE_FRAME_STATE.ANTICIPATING;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                    }
                    return;
                }

                if (readLinkStatus == READ_LINK_STATUS.READ_ON_WIRE) {
                    readLinkStatus = READ_LINK_STATUS.CLOSE_REQUESTED;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                    }

                    try {
                        readNotifyTriggered = false;
                        linkRead.cancelRead();

                        // PH56266 Check notify before wait
                        if (readNotifyTriggered == false) {
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "linkSync.wait(WAIT_ON_READ_TO_CLOSE).  waiting on Read to clear");
                            }

                            // PH42468 Add a timeout to the wait, in case notifyAll() isn't called
                            linkSync.wait(WAIT_ON_READ_TO_CLOSE);
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "linkSync.wait(WAIT_ON_READ_TO_CLOSE) cleared, continue closing");
                            }
                        }

                        done = true;

                    } catch (InterruptedException e) {
                        // do NOT allow instrumented FFDC to be used here
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "InterruptedException during close waiting for callback complete or error");
                        }
                    }

                    return;
                }
            }

            // wait for the status to get to a place where we can close it
            try {
                // don't freak on seeing the sleep.  this is only to slow the loop down, in case the the read thread is up in user code, and we
                // need to wait for it to return before proceeding.
                Thread.sleep(loopMsecWait);
            } catch (InterruptedException e) {
            }

        }
    }

    public void closeUsingSession(CloseReason cr, boolean readForCloseFrame, boolean rightToDeviceLink) {
        if ((wsocSession != null) && (!rightToDeviceLink)) {
            wsocSession.getSessionImpl().close(cr, true);
        }

        if ((!readForCloseFrame) || (rightToDeviceLink)) {
            linkStatus = LINK_STATUS.CLOSED;
            deviceConnLink.close(vConnection, null);
        }
    }

    public void outgoingCloseConnection(CloseReason cr) {
        // stop close loops. For example, if app code calls session.close() from onClose.
        if (signalLocalClose()) {
            return;
        } ;

        CloseReason updatedCr = new CloseReason(cr.getCloseCode(), cr.getReasonPhrase());

        try {
            appEndPoint.onClose(wsocSession, cr);
        } finally {

            // once the user's onClose has been called, we can now remove the session from the list
            // notify the endpoint manager that this session should no longer be considered open
            removeSession();

            //Construct and send close frame
            int closeValue = updatedCr.getCloseCode().getCode();
            //close frame data needs to be 125 UTF-8 encoded bytes. First 2 bytes are for close code and next 123 bytes are for reason phrase
            byte[] reasonBytes = cr.getReasonPhrase().getBytes(Utils.UTF8_CHARSET);
            //at this point of execution close reason is already trimmed off to <=123 UTF-8 encoded byte length. Hence no need to check for it again here.
            int len = reasonBytes.length;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "close reason len is: " + len);
            }

            byte[] closeData = new byte[len + 2];
            closeData[0] = (byte) ((closeValue >> 8) & (0x000000FF));
            closeData[1] = (byte) ((closeValue & 0x000000FF));
            System.arraycopy(reasonBytes, 0, closeData, 2, len);

            RETURN_STATUS status = okToWrite(true, true, false);
            if (status == RETURN_STATUS.OK) {
                try {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "outgoingCloseConnection :closeframe: sending close frame. readLinkStatus: " + readLinkStatus);
                    }
                    linkWrite.writeBuffer(getBufferManager().wrap(closeData), OpcodeType.CONNECTION_CLOSE, WRITE_TYPE.SYNC, null, WAIT_ON_WRITE_TO_CLOSE);
                    signalNotWriting();
                } catch (Exception x) {
                    //just log the message in FFDC and trace. Nothing else can be done at this point since onClose() is already called
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught Exception: " + x.getMessage());
                    }
                    signalNotWriting();

                    // if local is trying to close, but we can't write out a close frame, then close the link from here
                    if (getLinkStatus() == LINK_STATUS.LOCAL_CLOSING && (readLinkStatus == READ_LINK_STATUS.CLOSE_REQUESTED ||
                                                                         readLinkStatus == READ_LINK_STATUS.ON_READ_THREAD)) {
                        //close down the link ourselves if we can't write to it and we won't be receiving a read close response.
                        deviceConnLink.close(vConnection, null);
                    }
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "could not perform write due to status");
                }
            }
        }
    }

    public void setEndpointManager(EndpointManager x) {
        endpointManager = x;
    }

    public EndpointManager getEndpointManager() {
        return endpointManager;
    }

    public void destroy(Exception e) {
        setLinkStatusToNotOK();

        this.linkRead.destroy(e);
        this.linkWrite.destroy(e);
    }

    public WsByteBufferPoolManager getBufferManager() {
        return ServiceManager.getBufferPoolManager();
    }

    public void startReading() {
        startAsyncRead(tcpReadContext);
    }

    public void processDataThenStartRead(WsByteBuffer remainingBuf) {

        //  data passed in here has position set to 0, and we slice later in the code, set position similar to other
        // buffers returned from a read.
        remainingBuf.position(remainingBuf.limit());
        tcpReadContext.setBuffer(remainingBuf);

        processRead(tcpReadContext);
    }

    public void startAsyncRead(TCPReadRequestContext rrc) {
        OK_TO_READ ok = okToStartRead();
        if (ok != OK_TO_READ.NOT_OK) {
            int timeout = TCPRequestContext.NO_TIMEOUT;

            // allocate a byte buffer to read data into
            WsByteBufferPoolManager mgr = getBufferManager();

            WsByteBuffer buf = mgr.allocate(READ_BUFFER_SIZE);

            rrc.setBuffer(buf);

            boolean forceQueue = true;
            int numBytes = 1; // read at least 1 or more bytes

            if (ok == OK_TO_READ.OK_CLOSE_FRAME_TIMEOUT) {
                timeout = closeFrameReadTimeout;
            }

            // need to test if the read is ok and set the read link status and do the async read inside  the sync block,
            // to avoid race conditions.  With forceQueue true, the async read thread will not do the actual read.
            synchronized (linkSync) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                }
                if ((readLinkStatus == READ_LINK_STATUS.BEFORE_READ_ON_WIRE) || (readLinkStatus == READ_LINK_STATUS.OK_TO_READ)) {
                    readLinkStatus = READ_LINK_STATUS.READ_ON_WIRE;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "readLinkStatus: " + readLinkStatus);
                    }
                    rrc.read(numBytes, wrc, forceQueue, timeout);
                }
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "could not continue to read due to status");
            }
        }
    }

    public void processRead(TCPReadRequestContext rrc) {

        boolean moreToProcess = true;
        while (moreToProcess) {
            moreToProcess = linkRead.processRead(rrc, appEndPoint);
            // Set this for null in case we have more to process, startAsync will set it before doing another read.
            rrc.setBuffer(null);

        }

        startAsyncRead(rrc);

        this.readWrite = true;
    }

    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        linkRead.addMessageHandler(handler);
    }

    public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
        linkRead.addMessageHandler(clazz, handler);
    }

    public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
        linkRead.addMessageHandler(clazz, handler);
    }

    public Set<MessageHandler> getMessageHandlers() {
        return linkRead.getMessageHandlers();
    }

    public void removeMessageHandler(MessageHandler handler) {
        linkRead.removeMessageHandler(handler);
    }

    public RETURN_STATUS writeBuffer(@Sensitive WsByteBuffer buffer, OpcodeType ot, WRITE_TYPE writeType, SendHandler handler, int timeout, boolean wait, boolean fromPong) {

        RETURN_STATUS status = okToWrite(wait, false, fromPong);
        if (status == RETURN_STATUS.OK) {
            try {
                if (writeType == WRITE_TYPE.ASYNC) {
                    writeBufferToRelease = buffer;
                } else {
                    writeBufferToRelease = null;
                }

                linkWrite.writeBuffer(buffer, ot, writeType, handler, timeout);

            } catch (IOException x) {
                if (writeBufferToRelease != null) {
                    writeBufferToRelease.release();
                    writeBufferToRelease = null;
                }
                callOnError(x);
            } catch (RuntimeException up) {
                // make sure  buffer is released
                if (writeBufferToRelease != null) {
                    writeBufferToRelease.release();
                    writeBufferToRelease = null;
                }
                throw up;
            }

            if (writeType == WRITE_TYPE.SYNC) {
                signalNotWriting();
            }

        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "could not perform write due to status");
            }
        }

        return status;
    }

    public RETURN_STATUS writeBufferForBasicRemoteSync(@Sensitive WsByteBuffer buffer, OpcodeType ot, int timeout, boolean wait) throws IOException {

        RETURN_STATUS status = okToWrite(wait, false, false);
        if (status == RETURN_STATUS.OK) {
            linkWrite.writeBuffer(buffer, ot, WRITE_TYPE.SYNC, null, timeout);
            signalNotWriting();
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "could not perform write due to status");
            }
        }
        return status;
    }

    public RETURN_STATUS writeObject(@Sensitive Object objectToWrite, WRITE_TYPE writeType, SendHandler handler) {
        return writeObject(objectToWrite, writeType, handler, false);
    }

    public RETURN_STATUS writeObjectBasicRemoteSync(@Sensitive Object objectToWrite) throws EncodeException, IOException {
        return writeObjectBasicRemoteSync(objectToWrite, WRITE_TYPE.SYNC, false);
    }

    public RETURN_STATUS writeObjectBasicRemoteSync(@Sensitive Object objectToWrite, WRITE_TYPE writeType, boolean fromOnMessage) throws EncodeException, IOException {

        //this call is not from onMessage. Hence waitIfNeeded is always false.
        boolean waitIfNeeded = false;

        // could just of passed fromOnMessage instead of waitOK, but that gives fromOnMessage a double meaning.
        RETURN_STATUS status = okToWrite(waitIfNeeded, false, false);
        if (status == RETURN_STATUS.OK) {
            try {
                linkWrite.writeObject(objectToWrite, writeType, null, fromOnMessage);
            } finally {
                signalNotWriting();
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "could not perform write due to status");
            }
        }
        return status;
    }

    @FFDCIgnore(Throwable.class)
    public RETURN_STATUS writeObject(@Sensitive Object objectToWrite, WRITE_TYPE writeType, SendHandler handler, boolean fromOnMessage) {

        boolean waitIfNeeded = false;

        if (fromOnMessage) {
            waitIfNeeded = true;
        }

        // could just of passed fromOnMessage instead of waitOK, but that gives fromOnMessage a double meaning.
        RETURN_STATUS status = okToWrite(waitIfNeeded, false, false);
        if (status == RETURN_STATUS.OK) {
            try {
                linkWrite.writeObject(objectToWrite, writeType, handler, fromOnMessage);
            } catch (Throwable e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught Exception: " + e);
                }
                if (writeType == WRITE_TYPE.ASYNC) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unexpectedly Caught IOException on Async Write code path: " + e);
                    }
                    signalNotWriting();
                    linkWrite.processError(null, e);
                } else {
                    //call user's onError() method on the same thread
                    callOnError(e);
                }
            }
            if (writeType == WRITE_TYPE.SYNC) {
                signalNotWriting();
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "could not perform write due to status");
            }
        }
        return status;
    }

    public boolean cancelWriteBufferAsync() {
        return linkWrite.cancelWriteBufferAsync();
    }

    public void processWrite(TCPWriteRequestContext wsc) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "processWrite WsocConnLink: " + this.hashCode());
        }

        linkWrite.frameCleanup();
        if (writeBufferToRelease != null) {
            writeBufferToRelease.release();
            writeBufferToRelease = null;
        }
        signalNotWriting();

        linkWrite.processWrite(wsc);
        this.readWrite = true;
    }

    public void processWriteError(TCPWriteRequestContext wsc, IOException ioe) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "processWriteError WsocConnLink: " + this.hashCode());
        }

        if (writeBufferToRelease != null) {
            writeBufferToRelease.release();
            writeBufferToRelease = null;
        }
        signalNotWriting();

        linkWrite.processError(wsc, ioe);
    }

    public Session getWsocSession() {
        return this.wsocSession;
    }

    public void callOnError(Throwable throwable) {
        callOnError(throwable, false);
    }

    public void callOnError(Throwable throwable, boolean whileReading) {
        signalNotWriting();
        if (whileReading) {
            this.setReadLinkStatus(READ_LINK_STATUS.ON_READ_THREAD);
        }
        appEndPoint.onError(wsocSession, throwable);
    }

    public void callOnClose(String reasonPhrase, CloseReason.CloseCode closeCode) {
        boolean cleanupRead = false;

        if (checkIfClosingAlready()) {
            return;
        }

        //length of close reason can only be 123 UTF-8 encoded character bytes length
        reasonPhrase = Utils.truncateCloseReason(reasonPhrase);
        // ok to pass null for either of these parameters
        CloseReason closeReason = new CloseReason(closeCode, reasonPhrase);

        if (readLinkStatus == READ_LINK_STATUS.OK_TO_READ) {
            // error callback unexpectedly called while trying to read, rather than when we know we are closing, so cleanup the message read object
            // because we will not try to read for the close frame
            cleanupRead = true;
        }

        //close connection, device link
        close(closeReason, true, cleanupRead);
    }

    public Endpoint getEndpoint() {
        return this.appEndPoint;
    }

    /**
     * @return the readWrite
     */
    public boolean isReadWrite() {
        return readWrite;
    }

    /**
     * @param readWrite the readWrite to set
     */
    public void setReadWrite(boolean readWrite) {
        this.readWrite = readWrite;
    }

    public void waitReadPush() {
        synchronized (SyncReadPushPop) {
            while (readPush != 0) {
                try {
                    SyncReadPushPop.wait();
                } catch (InterruptedException e) {
                    // nothing to do but wait again
                }
            }
            readPush = 1;
        }
    }

    public void notifyReadPush() {
        synchronized (SyncReadPushPop) {
            readPush = 0;
            SyncReadPushPop.notify();
        }
    }

    public void waitWritePush() {
        synchronized (SyncWritePushPop) {
            while (writePush != 0) {
                try {
                    SyncWritePushPop.wait();
                } catch (InterruptedException e) {
                    // nothing to do but wait again
                }
            }
            writePush = 1;
        }
    }

    public void notifyWritePush() {
        synchronized (SyncWritePushPop) {
            writePush = 0;
            SyncWritePushPop.notify();
        }
    }

    public boolean checkIfClosingAlready() {
        // In case anyone calls session.close() from onClose, this should stop any loops
        synchronized (linkSync) {
            LINK_STATUS x = getLinkStatus();
            if ((x == LINK_STATUS.CLOSED) || (x == LINK_STATUS.LOCAL_CLOSING) || (x == LINK_STATUS.IO_NOT_OK)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Close has been called while a previous close/destroy is in progress. Link Status: " + getLinkStatus());
                }
                return true;
            }
            return false;
        }
    }

    public long getDefaultAsyncSendTimeout() {
        if (wsocSession != null) {
            SessionImpl si = wsocSession.getSessionImpl();
            if (si != null) {
                return si.getDefaultAsyncSendTimeout();
            }
        }
        return 0;
    }

    public int getMaxTextMessageBufferSize() {
        if (wsocSession != null) {
            SessionImpl si = wsocSession.getSessionImpl();
            if (si != null) {
                return si.getMaxTextMessageBufferSize();
            }
        }
        return (int) Constants.DEFAULT_MAX_MSG_SIZE;
    }

    public int getMaxBinaryMessageBufferSize() {
        if (wsocSession != null) {
            SessionImpl si = wsocSession.getSessionImpl();
            if (si != null) {
                return si.getMaxBinaryMessageBufferSize();
            }
        }
        return (int) Constants.DEFAULT_MAX_MSG_SIZE;
    }

    public InjectionThings pushContexts() {

        boolean appActivateResult = false;

        // setup the web app context class loader and Component Metadata for the websocket user app to use.
        ComponentMetaDataAccessorImpl cmdai = null;

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(things.getTccl());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "set class loader to: " + things.getTccl());
        }

        ComponentMetaData cmd = things.getCmd();

        if (cmd != null) {
            cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            cmdai.beginContext(cmd);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "begin Context with CMD of: " + cmd);
            }
        }

        // if CDI 1.2 is loaded, don't need to do anything here
        InjectionProvider12 ip12 = ServiceManager.getInjectionProvider12();
        if (ip12 == null) {
            // only try OWB CDI 1.0 if 1.2 is not loaded
            InjectionProvider ip = ServiceManager.getInjectionProvider();
            if (ip != null) {
                HttpSession httpSession = things.getHttpSession();

                // Activate Application Scope
                appActivateResult = ip.activateAppContext(cmd);

                // Session Scope needs started every time because thread could have an older deactivated Session scope on it
                if (httpSession != null) {
                    ip.startSesContext(httpSession);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "thread ID: " + Thread.currentThread().getId() + "Session ID: " + httpSession.getId());
                    }
                } else if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Attempted to use sessions scope when there was no valid HttpSession, guess the HttpSession expired?");
                }
            } else if (tc.isDebugEnabled()) {
                Tr.debug(tc, "InjectionProvider was null");
            }
        }

        InjectionThings it = new InjectionThings();
        it.setAppActivateResult(appActivateResult);
        it.setOriginalCL(originalCL);

        return it;
    }

    public void popContexts(InjectionThings it) {

        // if CDI 1.2 is loaded, don't need to do anything here
        InjectionProvider12 ip12 = ServiceManager.getInjectionProvider12();
        if (ip12 == null) {
            InjectionProvider ip = ServiceManager.getInjectionProvider();
            if (ip != null) {
                // if cdi injection is being used, then de-activate the application scopes only if our code did the activate
                if (it.getAppActivateResult() == true) {
                    ip.deActivateAppContext();
                }

                HttpSession httpSession = things.getHttpSession();
                //httpSession could be null for IBM WebSocket client API usage scenario.
                //httpSession null means session context was never activated/started in pushContexts method. Hence we should not attempt to deactivate session context
                //which was never activated/started in the first place. Also note that, this is the only way to avoid "Could NOT lazily initialize session context because of
                //null RequestContext" WARNING in the logs. If we call deActivateSesContext when it's not activated/started in the first place in pushContexts method,
                //what happens is deActivateSesContext() calls service.getCurrentContext(scopeType) which calls lazyStartSessionContext() because sessionContext
                //was null. lazyStartSessionContext() results in the above warning because context was null.
                if (httpSession != null) {
                    //attempt to deactivate the session scope always
                    ip.deActivateSesContext();
                }
            }
        }

        // remove access to the Component Metadata and put back the original classloader
        ComponentMetaDataAccessorImpl cmdai = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        if (cmdai != null) {
            cmdai.endContext();
        }
        Thread.currentThread().setContextClassLoader(it.getOriginalCL());
    }

}
