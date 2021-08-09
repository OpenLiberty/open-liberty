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
package com.ibm.ws.webcontainer31.srt;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.upgrade.UpgradeAsyncWriteCallback;
import com.ibm.ws.webcontainer31.upgrade.UpgradeWriteListenerRunnable;
import com.ibm.ws.webcontainer31.util.UpgradeOutputByteBufferUtil;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;


/**
 * 
 * This stream will be returned by WebConnectionImpl on getOutputStream.
 * The upgraded connection requires new output stream which is not HTTP.
 * This includes the APIs which the application can call and will be available for the Upgraded OutputStream provided to the customer. 
 * 
 * The OutputStream returned will be using Bytebuffers as per Java Servlet Specification 3.1 and will be implemented in UpgradeOutputByteBufferUtil. 
 * 
 * This stream can be invoked by multiple threads so writes needs to be synchronized.
 * 
 */
public class SRTUpgradeOutputStream31 extends ServletOutputStream
{
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(SRTUpgradeOutputStream31.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );
    private static final byte[] CRLF = new byte[] { 0x0D, 0x0A };
    private byte[] singleByte = new byte[1];
    protected UpgradeOutputByteBufferUtil _outHelper; // Helper class for implementing buffer
    //WriteListener to callback into user code when data is available
    private WriteListener _listener = null;
    public UpgradeAsyncWriteCallback callback;

