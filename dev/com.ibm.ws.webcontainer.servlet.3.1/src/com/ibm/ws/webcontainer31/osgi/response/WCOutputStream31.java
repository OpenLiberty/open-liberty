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
package com.ibm.ws.webcontainer31.osgi.response;

import java.io.IOException;

import javax.servlet.WriteListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.http.channel.outstream.HttpOutputStreamConnectWeb;
import com.ibm.ws.webcontainer.async.AsyncContextImpl;
import com.ibm.ws.webcontainer.osgi.request.IRequestImpl;
import com.ibm.ws.webcontainer.osgi.response.WCOutputStream;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.ws.webcontainer31.async.AsyncWriteCallback;
import com.ibm.ws.webcontainer31.async.ThreadContextManager;
import com.ibm.ws.webcontainer31.async.listener.WriteListenerRunnable;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.ee7.HttpOutputStreamEE7;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * Servlet stream wrapper around a regular IO output stream instance.
 * Added for Servlet 3.1
 */
public class WCOutputStream31 extends WCOutputStream
{

    //protected static final String CRLFString = Arrays.toString(CRLF);
    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(WCOutputStream31.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );    

    //WriteListener to callback into user code when data is available
    private WriteListener _listener = null;
    //callback used when communicating in an async manner with the HTTP Channel
    private AsyncWriteCallback _callback;
    private HttpOutputStreamEE7 _httpOut = null;
    private IRequestImpl reqImpl ;

    private static final byte[] CRLF = new byte[] { 0x0D, 0x0A };
    
    private boolean outputStreamNBClosed = false;

    /**
     * Constructor.
     * 
     * @param stream
     */
    public WCOutputStream31(HttpOutputStreamConnectWeb stream , IRequest req)
    {
        super(stream);           
        //if(stream instanceof HttpOutputStreamExtended) // findbugs says need this, but code review decided not to check.
        // We rather see NoSuchMethod instead of NullPointer
        _httpOut = (HttpOutputStreamEE7)stream;
        this.reqImpl = (IRequestImpl)req;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "constructor, out-->" + (output != null ? output : "null"));
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.osgi.response.WCOutputStream#close()
     */
    public void close() throws java.io.IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
            Tr.debug(tc, "close output");         
        }

        if(this._listener!= null && this.isOutputStreamNBClosed()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "output stream close previously completed ...return ");
            }
            return;
        }

        try{
            this._httpOut.setAsyncServletWriteListenerCallBack(null);    
            super.close();
        }
        finally {
            if(this._listener!= null)
                this.setOutputStreamNBClosed(true);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletOutputStream#isReady()
     */
    @Override
    public boolean isReady() {

        if(this.isOutputStreamNBClosed()){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "output stream closed, ready->" + false);
            }
            return false;
        }
        return this.isWriteReadyWork(true);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletOutputStream#setWriteListener(javax.servlet.WriteListener)
     */
    @Override
    public void setWriteListener(WriteListener appWLObject) {

        if(appWLObject == null){  
            Tr.error(tc, "writelistener.is.null");  
            throw new NullPointerException(Tr.formatMessage(tc, "writelistener.is.null"));
        }
        
        if(this.isOutputStreamNBClosed()){
            Tr.error(tc, "stream.is.closed.no.read.write");                         
            throw new IllegalStateException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));           
        }

        if(!this.reqImpl.isStartAsync()){               
            Tr.error(tc, "writelistener.async.not.started");  
            throw new IllegalStateException(Tr.formatMessage(tc, "writelistener.async.not.started"));
        } 

        //Throw an IllegalStateException is there was already a WriteListener set for this InputStream
        if(this._listener != null){           
            Tr.error(tc, "writelistener.already.started");                         
            throw new IllegalStateException(Tr.formatMessage(tc, "writelistener.already.started"));
        }                                  

        this._listener = appWLObject;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())  {  
            Tr.debug(tc, "set WriteListener enabled: " + this._listener);
        }

        //Create a new thread manager to pass into the callback and into the runnable
        ThreadContextManager tcm = new ThreadContextManager();
        //Create a new Callback so we can use it for our async write callbacks
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
        IExtendedRequest req = ((AsyncContextImpl) reqState.getAsyncContext()).getIExtendedRequest();
        SRTServletRequestThreadData threadData = req instanceof SRTServletRequest ? SRTServletRequestThreadData.getInstance(((SRTServletRequest) req).getRequestData()) : SRTServletRequestThreadData.getInstance();
        this._callback = new AsyncWriteCallback(this._listener, this, _httpOut, tcm, threadData);            

        //tell the outputStream that WriteListener has been set by application
        this._httpOut.setAsyncServletWriteListenerCallBack(_callback);    

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && this.output.hasBufferedContent()){  
            Tr.debug(tc, "stream has data before start write listener first time");
        }                                      
        //Creates the runnable to run the setWriteListener on a new thread
        WriteListenerRunnable wlRunnable = new WriteListenerRunnable(this._listener, _httpOut, _callback, tcm, threadData);
        
        // make sure no more write allowed on this Thread as this set WL.
