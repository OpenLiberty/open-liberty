/*
 * $Header: /cvshome/wascvs/jsp/src/org/apache/jasper/runtime/JspWriterImpl.java,v 1.1.1.1 2003/10/17 13:47:57 backhous Exp $
 * $Revision: 1.1.1.1 $
 * $Date: 2003/10/17 13:47:57 $
 *
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 
//Change history:
//Defect 312981 "PERF- JSPWriterImpl needs to be optimized to cut the pathlength"  2005/10/19 Scott Johnson
//Defect 347278 "PERF- JspWriter Performance Optimization"  2006/02/14 Scott Johnson
//APAR   PK90190 add check for property to create the writer even when the buffer is empty   2009/06/21 Jay Sartoris
//APAR   PM19500  Mem leak in WASJSPStrBufferImpl  07/28/10  pmdinh
//APAR   PM23029  Mem Leak in JspWriterImpl.response  (Bug 31510)   09/22/10   pmdinh
//APAR   PM47661  Return empty String rather than null value to be printed 09/13/2011 - pnicoluc
//APAR   PI24001  Non-reusable objects of type BodyContentImpl cause a memory leak when using custom tags in a JSP  11/11/2014  hmpadill

package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspWriter;

import com.ibm.ws.jsp.JSPStrBuffer;
import com.ibm.ws.jsp.JSPStrBufferFactory;
import com.ibm.wsspi.webcontainer.WCCustomProperties;  //PK90190


/**
 * Write text to a character-output stream, buffering characters so as
 * to provide for the efficient writing of single characters, arrays,
 * and strings. 
 *
 * Provide support for discarding for the output that has been 
 * buffered. 
 * 
 * This needs revisiting when the buffering problems in the JSP spec
 * are fixed -akv 
 *
 * @author Anil K. Vijendran
 */
public class JspWriterImpl extends JspWriter {
    private static final int K = 1024;
    private static final int DEFAULT_BUFFER_SIZE = 8*K;

    private Writer out;
    private ServletResponse response;    
    private char cb[];
    private int nextChar;
    private boolean flushed = false;
    private boolean closed = false;
    private JSPStrBuffer converterBuffer = null; // defect 347278
    
    public JspWriterImpl() {
		super( DEFAULT_BUFFER_SIZE, true );
		converterBuffer = JSPStrBufferFactory.getJSPStrBuffer();
    }

    /**
     * Create a buffered character-output stream that uses a default-sized
     * output buffer.
     *
     * @param  response  A Servlet Response
     */
    public JspWriterImpl(ServletResponse response) {
        this(response, DEFAULT_BUFFER_SIZE, true);
    }

    /**
     * Create a new buffered character-output stream that uses an output
     * buffer of the given size.
     *
     * @param  response A Servlet Response
     * @param  sz   	Output-buffer size, a positive integer
     *
     * @exception  IllegalArgumentException  If sz is <= 0
     */
    public JspWriterImpl(ServletResponse response, int sz, 
                         boolean autoFlush) {
        super(sz, autoFlush);
        if (sz < 0)
            throw new IllegalArgumentException("Buffer size <= 0");
		this.response = response;
	        cb = sz == 0 ? null : new char[sz];
		nextChar = 0;
		converterBuffer = JSPStrBufferFactory.getJSPStrBuffer();
    }

    void init( ServletResponse response, int sz, boolean autoFlush ) {
		this.response= response;
		if( sz > 0 && ( cb == null || sz > cb.length ) )
		    cb=new char[sz];
		nextChar = 0;
		this.autoFlush=autoFlush;
		this.bufferSize=sz;
    }

    /** Package-level access
     */
    void recycle() {
    	flushed = false;
        closed = false;
        out = null;
        nextChar = 0;
        converterBuffer.clear();							//PM19500
        response = null;									//PM23029
    }
    
    //PI24001 start
    /**
     * This will recycle JspWriterImpl no matter the value of recycleCb.
     * @param recycleCb - true if <i>cb</i> should not be set to null, false otherwise.
     */
    void recycle(boolean recycleCb) {
        if (!recycleCb) {
            recycle();
            cb = null;
        }
        else {
            recycle();
        }
    }
    //PI24001 end

