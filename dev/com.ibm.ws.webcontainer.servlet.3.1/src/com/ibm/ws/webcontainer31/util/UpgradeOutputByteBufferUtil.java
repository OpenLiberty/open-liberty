/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
package com.ibm.ws.webcontainer31.util;

import java.io.IOException;

import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.response.BlockingWriteNotAllowedException;
import com.ibm.ws.webcontainer31.upgrade.UpgradedWebConnectionImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

/**
 * This class is an helper class for the Upgraded Output Stream as it implements the buffer logic for write.
 * 
 * Added since Servlet 3.1
 * 
 */
public class UpgradeOutputByteBufferUtil {

    private static final TraceComponent tc = Tr.register(UpgradeOutputByteBufferUtil.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS);


    public UpgradedWebConnectionImpl _upConn = null;
    private TCPConnectionContext _tcpContext = null;
    /** Array of buffers used for the content buffering */
    protected WsByteBuffer[] _output = null;
    /** Index into the output array for the current writes */
    protected int outputIndex = 0;
    /** Total amount to buffer internally before triggering an auto-flush */
    private int amountToBuffer = 0;
    /** Size of the ByteBuffers to allocate */
    private int bbSize;
    /** Current amount buffered internally */
    protected int bufferedCount = 0;
    private long bytesRemaining = -1;

    /** Bytes written through this stream */
    protected long bytesWritten = 0L;
    /** Possible error that may have been seen during IO requests */
    protected IOException error = null;

    // added following for Async Write
    /** Reference to the connection object */
    private VirtualConnection _vc = null;


    public TCPWriteCompletedCallback _callback;
    private WriteListener _listener;
    private boolean _isInternalReady = true;
    private boolean _isReady = true;
    private int _bytesRemainingWhenAsync = 0;
    private byte[] _remValue = null;
    private boolean _dataSaved = false;
    private boolean _asyncFlushCalledFromCloseWork = false;

    public boolean status_not_ready_checked = false;
    public boolean write_crlf_pending= false;
    
    private final Object _lockObj = new Object() {}; /* The braces indicate we want to create and use an anonymous inner class as the actual object instance */
    Object _writeReadyLockObj = new Object() {
    };
    private boolean outputStream_close_initiated_but_not_Flush_ready = false;
    private boolean outputStream_closed = false;
    private static final byte[] CRLF = new byte[] { 0x0D, 0x0A };
    // added above for Async Write

    /**
     * 
     */
    public UpgradeOutputByteBufferUtil(UpgradedWebConnectionImpl up) {        
        _upConn = up;
        _vc = _upConn.getVirtualConnection();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {  
            Tr.debug(tc, "constructor");         
        }       
    }

    public UpgradeOutputByteBufferUtil(UpgradedWebConnectionImpl up, TCPConnectionContext tcpContext) {        
        _upConn = up;
        _tcpContext = tcpContext;
        _vc = _upConn.getVirtualConnection();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {  
            Tr.debug(tc, "constructor");         
        }       
    }


