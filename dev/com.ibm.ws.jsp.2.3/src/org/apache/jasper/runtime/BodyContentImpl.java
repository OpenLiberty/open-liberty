/*
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
//Defect PK04091 "PROVIDE A CONFIGURABLE PROPERTY FOR DETERMINING BUFFER SIZE."  2006/02/20 Scott Johnson
//PK33136 replace ch[] buffer with a StringBuffer to avoid reallocations
//PK95332 Memory usage management (similar to org.apache.jasper.runtime.BodyContentImpl.LIMIT_BUFFER)  09/04/09  pmdinh
//PM12137 NPE from clear() after stream is closed   05/18/10  pmdinh
//PI24001 Non-reusable objects of type BodyContentImpl cause a memory leak when using custom tags in a JSP  11/11/2014  hmpadill

package org.apache.jasper.runtime;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

import com.ibm.wsspi.webcontainer.WCCustomProperties;
/**
 * Write text to a character-output stream, buffering characters so as
 * to provide for the efficient writing of single characters, arrays,
 * and strings.
 *
 * Provide support for discarding for the output that has been buffered.
 *
 * @author Rajiv Mordani
 * @author Jan Luehe
 */
public class BodyContentImpl extends BodyContent {
    static protected Logger logger;
    private static final String CLASS_NAME="org.apache.jasper.runtime.BodyContentImpl";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    public static final int DEFAULT_TAG_BUFFER_SIZE = 512;

    private static final String LINE_SEPARATOR = (String)AccessController.doPrivileged(new PrivilegedAction() {
                                                                                           public Object run() {
                                                                                                return System.getProperty("line.separator");
                                                                                           }
                                                                                       });

    protected int bodyContentBuffSize = DEFAULT_TAG_BUFFER_SIZE; //PK04091 - new variable
    private StringBuffer strBuffer; // PK33136
    private int nextChar;
    private boolean closed;

    // Enclosed writer to which any output is written
    private Writer writer;

    // See comment in setWriter()
    private int bufferSizeSave;

