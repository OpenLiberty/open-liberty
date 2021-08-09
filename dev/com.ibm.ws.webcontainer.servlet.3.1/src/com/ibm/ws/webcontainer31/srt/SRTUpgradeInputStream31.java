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

import javax.servlet.ReadListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.srt.SRTInputStream;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.util.UpgradeInputByteBufferUtil;

/**
 * @author anupag
 * 
 * This stream will be returned by WebConnectionImpl on getInputStream.
 * The upgraded connection requires new input stream which is not HTTP. So we will not use SRTInputStream31 and make this as separate.
 * This includes the APIs which the application can call and will be available for the Upgraded InputStream provided to the customer. 
 * 
 * The InputStream returned will be using bytebuffers as per Java Servlet spec 3.1 and will be implemented in UpgradeInputByteBufferUtil. 
 * 
 */
public class SRTUpgradeInputStream31 extends SRTInputStream {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(SRTUpgradeInputStream31.class, 
                                                         WebContainerConstants.TR_GROUP, 
                                                         WebContainerConstants.NLS_PROPS );
    private boolean closed = false;
    protected UpgradeInputByteBufferUtil _inBuffer;
    // this.in defined in SRTInputStream

    public void init(UpgradeInputByteBufferUtil input)
    {    
        this._inBuffer = input;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {  
            Tr.debug(tc, "init upgrade input");         
        } 
    } 

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read()
     */
    @Override
    public int read() throws IOException
    {   
        synchronized(this){
            int value = 0;
            if(closed) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "stream.is.closed.no.read.write");
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));
            }

            if (this._inBuffer != null) {
                value = this._inBuffer.read();
            }
            else {
                value = this.in.read();
            }
            return value;
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read(byte[])
     */
    @Override
    public int read(byte[] output) throws IOException {

        return this.read(output, 0, output.length);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.webcontainer.srt.SRTInputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] output, int offset, int length) throws IOException {
        synchronized(this){
            int value = 0;
            if(closed) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                    Tr.error(tc, "stream.is.closed.no.read.write");
                throw new IOException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));
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

            if (this._inBuffer != null) {
                value = _inBuffer.read(output, offset, length);
            }
            else {
                value = this.in.read(output, offset, length);
            }

            return value;
        }
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isFinished()
     */
    @Override
    public boolean isFinished() {

        if(closed){
            return true;
        }
        
        return false;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#isReady()
     */
    @Override
    public boolean isReady() {
        //If we are closing it means we have read all the data
        if(closed){
            return true;
        }

        return _inBuffer.isReady();
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#setReadListener(javax.servlet.ReadListener)
     */
    @Override
    public void setReadListener(ReadListener arg0) {
        if(closed) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled())
                Tr.error(tc, "stream.is.closed.no.read.write");
            throw new IllegalStateException(Tr.formatMessage(tc, "stream.is.closed.no.read.write"));
        }
        _inBuffer.setupReadListener(arg0, this);
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    @Override
    @FFDCIgnore(InterruptedException.class)
    public void close() throws IOException {
        synchronized(this) {
            if(closed){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Input stream close previously called ...return ");
                }
                return;
            }
            else{
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling close on the util class");
                }
                closed = true;
                //Call close on the helper buffer class
                if(_inBuffer.close()){
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Timeout has been called, waiting for it to complete, " + _inBuffer.isClosing());
                        }
                        this.wait();
                    } catch (InterruptedException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Completed waiting for timeout to be called");
                        }
                    }
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling close on parent");
                }
                //Call the super close
                super.close();
            }
        }
    }

    public UpgradeInputByteBufferUtil getInputBufferHelper(){
        return _inBuffer;        
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletInputStream#readLine(byte[], int, int)
     */
    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
        //Call into the UpgradeInputByteBufferUtil to indicate a readLine is going on and we need to block
        _inBuffer.readLineCall();
        
        int readLineReturn = super.readLine(b, off, len);
        //Call into the UpgradeInputByteBufferUtil to indicate the readLine is complete
        _inBuffer.readLineCall();
        
        return readLineReturn;
    }
}