    /**
     * Flush the output buffer to the underlying character stream, without
     * flushing the stream itself.  This method is non-private only so that it
     * may be invoked by PrintStream.
     */
    protected final void flushBuffer() throws IOException {
        if (bufferSize == 0)
            return;
        flushed = true;
        // defect 312981 begin
        //ensureOpen();
    	if (closed) {
    	    throw new IOException("Stream closed");
        }
        // defect 312981 end

        if (nextChar == 0) {
            //PK90190 - start 
            //add check for property to create the writer even when the buffer is empty
            if (WCCustomProperties.GET_WRITER_ON_EMPTY_BUFFER) {
                initOut();
            }
            //PK90190 - end
            return;
        }
        initOut();
        out.write(cb, 0, nextChar);
        nextChar = 0;
    }

    private void initOut() throws IOException {
        if (out == null) {
        	// defect 312981 begin
        	if (response == null){
        	    throw new IOException("ServletResponse object is null."); 
            }
        	// defect 312981 end
            out = response.getWriter();
        }
    }
	

    /**
     * Discard the output buffer.
     */
    public final void clear() throws IOException {
        if ((bufferSize == 0) && (out != null))
            // clear() is illegal after any unbuffered output (JSP.5.5)
             throw new IllegalStateException("jsp.error.ise_on_clear");
        if (flushed)
            throw new IOException("jsp.error.attempt_to_clear_flushed_buffer");
        // defect 312981 begin
        //ensureOpen();
    	if (closed) {
    	    throw new IOException("Stream closed");
        }
        // defect 312981 end
        nextChar = 0;
    }

    public void clearBuffer() throws IOException {
        if (bufferSize == 0)
            throw new IllegalStateException("jsp.error.ise_on_clear");
        // defect 312981 begin
        //ensureOpen();
    	if (closed) {
    	    throw new IOException("Stream closed");
        }
        // defect 312981 end
        nextChar = 0;
    }

    private final void bufferOverflow() throws IOException {
        throw new IOException("jsp.error.overflow");
    }

    /**
     * Flush the stream.
     *
     */
    public void flush()  throws IOException {
        flushBuffer();
        if (out != null) {
            out.flush();
        }
    }

    /**
     * Close the stream.
     *
     */
    public void close() throws IOException {
        if (response == null || closed)
            // multiple calls to close is OK
            return;
        flush();
        if (out != null)
            out.close();
        out = null;
        closed = true;
        //            cb = null;
    }

    /**
     * @return the number of bytes unused in the buffer
     */
    public int getRemaining() {
        return bufferSize - nextChar;
    }

    /** check to make sure that the stream has not been closed */
    // commented-out for defect 312981
//    private void ensureOpen() throws IOException {
//	if (response == null || closed)
//	    throw new IOException("Stream closed");
//    }


    /**
     * Write a single character.
     */
    public void write(int c) throws IOException {
        // defect 312981 begin
        //ensureOpen();
    	if (closed) {
    	    throw new IOException("Stream closed");
        }
        // defect 312981 end
        if (bufferSize == 0) {
            initOut();
            out.write(c);
        }
        else {
            if (nextChar >= bufferSize)
                if (autoFlush)
                    flushBuffer();
                else
                    bufferOverflow();
            cb[nextChar++] = (char) c;
        }
    }

    /**
     * Our own little min method, to avoid loading java.lang.Math if we've run
     * out of file descriptors and we're trying to print a stack trace.
     */
    private int min(int a, int b) {
	if (a < b) return a;
	return b;
    }

