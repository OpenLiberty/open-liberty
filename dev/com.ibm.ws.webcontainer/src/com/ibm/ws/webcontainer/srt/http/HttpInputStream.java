/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.srt.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.srt.SRTServletResponse;
import com.ibm.wsspi.webcontainer.WebContainerRequestState;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.util.IInputStreamObserver;
import com.ibm.wsspi.webcontainer.util.WSServletInputStream;

/**
 * This class implements a buffered input stream for reading servlet request
 * data. It also keeps track of the number of bytes that have been read, and
 * allows the specification of an optional byte limit to ensure that the
 * content length has not been exceeded.
 *
 * @version	1.13, 10/13/97
 */
public class HttpInputStream extends WSServletInputStream
{
    /**
     * The actual input stream for this request.
     */
    protected InputStream in;

    /**
     * The input buffer.
     */
    protected byte[] buf;

    /**
     * The current number of bytes in the buffer.
     */
    protected int count;

    /**
     * The current position in the buffer.
     */
    protected int pos;

    /**
     * The total number of bytes for the current request.
     */
    protected long total;  // PK79219

    /**
     * The maximum number of bytes for the current request.
     */
    protected long limit;  // PK72919

    /**
     * The content length for this request.
     */
    protected long length;

protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.srt.http");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.srt.http.HttpInputStream";
    private static TraceNLS nls = TraceNLS.getTraceNLS(HttpInputStream.class, "com.ibm.ws.webcontainer.resources.Messages");
    
    private IInputStreamObserver obs;  // F003449