    public void writeWork(byte[] value, int start, int len) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {  
            Tr.debug(tc, "write len --> " + len + ", bytesRemaining->" + bytesRemaining);           
        }
        validate();
        writeToBuffers(value, start, len);

    }
    
    
    /**
     * @param value
     * @param start
     * @param len
     * @throws IOException
     */
    public void write_NonBlocking(byte[] value, int start, int len) throws IOException
    {
        // check if we are allowed to write on this thread, check for com.ibm.ws.webcontainer.WriteAllowedonThisThread
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);        
        if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread")!=null){
            validate();
            writeToBuffers(value, start, len);

            if (reqState.getAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread")!=null){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                    Tr.debug(tc, "back to write_NonBlocking , remove write allowed attribute, -> WriteListener enabled: " + this._listener);
                reqState.removeAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread");
            }
        }
        else{
            Tr.error(tc, "blocking.write.not.allowed", new Object[] {this._listener}) ;            
            throw new BlockingWriteNotAllowedException(Tr.formatMessage(tc, "blocking.write.not.allowed", new Object[] {this._listener})); 
        }
    }
 

    /**
     * @param value
     * @throws IOException
     */
    public void print_NonBlocking(String value) throws IOException{
        synchronized(this){      
            if(this.isOutputStream_closed() || this.isOutputStream_close_initiated_but_not_Flush_ready()){
                Tr.error(tc, "stream.is.closed.no.read.write");                         
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));        
            }
            byte[] stringBytes = value.getBytes();
            this.write_NonBlocking(stringBytes, 0, stringBytes.length);
        }
    }
    
    /**
     * @param value
     * @throws IOException
     */
    public void println_NonBlocking(String value) throws IOException{
        synchronized(this){

            if(this.isOutputStream_closed() || this.isOutputStream_close_initiated_but_not_Flush_ready()){
                Tr.error(tc, "stream.is.closed.no.read.write");                         
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));      
            }
            byte[] stringBytes = value.getBytes();
            this.write_NonBlocking(stringBytes, 0, stringBytes.length);

            // need to check if we can write first in case the first write has gone async
            if(this.isWriteReadyForApp()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
                    Tr.debug(tc, "println crlf , write allowed now , WriteListener enabled: " + this._listener + " , check crlf_pending " +  this.write_crlf_pending);     
                }
                WebContainerRequestState.getInstance(true).setAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread", true);
                this.writeCRLFIfNeeded();
            }
            else{
                // the previous write must have gone async and has not completed yet.
                // Do the next write of CRLF in complete
                this.write_crlf_pending= true;
            }
        }
    }
    
    /**
     * @throws IOException
     */
    public void writeCRLFIfNeeded() throws IOException{
        synchronized(this){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
                Tr.debug(tc, "write queue is empty and now write crlf, WriteListener enabled: " + this._listener);
            }                                       
            this.write_crlf_pending = false;                                
            // write from here
            this.write_NonBlocking(CRLF, 0, 2);                                                                                                     
        }
    }

    /**
     * @throws IOException
     */
    public void flushHelper() throws IOException {
        boolean crlfWrite = false;
        if (_callback != null){

            synchronized (this) {
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
                if (reqState != null && reqState.getAttribute("com.ibm.ws.webcontainer.upgrade.CRLFWriteinPorgress") != null) {
                    crlfWrite = true;
                }
                if (this.isWriteReadyForApp() || crlfWrite) {
                    validate();
                    flushUpgradedOutputBuffers();
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cannot flush Upgraded output stream , output not ready: " + this);
                    }
                    return;
                }
            }

        }
        else{
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Flushing Upgraded output stream: " + this);
            }
            validate();
            flushUpgradedOutputBuffers();
        }
    }  


    /**
     * @throws IOException
     */
    public void writeRemainingToBuffers() throws Exception {
        
        // clear the saved buffers and free them   
        this.clearBuffersAfterWrite();
        
        if(this.is_asyncFlushCalledFromCloseWork()){           
            // no more data to write as Flush was called from close so all the data must already written when complete callback was called.
            this.setInternalReady(true);
            this.set_asyncFlushCalledFromCloseWork(false);
            return;
        }
        // if more data remaining now write
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Write out remainig bytes --> " + this._bytesRemainingWhenAsync);
        }
        if (this._bytesRemainingWhenAsync > 0) {
            // Make sure complete is not called faster on another thread before remaining data is saved of
            synchronized(this._lockObj) {    
                if (!this.isDataSaved()) {
                    this._lockObj.wait();                    
                }
                this.setDataSaved(false);            
            }
            this.setInternalReady(true);
            this.writeToBuffers(this._remValue, 0, this._bytesRemainingWhenAsync);
        }
        else{
            this.setInternalReady(true);
        }
    }

    /**
     * Set the amount of data to buffer internally before the stream itself
     * initiates a flush. A zero size means no buffer is done, each write
     * call will flush data.
     * 
     * @param size
     * @throws IllegalStateException if already writing data or closed
     */
    private void setBufferSize(int size) {

        this.amountToBuffer = size;
        this.bbSize = (49152 < size) ? 32768 : 8192;
        int numBuffers = (size / this.bbSize);
        if (0 == size || 0 != (size % this.bbSize)) {
            numBuffers++;
        }
        this._output = new WsByteBuffer[numBuffers];
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setBufferSize=[" + size + "]; " + this);
        }
    }

    /**
     * Perform validation of the stream before processing external requests
     * to write data.
     * 
     * @throws IOException
     */
    private void validate() throws IOException {
        if (null != this.error) {
            throw this.error;
        }
        if (null == this._output) {
            this.setBufferSize(32768);           
        }
    }

    /**
     * Access the proper output buffer for the current write attempt.
     * 
     * @return WsByteBuffer
     */
    private WsByteBuffer getBuffer() {
        WsByteBuffer buffer = this._output[this.outputIndex];
        if (null == buffer) {
            buffer = getNewByteBuffer();
            buffer.clear();
            this._output[this.outputIndex] = buffer;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getBuffer, new buffer -->" + buffer + " ,outputIndex -->" + this.outputIndex);
            }
        } else if (!buffer.hasRemaining()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getBuffer, buffer  -->" + buffer + " ,outputIndex -->" + this.outputIndex + " ,outputLength -->" + this._output.length);
            }
            buffer.flip();
            this.outputIndex++;
            // next array spot may or may not exist from previous iterations
            if (null == this._output[this.outputIndex]) {
                this._output[this.outputIndex] = getNewByteBuffer();
            }
            buffer = this._output[this.outputIndex];
            buffer.clear();
        }
        return buffer;
    }

    /**
     * @return
     */
    private WsByteBuffer getNewByteBuffer()
    {
        return (ChannelFrameworkFactory.getBufferManager().allocateDirect(this.bbSize));
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
            Tr.debug(tc, "writeToBuffers, Writing " + len + ", buffered=" + this.bufferedCount);
        }
        if (value.length < (start + len)) {
            throw new IllegalArgumentException("Length outside value range");
        }
        //this.writing = true;
        int remaining = len;
        int offset = start;
        while (0 < remaining) {   
            // if async write required
            if ((_callback != null) && (!this.isInternalReady())){
                // remaining is yet to write. 
                //save of the data and amount to write                
                _remValue = new byte[remaining];

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeToBuffers, Save of bytesRemainingWhenAsync -->" + _bytesRemainingWhenAsync 
                             + ", value size -->" + value.length + ", remValue size -->" + _remValue.length);
                }
                synchronized (this._lockObj) {
 
                    System.arraycopy(value, offset, _remValue, 0, remaining);
                    setDataSaved(true);

                    this._lockObj.notifyAll();
                }
                break;
            }
            WsByteBuffer buffer = getBuffer();
            int avail = buffer.remaining();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeToBuffers: avail -->"+  avail + " ,bytesRemaining --> " + remaining);
            }
            if (avail >= remaining) {
                // write all remaining data
                this.bufferedCount += remaining;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeToBuffers: remaining --> " + remaining);
                }
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
                Tr.debug(tc, "Writing " + len + ", buffered=" + this.bufferedCount + ", this.amountToBuffer=" + this.amountToBuffer +", remaining=" + remaining);
                Tr.debug(tc, "writeToBuffers: buffer now -->" + buffer);
            }
            if (this.bufferedCount >= this.amountToBuffer) {
                if (_callback == null) {
                    flushUpgradedOutputBuffers();
                }
                else{
                    _bytesRemainingWhenAsync = remaining;
                    flushAsyncUpgradedOutputBuffers();    
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
    protected void flushUpgradedOutputBuffers() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "flushUpgraded: Flushing buffers for Upgraded output: " + this);
        }
        final boolean writingBody = (hasBufferedContent());
        // flip the last buffer for the write...
        if (writingBody && null != this._output[this.outputIndex]) {
            this._output[this.outputIndex].flip();
        }
        try {
            WsByteBuffer[] content = (writingBody) ? this._output : null;
            // write it out to TCP            
            if(content != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushUpgraded:: Now write the content ");
                }
                _tcpContext.getWriteInterface().setBuffers(content);
                _tcpContext.getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, WCCustomProperties31.UPGRADE_WRITE_TIMEOUT);
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushUpgraded: No more data to flush ");
                } 
            }

        } catch (IOException ioe) {
            this.error = ioe;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flushUpgraded: Received exception during write: " + ioe);
            }
            throw ioe;
        } finally {
            this.bytesWritten += this.bufferedCount;            
            this.bufferedCount = 0;
            this.outputIndex = 0;
            // Note: this logic only works for sync writes
            if (writingBody) {
                if (null != this._output){
                    if (null != this._output[0]) {
                        this._output[0].clear();
                    }
                    for (int i = 1; i < this._output.length; i++) {
                        if (null != this._output[i]) {
                            // mark them empty so later writes don't mistake them
                            // as having content
                            this._output[i].position(0);
                            this._output[i].limit(0);
                        }
                    }
                }
            }
            // disconnect write buffers in TCP when done
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flushUpgraded: disconnect write buffers in TCP when done");
            }
            _tcpContext.getWriteInterface().setBuffers(null);
        }
    }


    /**
     * Flush the output array of buffers to the network below.
     * 
     * @throws IOException
     */
    protected void flushAsyncUpgradedOutputBuffers() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "flushAsyncUpgraded: Flushing async buffers  for Upgraded output: " + this);
        }

        final boolean writingBody = (hasBufferedContent());
        // flip the last buffer for the write...
        if (writingBody && null != this._output[this.outputIndex]) {
            this._output[this.outputIndex].flip();
        }
        VirtualConnection _vcWrite = null;
        try {
            WsByteBuffer[] content = (writingBody) ? this._output : null;
            // write it out to TCP           
            if(content != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushAsyncUpgraded: Now write it out to TCP");
                }
                _tcpContext.getWriteInterface().setBuffers(content);

                _vcWrite =_tcpContext.getWriteInterface().write(TCPWriteRequestContext.WRITE_ALL_DATA, _callback, false, WCCustomProperties31.UPGRADE_WRITE_TIMEOUT);
                if (_vcWrite == null) { // then we will have to wait for data to be written, async write in progress                                        
                    this.setInternalReady(false); // tell internal
                    this.setReadyForApp(false); // tell app
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "flushAsyncUpgraded:  wait for data to be written, async write in progress, set ready to false");
                    }
                }
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "flushAsyncUpgraded: No more data to flush ");
                } 
            }        
        } finally {
            this.bytesWritten += this.bufferedCount;            
            this.bufferedCount = 0;
            this.outputIndex = 0;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "flushAsyncUpgraded: finally, this " + this + " , bytesWritten -->" + this.bytesWritten);
            }
            if (writingBody && _vcWrite != null) {
                clearBuffersAfterWrite();
            }
        }
    }

    /**
     * Test whether this stream has any current data buffered, waiting to
     * be written out.
     * 
     * @return boolean
     */
    protected boolean hasBufferedContent() {
        return (0 < this.bufferedCount);
    }


    /**
     * @throws IOException
     */
    public void closeWork() throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "close upgrade output");         
        }
        boolean clearBuffers = true;
        try{  
            // we will need to flush out anything which is buffered if any
            if (this._listener!= null){
                this.setOutputStream_close_initiated_but_not_Flush_ready(false);
                if(this._isReady){

                    this.validate();
                    this.set_asyncFlushCalledFromCloseWork(true);
                    this.flushAsyncUpgradedOutputBuffers();
                    // check again after flush
                    if(!this._isReady){
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "close called but output not ready ,return, close will be done later after complete of pending write" );
                        }
                        clearBuffers= false;
                        this.setOutputStream_close_initiated_but_not_Flush_ready(true);
                        return;
                    }                            
                } 
                else {
                    clearBuffers= false;
                    this.setOutputStream_close_initiated_but_not_Flush_ready(true);
                    return;
                }
            }// if WL not enabled then flush sync
            else{                            
                this.validate();
                // we will need to flush out anything which is buffered if any
                this.flushUpgradedOutputBuffers();                                           
            }
        }
        finally{
            if(clearBuffers){
                this.setOutputStream_closed(true);
                // must release the buffers even if the flush fails
                this.clear();
            }
        }

    }

    /**
     * Release any current buffer content in the stream.
     */
    private void clear() {
        if (null != this._output) {
            for (int i = 0; i < this._output.length; i++) {
                if (null != this._output[i]) {
                    this._output[i].release();
                    this._output[i] = null;
                }
            }
        }
        this.outputIndex = 0;
        this.bufferedCount = 0;
        this.bytesWritten = 0L;
        this.setWriteListenerCallBack(null);
    }

    /**
     * 
     */
    protected void clearBuffersAfterWrite() {
        if (null != this._output) {
            if (null != this._output[0]) {
                this._output[0].clear();
            }
            for (int i = 1; i < this._output.length; i++) {
                if (null != this._output[i]) {
                    this._output[i].position(0);
                    this._output[i].limit(0);
                }
            }
        }
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
     * @param callback
     */
    public TCPWriteCompletedCallback getWriteListenerCallBack() {
        return _callback;
    }

    public void setWriteListenerCallBack(TCPWriteCompletedCallback callback){

        if(callback!=null)
            _vc.getStateMap().put(TransportConstants.UPGRADED_LISTENER, "true");
        //tell the outputStream that WriteListener has been set by application
        this._callback = callback;       
    }
    
    /**
     * @param _listener the _listener to set
     */
    public void set_listener(WriteListener _listener) {
        this._listener = _listener;
    }

    /**
     * @param isReady the isReady to set
     */
    public void setReadyForApp(boolean isReady) {
        this._isReady = isReady;
    }

    /**
     * @return
     */
    public boolean isWriteReadyForApp() {
        return this._isReady;
    }

    /**
     * @return the _isInternalReady
     */
    public boolean isInternalReady() {
        return _isInternalReady;
    }

    /**
     * @param _isInternalReady the _isInternalReady to set
     */
    public void setInternalReady(boolean _isInternalReady) {
        this._isInternalReady = _isInternalReady;
    }
    
    /**
     * @param externalCall
     * @return
     */
    public boolean isWriteReadyWork(boolean externalCall) {
        
        if(this.isOutputStream_closed()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
                Tr.debug(tc, "output stream closed, ready->" + false);
            }
            return false;
        }

        boolean ready = true;
        synchronized(this._writeReadyLockObj){
            if(!this.isWriteReadyForApp()){
                ready = false;
                if(externalCall) {
                    this.status_not_ready_checked= true; 
                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
                    if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread")!=null){
                        reqState.removeAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread"); 
                    }
                }
            }
            else{
                // we are ready , set the attribute if this thread did not setWL
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
//                if (reqState.getAttribute("com.ibm.ws.webcontainer.upgrade.ThisThreadSetWL")!=null){
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
//                        Tr.debug(tc, "isWriteReadyWork: cannot write from this Thread as this SetWL->"+ this._listener);
//                    }
//                    ready = false;            
//                }
//                else{
                    reqState.setAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread", true);                       
//                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
            Tr.debug(tc," ready->"+ready);
        }
        return ready;           
    }
    
    /**
     * @return the _vc
     */
    public VirtualConnection get_vc() {
        return _vc;
    }
    
    /**
     * @return the outputStream_close_initiated_but_not_Flush_ready
     */
    public boolean isOutputStream_close_initiated_but_not_Flush_ready() {
        return outputStream_close_initiated_but_not_Flush_ready;
    }

    /**
     * @param outputStream_close_initiated_but_not_Flush_ready the outputStream_close_initiated_but_not_Flush_ready to set
     */
    public void setOutputStream_close_initiated_but_not_Flush_ready(boolean outputStream_close_initiated_but_not_Flush_ready) {
        this.outputStream_close_initiated_but_not_Flush_ready = outputStream_close_initiated_but_not_Flush_ready;
    }


    /**
     * @return the outputStream_closed
     */
    public boolean isOutputStream_closed() {
        return outputStream_closed;
    }

    /**
     * @param outputStream_closed the outputStream_closed to set
     */
    public void setOutputStream_closed(boolean outputStream_closed) {
        this.outputStream_closed = outputStream_closed;
    }
   
    /**
     * @return the _asyncFlushCalledFromCloseWork
     */
    public boolean is_asyncFlushCalledFromCloseWork() {
        return _asyncFlushCalledFromCloseWork;
    }


    /**
     * @param _asyncFlushCalledFromCloseWork the _asyncFlushCalledFromCloseWork to set
     */
    public void set_asyncFlushCalledFromCloseWork(boolean _asyncFlushCalledFromCloseWork) {
        this._asyncFlushCalledFromCloseWork = _asyncFlushCalledFromCloseWork;
    }


}