    /**
     * Write a portion of an array of characters.
     *
     * <p> Ordinarily this method stores characters from the given array into
     * this stream's buffer, flushing the buffer to the underlying stream as
     * needed.  If the requested length is at least as large as the buffer,
     * however, then this method will flush the buffer and write the characters
     * directly to the underlying stream.  Thus redundant
     * <code>DiscardableBufferedWriter</code>s will not copy data unnecessarily.
     *
     * @param  cbuf  A character array
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to write
     */
    public void write(char cbuf[], int off, int len) 
        throws IOException 
    {
        // defect 312981 begin
        //ensureOpen();
    	if (closed) {
    	    throw new IOException("Stream closed");
        }
        // defect 312981 end

        if (bufferSize == 0) {
            initOut();
            out.write(cbuf, off, len);
            return;
        }

        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
            ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        } 

        if (len >= bufferSize) {
            /* If the request length exceeds the size of the output buffer,
               flush the buffer and then write the data directly.  In this
               way buffered streams will cascade harmlessly. */
            if (autoFlush)
                flushBuffer();
            else
                bufferOverflow();
            initOut();
            out.write(cbuf, off, len);
            return;
        }

        int b = off, t = off + len;
        while (b < t) {
            int d = min(bufferSize - nextChar, t - b);
            System.arraycopy(cbuf, b, cb, nextChar, d);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize) 
                if (autoFlush)
                    flushBuffer();
                else
                    bufferOverflow();
        }

    }

    /**
     * Write an array of characters.  This method cannot be inherited from the
     * Writer class because it must suppress I/O exceptions.
     */
    public void write(char buf[]) throws IOException {
	write(buf, 0, buf.length);
    }

    /**
     * Write a portion of a String.
     *
     * @param  s     String to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     */
    public void write(String s, int off, int len) throws IOException {
        // defect 312981 begin
        //ensureOpen();
    	if (closed) {
    	    throw new IOException("Stream closed");
        }
        // defect 312981 end
        if (bufferSize == 0) {
            initOut();
            out.write(s, off, len);
            return;
        }
        int b = off, t = off + len;
        while (b < t) {
            int d = min(bufferSize - nextChar, t - b);
            s.getChars(b, b + d, cb, nextChar);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize) 
                if (autoFlush)
                    flushBuffer();
                else
                    bufferOverflow();
        }
    }

    /**
     * Write a string.  This method cannot be inherited from the Writer class
     * because it must suppress I/O exceptions.
     */
    public void write(String s) throws IOException {
	write(s, 0, s.length());
    }


    static String lineSeparator = (String)AccessController.doPrivileged(new PrivilegedAction() {
                                                                            public Object run() {
                                                                                return System.getProperty("line.separator");
                                                                            }
                                                                        });


    /**
     * Write a line separator.  The line separator string is defined by the
     * system property <tt>line.separator</tt>, and is not necessarily a single
     * newline ('\n') character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    
    public void newLine() throws IOException {
        write(lineSeparator);
    }


    /* Methods that do not terminate lines */

    /**
     * Print a boolean value.  The string produced by <code>{@link
     * java.lang.String#valueOf(boolean)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link
     * #write(int)}</code> method.
     *
     * @param      b   The <code>boolean</code> to be printed
     */
    public void print(boolean b) throws IOException {
	write(b ? "true" : "false");
    }

    /**
     * Print a character.  The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link
     * #write(int)}</code> method.
     *
     * @param      c   The <code>char</code> to be printed
     */
    public void print(char c) throws IOException {
	//write(String.valueOf(c));
        write(c);
    }

    /**
     * Print an integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(int)}</code> is translated into bytes according
     * to the platform's default character encoding, and these bytes are
     * written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param      i   The <code>int</code> to be printed
     */
    public void print(int integer) throws IOException {
	//write(String.valueOf(i));
        converterBuffer.append(integer);
        for (int i = 0; i < converterBuffer.length(); i++) {
        	write(converterBuffer.charAt(i));
        }
        converterBuffer.delete(0, converterBuffer.length());
    }

    /**
     * Print a long integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(long)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param      l   The <code>long</code> to be printed
     */
    public void print(long l) throws IOException {
	//write(String.valueOf(l));
    converterBuffer.append(l);
    for (int i = 0; i < converterBuffer.length(); i++) {
        write(converterBuffer.charAt(i));
    }
    converterBuffer.delete(0, converterBuffer.length());
    }

    /**
     * Print a floating-point number.  The string produced by <code>{@link
     * java.lang.String#valueOf(float)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param      f   The <code>float</code> to be printed
     */
    public void print(float f) throws IOException {
	//write(String.valueOf(f));
    converterBuffer.append(f);
    for (int i = 0; i < converterBuffer.length(); i++) {
        write(converterBuffer.charAt(i));
    }
    converterBuffer.delete(0, converterBuffer.length());
    }

    /**
     * Print a double-precision floating-point number.  The string produced by
     * <code>{@link java.lang.String#valueOf(double)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the <code>{@link
     * #write(int)}</code> method.
     *
     * @param      d   The <code>double</code> to be printed
     */
    public void print(double d) throws IOException {
	//write(String.valueOf(d));
    converterBuffer.append(d);
    for (int i = 0; i < converterBuffer.length(); i++) {
        write(converterBuffer.charAt(i));
    }
    converterBuffer.delete(0, converterBuffer.length());
    }

    /**
     * Print an array of characters.  The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param      s   The array of chars to be printed
     *
     * @throws  NullPointerException  If <code>s</code> is <code>null</code>
     */
    public void print(char s[]) throws IOException {
	write(s);
    }

    /**
     * Print a string.  If the argument is <code>null</code> then the string
     * <code>"null"</code> is printed.  Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param      s   The <code>String</code> to be printed
     */
    public void print(String s) throws IOException {
    	
    	// PM47661 - Start 
        if (s == null) {
            if (WCCustomProperties.EXPRESSION_RETURN_EMPTY_STRING) {
                write("");
            } else {
                write("null");
            }
        } else {
            write(s);
        }
    	// PM47661 - End
 
    }

    /**
     * Print an object.  The string produced by the <code>{@link
     * java.lang.String#valueOf(Object)}</code> method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param      obj   The <code>Object</code> to be printed
     */
    public void print(Object obj) throws IOException {
	//write(String.valueOf(obj));
    converterBuffer.append(obj);
    for (int i = 0; i < converterBuffer.length(); i++) {
        write(converterBuffer.charAt(i));
    }
    converterBuffer.delete(0, converterBuffer.length());
    }

    /* Methods that do terminate lines */

    /**
     * Terminate the current line by writing the line separator string.  The
     * line separator string is defined by the system property
     * <code>line.separator</code>, and is not necessarily a single newline
     * character (<code>'\n'</code>).
     *
     * Need to change this from PrintWriter because the default
     * println() writes  to the sink directly instead of through the
     * write method...  
     */
    public void println() throws IOException {
	newLine();
    }

    /**
     * Print a boolean value and then terminate the line.  This method behaves
     * as though it invokes <code>{@link #print(boolean)}</code> and then
     * <code>{@link #println()}</code>.
     */
    public void println(boolean x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a character and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(char)}</code> and then <code>{@link
     * #println()}</code>.
     */
    public void println(char x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print an integer and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(int)}</code> and then <code>{@link
     * #println()}</code>.
     */
    public void println(int x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a long integer and then terminate the line.  This method behaves
     * as though it invokes <code>{@link #print(long)}</code> and then
     * <code>{@link #println()}</code>.
     */
    public void println(long x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a floating-point number and then terminate the line.  This method
     * behaves as though it invokes <code>{@link #print(float)}</code> and then
     * <code>{@link #println()}</code>.
     */
    public void println(float x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a double-precision floating-point number and then terminate the
     * line.  This method behaves as though it invokes <code>{@link
     * #print(double)}</code> and then <code>{@link #println()}</code>.
     */
    public void println(double x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print an array of characters and then terminate the line.  This method
     * behaves as though it invokes <code>{@link #print(char[])}</code> and then
     * <code>{@link #println()}</code>.
     */
    public void println(char x[]) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a String and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(String)}</code> and then
     * <code>{@link #println()}</code>.
     */
    public void println(String x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print an Object and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(Object)}</code> and then
     * <code>{@link #println()}</code>.
     */
    public void println(Object x) throws IOException {
        print(x);
        println();
    }

}