    /**
     * Creates a new, uninitialized input stream using the specified
     * buffer size.
     * @param size the input buffer size
     */
    public HttpInputStream(int size)
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"HttpInputStream", "Constructor --> "+size);
        } 
        buf = new byte[size];
    }

    /**
     * Creates a new, uninitialized input stream using a default
     * buffer size.
     */
    public HttpInputStream()
    {
        this(512);
    }

    /**
     * Initializes the servlet input stream with the specified raw input stream.
     * @param in the raw input stream
     */
    public void init(InputStream in) throws IOException 
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"init", "init"); 
        } 
        this.in = in;
        next();
        obs=null;
    }

    /**
     * Begins reading the next request.
     */
    public void next()
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"next", "next");
        } 
        length = -1;
        limit = Long.MAX_VALUE;   // PM03146
        total = 0;
        count = 0;
        pos = 0;
    }

    /**
     * Finishes reading the request without closing the underlying stream.
     * @exception IOException if an I/O error has occurred
     */
    public void finish() throws IOException 
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"finish", "finish");
        } 
        // PM18453 start
        WebContainerRequestState requestState = WebContainerRequestState.getInstance(false);
        if (requestState != null && (Boolean.valueOf((String)requestState.getAttribute("InputStreamEarlyReadCompleted"))).booleanValue()) {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"finish", "Skip read because data has already been read and destroyed.");
            } 
            total=limit;
        }
        // PM18453 End
        else if (!SRTServletResponse.isSkipInputStreamRead() &&  length != -1 )        {
            // if content length set then skip remaining bytes in message body
            long remaining = limit - total;   // PK79219
            while ( remaining > 0 )
            {
                long n = skip(remaining);   // PM03146
                if ( n == 0 )
                {
                    // begin 280584.3    6021: Cleanup of  defect 280584.2    WAS.webcontainer
                    //logger.logp(Level.SEVERE, CLASS_NAME,"finish", nls.getString("Invalid.Content.Length"));
                    // end 280584.3    6021: Cleanup of  defect 280584.2    WAS.webcontainer
                    throw new IOException(nls.getString("Invalid.Content.Length","Invalid content length"));
                }
                remaining -= n;
            }
        }
    }

    /**
     * Resets the input stream for a new connection.
     */
    public void resets()
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"resets", "resets");
        } 
        this.in = null;
    }

    /**
     * Returns the total number of bytes read so far.
     * PK79219 change return to long
     */
    public long getTotal()
    {
        return total;
    }

    /**
     * Sets the content length for this input stream. This should be called
     * once the headers have been read from the input stream.
     * @param len the content length
     */
    public void setContentLength(long len)
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"setContentLength", "setContentLength --> "+len);
        } 
        if ( len < 0 )
        {
            logger.logp(Level.SEVERE, CLASS_NAME,"setContentLength", "Illegal.Argument.Invalid.Content.Length");
            throw new IllegalArgumentException(nls.getString("Illegal.Argument.Invalid.Content.Length","Illegal Argument: Invalid Content Length"));
        }
        length = len;
        if ( Long.MAX_VALUE - total > len )   // PK79219
        {
            limit = total + len;
        }
    }

    /**
     * Returns the content length for this input stream, or -1 if not set.
     */
    public long getContentLength()
    {
        return length;
    }

    /**
     * Reads a byte of data. This method will block if no input is available.
     * @return the byte read, or -1 if the end of the stream is reached or
     *	       the content length has been exceeded
     * @exception IOException if an I/O error has occurred
     */
    public int read() throws IOException 
    {
        if ( total >= limit )
        {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"read", "Over the limit: -1");
            } 
            return -1;
        }
        if ( pos >= count )
        {
            fill();
            if ( pos >= count )
            {
                if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"read", pos+" >= "+count+" : -1");
                } 
                return -1;
            }
        }
        total++;
        return buf[pos++] & 0xff;
    }

    /**
     * Reads into an array of bytes. This method will block until some input
     * is available.
     * @param b the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return the actual number of bytes read, or -1 if the end of the
     *         stream is reached
     * @exception IOException if an I/O error has occurred
     */
    public int read(byte []read_buffer, int offset, int length) throws IOException {
       // Copy as much as possible from the read buffer 
       if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
           logger.logp(Level.FINE, CLASS_NAME,"read", "read length -->"+length);
       } 
       
       if ( total >= limit )
       {
           if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
               logger.logp(Level.FINE, CLASS_NAME,"read", "Over the limit: -1");
           } 
           return -1;
       }
       
       int buf_len = count - pos;
       if (buf_len > 0) {
          if (buf_len >= length) {
             // Copy part of read buffer
             System.arraycopy(buf,pos,read_buffer,offset,length);
             pos += length;

             // begin 280584.2    java.io.IOException: SRVE0080E: Invalid content length    WAS.webcontainer    
             this.total += length;
             // end 280584.2    java.io.IOException: SRVE0080E: Invalid content length    WAS.webcontainer

             if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                 logger.logp(Level.FINE, CLASS_NAME,"read", "read returning -->"+length);
             }
             return length;
          }
          // Copy all read buffer
          System.arraycopy(buf,pos,read_buffer,offset,buf_len);
          count = pos = 0;  // reset buffer
          offset += buf_len;
          length -= buf_len;
       }
       // Try to read remainder directly from the input stream into
       // the caller's buffer, avoiding an extra copy
       int bytes_read = buf_len;
	   int rtn = 0;
	   if(length>0)
	   {
			rtn = in.read(read_buffer,offset,length);
	   }
	   if (rtn > 0) {
		  bytes_read += rtn;
	   }
       // begin 280584.2    java.io.IOException: SRVE0080E: Invalid content length    WAS.webcontainer    
       this.total += bytes_read;
       // end 280584.2    java.io.IOException: SRVE0080E: Invalid content length    WAS.webcontainer    


       if (bytes_read == 0) {
           bytes_read = -1;
       }
       
       if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
       		logger.logp(Level.FINE, CLASS_NAME,"read", "read returning -->"+bytes_read+", total="+total+",limit="+limit);
       }
       return bytes_read;
    }


    /**
     * Reads into an array of bytes until all requested bytes have been
     * read or a '\n' is encountered, in which case the '\n' is read into
     * the array as well.
     * @param b the buffer where data is stored
     * @param off the start offset of the data
     * @param len the length of the data
     * @return the actual number of bytes read, or -1 if the end of the
     *         stream is reached or the byte limit has been exceeded
     * @exception IOException if an I/O error has occurred
     */
    public int readLine(byte[] b, int off, int len) throws IOException 
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"readLine", "readLine");
        } 
        
        if ( total >= limit )
        {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"readLine", "readLine Over the limit: -1");
            } 
            return -1;
        }
        int avail; //bytes available in buffer
        int readlen; //amount to be read by copyline
        int remain = 0; //amount remaining to be read
        int newlen; //amount read by copyline
        int totalread; //total amount read so far

        remain = len;
        avail = count - pos;
        if ( avail <= 0 )
        {
            fill();
            avail = count - pos;
            if ( avail <= 0 )
            {
                if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"readLine", "readLine avail less than 0: -1");
                } 
                return -1;
            }
        }
        if ( avail < len )
        {
            readlen = avail;
        }
        else
        {
            readlen = len;
        }
        newlen = copyLine(buf, pos, b, off, readlen);
        pos += newlen;
        total += newlen;
        remain -= newlen;
        totalread = newlen;
        if ( totalread == 0 )
        {
            //should never happen
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"readLine", "readLine totalRead is 0: -1");
            } 
            return -1;
        }
        while ( remain > 0 && b[off+totalread-1] != '\n' )
        {
            //loop through until the conditions of the method are satisfied
            fill();
            avail = count - pos;
            if ( avail <= 0 )
            {
                // The stream is finished, return what we have.
                if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"readLine", "readLine returning --> "+totalread);
                } 
                return totalread;
            }
            if ( avail < remain )
            {
                readlen = avail;
            }
            else
            {
                readlen = remain;
            }
            newlen = copyLine(buf, pos, b, off+totalread, readlen);
            pos += newlen;
            total += newlen;
            remain -= newlen;
            totalread += newlen;
        }

        return totalread;
    }

    /*
     * Copies up to a line of data from source to destination buffer.
     */
    private static int copyLine(byte[] src, int srcoff,
                                byte[] dst, int dstoff, int len)
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"readLine", "copyLine");
        } 
        int off = srcoff;
        while ( len-- > 0 && src[off++] != '\n' ) ;
        System.arraycopy(src, srcoff, dst, dstoff, off - srcoff);
        return off - srcoff;
    }

    /**
     * Skips n bytes of input.
     * @param n the number of bytes to skip
     * @return the actual number of bytes skipped
     * @exception IOException if an I/O error has occurred
     */
    public long skip(long n) throws IOException 
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"skip", "skip");
        }
        
        if ( total >= limit )
        {
            if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                logger.logp(Level.FINE, CLASS_NAME,"skip", "Total over limit: 0");
            } 
            return 0;
        }
        long remaining = n;
        while ( remaining > 0 )
        {
            int avail = count - pos;
            if ( avail <= 0 )
            {
                fill();
                avail = count - pos;
                if ( avail <= 0 )
                {
                    if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                        logger.logp(Level.FINE, CLASS_NAME,"skip", "skip avail < 0: "+(n - remaining));
                    } 
                    return n - remaining;
                }
            }
            if ( remaining < avail )
            {
                avail = (int)remaining;
            }
            remaining -= avail;
            pos += avail;
            total += avail;
        }
        return n;
    }

    /**
     * Returns the number of bytes that can be read without blocking.
     * @return the number of available bytes
     * @exception IOException if an I/O error has occurred
     */
    public int available() throws IOException {
    	
    	// PK79219 start
    	long longLeft = limit - total;
    	if (longLeft>Integer.MAX_VALUE)
    		return ((count - pos) + in.available());
    	else
            return Math.min((count - pos) + in.available(), (int)longLeft);
    	// PK79219 end	
    }

    /**
     * Closes the input stream.
     * @exception IOException if an I/O error has occurred
     */
    public void close() throws IOException 
    {
    	if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.entering(CLASS_NAME,"close","total="+total+",limit="+limit);
        } 
        // begin 280584.3    6021: Cleanup of  defect 280584.2    WAS.webcontainer
        // finish();
        // in.close();
        try{
        	finish();
        }finally{
        	in.close();
        	// F003449 Start
        	if (obs!=null) {
                if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
                    logger.logp(Level.FINE, CLASS_NAME,"close", "Notify observer that input stream has been closed.");
                } 
           	    obs.alertClose();
           	    // ensure future reads return -1 until restart() is called.
           	    // requried for multi read of post data when 
           	    // SRTServletResponse.isSkipInputStreamRead() returns true
           	    // ssee finish().
           	    total=limit;
        	}    
        	// F003449 End
        }
        // end 280584.3    6021: Cleanup of  defect 280584.2    WAS.webcontainer    
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.exiting(CLASS_NAME,"close","total="+total+",limit="+limit);
        } 
    }

    /**
     * Fills input buffer with more bytes.
     */
    protected void fill() throws IOException 
    {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"fill", "fill");
        } 
        
        // PK79219 Start
        long longLeft = limit-total;
        int len;
    	if (longLeft>Integer.MAX_VALUE)
    		len = buf.length;
    	else
            len = Math.min(buf.length, (int)longLeft);
    	// PK79219 End	
    	
        if ( len > 0 )
        {
            len = in.read(buf, 0, len);
            if ( len > 0 )
            {
                pos = 0;
                count = len;
            }
        }
    }
    
	// F003449 Start
    public void restart() {
        if (TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {  //306998.15
            logger.logp(Level.FINE, CLASS_NAME,"restart", "Start re-read of data");
        } 
        // Don't update length or limit because they were set by setContentLength() and
        // will not be reset when the data is re-read.
        total = 0;
        count = 0;
        pos = 0;
        // With F003449 obs should never be null if restart() is called.
        // However check for null to make code future proof.
        if (obs!=null) {
            obs.alertOpen();
        }
    }
    
    /*
     * The observer will be called when the data stream is closed and when it is re-opened.
     */
	public void setObserver(IInputStreamObserver obs) {
		this.obs = obs;
	}
	// F003449 End


}
