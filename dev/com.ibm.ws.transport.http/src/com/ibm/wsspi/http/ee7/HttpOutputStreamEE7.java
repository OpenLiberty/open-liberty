/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.ee7;

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.outbound.HttpOutputStreamImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.exception.WriteBeyondContentLengthException;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * This class will implement the non-blocking write (Servlet 3.1)
 */
public class HttpOutputStreamEE7 extends HttpOutputStreamImpl {

    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpOutputStreamEE7.class,
                                                         HttpMessages.HTTP_TRACE_NAME,
                                                         HttpMessages.HTTP_BUNDLE);

    // added following for non-blocking Write
    private InterChannelCallback _callback;
    private boolean _isReady = true;
    private boolean _internalReady = true;
    private int _bytesRemainingWhenAsync = 0;
    private byte[] _remValue = null;
    private boolean _dataSaved = false;
    private final Object _lockObj = new Object() {}; /* The braces indicate we want to create and use an anonymous inner class as the actual object instance */
    public Object _writeReadyLockObj = new Object() {};
    public boolean status_not_ready_checked = false;
    public boolean write_crlf_pending = false;
    private boolean exceptionDuringOnWP = false;

    // added above for non-blocking Write

    /**
     * @param context
     */
    public HttpOutputStreamEE7(HttpInboundServiceContext context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    /**
     * @throws Exception
     */
    public void writeRemainingToBuffers() throws Exception {
        this.clearBuffersAfterWrite();
        // if more data remaining now write
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Write out remainig bytes if any --> " + this._bytesRemainingWhenAsync);
        }
        if (this._bytesRemainingWhenAsync > 0) {

            // Make sure complete is not called faster on another thread before remaining data is saved of
            synchronized (this._lockObj) {
                if (!this.isDataSaved()) {
                    this._lockObj.wait();
                }
                this.setDataSaved(false);
            }
            this.set_internalReady(true);
            this.writeToBuffers(this._remValue, 0, this._bytesRemainingWhenAsync);
        }
        else {
            this.set_internalReady(true);
        }
    }

    /**
     * Write the given information to the output buffers.
     * If it went async during flush , save the remaining data and stop.
     * When callback on complete, write the remaining data.
     * 
     * @param value
     * @param start - offset into value
     * @param len - length from that offset to write
     * @throws IOException
     */
    private void writeToBuffers(byte[] value, int start, int len) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Writing " + len + ", buffered=" + this.bufferedCount);
        }
        if (value.length < (start + len)) {
            throw new IllegalArgumentException("Length outside value range");
        }
        this.writing = true;
        int remaining = len;
        int offset = start;
        while (0 < remaining) {
            // if async write required
            if ((_callback != null) && (!this.get_internalReady())) {
                // remaining is yet to write. 
                //save of the data and amount to write

                _remValue = new byte[remaining];

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Save of bytesRemainingWhenAsync -->" + _bytesRemainingWhenAsync + ", value size -->" + value.length + ", remValue size -->" + _remValue.length);
                }
                synchronized (_lockObj) {
                    System.arraycopy(value, offset, _remValue, 0, remaining);
                    setDataSaved(true);

                    _lockObj.notifyAll();
                }
                break;
            }
            WsByteBuffer buffer = getBuffer();
            int avail = buffer.remaining();
            if (contentLengthSet && bytesRemaining < bufferedCount + remaining) {
                //write what we can and throw an exception - it will be caught in servletWrapper
                int numberToWrite = (int) bytesRemaining - bufferedCount;
                boolean throwExceptionThisTime = true;
                if (numberToWrite > avail) {
                    numberToWrite = avail;
                    throwExceptionThisTime = false;
                }
                this.bufferedCount += numberToWrite;
                buffer.put(value, offset, numberToWrite);
                remaining = remaining - numberToWrite;
                if (throwExceptionThisTime) {
                    throw new WriteBeyondContentLengthException();
                }
            }
            if (avail >= remaining) {
                // write all remaining data
                this.bufferedCount += remaining;
                buffer.put(value, offset, remaining);
                remaining = 0;
            } else {
                // write what we can
                this.bufferedCount += avail;
                buffer.put(value, offset, avail);
                offset += avail;
                remaining -= avail;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing " + len + ", buffered=" + this.bufferedCount + ", this.amountToBuffer=" + this.amountToBuffer);
                Tr.debug(tc, "buffer now -->" + buffer);
            }
            if (this.bufferedCount >= this.amountToBuffer) {
                this.ignoreFlush = false;
                if (_callback == null) {
                    flushBuffers();
                }
                else {
                    _bytesRemainingWhenAsync = remaining;
                    flushAsyncBuffers();
                }
            }
        }
    }

    /**
     * Flush the output array of buffers to the network below.
     * 
     * @throws IOException
     */
    @FFDCIgnore({ IOException.class })
    private void flushAsyncBuffers() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Flushing Async buffers: " + this);
        }
        if (!this.isc.getResponse().isCommitted()) {
            if ((obs != null) && !this.WCheadersWritten) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "obs  ->" + obs);
                }
                obs.alertOSFirstFlush();
            }

            this.isc.getResponse().setCommitted();
        }

        if (this.ignoreFlush) {
            this.ignoreFlush = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring first flush attempt");
            }
            return;
        }

        final boolean writingBody = (hasBufferedContent());
        // flip the last buffer for the write...
        if (writingBody && null != this.output[this.outputIndex]) {
            this.output[this.outputIndex].flip();
        }
        try {
            WsByteBuffer[] content = (writingBody) ? this.output : null;
            if (isClosed() || this.isClosing) {
                if (!hasFinished) { //if we've already called finishResponseMessage - don't call again
                    // on a closed stream, use the final write api
                    this.isc.finishResponseMessage(content); // check if this also needs to be async
                    this.isClosing = false;
                    this.hasFinished = true;
                }
            } else {
                // else use the partial body api
                // Add the async option.               
                vc = this.isc.sendResponseBody(content, _callback, false);
                if (vc == null) { // then we will have to wait for data to be written, async write in progress
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "wait for data to be written, async write in progress, set ready to false");
                    }
                    synchronized (this._writeReadyLockObj) {
                        this.setWriteReady(false);
                        this.set_internalReady(false);
                    }
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "able to write out, no callback required, vc -->" + vc);
                    }
                }
            }
        } catch (MessageSentException mse) {
            FFDCFilter.processException(mse, getClass().getName(),
                                        "flushAsyncBuffers", new Object[] { this, this.isc });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Invalid state, message-sent-exception received; " + this.isc);
            }
            this.error = new IOException("Invalid state");
            throw this.error;
        } catch (IOException ioe) {
            // no FFDC required
            this.error = ioe;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received exception during write: " + ioe);
            }
            throw ioe;
        } finally {
            this.bytesWritten += this.bufferedCount;
            if (this.contentLengthSet) {
                this.bytesRemaining -= this.bufferedCount;
            }
            this.bufferedCount = 0;
            this.outputIndex = 0;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "finally, this " + this + " ,bytesRemaining -->" + this.bytesRemaining + ", bytesWritten -->" + this.bytesWritten);
            }
            if (writingBody && vc != null) {
                clearBuffersAfterWrite();
            }
        }
    }

    private void clearBuffersAfterWrite() {
        if (this.output != null) {
            if (null != this.output[0]) {
                this.output[0].clear();
            }
            for (int i = 1; i < this.output.length; i++) {
                if (null != this.output[i]) {
                    this.output[i].position(0);
                    this.output[i].limit(0);
                }
            }
        }
    }

    /*
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException {

        if (_callback != null) {
            if (!this._isReady) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot flush stream , output not ready: " + this);
                }
                return;
            }
            else {
                validate();
                flushBuffers();
            }
        }
        else {
            super.flush();
        }

    }

    /**
     * @param callback
     */
    public void setAsyncServletWriteListenerCallBack(InterChannelCallback callback) {
        this._callback = callback;
    }

    /**
     * @param callback
     */
    public InterChannelCallback getAsyncServletWriteListenerCallBack() {
        return _callback;
    }

    /**
     * @return
     */
    public boolean isWriteReady() {
        return this._isReady;
    }

    /**
     * @param ready
     */
    public void setWriteReady(boolean ready) {
        this._isReady = ready;
    }

    /**
     * @return the dataSaved
     */
    private boolean isDataSaved() {
        return _dataSaved;
    }

    /**
     * @param dataSaved the dataSaved to set
     */
    private void setDataSaved(boolean dataSaved) {
        this._dataSaved = dataSaved;
    }

    /**
     * @return the _internalReady
     */
    public boolean get_internalReady() {
        return _internalReady;
    }

    /**
     * @param _internalReady the _internalReady to set
     */
    public void set_internalReady(boolean _internalReady) {
        this._internalReady = _internalReady;
    }

    /**
     * @return the exceptionDuringOnWP
     */
    public boolean getExceptionDuringOnWP() {
        return exceptionDuringOnWP;
    }

    /**
     * @param exceptionDuringOnWP the exceptionDuringOnWP to set
     */
    public void setExceptionDuringOnWP(boolean exceptionDuringOnWP) {
        this.exceptionDuringOnWP = exceptionDuringOnWP;
    }

    /**
     * @return the vc
     */
    @Override
    public VirtualConnection getVc() {
        return super.getVc();
    }

}