    /**
     * @param outBuff
     * @param req
     */
    public void init(UpgradeOutputByteBufferUtil outBuff, IExtendedRequest req){    
        this._outHelper = outBuff;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "init upgrade output");         
        } 
    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#close()
     */
    public void close() throws java.io.IOException{
        synchronized(this){
            if(_outHelper.isOutputStream_closed()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "output stream close previously completed ...return ");
                }
                return;
            }
            else{
                _outHelper.closeWork();
            }
        }
    }


    public UpgradeOutputByteBufferUtil getBufferHelper(){
        return _outHelper;        
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletOutputStream#isReady()
     */
    @Override
    public boolean isReady() {       
        return _outHelper.isWriteReadyWork(true);
    }   

    /* (non-Javadoc)
     * @see javax.servlet.ServletOutputStream#setWriteListener(javax.servlet.WriteListener)
     */
    @Override
    public void setWriteListener(WriteListener appWLObject) {
        //Throw a NullPointerException if the WriteListener passed in was null
        if(appWLObject == null){            

            Tr.error(tc, "writelistener.is.null");  
            throw new NullPointerException(Tr.formatMessage(tc, "writelistener.is.null"));
        } 
        if(_outHelper.isOutputStream_closed() || _outHelper.isOutputStream_close_initiated_but_not_Flush_ready()){
            Tr.error(tc, "stream.is.closed.no.read.write");                         
            throw new IllegalStateException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));           
        }
        //Throw an IllegalStateException is there was already a WriteListener set for this InputStream
        if(this._listener != null){            
            Tr.error(tc, "writelistener.already.started");                         
            throw new IllegalStateException(Tr.formatMessage(tc, "writelistener.already.started"));
        } 
        this._listener = appWLObject;        
        //Create the ThreadContextManager to save off the threads information to pass onto new threads
        ThreadContextManager tcm = new ThreadContextManager();
        //Create a new callback
        this.callback = new UpgradeAsyncWriteCallback(this._listener, this , tcm, _outHelper._upConn);                         
        _outHelper.setWriteListenerCallBack(callback);
        _outHelper.set_listener(_listener);

        //Create the WriteListenerRunnable to call onWritePossible on a new thread
        UpgradeWriteListenerRunnable wlRunnable = new UpgradeWriteListenerRunnable(this._listener, this , tcm, callback);

        // make sure no more write allowed on this Thread as this set
        // WriteListener.
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
//        reqState.setAttribute("com.ibm.ws.webcontainer.upgrade.ThisThreadSetWL", true);
        reqState.removeAttribute("com.ibm.ws.webcontainer.upgrade.WriteAllowedonThisThread");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "setWriteListener: start new thread with wlRunnable");
        }

        try {
            com.ibm.ws.webcontainer.osgi.WebContainer.getExecutorService().execute(wlRunnable);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "setWriteListener: An exception occurred during the execute : " + e);
            }
            this.callback.error(_outHelper.get_vc(), e);                
        }


    }

    /**
     * 
     * @param b int
     */
    /* (non-Javadoc)
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {

        byte[] buf = new byte[1];
        buf[0] = (byte) b;

        this.write(buf, 0, 1);

    }

    /**
     * @param b byte[]
     * @param off int
     * @param len int
     */
    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
        if(_outHelper.isOutputStream_closed()|| _outHelper.isOutputStream_close_initiated_but_not_Flush_ready()){
            Tr.error(tc, "stream.is.closed.no.read.write");                         
            throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));           
        }
        if(b == null){
            Tr.error(tc, "read.write.bytearray.null"); 
            throw new NullPointerException(Tr.formatMessage(tc, "read.write.bytearray.null"));
        }
        if (off <0 || len < 0 || off+len > b.length)
        {            
            Tr.error(tc, "read.write.offset.length.bytearraylength", new Object[] { off,  len, b.length} );
            throw new IndexOutOfBoundsException(Tr.formatMessage(tc, "read.write.offset.length.bytearraylength", new Object[] { off,  len, b.length}));            
        }
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "write : non blocking write requested, WriteListener enabled: " + this._listener);
            synchronized(this){                 
              _outHelper.write_NonBlocking(b, off, len);                
            }
        }           
        else {
            _outHelper.writeWork(b, off, len);
            
        }

    }

    /* (non-Javadoc)
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException {       
        this.write(b, 0, b.length);        
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(boolean)
     */
    public void print(boolean b) throws IOException
    {   
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){   
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print boolean , WriteListener enabled: " + this._listener);       
            _outHelper.print_NonBlocking(Boolean.toString(b));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print boolean");        
            String value = Boolean.toString(b);
            this.write(value.getBytes(), 0, value.length()); 
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(char)
     */
    public void print(char c) throws IOException
    {
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print char , WriteListener enabled: " + this._listener); 
            _outHelper.print_NonBlocking(Character.toString(c));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print char");
            this.singleByte[0] = (byte) c;
            this.write(this.singleByte, 0, 1);
        }    
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(double)
     */
    public void print(double d) throws IOException
    {      
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print double , WriteListener enabled: " + this._listener); 
            _outHelper.print_NonBlocking(Double.toString(d));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print double");
            String value = Double.toString(d);
            this.write(value.getBytes(), 0, value.length());
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(float)
     */
    public void print(float f) throws IOException
    {      
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print float , WriteListener enabled: " + this._listener);
            _outHelper.print_NonBlocking(Float.toString(f));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print float");
            String value = Float.toString(f);
            this.write(value.getBytes(), 0, value.length());
        }

    }

    /*
     * @see javax.servlet.ServletOutputStream#print(int)
     */
    public void print(int i) throws IOException
    {      
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print int , WriteListener enabled: " + this._listener);
            _outHelper.print_NonBlocking(Integer.toString(i));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print int");
            String value = Integer.toString(i);
    this.write(value.getBytes(), 0, value.length());
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(long)
     */
    public void print(long l) throws IOException
    {      
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print long , WriteListener enabled: " + this._listener);

            _outHelper.print_NonBlocking(Long.toString(l));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print long");
            String value = Long.toString(l);
    this.write(value.getBytes(), 0, value.length());
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(java.lang.String)
     */
    public void print(String value) throws IOException
    {
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print String , WriteListener enabled: " + this._listener);
            
            if(value!=null){
                _outHelper.print_NonBlocking(value);
            }            
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print String");
            if(value!=null) {
                this.write(value.getBytes(), 0, value.length());        
            }
        }
    }


    /*
     * @see javax.servlet.ServletOutputStream#println()
     */
    public void println() throws IOException
    {
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            
            _outHelper.write_NonBlocking(CRLF, 0, 2);
        }
        else {
            this.write(CRLF, 0, 2);
        }

    }

    /*
     * @see javax.servlet.ServletOutputStream#println(boolean)
     */
    public void println(boolean b) throws IOException
    {    
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println boolean , WriteListener enabled: " + this._listener);          

            _outHelper.println_NonBlocking(Boolean.toString(b));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println boolean");
            String value = Boolean.toString(b);
            synchronized(this) {
                    this.write(value.getBytes(), 0, value.length());
                    this.write(CRLF, 0, 2);
            }
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(char)
     */
    public void println(char c) throws IOException
    {  
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println char , WriteListener enabled: " + this._listener);
            _outHelper.println_NonBlocking(Character.toString(c));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println char");
            this.singleByte[0] = (byte) c;
            synchronized(this) {
                this.write(this.singleByte, 0, 1);
                this.write(CRLF, 0, 2);
            }
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(double)
     */
    public void println(double d) throws IOException
    {
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println double , WriteListener enabled: " + this._listener);
            _outHelper.println_NonBlocking(Double.toString(d));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println double");
            String value = Double.toString(d);
            synchronized(this) {
                this.write(value.getBytes(), 0, value.length());
                this.write(CRLF, 0, 2);
            }
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(float)
     */
    public void println(float f) throws IOException
    {       
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println float , WriteListener enabled: " + this._listener);
            _outHelper.println_NonBlocking(Float.toString(f));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println float");
            String value = Float.toString(f);
            synchronized(this) {
                this.write(value.getBytes(), 0, value.length());
                this.write(CRLF, 0, 2);
            }
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(int)
     */
    public void println(int i) throws IOException
    {       
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println int , WriteListener enabled: " + this._listener);
            _outHelper.println_NonBlocking(Integer.toString(i));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println int");
            String value = Integer.toString(i);
            synchronized(this) {
                this.write(value.getBytes(), 0, value.length());
                this.write(CRLF, 0, 2);
            }
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(long)
     */
    public void println(long l) throws IOException
    {        
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println long , WriteListener enabled: " + this._listener);
            _outHelper.println_NonBlocking(Long.toString(l));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println long");
            String value = Long.toString(l);
            synchronized(this) {
                this.write(value.getBytes(), 0, value.length());
                this.write(CRLF, 0, 2);
            }
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println(java.lang.String)
     */
    public void println(String s) throws IOException
    {
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking println String , WriteListener enabled: " + this._listener);
            if(s!=null){
                _outHelper.println_NonBlocking(s);
            }            
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println String");
            synchronized(this) {
                if(s!=null) {
                    this.write(s.getBytes(), 0, s.length());
                }
                this.write(CRLF, 0, 2);
            }
        }
    }


    /**
     * @return
     */
    public boolean checkIfCalledFromWLonError(){
        boolean allowBlockingWrite = false;
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);        
        if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.upgrade.AllowWriteFromE")!=null){
            allowBlockingWrite = true;
        }
        return allowBlockingWrite;
    }


    /* (non-Javadoc)
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException {
        synchronized(this) {           
             _outHelper.flushHelper();
            
        }
    }  
    



}