//        reqState.setAttribute("com.ibm.ws.webcontainer.ThisThreadSetWL", true);
        if (reqState.getAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread")!=null){
            reqState.removeAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread");               
        }
        try {
            com.ibm.ws.webcontainer.osgi.WebContainer.getExecutorService().execute(wlRunnable);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){  
                Tr.debug(tc, "An exception occurred during the execute , onWritePossible may not be called: " + e.toString());
            }
            this._listener.onError(e);           
        }      
    }

    /**
     * @return
     */
    public boolean checkIfCalledFromWLonError(){
        boolean allowBlockingWrite = false;
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);        
        if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.AllowWriteFromE")!=null){
            allowBlockingWrite = true;
        }
        return allowBlockingWrite;
    }
    
    /*
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int start, int len) throws IOException
    {
        
        if(b == null){
            Tr.error(tc, "read.write.bytearray.null"); 

            throw new NullPointerException(Tr.formatMessage(tc, "read.write.bytearray.null"));
        }
        if (start <0 || len < 0 || start+len > b.length)
        {
            Tr.error(tc, "read.write.offset.length.bytearraylength", new Object[] { start,  len, b.length} );

            throw new IndexOutOfBoundsException(Tr.formatMessage(tc, "read.write.offset.length.bytearraylength", new Object[] { start,  len, b.length}));            
        }
        
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            synchronized(this){ 
                
                if(this.isOutputStreamNBClosed()){
                    Tr.error(tc, "stream.is.closed.no.read.write");                         
                    throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));           
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                    Tr.debug(tc, "non blocking write requested, WriteListener enabled: " + this._listener);
                write_NonBlocking(b, start, len); 
            }
        }
        else{
            super.write(b, start, len);
        }
    }

    /**
     * @param value
     * @param start
     * @param len
     * @throws IOException
     */
    private void write_NonBlocking(byte[] value, int start, int len) throws IOException
    {
        // check if we are allowed to write on this thread, check for com.ibm.ws.webcontainer.WriteAllowedonThisThread
        WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);        
        if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread")!=null){
            super.write(value, start, len);

            if (reqState.getAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread")!=null){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                    Tr.debug(tc, "back to write_NonBlocking , remove write allowed attribute, -> WriteListener enabled: " + this._listener);
                reqState.removeAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread");
            }

        }
        else{
            Tr.error(tc, "blocking.write.not.allowed", new Object[] {this._listener}) ;
            
            throw new BlockingWriteNotAllowedException(Tr.formatMessage(tc, "blocking.write.not.allowed", new Object[] {this._listener})); 
        }
    }

    /*
     * @see java.io.OutputStream#write(byte[])
     */
    public void write(byte[] value) throws IOException
    {
        this.write(value, 0, value.length);  

    }

    /*
     * @see java.io.OutputStream#write(int)
     */
    public void write(int value) throws IOException
    {
        byte[] buf = new byte[1];
        buf[0] = (byte) value;

        this.write(buf, 0, 1);
    }

    /*
     * @see javax.servlet.ServletOutputStream#print(boolean)
     */
    public void print(boolean b) throws IOException
    {   
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "non blocking print boolean , WriteListener enabled: " + this._listener);
            this.print_NonBlocking(Boolean.toString(b));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print boolean");        
            super.print(b);
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
            this.print_NonBlocking(Character.toString(c));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print char");
            super.print(c);
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
            this.print_NonBlocking(Double.toString(d));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print double");
            super.print(d);
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
            this.print_NonBlocking(Float.toString(f));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print float");
            super.print(f);
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
            this.print_NonBlocking(Integer.toString(i));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print int");
            super.print(i);
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
            this.print_NonBlocking(Long.toString(l));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print long");
            super.print(l);
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
                this.print_NonBlocking(value);
            }            
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "print String");
            super.print(value);
        }
    }

    /**
     * @param value
     * @throws IOException
     */
    private void print_NonBlocking(String value) throws IOException{
        synchronized(this){  
            if(this.isOutputStreamNBClosed()){
                Tr.error(tc, "stream.is.closed.no.read.write");                         
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));        
            } 
            this.write_NonBlocking(value.getBytes(), 0, value.length());         
        }
    }

    /*
     * @see javax.servlet.ServletOutputStream#println()
     */
    public void println() throws IOException
    {
        if(this._listener!= null &&  !checkIfCalledFromWLonError()){
            this.write_NonBlocking(CRLF, 0, 2);
        }
        else {
            super.println();
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
            
            this.println_NonBlocking(Boolean.toString(b));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println boolean");
            super.println(b);
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
            this.println_NonBlocking(Character.toString(c));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println char");
            super.println(c);
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
            this.println_NonBlocking(Double.toString(d));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println double");
            super.println(d);
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
            this.println_NonBlocking(Float.toString(f));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println float");
            super.println(f);
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
            this.println_NonBlocking(Integer.toString(i));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println int");
            super.println(i);
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
            this.println_NonBlocking(Long.toString(l));
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println long");
            super.println(l);
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
                this.println_NonBlocking(s);
            }            
        }
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())    
                Tr.debug(tc, "println String");
            super.println(s);
        }
    }

    /**
     * @param value
     * @throws IOException
     */
    private void println_NonBlocking(String value) throws IOException{
        synchronized(this){
            if(this.isOutputStreamNBClosed()){
                Tr.error(tc, "stream.is.closed.no.read.write");                         
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));        
            } 
            this.write_NonBlocking(value.getBytes(), 0, value.length());

            // need to check if we can write first in case the first write has gone async
            if(_httpOut.isWriteReady()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
                    Tr.debug(tc, "println crlf , write allowed now , WriteListener enabled: " + this._listener + " , check crlf_pending " +  _httpOut.write_crlf_pending);     
                }
                WebContainerRequestState.getInstance(true).setAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread", true);
                this.writeCRLFIfNeeded();
            }
            else{
                // the previous write must have gone async and has not completed yet.
                // Do the next write of CRLF in complete
                _httpOut.write_crlf_pending= true;
            }
        }
    }
    
    /*
     * @see java.io.OutputStream#flush()
     */    
    public void flush() throws IOException
    {   
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "flush");
        boolean crlfWrite = false;
        // check for nonblocking
        if(this._listener!= null){
            synchronized(this){

                WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
                if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.CRLFWriteinPorgress")!=null){                                      
                    crlfWrite = true;
                }
                if(this.isWriteReadyWork(false) || crlfWrite ){
                    super.flush();
                }
                else{
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "cannot flush, not write ready");
                    
                    return;
                }
            }
        }
        else{
            super.flush();
        }
    }
    
    
    /**
     * @param externalCall
     * @return
     */
    public boolean isWriteReadyWork(boolean externalCall) {

        boolean ready = true;
        synchronized(_httpOut._writeReadyLockObj){
            if(!_httpOut.isWriteReady()){
                ready = false;
                if(externalCall) {
                    _httpOut.status_not_ready_checked= true; 
                    WebContainerRequestState reqState = WebContainerRequestState.getInstance(false);
                    if (reqState!=null && reqState.getAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread")!=null){
                        reqState.removeAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread"); 
                    }
                }
            }
            else{
                // we are ready , set the attribute if this thread did not setWL
                WebContainerRequestState reqState = WebContainerRequestState.getInstance(true);
//                if (reqState.getAttribute("com.ibm.ws.webcontainer.ThisThreadSetWL")!=null){
//                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
//                        Tr.debug(tc, "cannot write from this Thread as this SetWL->"+ this._listener);
//                    }
//                    ready = false;            
//                }
//                else{
                    reqState.setAttribute("com.ibm.ws.webcontainer.WriteAllowedonThisThread", true);                       
//                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
            Tr.debug(tc," ready->"+ready);
        }
        return ready;           
    }
    
    /**
     * @throws IOException
     */
    public void writeCRLFIfNeeded() throws IOException{
        synchronized(this){
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){ 
                Tr.debug(tc, "write queue is empty and now write crlf, WriteListener enabled: " + this._listener);
            }                                       
            _httpOut.write_crlf_pending = false;                                
            // write from here
            this.write_NonBlocking(CRLF, 0, 2);                                                                                                     
        }
    }
    
    /**
     * @return the outputStreamNBClosed
     */
    public boolean isOutputStreamNBClosed() {
        return outputStreamNBClosed;
    }

    /**
     * @param outputStreamNBClosed the outputStreamNBClosed to set
     */
    public void setOutputStreamNBClosed(boolean outputStreamNBClosed) {
        this.outputStreamNBClosed = outputStreamNBClosed;
    }

}
