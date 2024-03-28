/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import javax.servlet.ReadListener;
import javax.servlet.http.WebConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.srt.SRTUpgradeInputStream31;
import com.ibm.ws.webcontainer31.upgrade.UpgradeReadCallback;
import com.ibm.ws.webcontainer31.upgrade.UpgradedWebConnectionImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.webcontainer31.WCCustomProperties31;

/**
 * This is the utility class that SRTUpgradeInputStream31 uses during the upgrade path of a servlet. This does all the reading and writing of the data.
 */
public class UpgradeInputByteBufferUtil {
    
    private static final TraceComponent tc = Tr.register(UpgradeInputByteBufferUtil.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    
    //WebConnection associated with this utility stream
    protected UpgradedWebConnectionImpl _upConn;
    //TCP Connection Context from the WebConnection which we use to do our reads
    private TCPConnectionContext _tcpContext;
    //Number of total bytes read over the duration of this stream. Please note: this isn't printed anywhere, but could be for debug purposes
    private long _totalBytesRead = 0L;
    //The ReadListener provided from the application
    protected ReadListener _rl;
    //The callback used by the TCP Channel when we do an async read. This will trigger the application's ReadListener
    protected UpgradeReadCallback _tcpChannelCallback;
    //Flag if the first async read is in progress. This read happens whenever the immediate reads don't return with more data and after the ReadListener has been invoked
    private boolean _isInitialRead = false;
    // The current buffer we are reading into
    private WsByteBuffer _buffer = null;
    //An IOException that may have occurred
    protected IOException _error = null;
    //A Flag for whether we are in the process of closing or not
    private boolean _isClosing = false;
    //A flag for isReady, only used to throw an exception in read methods
    private boolean _isReady = true;
    //A flag for if this is the very first read on the connection
    protected boolean _isFirstRead = true;
    //A flag for if the stream is closed
    private boolean _closed = false;
    //Know when we need to force a sync read because of a readLine
    private boolean _isReadLine = false;
    //this flag will track if onAllDataRead has been called
    private boolean isAlldataReadCalled = false;
    //this flag will track if onError has been called
    private boolean isonErrorCalled = false;


    public UpgradeInputByteBufferUtil(UpgradedWebConnectionImpl up)
    {
        _upConn = up;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "UpgradeInputByteBufferUtil:: constructor");         
        }
    }

    public UpgradeInputByteBufferUtil(UpgradedWebConnectionImpl up, TCPConnectionContext tcpContext)
    {
        _upConn = up;
        _tcpContext = tcpContext;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "UpgradeInputByteBufferUtil:: constructor");         
        }
    }
    
    /**
     * This method will call the synchronous or asynchronous method depending on how everything is set up
     * 
     * @return If we have read any data or not
     * @throws IOException
     */
    private boolean doRead(int amountToRead) throws IOException{
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "doRead, Current buffer, " + _buffer + ", reading from the TCP Channel, readLine : " + _isReadLine);
        }
        
        try {
            
            if(_tcpChannelCallback != null && !_isReadLine){
                //async read logic
                return immediateRead(amountToRead);
            } else {
                return syncRead(amountToRead);
            }
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "doRead, we encountered an exception during the read : " + e);
            }
            
            if(_error != null){
                return false;
            }
            _error = e;
            throw e;
        }
    }
    
    /**
     * Issues a synchronous read to the TCP Channel.
     * 
     * @return If we have read any data or not
     * @throws IOException
     */
    private boolean syncRead(int amountToRead) throws IOException{
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "syncRead, Executing a synchronous read");
        }
        
