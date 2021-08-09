/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.srt;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.inputstream.HttpInputStreamConnectWeb;
import com.ibm.ws.webcontainer.srt.SRTInputStream;
import com.ibm.ws.webcontainer31.async.AsyncAlreadyReadCallback;
import com.ibm.ws.webcontainer31.async.AsyncContext31Impl;
import com.ibm.ws.webcontainer31.async.AsyncReadCallback;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.async.listener.ReadListenerRunnable;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.channelfw.InterChannelCallback;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;

/**
*
*/
public class SRTInputStream31 extends SRTInputStream
{
    protected HttpInputStreamEE7 httpin;
    //ReadListener to callback into user code when data is available
    private ReadListener listener = null;
    //callback used when communicating in an async manner with the HTTP Channel
    private InterChannelCallback callback;
    //The request we use to find out if Async has been started
    private SRTServletRequest31 request;
    private final Object lockObj = new Object() {};
    private final Object completeLockObj = new Object() {};  
    private boolean asyncReadOutstanding = false;
    private boolean readLineCall = false;
    private boolean isClosed = false;
    private final static TraceComponent tc = Tr.register(SRTInputStream31.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param request
     */
    public SRTInputStream31(SRTServletRequest31 request) {
        super();
        this.request = request;
    }
    
    @Override
    public void init(InputStream in) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Initializing the stream : " + this);
        if(in != null){

            //The passed in should always be an HttpInputStreamExtended if Servlet 3.1 is enabled
            if (in instanceof HttpInputStreamEE7) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "init , Servlet 3.1 enabled, casting to HttpInputStreamExtended");
                }
                this.httpin = (HttpInputStreamEE7)in;
            }           
            this.in = in;
            // below cast needed for SIP
            if (in instanceof HttpInputStreamConnectWeb)
                this.inStream = (HttpInputStreamConnectWeb) in;
            
        } else {
            this.httpin = null;
            this.in = null;
            this.inStream = null;
        }

        this.listener = null;
        this.asyncReadOutstanding = false;
        this.readLineCall = false;
        this.isClosed = false;
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isFinished()
     */
    @Override
    public boolean isFinished() {
        
      //Return -1 here because if we are closed or finished, we're at the end of the stream
        if(isClosed){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Stream is closed so it is finished");
            return true;
        }
        //Call into the HttpInboundServiceContext to find out if the headers and body
        //have been fully read by the HTTP Channel, then return the result
        boolean isFinished = this.httpin.isFinished();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isFinished returning : " + isFinished);
        }
        return isFinished;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isReady()
     */
    @Override
    public boolean isReady() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "isReady", this.listener);
        
        //If the listener is not set then we have no way to know if we can read in a non-blocking manner
        //return true so we don't accidentally block an app who doesn't set a listener but calls isReady
        if(this.listener == null || isFinished()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "isReady", "ReadListener not set or isFinished returned true");
            return true;
        }
        boolean isReady = false;

        //Synchronize to ensure that no two methods can overlap in here as that could cause issue
        synchronized (this.lockObj){
            //If asyncReadOutstanding returns true, it means we don't need to do this check
            //If checkAvailable returns true then it means data is available right now
            //If it returns false it means we need to check if data is available immediately
            //If data is available immediately it will be populated and we return true
            //If not then we return false and an async read is in progress or we're finished
            if(!asyncReadOutstanding){
                if(!checkAvailable()){
                    isReady = this.httpin.asyncCheckBuffers(this.callback);
                    if(!isReady){
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "asyncCheckBuffers returned false. An async read is now outstanding or no more data");
                        }
                        this.asyncReadOutstanding = true;
                    }
                } else
                    isReady = true;
            }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "isReady", isReady + " " + this.listener);
        return isReady;
    }

    /*
     * Call into the HttpInputStream to find out the result of their available call,
     * which returns how much data, if any, is available in the current buffer.
     * If there is more than 0 bytes of data we return true
     */
    private boolean checkAvailable(){
        int i = 0;
        try{
            i = this.in.available();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Current amount immediately available in buffers : " + i);
            }
        } catch (Exception e){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was an IOException during the checkAvailable method : " + e);
            }
        }
        return ((i > 0) ? true : false);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#setReadListener(javax.servlet.ReadListener)
     */
    @Override
    public void setReadListener(ReadListener arg0) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setReadListener", this.listener);

        //Throw a NullPointerException if the ReadListener passed in was null
        if(arg0 == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.is.null");
            throw new NullPointerException(Tr.formatMessage(tc, "readlistener.is.null"));
        }
        //Throw an IllegalStateException if async has not been started
        if(!this.request.isAsyncStarted()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.async.not.started");
            throw new IllegalStateException(Tr.formatMessage(tc, "readlistener.async.not.started"));
        }
        //Throw an IllegalStateException is there was already a ReadListener set for this InputStream
        if(this.listener != null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "readlistener.already.started");
            throw new IllegalStateException(Tr.formatMessage(tc, "readlistener.already.started"));
        }
        
        this.listener = arg0;
        
        //Create a new ThreadContextManager to pass into the read Callback
        ThreadContextManager tcm = new ThreadContextManager();
        
        // If ISC is null the inbound request would have resulted in a login being needed so
        // post data has already been read and re-populated by the security component.In 
        // this case create a callback which works for the already read data.
        if (getISC()==null) {
            this.callback = new AsyncAlreadyReadCallback(this, tcm);
        } else {
            //Create a new HttpServletCallback so we can use it for our async read callbacks
            this.callback = new AsyncReadCallback(this, tcm, request.getAsyncContext());
        }    
        
        AsyncContext31Impl ac = (AsyncContext31Impl)request.getAsyncContext();

        try {
            ac.startReadListener(tcm, this);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "An exception occurred while setting the ReadListener : " + e);
            }
            //There was a problem with the read so we should invoke their onError, since technically it's been set now
            this.listener.onError(e);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setReadListener", this.listener);
    }

    //A method to get access to the HttpInputStream's HttpInboundServiceContext
    public HttpInboundServiceContext getISC(){
        return this.httpin.getISC();
    }

    //Pass through to the HttpInputStream to do the first read when we return from an async read
    public void initialRead(){
        this.httpin.initialRead();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read()
     */
    @Override
    public int read() throws IOException {
        
        //Return -1 here because if we are closed or finished, we're at the end of the stream
        if(isClosed || isFinished()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Nothing to read, stream is closed : " + isClosed + ", or finished : " + isFinished());
            return -1;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read" , "SRTInputStream31.read()");
        }
        
        int returnByte;
        
        if(this.request.isAsyncStarted()&& (this.getReadListener()!= null)){
            synchronized(this){
//                Check if isReady had returned false. If it did then an IllegalArgumentException will be thrown if a listener is set
                isReadyFalseCheck();
                returnByte = super.read();
            }
        } else{
            returnByte = super.read();
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read" , "SRTInputStream31.read() : " + returnByte);
        }
        
        return returnByte;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read(byte[])
     */
    @Override
    public int read(byte[] output) throws IOException {
        
        //Return -1 here because if we are closed or finished, we're at the end of the stream
        if(isClosed || isFinished()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Nothing to read, stream is closed : " + isClosed + ", or finished : " + isFinished());
            return -1;
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read" , "SRTInputStream31.read(byte[])");
        }
        if(output == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "read.write.bytearray.null");
            throw new NullPointerException(Tr.formatMessage(tc, "read.write.bytearray.null"));
        }
        
        int returnSize = 0;
        
        if(this.request.isAsyncStarted() && (this.getReadListener()!= null)){
            synchronized(this){
                
                //Check if isReady had returned false. If it did then an IllegalArgumentException will be thrown if a listener is set
                isReadyFalseCheck();
                returnSize = super.read(output);
            }
        }else{
            returnSize = super.read(output);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read" , "SRTInputStream31.read(byte[]) : " + returnSize);
        }
        
        return returnSize;
    }



    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] output, int offset, int length) throws IOException {
        
        //Return -1 here because if we are closed or finished, we're at the end of the stream
        if(isClosed || isFinished()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Nothing to read, stream is closed : " + isClosed + ", or finished : " + isFinished());
            return -1;
        }        
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read" , "SRTInputStream31.read(byte[], int, int)");
        }
        
        if(output == null){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "read.write.bytearray.null");
            throw new NullPointerException(Tr.formatMessage(tc, "read.write.bytearray.null"));
        }
        if((offset < 0) || (length < 0) || (length > (output.length - offset))){
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "read.write.offset.length.bytearraylength", new Object[] {offset, length, output.length});
            throw new IndexOutOfBoundsException(Tr.formatMessage(tc, "read.write.offset.length.bytearraylength", new Object[] {offset, length, output.length}));
        }
        
        int returnSize = 0;
        
        if(this.request.isAsyncStarted() && (this.getReadListener()!= null)){
            synchronized(this){
                //Check if isReady had returned false. If it did then an IllegalArgumentException will be thrown if a listener is set
                isReadyFalseCheck();
                
                returnSize = super.read(output, offset, length);
            }
        } else{
            returnSize = super.read(output, offset, length);
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read" , "SRTInputStream31.read(byte[], int, int) : " + returnSize);
        }
        
        return returnSize;
    }

    /*
     * This method will check if there is a listener set. If there is a listener set, then call checkAvailable.
     * checkAvailable checks the current read buffer to determine if there is data immediately available.
     * If a call to isReady is done first then this should always be in the correct state and return the proper response
     */
    private void isReadyFalseCheck(){
        if(this.listener != null && !isFinished()){
            //checkAvilable returns true or false depending on if there is data available in the read buffer
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Checking for readLine or if there is data available : " + readLineCall + ", " + checkAvailable());
            if(!readLineCall && !checkAvailable()){
                //If there is no data available then isReady will have returned false and this throw an IllegalStateException
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "read.failed.isReady.false");
                throw new IllegalStateException(Tr.formatMessage(tc, "read.failed.isReady.false"));
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "ReadListener is not set or the stream is finished : " + this.listener + ", " + isFinished());
        }
    }
    
    /**
     * @return the listener
     */
    public ReadListener getReadListener() {
        return listener;
    }
    
        
    public void setAsyncReadOutstanding(boolean asyncReadOutstanding){
        this.asyncReadOutstanding = asyncReadOutstanding;
    }
    
    /**
     * Sets up for driving the read listener again on another thread.
     */
    public void prepareAsyncReadListener() {
        // Need to call pre-join since the async read will occur on another
        // thread.
        AsyncContext31Impl ac = (AsyncContext31Impl)request.getAsyncContext();
        ac.continueReadListener();
    }
    
    /**
     * @return the completeLockObj
     */
    public Object getCompleteLockObj() {
        return completeLockObj;
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#readLine(byte[], int, int)
     */
    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        
        //Return -1 here because if we are closed or finished, we're at the end of the stream
        if(isClosed || isFinished()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Nothing to read, stream is closed : " + isClosed + ", or finished : " + isFinished());
            return -1;
        }
        
        //We want to prevent readLine calls from going async since they can be unpredictable
        this.readLineCall =  true;
        
        int readLineReturn = super.readLine(b, off, len);
        
        this.readLineCall = false;
        
        return readLineReturn;
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException {
        //Return -1 here because if we are closed or finished, we're at the end of the stream
        if(isClosed || isFinished()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Nothing to read, stream is closed : " + isClosed + ", or finished : " + isFinished());
            return -1;
        }
        
        return super.skip(n);
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "The input stream has been closed : " + this + " read Listener running ->" + listener);
        
        isClosed = true;
        listener = null;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "The input stream has been closed : " + this + " read Listener ->" + listener);
        
        super.close();
    }
    
    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#restart()
     */
    @Override
    public void restart() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "restart", "SRTInputStream31: Start re-read of data");
        }
        this.isClosed = false;
        super.restart();
    }
    
    /**
     * @return the callback
     */
    public InterChannelCallback getCallback() {
        return callback;
    }
}