    private static boolean limitBuffer = WCCustomProperties.LIMIT_BUFFER;	//PK95332
    /**
     * Constructor.
     */
    public BodyContentImpl(JspWriter enclosingWriter) {
        this(enclosingWriter, DEFAULT_TAG_BUFFER_SIZE);
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME, "BodyContentImpl", "BodyContentImpl ctor 1 buffsize=["+DEFAULT_TAG_BUFFER_SIZE+"]  this=["+this+"]");
        }
    }

    /**
     * Constructor.
     */
    public BodyContentImpl(JspWriter enclosingWriter, int bodyContentBuffSize) {
        super(enclosingWriter);
        this.bodyContentBuffSize=bodyContentBuffSize; //PK04091
        this.bufferSize = bodyContentBuffSize;
        this.bufferSizeSave = this.bufferSize;
        strBuffer = new StringBuffer(bufferSize); // PK33136
        nextChar = 0;
        closed = false;
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
            logger.logp(Level.FINE, CLASS_NAME, "BodyContentImpl", "BodyContentImpl ctor 2 buffsize=["+this.bodyContentBuffSize+"]  this=["+this+"]");
        }
    }

    /**
     * Write a single character.
     */
    public void write(int c) throws IOException {
	    if (writer != null) {
	        writer.write(c);
	    } else {
	        ensureOpen();
	        //PK33136
	        strBuffer.append((char)c);
	        ++nextChar;
			//PK33136
	    }
    }

    /**
     * Write a portion of an array of characters.
     *
     * <p> Ordinarily this method stores characters from the given array into
     * this stream's buffer, flushing the buffer to the underlying stream as
     * needed.  If the requested length is at least as large as the buffer,
     * however, then this method will flush the buffer and write the characters
     * directly to the underlying stream.  Thus redundant
     * <code>DiscardableBufferedWriter</code>s will not copy data
     * unnecessarily.
     *
     * @param cbuf A character array
     * @param off Offset from which to start reading characters
     * @param len Number of characters to write
     */
    public void write(char[] cbuf, int off, int len) throws IOException {
	    if (writer != null) {
	        writer.write(cbuf, off, len);
	    } else {
	        ensureOpen();
	
	        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
	        ((off + len) > cbuf.length) || ((off + len) < 0)) {
	        	throw new IndexOutOfBoundsException();
	        } else if (len == 0) {
	        	return;
	        }
			//PK33136
	        strBuffer.append(cbuf, off, len);
	        nextChar+=len;
	        //PK33136
	    }
    }

    /**
     * Write an array of characters.  This method cannot be inherited from the
     * Writer class because it must suppress I/O exceptions.
     */
    public void write(char[] buf) throws IOException {
	    if (writer != null) {
	        writer.write(buf);
	    } else {
	        write(buf, 0, buf.length);
	    }
    }

    /**
     * Write a portion of a String.
     *
     * @param s String to be written
     * @param off Offset from which to start reading characters
     * @param len Number of characters to be written
     */
    public void write(String s, int off, int len) throws IOException {
	    if (writer != null) {
	        writer.write(s, off, len);
	    } else {
	        ensureOpen();
	        //PK33136
	        strBuffer.append(s.substring(off, off+len));
	        nextChar += len;
	        //PK33136
	    }
    }

    /**
     * Write a string.  This method cannot be inherited from the Writer class
     * because it must suppress I/O exceptions.
     */
    public void write(String s) throws IOException {
	    if (writer != null) {
	        writer.write(s);
	    } else {
	        write(s, 0, s.length());
	    }
    }

    /**
     * Write a line separator.  The line separator string is defined by the
     * system property <tt>line.separator</tt>, and is not necessarily a single
     * newline ('\n') character.
     *
     * @throws IOException If an I/O error occurs
     */
    public void newLine() throws IOException {
	    if (writer != null) {
	        writer.write(LINE_SEPARATOR);
	    } else {
	        write(LINE_SEPARATOR);
	    }
    }

    /**
     * Print a boolean value.  The string produced by <code>{@link
     * java.lang.String#valueOf(boolean)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link
     * #write(int)}</code> method.
     *
     * @param b The <code>boolean</code> to be printed
     * @throws IOException
     */
    public void print(boolean b) throws IOException {
	    if (writer != null) {
	        writer.write(b ? "true" : "false");
	    } else {
	        write(b ? "true" : "false");
	    }
    }

    /**
     * Print a character.  The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link
     * #write(int)}</code> method.
     *
     * @param c The <code>char</code> to be printed
     * @throws IOException
     */
    public void print(char c) throws IOException {
	    if (writer != null) {
	        writer.write(String.valueOf(c));
	    } else {
	        write(String.valueOf(c));
	    }
    }

    /**
     * Print an integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(int)}</code> is translated into bytes according
     * to the platform's default character encoding, and these bytes are
     * written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     *
     * @param i The <code>int</code> to be printed
     * @throws IOException
     */
    public void print(int i) throws IOException {
	    if (writer != null) {
	        writer.write(String.valueOf(i));
	    } else {
	        write(String.valueOf(i));
	    }
    }

    /**
     * Print a long integer.  The string produced by <code>{@link
     * java.lang.String#valueOf(long)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param l The <code>long</code> to be printed
     * @throws IOException
     */
    public void print(long l) throws IOException {
	    if (writer != null) {
	        writer.write(String.valueOf(l));
	    } else {
	        write(String.valueOf(l));
	    }
    }

    /**
     * Print a floating-point number.  The string produced by <code>{@link
     * java.lang.String#valueOf(float)}</code> is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param f The <code>float</code> to be printed
     * @throws IOException
     */
    public void print(float f) throws IOException {
	    if (writer != null) {
	        writer.write(String.valueOf(f));
	    } else {
	        write(String.valueOf(f));
	    }
    }

    /**
     * Print a double-precision floating-point number.  The string produced by
     * <code>{@link java.lang.String#valueOf(double)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the <code>{@link
     * #write(int)}</code> method.
     *
     * @param d The <code>double</code> to be printed
     * @throws IOException
     */
    public void print(double d) throws IOException {
	    if (writer != null) {
	        writer.write(String.valueOf(d));
	    } else {
	        write(String.valueOf(d));
	    }
    }

    /**
     * Print an array of characters.  The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param s The array of chars to be printed
     *
     * @throws NullPointerException If <code>s</code> is <code>null</code>
     * @throws IOException
     */
    public void print(char[] s) throws IOException {
	    if (writer != null) {
	        writer.write(s);
	    } else {
	        write(s);
	    }
    }

    /**
     * Print a string.  If the argument is <code>null</code> then the string
     * <code>"null"</code> is printed.  Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param s The <code>String</code> to be printed
     * @throws IOException
     */
    public void print(String s) throws IOException {
	    if (s == null) s = "null";
	    if (writer != null) {
	        writer.write(s);
	    } else {
	        write(s);
	    }
    }

    /**
     * Print an object.  The string produced by the <code>{@link
     * java.lang.String#valueOf(Object)}</code> method is translated into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     *
     * @param obj The <code>Object</code> to be printed
     * @throws IOException
     */
    public void print(Object obj) throws IOException {
	    if (writer != null) {
	        writer.write(String.valueOf(obj));
	    } else {
	        write(String.valueOf(obj));
	    }
    }

    /**
     * Terminate the current line by writing the line separator string.  The
     * line separator string is defined by the system property
     * <code>line.separator</code>, and is not necessarily a single newline
     * character (<code>'\n'</code>).
     *
     * @throws IOException
     */
    public void println() throws IOException {
    	newLine();
    }

    /**
     * Print a boolean value and then terminate the line.  This method behaves
     * as though it invokes <code>{@link #print(boolean)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(boolean x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a character and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(char)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(char x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print an integer and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(int)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(int x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a long integer and then terminate the line.  This method behaves
     * as though it invokes <code>{@link #print(long)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(long x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a floating-point number and then terminate the line.  This method
     * behaves as though it invokes <code>{@link #print(float)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(float x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a double-precision floating-point number and then terminate the
     * line.  This method behaves as though it invokes <code>{@link
     * #print(double)}</code> and then <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(double x) throws IOException{
        print(x);
        println();
    }

    /**
     * Print an array of characters and then terminate the line.  This method
     * behaves as though it invokes <code>{@link #print(char[])}</code> and
     * then <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(char x[]) throws IOException {
        print(x);
        println();
    }

    /**
     * Print a String and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(String)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(String x) throws IOException {
        print(x);
        println();
    }

    /**
     * Print an Object and then terminate the line.  This method behaves as
     * though it invokes <code>{@link #print(Object)}</code> and then
     * <code>{@link #println()}</code>.
     *
     * @throws IOException
     */
    public void println(Object x) throws IOException {
        print(x);
        println();
    }

    /**
     * Clear the contents of the buffer. If the buffer has been already
     * been flushed then the clear operation shall throw an IOException
     * to signal the fact that some data has already been irrevocably
     * written to the client response stream.
     *
     * @throws IOException If an I/O error occurs
     */
    public void clear() throws IOException {
	    if (writer != null) {
	        throw new IOException();
	    } else {
	    	nextChar = 0;
	    	if (limitBuffer && (strBuffer.length() > this.bodyContentBuffSize)){         	//PK95332 - starts
	    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
	    			logger.logp(Level.FINE, CLASS_NAME, "BodyContentImpl", "clear buffer, create new one with buffer size ["+ this.bodyContentBuffSize +"]");
	    		}
	    		strBuffer = new StringBuffer(this.bodyContentBuffSize);
	    	} else if (strBuffer != null) { // PI24001															//PK95332 - ends
	    		strBuffer.setLength(0); // PK33136
	    	}																				//PK95332
	    }
    }

    /**
     * Clears the current contents of the buffer. Unlike clear(), this
     * mehtod will not throw an IOException if the buffer has already been
     * flushed. It merely clears the current content of the buffer and
     * returns.
     *
     * @throws IOException If an I/O error occurs
     */
    public void clearBuffer() throws IOException {
        if (writer == null) {
	        this.clear();
	    }
    }

    /**
     * Close the stream, flushing it first.  Once a stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown.  Closing a previously-closed stream, however, has no effect.
     *
     * @throws IOException If an I/O error occurs
     */
    public void close() throws IOException {
    	if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){   	// PM12137
    		logger.logp(Level.FINE, CLASS_NAME, "close", "this=["+this+"]");						// PM12137
    	}																							// PM12137

    	if (writer != null) {
    		writer.close();
    	} else {
    		//PK33136
    		closed = true;
    		strBuffer = null;
    		//PK33136
    	}
    }

    /**
     * @return the number of bytes unused in the buffer
     */
    public int getRemaining() {
        return (writer == null) ? strBuffer.capacity()-strBuffer.length() : 0; // PK33136
    }

    /**
     * Return the value of this BodyJspWriter as a Reader.
     * Note: this is after evaluation!!  There are no scriptlets,
     * etc in this stream.
     *
     * @return the value of this BodyJspWriter as a Reader
     */
    public Reader getReader() {
        // PK33136
        char[] charBuffer = new char[strBuffer.length()] ;
        strBuffer.getChars(0, strBuffer.length(), charBuffer, 0);
        //PK33136
        return (writer == null) ? new CharArrayReader (charBuffer, 0, strBuffer.length()) : null;
    }

    /**
     * Return the value of the BodyJspWriter as a String.
     * Note: this is after evaluation!!  There are no scriptlets,
     * etc in this stream.
     *
     * @return the value of the BodyJspWriter as a String
     */
    public String getString() {
        return (writer == null) ? strBuffer.toString() : null; // PK33136
    }

    /**
     * Write the contents of this BodyJspWriter into a Writer.
     * Subclasses are likely to do interesting things with the
     * implementation so some things are extra efficient.
     *
     * @param out The writer into which to place the contents of this body
     * evaluation
     */
    public void writeOut(Writer out) throws IOException {
	    if (writer == null) {
	        out.write(strBuffer.toString()); // PK33136
	        // Flush not called as the writer passed could be a BodyContent and
	        // it doesn't allow to flush.
	    }
    }

    public static void main (String[] args) throws Exception {
	    char[] buff = {'f','o','o','b','a','r','b','a','z','y'};
	    BodyContentImpl bodyContent
	        = new BodyContentImpl(new JspWriterImpl(null, 100, false));
	    bodyContent.println (buff);
	    System.out.println (bodyContent.getString ());
	    bodyContent.writeOut (new PrintWriter (System.out));
    }

    /**
     * Sets the writer to which all output is written.
     */
    void setWriter(Writer writer) {
    	// PM12137 - starts
    	if (closed) {
    		if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME, "setWriter", "resetting closed to false for this=["+this+"]");
            }
    		closed = false;
    		strBuffer = new StringBuffer(this.bodyContentBuffSize);
    	}
    	// PM12137 - ends
	    this.writer = writer;
	    if (writer != null) {
	        // According to the spec, the JspWriter returned by
	        // JspContext.pushBody(java.io.Writer writer) must behave as
	        // though it were unbuffered. This means that its getBufferSize()
	        // must always return 0. The implementation of
	        // JspWriter.getBufferSize() returns the value of JspWriter's
	        // 'bufferSize' field, which is inherited by this class.
	        // Therefore, we simply save the current 'bufferSize' (so we can
	        // later restore it should this BodyContentImpl ever be reused by
	        // a call to PageContext.pushBody()) before setting it to 0.
	        if (bufferSize != 0) {
	            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
	                logger.logp(Level.FINE, CLASS_NAME, "setWriter", "BodyContentImpl setWriter A. bufferSize=["+bufferSize+"]  this=["+this+"]");
	            }
	            bufferSizeSave = bufferSize;
	            bufferSize = 0;
	        }
	    } else {
	        bufferSize = bufferSizeSave;
	        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)){
	            logger.logp(Level.FINE, CLASS_NAME, "setWriter", "BodyContentImpl setWriter B. bufferSize=["+bufferSize+"]  this=["+this+"]");
	        }
	        clearBody();
	    }
    }

    private void ensureOpen() throws IOException {
    	if (closed) throw new IOException("Stream closed");
    }
    
    //PI24001 starts
    /**
     * This method shall "reset" the internal state of a BodyContentImpl,
     * releasing all internal references, and preparing it for potential
     * reuse by a later invocation of {@link PageContextImpl#pushBody(Writer)}.
     *
     * <p>Note, that BodyContentImpl instances are usually owned by a
     * PageContextImpl instance, and PageContextImpl instances are recycled
     * and reused.
     *
     * @see PageContextImpl#release()
     */
    protected void recycle() {
        this.writer = null;
        try {
            this.clear();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    //PI24001 ends
}