//      Allocate the buffer and set it on the TCP Channel
      setAndAllocateBuffer(amountToRead);
      
        try{
            long bytesRead = _tcpContext.getReadInterface().read(1, WCCustomProperties31.UPGRADE_READ_TIMEOUT);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "syncRead, Completed the read, " + bytesRead);
            }
            if(bytesRead > 0){
                //Get the buffer from the TCP Channel after we have told them to read.
                _buffer = _tcpContext.getReadInterface().getBuffer(); 
                
                //We don't need to check for null first as we know we will always get the buffer we just set
                configurePostReadBuffer();
                // record the new amount of data read from the channel
                _totalBytesRead += _buffer.remaining();
                return true;
            }
            return false;
        }catch (IOException e){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "syncRead, We encountered an exception during the read : " + e);
            }
            _error = e;
            throw e;
        }
    }
    
    /**
     * This method will execute an immediate read
     * The immediate read will issue a read to the TCP Channel and immediately return with whatever can fit in the buffers
     * This will only ever be called after we had read the 1 byte from the isReady or initialRead methods. As such
     * we will allocate a buffer and add in the 1 byte. This method should always return at least 1 byte, even if the
     * read fails, since we have already read that byte
     * 
     * @return If we have read any data or not
     * @throws IOException
     */
    private boolean immediateRead(int amountToRead){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "immediateRead, Executing a read");
        }
        
        if(amountToRead > 1){
          //Allocate a new temp buffer, then set the position to 0 and limit to the amount we want to read
            //Copy in the current this.buffer as it should only have one byte in it
            WsByteBuffer tempBuffer = allocateBuffer(amountToRead);
            tempBuffer.position(0);
            tempBuffer.limit(amountToRead);
            tempBuffer.put(_buffer);
            tempBuffer.position(1);
            _buffer.release();
            _buffer = tempBuffer;
            tempBuffer = null;
            
            _tcpContext.getReadInterface().setBuffer(_buffer);
            
            long bytesRead = 0;
            
            try{
                bytesRead = _tcpContext.getReadInterface().read(0, WCCustomProperties31.UPGRADE_READ_TIMEOUT);
            } catch (IOException readException){
                //If we encounter an exception here we need to return the 1 byte that we already have.
                //Returned true immediately and the next read will catch the exception and propagate it properly
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "immediateRead, The read encountered an exception. " + readException);
                    Tr.debug(tc, "immediateRead, Return with our one byte");
                }
                
                configurePostReadBuffer();
                return true;
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "immediateRead, Complete, " + bytesRead);
            }
            //Get the buffer from the TCP Channel after we have told them to read.
            _buffer = _tcpContext.getReadInterface().getBuffer(); 
            
            //We don't need to check for null first as we know we will always get the buffer we just set
            configurePostReadBuffer();
            // record the new amount of data read from the channel
            _totalBytesRead += _buffer.remaining();
        }

        //We will return true here in all circumstances because we always have 1 byte read from the isReady call or the initial read of the connection
        return true;
    }

    /**
     * Read the first available byte
     * 
     * @return int - the byte read
     * @throws IOException
     */
    public int read() throws IOException {
        validate();
        int rc = -1;
        
        if(doRead(1)){
            rc = _buffer.get() & 0x000000FF;
        }

        _buffer.release();
        _buffer = null;
            
        return rc;
    }
    
    /**
     * Read into the provided byte array
     * 
     * @param output
     * @return int - the number of bytes read
     * @throws IOException
     */
    public int read(byte[] output) throws IOException {
        return read(output, 0, output.length);
    }
    
    /**
     * Read into the provided byte array with the length and offset provided
     * 
     * @param output
     * @param offset
     * @param length
     * @return int - the number of bytes read
     * @throws IOException
     */
    public int read(byte[] output, int offset, int length) throws IOException {
        
        int size = -1;
        validate();
        
        if (0 == length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "read(byte[],int,int), Target length was 0");
            }
            return length;
        }
        
        if(doRead(length)){
            size = _buffer.limit() - _buffer.position();
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {  
              Tr.debug(tc, "(byte[],int,int) Filling byte array, size --> " + size);         
            }
            _buffer.get(output, offset, size);
        }
        _buffer.release();
        _buffer = null;
        
        return size;
    }
    
    /**
     * Allocate the buffer size we need and then pre-configure the buffer to prepare it to be read into
     * Once it has been prepared set the buffer to the TCP Channel
     */
    private void setAndAllocateBuffer(int sizeToAllocate) {
        
        if(_buffer == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "setAndAllocateBuffer, Buffer is null, size to allocate is : " + sizeToAllocate);
            }
            _buffer = allocateBuffer(sizeToAllocate);
        }
        
        configurePreReadBuffer(sizeToAllocate);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
          Tr.debug(tc, "setAndAllocateBuffer, Setting the buffer : " + _buffer );         
        }
        
        _tcpContext.getReadInterface().setBuffer(_buffer);
    }
    
    /**
     * This checks if we have already had an exception thrown. If so it just rethrows that exception
     * This check is done before any reads are done
     * 
     * @throws IOException
     */
    protected void validate() throws IOException {
        if (null != _error) {
            throw _error;
        }
        
        if(!_isReadLine && !_isReady){
            //If there is no data available then isReady will have returned false and this throw an IllegalStateException
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "read.failed.isReady.false");
            throw new IllegalStateException(Tr.formatMessage(tc, "read.failed.isReady.false"));
        }
    }

    /**
     * After a read has completed into the given buffer, this method is used to
     * prepare it for handling of the new data. It will set the limit to the
     * current position and the position to 0
     * 
     * @param buffer
     */
    private void configurePostReadBuffer() {
        _buffer.flip();
    }
    
    /**
     * Using the given buffer, this will configure the buffer to the appropriate
     * size by setting the limit to the number of bytes we need to read, then
     * set the position to 0 to start from the beginning of the data
     * 
     * @param buffer
     */
    private void configurePreReadBuffer(int amountToRead) {
        _buffer.limit(amountToRead);
        _buffer.position(0);
    }
    
    
    /**
     * Allocates a buffer of the passed in size
     * 
     * @param bufferSize
     * @return WsByteBuffer - the newly allocated byte buffer
     */
    private WsByteBuffer allocateBuffer(int bufferSize)
    {
      return (ChannelFrameworkFactory.getBufferManager().allocateDirect(bufferSize));
    }
    
    /**
     * Sets the ReadListener provided by the application to this stream
     * Once the ReadListener is set we will kick off the initial read
     * @param readListenerl
     */
    public void setupReadListener(ReadListener readListenerl, SRTUpgradeInputStream31 srtUpgradeStream){
        
        if(readListenerl == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.is.null");
            throw new NullPointerException(Tr.formatMessage(tc, "readlistener.is.null"));
        }
        if(_rl != null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.already.started");
            throw new IllegalStateException(Tr.formatMessage(tc, "readlistener.already.started"));
            
        }
        
        //Save off the current Thread data by creating the ThreadContextManager. Then pass it into the callback       
        ThreadContextManager tcm = new ThreadContextManager();
        
        _tcpChannelCallback = new UpgradeReadCallback(readListenerl, this, tcm, srtUpgradeStream);
        _rl = readListenerl;
        _isReady = false;
        _upConn.getVirtualConnection().getStateMap().put(TransportConstants.UPGRADED_LISTENER, "true");
        
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "setupReadListener, Starting the initial read");
        }
        initialRead();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "setupReadListener, ReadListener set : " + _rl);
        }
        
    }
    
    /**
     * This method will check if the stream can be read from without blocking.
     * We accomplish this by looking at the buffer, and if it's not null and has remaining(which it will if we are returning from the initial read),
     * then we will return true. If those conditions aren't true then we will issue a read for 1 byte.
     * This read checks if the stream is viable or not by immediately reading for one byte. If it is then it configures that buffer
     * and then returns true. 
     * 
     * @return isReady - a boolean to indicate if the stream can be read from without blocking
     */
    public boolean isReady(){
        
        //If there is no ReadListener set we never know if we can read without blocking
        if(_rl == null) {
            return true;
        }
        
        long bytesRead = 0;
        
        //We will check here if the buffer has anything in it.
        //If so we will return immediately. The only time this should ever happen is after we read the first byte
        //of data from a new connection
        //If not then we will read immediately for 1 byte
        if(_buffer != null && _buffer.hasRemaining()){
            //This must have been an initial read, so return true that we have one byte of data
            _isReady = true;
            return _isReady;
        } else {
            //We only want to read for 1 byte so we aren't buffering much data
            //We will preserve this byte on the async read and return it back to the customer when necessary
            try {
                setAndAllocateBuffer(1);
                
                //Immediately read for some data. This will return immediately if there was anything
                bytesRead = _tcpContext.getReadInterface().read(0, WCCustomProperties31.UPGRADE_READ_TIMEOUT);
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "isReady, Completed the read, " + bytesRead);
                }
                //If there was one byte read, then great. Get it and return true that we have some data
                if(bytesRead == 1){
                    //Get the buffer from the TCP Channel after we have told them to read.
                    _buffer = _tcpContext.getReadInterface().getBuffer(); 
                    
                    //We don't need to check for null first as we know we will always get the buffer we just set
                    configurePostReadBuffer();
                    
                    _isReady = true;
                    return _isReady;
                } else {
                    //We only ever expect to read 1 byte here if we read more then something went wrong
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                        Tr.debug(tc, "isReady, Read some amount of data other than one byte on the read : " + bytesRead);
                    }
                    
                    _buffer.release();
                    _buffer = null;
                }
            } catch (IOException ioe){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "isReady, An exception happened during the check of isReady : " + ioe + ", returning false");
                }
            }
        }
        
        _isReady = false;
        return _isReady;
    }
    
    /**
     * This method triggers the initial read on the connection, or the read for after the ReadListener.onDataAvailable has run
     * The read done in this method is a forced async read, meaning it will always return on another thread
     * The provided callback will be called when the read is completed and that callback will invoke the ReadListener logic. 
     */
    public void initialRead(){
        _isInitialRead = true;
        if(_buffer != null){
            _buffer.release();
            _buffer = null;
        }
        
        setAndAllocateBuffer(1);
        configurePreReadBuffer(1);
        
        //This if the first read of the ReadListener, which means force the read to go async
        //We won't get an actual response from this read as it will always come back on another thread
        _tcpContext.getReadInterface().setBuffer(_buffer);
        _tcpContext.getReadInterface().read(1, _tcpChannelCallback, true, WCCustomProperties31.UPGRADE_READ_TIMEOUT);
    }
    
    /**
     * Called after the initial read is completed. This will set the first read flag to false, get the buffer from the TCP Channel,
     * and post configure the buffer. Without this method we would lose the first byte we are reading
     */
    public void configurePostInitialReadBuffer(){
        _isInitialRead = false;
        _isFirstRead = false;
        _buffer = _tcpContext.getReadInterface().getBuffer();
        configurePostReadBuffer();
    }
    
    /**
     * Returns whether this is the initial read for this async read. The initial read
     * is the async read before customer's code is called
     * @return boolean
     */
    public boolean isInitialRead(){
        return _isInitialRead;
    }
    
    public void setIsInitialRead(boolean isInitialRead){
        _isInitialRead = isInitialRead;
    }
    
    /**
     * Returns whether this is the first read of the upgraded connection. The first read
     * is the very first initial read we do during the setup of the ReadListener
     * @return boolean
     */
    public boolean isFirstRead(){
        return _isFirstRead;
    }
    
    /**
     * Close the connection down by immediately timing out any existing read
     */
    public Boolean close() {
        _isClosing = true;
        boolean closeResult = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
            Tr.debug(tc, "close, Initial read outstanding : " + _isInitialRead);
        }
        
        if(_isInitialRead){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "close, Cancelling any outstanding read");
            }
            
            _tcpContext.getReadInterface().read(1, _tcpChannelCallback, false, TCPReadRequestContext.IMMED_TIMEOUT);
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close, Call to cancel complete");
            }
            
            //This seems strange, but what happens during the timeout it will be set to false.
            //If it's false we don't want to do the wait since it's been called in line.
            //If it's true we will want to wait until the timeout has been processed
            if(_isInitialRead){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close, Timeout has been called, waiting for it to complete");
                }
                closeResult = true;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                    Tr.debug(tc, "close, No read outstanding, no reason to call cancel");
                }
                closeResult = false;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                Tr.debug(tc, "close, No read outstanding, no reason to call cancel");
            }
            closeResult = false;
        }
        
        if(_rl != null){
            if(!this.isAlldataReadCalled()) {
                try {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                        Tr.debug(tc, "close, We are now closed, calling the ReadListener onAllDataRead");
                    }
                    this.setAlldataReadCalled(true);
                    _rl.onAllDataRead();
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
                        Tr.debug(tc, "close, Encountered an exception while calling onAllDAtaRead : " + ioe);
                    }
                }
            }
        }
        
        return closeResult;
    }
    
    /**
     * Determine whether we are in the process of closing the connection or not
     * 
     * @return
     */
    public boolean isClosing(){
        return _isClosing;
    }
    
    /**
     * @return the _tcpChannelCallback
     */
    public UpgradeReadCallback get_tcpChannelCallback() {
        return _tcpChannelCallback;
    }
    
    public WebConnection getWebConn() {
        return _upConn;
    }

    public void readLineCall() throws IOException {
        //Want to flip the variable when this is called. First time it's called is when we are going into readLine
        //next time it's called is on the way out. The way async is designed is this will only ever happen on one thread
        _isReadLine = _isReadLine ? false : true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "readLine", "readLine flag : " + this._isReadLine);         
        }
    }

    /**
     * @return the isAlldataReadCalled
     */
    public boolean isAlldataReadCalled() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "isAlldataReadCalled", "value -->"+isAlldataReadCalled);
        }        
        return isAlldataReadCalled;
    }

    /**
     * @param isAlldataReadCalled the isAlldataReadCalled to set
     */
    public void setAlldataReadCalled(boolean isAlldataReadCalled) {
        this.isAlldataReadCalled = isAlldataReadCalled;
    }

    /**
     * @return the isonErrorCalled
     */
    public boolean isIsonErrorCalled() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "isIsonErrorCalled", "value -->"+isonErrorCalled);
        }  
        return isonErrorCalled;
    }

    /**
     * @param isonErrorCalled the isonErrorCalled to set
     */
    public void setIsonErrorCalled(boolean isonErrorCalled) {
        this.isonErrorCalled = isonErrorCalled;
    }

}
