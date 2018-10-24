/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.util;

import java.io.IOException;
import java.io.Writer;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.srt.WriteBeyondContentLengthException;
import com.ibm.wsspi.webcontainer.WCCustomProperties;

/**
 * This class implements a buffered writer for writing servlet
 * response data. It also keeps track of the number of chars that have
 * been written Additionally, an observer list is maintained which
 * can be used to notify observers the first time the stream is written to.
 * 
 */
public class BufferedWriter extends Writer implements ResponseBuffer
{

    private static final TraceComponent tc = Tr.register(BufferedWriter.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * The actual writer
     */
    protected Writer out;

    /**
     * The output buffer.
     */
    protected char[] buf = new char[0];

    /**
     * The current number of chars in the buffer.
     */
    protected int count;

    /**
     * The total number of chars written so far.
     */
    protected long total;

    /**
     * The maximum number of chars that can be written. This is initially
     * set to -1 in order to indicate that observers must be notified.
     */
    protected long limit;

    protected IResponse response;

    /**
     * The content length for this stream.
     */
    protected long length = -1L;

    /**
     * The observer that will be notified when the stream is first written.
     */
    protected IOutputStreamObserver obs;

    /**
     * Flag indicating that the first write has already occurred.
     */
    protected boolean _hasWritten;

    /**
     * Flag indicating that the first write has already occurred.
     */
    protected boolean _hasFlushed;

    /**
     * If set then an I/O exception is pending.
     */
    protected IOException except;

    /**
     * Indicated whether the buffer has been written to the stream
     */
    protected boolean committed;

    private int bufferSize;

    // private static final String
    // CLASS_NAME="com.ibm.wsspi.webcontainer.util.BufferedWriter";

    /**
     * Should we close the underlying stream on close ?
     * This flag is used for handling servlet chains which uses piped streams
     * to establish comunication from the filtered servlet to its servlet
     * filter.
     * By closing the filtered servlet output stream, we trigger the end
     * of the filter's input stream.
     */
    private boolean closeOnClose = false;

    private static TraceNLS nls = TraceNLS.getTraceNLS(BufferedWriter.class, "com.ibm.ws.webcontainer.resources.Messages");

    /**
     * Creates a new servlet output stream using the specified buffer size.
     * 
     * @param size
     *          the output buffer size
     */
    public BufferedWriter(int size)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "BufferedWriter(), size --> " + size + " this --> " + this);
        }
        buf = new char[size];
        bufferSize = size;
        _hasWritten = false;
        _hasFlushed = false;
    }

    /**
     * Creates a new, uninitialized servlet output stream with a default
     * buffer size.
     */
    public BufferedWriter()
    {
        this(1 * 1024);
    }

    /**
     * Initializes the iwriter with the specified raw writer.
     * 
     * @param out
     *          the raw writer
     */
    public void init(Writer out, int bufSize)
    {
        // make sure that we don't have anything hanging around between
        // init()s -- this is the fix for the broken pipe error being
        // returned to the browser
        initNewBuffer(out, bufSize);
    }

    /**
     * Initializes the output stream with the specified raw output stream.
     * 
     * @param out
     *          the raw output stream
     */
    void initNewBuffer(Writer out, int bufSize)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "initNewBuffer, size --> " + bufSize + " this --> " + this);
        }
        this.out = out;
        this.except = null;
        bufferSize = bufSize;
        buf = new char[bufferSize];
    }

    /**
     * Finishes the current response.
     */
    public void finish() throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "finish");
        }

        if (length == -1 && total != 0)
            length = total;

        //PM71666 - DBCS and application calls out.close(), the length set here is not correct (depending on # of bytes for each character)
        //          It will be more length by the time the data is encoded by the JDK sun.nio.cs.StreamEncoder, so more bytes are
        //          sent to client than specified in content-length.  Those extra will be dropped by the client without any exception.
        if (WCCustomProperties.SET_CONTENT_LENGTH_ON_CLOSE){
            if (!committed)
            {
                // first write on the close/finish path should set an explicit
                // content-length if we can
                if (!_hasFlushed && obs != null)
                {
                    if (!this.response.isCommitted())
                    {
                        setContentLengthHeader(length);
                    }
                    _hasFlushed = true;
                    obs.alertFirstFlush();
                }
                committed = true;
            }
        } //PM71666
        flush();
    }

    /**
     * Resets the output stream for a new connection.
     */
    public void reset()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "reset");
        }
        out = null;
        // obs = null;
        count = 0;
        total = 0L;
        limit = -1L;
        length = -1L;
        committed = false;
        _hasWritten = false;
        _hasFlushed = false;
        response = null;
    }

    /**
     * Returns the total number of chars written so far as an integer.
     */
    public int getTotal()
    {
        if (total > Integer.MAX_VALUE)
        {
          return -1;
        }
        return (int)total;
    }    
    
    /**
     * Returns the total number of chars written so far as a long.
     */
    public long getTotalLong()
    {
        return total;
    }



    /**
     * Sets an observer for this output stream. The observer will be
     * notified when the stream is first written to.
     */
    public void setObserver(IOutputStreamObserver obs)
    {
        this.obs = obs;
        limit = -1;
    }

    /**
     * Returns whether the output has been committed or not.
     */
    public boolean isCommitted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "isCommitted: " + committed);
        }
        return committed;
    }

    /**
     * Checks the output stream for a pending IOException that needs to be
     * thrown, or a content length that has been exceeded.
     * 
     * @param len
     *          the number of chars about to be written
     */
    protected void check() throws IOException
    {
        // check for pending IOException
        if (except != null)
        {
            flush();
            throw except;
        }
    }

    /**
     * Writes a char. This method will block until the char is actually
     * written.
     * 
     * @param b
     *          the char
     * @exception IOException
     *              if an I/O error has occurred
     */
    public void write(int c) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "write --> " + c);
        }

        if (!_hasWritten && obs != null)
        {
            _hasWritten = true;
            obs.alertFirstWrite();
        }

        if (limit > -1)
        {
            if (total >= limit)
            {
                throw new WriteBeyondContentLengthException();
            }
        }
        if (count == buf.length)
        {
            response.setFlushMode(false);
            flushChars();
            response.setFlushMode(true);
        }
        buf[count++] = (char) c;
        total++;
    }

    /**
     * Writes a String. This method will block until the String
     * is actually written.
     * 
     * Any changes to this method should also be made to the write(char[], int, int) method
     * 
     * @param str
     *          the data to be written
     * @param off
     *          the start offset of the data
     * @param len
     *          the number of chars to write
     * @exception IOException
     *              if an I/O error has occurred
     * @Override
     */
    public void write(@Sensitive String str, int off, int len) throws IOException {
        if (len < 0) { throw new IndexOutOfBoundsException(); }
        synchronized (lock) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            { // 306998.15
                Tr.debug(tc, "write(String) total: " + total + " len: " + len + " limit: " + limit + " buf.length: " + buf.length + " count: " + count);
            }
            if (!_hasWritten && obs != null)
            {
                _hasWritten = true;
                obs.alertFirstWrite();
            }

            if (limit > -1)
            {
                if (total + len > limit)
                {
                    len = (int)(limit - total);
                    except = new WriteBeyondContentLengthException();
                }
            }

            if (len >= buf.length)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                { // 306998.15
                    Tr.debug(tc, "write(String), len >= buf.length");
                }
                response.setFlushMode(false);
                flushChars();
                total += len;
                writeOut(str, off, len);
                // out.flush(); moved to writeOut 277717 SVT:Mixed information shown on
                // the Admin Console WAS.webcontainer
                response.setFlushMode(true);
                check();
                return;
            }
            int avail = buf.length - count;
            if (len > avail)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                { // 306998.15
                    Tr.debug(tc, "write(String), len >= avail");
                }
                response.setFlushMode(false);
                flushChars();
                response.setFlushMode(true);
            }
            str.getChars(off, off+len, buf, count);
            count += len;
            total += len;
            check();
        }
    }

    /**
     * Writes an array of chars. This method will block until all the chars
     * are actually written.
     * 
     * Any changes to this method should also be made to the write(String, int, int) method
     *
     * @param b
     *          the data to be written
     * @param off
     *          the start offset of the data
     * @param len
     *          the number of chars to write
     * @exception IOException
     *              if an I/O error has occurred
     * @Override
     */
    public void write(@Sensitive char[] b, int off, int len) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "write(char[]) total: " + total + " len: " + len + " limit: " + limit + " buf.length: " + buf.length + " count: " + count);
        }
        if (len < 0)
        {
            if (tc.isErrorEnabled())
                Tr.error(tc, "Illegal.Argument.Trying.to.write.chars");
            throw new IllegalArgumentException();
        }

        if (!_hasWritten && obs != null)
        {
            _hasWritten = true;
            obs.alertFirstWrite();
        }

        if (limit > -1)
        {
            if (total + len > limit)
            {
                len = (int)(limit - total);
                except = new WriteBeyondContentLengthException();
            }
        }

        if (len >= buf.length)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            { // 306998.15
                Tr.debug(tc, "write(char[]), len >= buf.length");
            }
            response.setFlushMode(false);
            flushChars();
            total += len;
            writeOut(b, off, len);
            // out.flush(); moved to writeOut 277717 SVT:Mixed information shown on
            // the Admin Console WAS.webcontainer
            response.setFlushMode(true);
            check();
            return;
        }
        int avail = buf.length - count;
        if (len > avail)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            { // 306998.15
                Tr.debug(tc, "write(char[]), len >= avail");
            }
            response.setFlushMode(false);
            flushChars();
            response.setFlushMode(true);
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
        total += len;
        check();
    }

    /**
     * Flushes the output stream.
     */
    public void flush() throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "flush");
        }

        flushChars();
    }

    /**
     * Flushes the writer chars.
     */
    protected void flushChars() throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "flushChars");
        }

        if (!committed)
        {
            if (!_hasFlushed && obs != null)
            {
                _hasFlushed = true;
                obs.alertFirstFlush();
            }
        }
        committed = true;
        if (count > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "flushChars, Count = " + count);
            }
            writeOut(buf, 0, count);
            // out.flush(); moved to writeOut 277717 SVT:Mixed information shown on
            // the Admin Console WAS.webcontainer
            count = 0;
        }
        else
        {// PK22392 start
            if (response.getFlushMode())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "flushChars, Count 0 still flush mode is true , forceful flush");
                }
                response.flushBufferedContent();
            }// PK22392 END
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "flushChars, flush mode is false");
                }
            }
        }
    }

    /**
     * Prints a string.
     * 
     * @exception IOException
     *              if an I/O error has occurred
     */
    public void print(String s) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "print --> " + s);
        }
        if (!_hasWritten && obs != null)
        {
            _hasWritten = true;
            obs.alertFirstWrite();
        }
        int len = s.length();
        if (limit > -1)
        {
            if (total + len > limit)
            {
                len = (int)(limit - total);
                except = new WriteBeyondContentLengthException();
            }
        }

        int off = 0;
        while (len > 0)
        {
            int n = buf.length - count;
            if (n == 0)
            {
                response.setFlushMode(false);
                flushChars();
                response.setFlushMode(true);
                n = buf.length - count;
            }
            if (n > len)
            {
                n = len;
            }

            s.getChars(off, off + n, buf, count);

            count += n;
            total += n;
            off += n;
            len -= n;
        }
        check();
    }

    /**
     * Closes the servlet output stream.
     */
    public void close() throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "close");
        }

        // Were we requested to close the underlying stream ?
        finish();
        try
        {
            // 104771 - alert the observer that the underlying stream is being closed
            obs.alertClose();

            // don't close the underlying stream...we want to reuse it
            // out.close();
        }
        catch (Exception ex)
        {
            com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ex, "com.ibm.ws.webcontainer.srt.BufferedWriter.close", "397", this);
        }
    }

    public void setLimit(int lim)
    {
        limit = (long)lim;
    }
    
    /**
     * Set the maximum number of chars which can be written as a long
     */   
    public void setLimitLong(long lim)
    {
        limit = lim;
    }  



    public void setResponse(IResponse resp)
    {
        response = resp;
    }

    /*
     * Writes to the underlying stream
     * 
     * Any changes to this method should also be made to the writeOut(char[], int, int) method
     */
    protected void writeOut(String str, int offset, int len) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "writeOut(String) --> " + len);
        }
        try
        {
            out.write(str, offset, len);
            out.flush(); // 277717 SVT:Mixed information shown on the Admin Console
            // WAS.webcontainer
        }
        catch (IOException ioe)
        {
            // No FFDC -- promote debug to event
            //com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ioe, "com.ibm.ws.webcontainer.srt.BufferedWriter.writeOut", "416", this);
            count = 0;

            // begin pq54943
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            { // 306998.15
                Tr.event(tc, "IOException occurred in writeOut(String) method, observer alerting close.", ioe);
            }
            // IOException occurred possibly due to SocketError from early browser
            // closure...alert observer to close writer
            obs.alertClose();
            // end pq54943

            // let the observer know that an exception has occurred...
            obs.alertException();

            throw ioe;
        }
    }

    /*
     * Writes to the underlying stream
     * 
     * Any changes to this method should also be made to the writeOut(String, int, int) method
     */
    protected void writeOut(char[] buf, int offset, int len) throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "writeOut(char[]) --> " + len);
        }
        try
        {
            out.write(buf, offset, len);
            out.flush(); // 277717 SVT:Mixed information shown on the Admin Console
            // WAS.webcontainer
        }
        catch (IOException ioe)
        {
            // No FFDC -- promote debug to event
            //com.ibm.wsspi.webcontainer.util.FFDCWrapper.processException(ioe, "com.ibm.ws.webcontainer.srt.BufferedWriter.writeOut", "416", this);
            count = 0;

            // begin pq54943
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            { // 306998.15
                Tr.event(tc, "IOException occurred in writeOut(char[]) method, observer alerting close.", ioe);
            }
            // IOException occurred possibly due to SocketError from early browser
            // closure...alert observer to close writer
            obs.alertClose();
            // end pq54943

            // let the observer know that an exception has occurred...
            obs.alertException();

            throw ioe;
        }
    }

    public int getBufferSize()
    {
        return bufferSize;
    }

    public void setBufferSize(int size)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "setBufferSize --> " + size);
        }
        if (total > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            { // 306998.15
                Tr.debug(tc, "setBufferSize(): illegal state--> already wrote " + total + " bytes");
            }
            throw new IllegalStateException(nls.getString("Cannot.set.buffer.size.after.data", "Can't set buffer size after data has been written to stream"));
        }
        initNewBuffer(out, size);
    }

    public void clearBuffer()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "clearBuffer");
        }

        if (isCommitted())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            { // 306998.15
                Tr.debug(tc, "clearBuffer(): illegal state--> stream is committed ");
            }
            throw new IllegalStateException();
        }
        total = 0;
        count = 0;
        _hasWritten = false;
    }

    public void flushBuffer() throws IOException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { // 306998.15
            Tr.debug(tc, "flushBuffer");
        }

        flush();
    }
    
    private void setContentLengthHeader(long length) {
        this.response.setHeader("Content-Length", (Long.toString(length)));
    }
    
    public void clean()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        { 
            Tr.debug(tc, "clean, this --> " + this);
        }

        buf = new char[0];
    }
}
