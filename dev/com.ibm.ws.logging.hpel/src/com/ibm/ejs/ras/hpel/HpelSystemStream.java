/*******************************************************************************
 * Copyright (c) 1998, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras.hpel;

import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.ws.logging.hpel.handlers.LogRecordHandler;
import com.ibm.ws.logging.hpel.handlers.LogRecordTextHandler;
import com.ibm.ws.logging.hpel.handlers.LogRepositoryConfiguration;

/**
 * The SystemStream is an abstract subclass of PrintStream. Concrete subclasses
 * of this class are used to replace one of the Java-defined PrintStream-based
 * streams for the process (accessed as System.err or System.out). The purpose
 * of this class is to allow the provision of additional function, such as the
 * following:
 * <ul>
 * <li>Allows the user the option to specify whether or not to actually perform
 * or ignore all writes directed to this stream.</li>
 * <li>Provide the option to format all events written to this stream with
 * WebSphere defined formatting, such as timestamp and thread id.</li>
 * <li>Forward events written to this stream on to WebSphere ras so that the
 * events may be written the trace destination. This allows the gathering of all
 * information from disparate sources (trace, messages and System.out.println)
 * to be gethered into a single comprehensive view.</li>
 * </ul>
 * The preferred implementation of this class would allow the intercepting of
 * all methods defined by PrintStream, providing our value-add, then simply
 * redispatching to the default implementation (e.g. call super). However, the
 * recursive nature of the java.io.PrintStream implementation does not allow
 * this, in practice. To get around this implementation difficulty, a level of
 * indirection is necessary. Although this class extends PrintStream, in reality
 * the stream that this class directly implements is never used. Instead, the
 * method implementations intercept and redispatch the method calls to the real
 * stream is passed on the constructor and method invocations on this object
 * provide the additional function, then redispatch to the real stream.
 * <p>
 * WARNING: Since objects that are concrete subclasses of this class
 * (SystemOutStream and SystemErrStream) are publicly accessible objects, user
 * code may do things such as synchronizing on these objects before methods are
 * called. If calls are made to other ras objects, or if other ras objects call
 * objects of this class, there is the possibility of deadlocks, if locks are
 * held. Therefore other ras classes that call this class should NEVER hold any
 * locks while they do. (in general)
 */
public class HpelSystemStream extends PrintStream {
    private static final String TEXTLOGGER_HANDLER_NAME = LogRecordTextHandler.class.getName();
    private static final String BINLOGGER_HANDLER_NAME = LogRecordHandler.class.getName();
    private static final LogRepositoryConfiguration logRepositoryConfiguration = LogRepositoryConfiguration.getLogRepositoryConfiguration() ;
    /**
     * Constants
     */
    private static final String svTrue = "true";
    private static final String svFalse = "false";
    private static final String svLineSeparator = HpelHelper.getSystemProperty("line.separator");
    private static final byte[] svLineSeparatorBytes = svLineSeparator.getBytes();
    private static final char[] svLineSeparatorChars = svLineSeparator.toCharArray();

    /**
     * A boolean that indicates whether or not writes to this stream should be
     * suppressed.
     */
    private boolean ivSuppress;

    private StringBuilder buffer = new StringBuilder();
    private int ivCacheSize = 0;
    private static final int svMaxCacheSize = 8192;

    private Logger logger;
    private static LogRecordHandler binaryHandler;
    private static LogRecordTextHandler textHandler;

    /**
     * Constructor
     * <p>
     * 
     * @param dummy
     *            the <code>OutputStream</code> object that is passed to the
     *            PrintStream constructor. Must be non-null.
     * @param stream
     *            the <code>PrintStream</code> to which the actual writes will
     *            be forwarded, once all the value-add function has been
     *            applied. Must be non-null.
     * @param suppress
     *            a boolean that indicates whether or not all writes to this
     *            stream should be suppressed.
     * @param formatted
     *            a boolean that indicates whether or not data written to this
     *            stream should be formatted like a RasEvent.
     * @param formatType
     *            one of the enumerated types in ManagerAdmin (e.g. BASIC or
     *            ADVANCED) which indicates the type of WebSphere log formatting
     *            in effect for this stream.
     */
    HpelSystemStream(String loggerName) {
        super(new NullOutputStream());
        logger = Logger.getLogger(loggerName);
    }

    /**
     * Forward the StreamEvent on to Tr, indicating tha it came from System.out
     * <p>
     * 
     * @param event
     *            the StreamEvent to forward to Tr.
     */
    public synchronized void dispatchEvent(LogRecord event) {
        if (binaryHandler == null || 
        	((textHandler == null && logRepositoryConfiguration.isTextEnabled()) || 
        	(textHandler != null && !logRepositoryConfiguration.isTextEnabled()))) {
            determineHandlers();
        }
        if (binaryHandler == null)	{ 	// Either doing LogManager Reset, or major issue occurring 666241.1
        	return ;
        } else {
            binaryHandler.publish(event);
        }
        if (textHandler != null) {
            textHandler.publish(event);
        }
    }

    /**
     * null out the handlers.  Something external occurred, and they should be reDetermined on next call
     */
    public synchronized static void reset() {
    	binaryHandler = null ;
    	textHandler = null ;
    }
    
    /**
     * The flush method required by the OutputStream class. Delegate to the real
     * stream.
     */
    public void flush() {
        // ivStream.flush();
    }

    /**
     * The close method required by the OutputStream class. We do not allow
     * callers to close this stream.
     */
    public void close() {
        // ivStream.close();
    }

    /**
     * Flush the stream and return its error state. The error state is set to
     * <code>true</code> when the underlying output stream throws an
     * <code>IOException</code> other than <code>InterruptedIOException</code>,
     * and when the <code>setError</code> method is invoked. If an operation on
     * the underlying output stream throws an
     * <code>InterruptedIOException</code>, then the <code>PrintStream</code>
     * converts the exception back into an interrupt by doing:
     * 
     * <pre>
     * Thread.currentThread().interrupt();
     * </pre>
     * 
     * or the equivalent.
     * <p>
     * 
     * @return True if and only if this stream has encountered an
     *         <code>IOException</code> other than
     *         code>InterruptedIOException</code>, or the <code>setError</code>
     *         method has been invoked
     */
    public boolean checkError() {
        // return ivStream.checkError();
        return false;
    }

    /**
     * Write the byte consisting of the 8 least significant digits of the
     * specified int to the stream. If the byte is a newline and automatic
     * flushing is enabled then the <code>flush</code> method will be invoked.
     * <p>
     * Note that the byte is written as given; to write a character that will be
     * translated according to the platform's default character encoding, use
     * the <code>print(char)</code> or <code>println(char)</code> methods.
     * <p>
     * 
     * @param b
     *            The byte to be written
     */
    public void write(int b) {
        if (ivSuppress == true) {
            return;
        }

        byte[] data = new byte[1];
        data[0] = (byte) b;
        // cache data now.
        {
            LogRecord se = cacheTraceData(data);
            if (se != null) {
                dispatchEvent(se);
            }
        }

        // // Write the data to the PrintStream this stream is wrapping.
        // if (ivFormatted == false)
        // ivStream.write(b);
        // else {
        // // Write data in formatted form. If a write is pending a header has
        // // already been written
        // // Otherwise new up an event and write it, which also writes the
        // // header.
        // synchronized (this) {
        // if (ivWritePending) {
        // ivStream.write(b);
        // } else {
        // LogRecord sse = createEvent(data);
        // sse.writeSelfToStream(ivStream, ivFormatType, false, ivBuffer,
        // ivFormatter, ivDate, ivFieldPos);
        // ivWritePending = true;
        // }
        // }
        // }
    }

    /**
     * Write <code>len</code> bytes from the specified byte array starting at
     * offset <code>off</code> to this stream. If automatic flushing is enabled
     * then the <code>flush</code> method will be invoked.
     * <p>
     * Note that the bytes will be written as given; to write characters that
     * will be translated according to the platform's default character
     * encoding, use the <code>print(char)</code> or <code>println(char)</code>
     * methods.
     * <p>
     * 
     * @param buffer
     *            A byte array
     * @param offset
     *            Offset from which to start taking bytes
     * @param len
     *            Number of bytes to write
     * @exception NullPointerException
     *                is thrown if byte array is null.
     * @exception IndexOutOfBoundsException
     *                is thrown if offset or size is invalid, or some legal
     *                comination of offset and size exceeds the size of the byte
     *                array
     */
    public void write(byte[] buffer, int offset, int len) {
        if (ivSuppress == true)
            return;
        // provide same checking as PrintStream does, however do it in a
        // friendlier way. Avoid
        // exceptions if at all possible
        if (len == 0)
            return;
        if (buffer == null)
            throw new NullPointerException();
        if ((offset < 0) || (len < 0))
            throw new IndexOutOfBoundsException();
        int size = buffer.length;
        if ((offset > size) || (len > size) || ((offset + len) > size))
            throw new IndexOutOfBoundsException();
        if ((offset + len) < 0)
            throw new IndexOutOfBoundsException();
        // all checks passed, continue
        byte[] data = new byte[len];
        System.arraycopy(buffer, offset, data, 0, len);

        // cache data now.
        {
            LogRecord se = cacheTraceData(data);
            if (se != null) {
                dispatchEvent(se);
            }
        }

        // Write the data to the PrintStream this stream is wrapping. Doing this
        // write can cause
        // an IOException to occur. In the real PrintStream, this exception is
        // caught, suppressed
        // and the error bit set. Since we delegate to the real stream, we can't
        // actually get an
        // exception back, but the compiler doesn't realize that....so we need a
        // try/catch/eat block
        // try {
        // if (ivFormatted == false)
        // ivStream.write(data);
        // else {
        // synchronized (this) {
        // // Write data in formatted form. If a write is pending a
        // // header has already been written.
        // // Otherwise new up an event and write it, which also writes
        // // the header.
        // if (ivWritePending) {
        // ivStream.write(data);
        // } else {
        // LogRecord sse = createEvent(data);
        // sse.writeSelfToStream(ivStream, ivFormatType, false, ivBuffer,
        // ivFormatter, ivDate, ivFieldPos);
        // // ivWritePending = true; //D205075
        // }
        //
        // // terminate the event if last character is a line separator
        // if (endsWithLineSeparator(data)) // D205075 begin
        // ivWritePending = false;
        // else
        // ivWritePending = true; // D205075 end
        // }
        // }
        // } catch (java.io.IOException ioe) {
        // }
    }

    private static boolean endsWithLineSeparator(String s) {
        if (s == null)
            return false;

        return s.endsWith(svLineSeparator);
    }

    private static boolean endsWithLineSeparator(byte[] b) {
        if (b == null)
            return false;

        if (b.length < svLineSeparatorBytes.length)
            return false;

        for (int i = 1; i <= svLineSeparatorBytes.length; i++) {
            if (b[b.length - i] != svLineSeparatorBytes[svLineSeparatorBytes.length - i])
                return false;
        }

        return true;
    }

    private static boolean endsWithLineSeparator(char[] c) {
        if (c == null)
            return false;

        if (c.length < svLineSeparatorChars.length)
            return false;

        for (int i = 1; i <= svLineSeparatorBytes.length; i++) {
            if (c[c.length - i] != svLineSeparatorBytes[svLineSeparatorBytes.length - i])
                return false;
        }

        return true;
    }

    /**
     * Print a boolean value. The string produced by
     * <code>{@link java.lang.String#valueOf(boolean)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param x
     *            The <code>boolean</code> to be printed
     */
    public void print(boolean x) {
        if (ivSuppress == true)
            return;
        if (x == false)
            doPrint(svFalse);
        else
            doPrint(svTrue);
    }

    /**
     * Print a character. The character is translated into one or more bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     * <p>
     * 
     * @param x
     *            The <code>char</code> to be printed.
     */
    public void print(char x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrint(writeData);
    }

    /**
     * Print an integer. The string produced by
     * <code>{@link java.lang.String#valueOf(int)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param x
     *            The <code>int</code> to be printed.
     */
    public void print(int x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrint(writeData);
    }

    /**
     * Print a long. The string produced by
     * <code>{@link java.lang.String#valueOf(long)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param x
     *            The <code>long</code> to be printed.
     */
    public void print(long x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrint(writeData);
    }

    /**
     * Print a float. The string produced by
     * <code>{@link java.lang.String#valueOf(float)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param x
     *            The <code>float</code> to be printed
     */
    public void print(float x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrint(writeData);
    }

    /**
     * Print a double. The string produced by
     * <code>{@link java.lang.String#valueOf(double)}</code> is translated into
     * bytes according to the platform's default character encoding, and these
     * bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param x
     *            The <code>double</code> to be printed
     */
    public void print(double x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrint(writeData);
    }

    /**
     * Print an array of characters. The characters are converted into bytes
     * according to the platform's default character encoding, and these bytes
     * are written in exactly the manner of the <code>{@link #write(int)}</code>
     * method.
     * <p>
     * 
     * @param x
     *            The array of chars to be printed.
     * @throws NullPointerException
     *             If <code>s</code> is <code>null</code>
     */
    public void print(char[] x) {
        if (ivSuppress == true)
            return;

        // cache data now.
        {
            LogRecord se = cacheTraceData(x);
            if (se != null) {
                dispatchEvent(se);
            }
        }
        // Write the data to the PrintStream this stream is wrapping.
        // if (ivFormatted == false) {
        // ivStream.print(x);
        // } else {
        // // Write data in formatted form. If a write is pending a header has
        // // already been written.
        // // Otherwise new up an event and write it, which also writes the
        // // header.
        // synchronized (this) {
        // if (ivWritePending) {
        // ivStream.print(x);
        // } else {
        // LogRecord sse = createEvent(x);
        // sse.writeSelfToStream(ivStream, ivFormatType, false, ivBuffer,
        // ivFormatter, ivDate, ivFieldPos);
        // // ivWritePending = true; //D205075
        // }
        // // terminate the event if last character is a line separator
        // if (endsWithLineSeparator(x)) // D205075 begin
        // ivWritePending = false;
        // else
        // ivWritePending = true; // D205075 end
        // }
        // }
    }

    /**
     * Print a string. If the argument is <code>null</code> then the string
     * <code>"null"</code> is printed. Otherwise, the string's characters are
     * converted into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param x
     *            The <code>String</code> to be printed.
     */
    public void print(String x) {
        if (ivSuppress == true)
            return;
        doPrint(x);
    }

    /**
     * Print an object. The string produced by the
     * <code>{@link java.lang.String#valueOf(Object)}</code> method is
     * translated into bytes according to the platform's default character
     * encoding, and these bytes are written in exactly the manner of the
     * <code>{@link #write(int)}</code> method.
     * <p>
     * 
     * @param obj
     *            The <code>Object</code> to be printed.
     */
    public void print(Object obj) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(obj);
        doPrint(writeData);
    }

    /**
     * Terminate the current line by writing the line separator string. The line
     * separator string is defined by the system property
     * <code>line.separator</code>, and is not necessarily a single newline
     * character (<code>'\n'</code>).
     */
    public void println() {
        if (ivSuppress == true)
            return;
        String writeData = null;
        LogRecord sse = null;

        // Add data to cache, then forward on to logging subsystem. Must not
        // hold the object synchronizer on the dispatch.
        {
            sse = getTraceData(writeData);
            dispatchEvent(sse);
        }

        // // Write the data to the PrintStream this stream is wrapping.
        // if (ivFormatted == false)
        // ivStream.println();
        // else {
        // // Write data in formatted form. If a write is pending a header has
        // // already been written.
        // // Otherwise new up an event and write it, which also writes the
        // // header.
        // synchronized (this) {
        // if (ivWritePending)
        // ivStream.println();
        // else {
        // sse = createEvent(writeData);
        // sse.writeSelfToStream(ivStream, ivFormatType, true, ivBuffer,
        // ivFormatter, ivDate, ivFieldPos);
        // }
        // ivWritePending = false;
        // }
        // }
    }

    /**
     * Print a boolean and then terminate the line. This method behaves as
     * though it invokes <code>{@link #print(boolean)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>boolean</code> to be printed.
     */
    public void println(boolean x) {
        if (ivSuppress == true)
            return;
        if (x == true)
            doPrintLine(svTrue);
        else
            doPrintLine(svFalse);
    }

    /**
     * Print a character and then terminate the line. This method behaves as
     * though it invokes <code>{@link #print(char)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>char</code> to be printed.
     */
    public void println(char x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrintLine(writeData);
    }

    /**
     * Print an integer and then terminate the line. This method behaves as
     * though it invokes <code>{@link #print(int)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>int</code> to be printed.
     */
    public void println(int x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrintLine(writeData);
    }

    /**
     * Print a long and then terminate the line. This method behaves as though
     * it invokes <code>{@link #print(long)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>long</code> to be printed.
     */
    public void println(long x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrintLine(writeData);
    }

    /**
     * Print a float and then terminate the line. This method behaves as though
     * it invokes <code>{@link #print(float)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>float</code> to be printed.
     */
    public void println(float x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrintLine(writeData);
    }

    /**
     * Print a double and then terminate the line. This method behaves as though
     * it invokes <code>{@link #print(double)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>double</code> to be printed.
     */
    public void println(double x) {
        if (ivSuppress == true)
            return;
        String writeData = String.valueOf(x);
        doPrintLine(writeData);
    }

    /**
     * Print an array of characters and then terminate the line. This method
     * behaves as though it invokes <code>{@link #print(char[])}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            an array of chars to print.
     */
    public void println(char[] x) {
        if (ivSuppress == true)
            return;
        LogRecord sse = null;

        // Add data to cache, then forward on to logging subsystem. Must not
        // hold the object synchronizer on the dispatch.
        {
            sse = getTraceData(x);
            dispatchEvent(sse);
        }

        // // Write the data to the PrintStream this stream is wrapping.
        // if (ivFormatted == false)
        // ivStream.println(x);
        // else {
        // // Write data in formatted form. If a write is pending a header has
        // // already been written.
        // // Otherwise new up an event and write it, which also writes the
        // // header.
        // synchronized (this) {
        // if (ivWritePending)
        // ivStream.println(x);
        // else {
        // sse = createEvent(x);
        // sse.writeSelfToStream(ivStream, ivFormatType, true, ivBuffer,
        // ivFormatter, ivDate, ivFieldPos);
        // }
        // ivWritePending = false;
        // }
        // }
    }

    /**
     * Print a String and then terminate the line. This method behaves as though
     * it invokes <code>{@link #print(String)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>String</code> to be printed.
     */
    public void println(String x) {
        if (ivSuppress == true)
            return;
        doPrintLine(x);
    }

    /**
     * Print an Object and then terminate the line. This method behaves as
     * though it invokes <code>{@link #print(Object)}</code> and then
     * <code>{@link #println()}</code>.
     * <p>
     * 
     * @param x
     *            The <code>Object</code> to be printed.
     */
    public void println(Object x) {
        if (ivSuppress == true)
            return;
        String writeData = null;
        if (x instanceof java.lang.Throwable) { // D230373
            writeData = HpelHelper.throwableToString((java.lang.Throwable) x);
        } else {
            writeData = String.valueOf(x);
        }
        doPrintLine(writeData);
    }

    /**
     * A worker method for print() methods that have data in the form of a
     * String
     */
    private final void doPrint(String data) {
        // cache data now.
        LogRecord se = cacheTraceData(data);
        if (se != null) {
            dispatchEvent(se);
        }

    }

    /**
     * A worker method for println() methods that have data in form of a String
     */
    private final void doPrintLine(String data) {
        // Add data to cache, then forward on to logging subsystem. Must not
        // hold the object synchronizer on the dispatchEvent call.
        LogRecord sse = getTraceData(data);
        dispatchEvent(sse);

    }

    /**
     * Add the specified data to the trace cache.
     * <p>
     * This method is called when a write or print (non terminating) event is
     * encountered. This data will be cached, and will be forwarded to the trace
     * output as part of a larger event. Printlns (terminating) events are used
     * as the demarcation point where all cached data is written. In addition,
     * once the cache reaches a predetermined maximum size, it must be flushed.
     * <p>
     * The first operation that initiates a "sequence" will create the initial
     * event that captures the timestamp, etc. As more nonterminating events are
     * encountered, the data is simply added to a cache. A terminating event
     * will orphan the event and cache, and return the event so it can be
     * forwarded.
     * <p>
     * 
     * @param data
     *            a String to cache.
     */
    private synchronized LogRecord cacheTraceData(String data) {
        // If the data size, plus the size of any data already cached is too
        // large the event must
        // be fired. Return the event to the caller.
        // Assume a data size of 2 x "null".length() for null data
        int size = (data == null) ? 8 : data.length() * 2;
        if (ivCacheSize + size > svMaxCacheSize) {
            return getTraceData(data);
        }

        if (endsWithLineSeparator(data)) {
            return getTraceData(data);
        }

        // Data can be cached. If "root" event does not exist, this should be
        // the first operation
        // in the sequence. Create the event. If event already exists, add data
        // to the cache.
        buffer.append(data);
        ivCacheSize += size;
        return null;
    }

    /**
     * Add the specified data to the trace cache.
     * <p>
     * This method is called when a write or print (non terminating) event is
     * encountered. This data will be cached, and will be forwarded to the trace
     * output as part of a larger event. Printlns (terminating) events are used
     * as the demarcation point where all cached data is written. In addition,
     * once the cache reaches a predetermined maximum size, it must be flushed.
     * <p>
     * The first operation that initiates a "sequence" will create the initial
     * event that captures the timestamp, etc. As more nonterminating events are
     * encountered, the data is simply added to a cache. A terminating event
     * will orphan the event and cache, and return the event so it can be
     * forwarded.
     * <p>
     * 
     * @param data
     *            a byte[] to cache.
     */
    private synchronized LogRecord cacheTraceData(byte[] data) {
        // If the data size, plus the size of any data already cached is too
        // large the event must
        // be fired. Return the event to the caller.
        int size = (data == null) ? 0 : data.length;
        if (ivCacheSize + size > svMaxCacheSize) {
            return getTraceData(data);
        }

        if (endsWithLineSeparator(data)) {
            return getTraceData(data);
        }

        // Data can be cached.If "root" event does not exist, this should be the
        // first operation
        // in the sequence. Create the event. If event already exists, add data
        // to the cache.
        buffer.append(data);
        ivCacheSize += size;
        return null;
    }

    /**
     * Add the specified data to the trace cache.
     * <p>
     * This method is called when a write or print (non terminating) event is
     * encountered. This data will be cached, and will be forwarded to the trace
     * output as part of a larger event. Printlns (terminating) events are used
     * as the demarcation point where all cached data is written. In addition,
     * once the cache reaches a predetermined maximum size, it must be flushed.
     * <p>
     * The first operation that initiates a "sequence" will create the initial
     * event that captures the timestamp, etc. As more nonterminating events are
     * encountered, the data is simply added to a cache. A terminating event
     * will orphan the event and cache, and return the event so it can be
     * forwarded.
     * <p>
     * 
     * @param data
     *            a char[] to cache.
     */
    private synchronized LogRecord cacheTraceData(char[] data) {
        // If the data size, plus the size of any data already cached is too
        // large the event must
        // be fired. Return the event to the caller.
        int size = (data == null) ? 0 : data.length;
        if (ivCacheSize + size > svMaxCacheSize) {
            return getTraceData(data);
        }

        if (endsWithLineSeparator(data)) {
            return getTraceData(data);
        }

        // If "root" event does not exist, this should be the first operation in
        // the sequence.
        // Create the event. If event already exists, add data to the cache.
        buffer.append(data);
        ivCacheSize += size;
        return null;
    }

    /**
     * Add the specified data to the trace cache, then orphan the event and
     * cache, returning the event so it can be forwareded to trace.
     * <p>
     * This method is called when a println (terminating) event is encountered.
     * An event will be created if one does not exist, else the data is added to
     * the cache for an existing event. The entire event is then orphaned and
     * returned to the caller so it can be written to the trace output device.
     * <p>
     * The first operation that initiates a "sequence" will create the initial
     * event that captures the timestamp, etc. As more nonterminating events are
     * encountered, the data is simply added to a cache. A terminating event
     * will orphan the event and cache, and return the event so it can be
     * forwarded.
     * <p>
     * 
     * @param data
     *            a String to cache.
     */
    private synchronized LogRecord getTraceData(String data) {
        buffer.append(data);
        LogRecord se = new LogRecord(Level.parse("625"), buffer.toString());
        /*
         * set source name and method to be empty string
         */
        se.setSourceClassName("");
        se.setSourceMethodName("");

        se.setLoggerName(logger.getName());

        /*
         * reset everythign else
         */
        buffer = new StringBuilder();

        ivCacheSize = 0;
        return se;
    }

    /**
     * Add the specified data to the trace cache, then orphan the event and
     * cache, returning the event so it can be forwareded to trace.
     * <p>
     * This method is called when a println (terminating) event is encountered.
     * An event will be created if one does not exist, else the data is added to
     * the cache for an existing event. The entire event is then orphaned and
     * returned to the caller so it can be written to the trace output device.
     * <p>
     * The first operation that initiates a "sequence" will create the initial
     * event that captures the timestamp, etc. As more nonterminating events are
     * encountered, the data is simply added to a cache. A terminating event
     * will orphan the event and cache, and return the event so it can be
     * forwarded.
     * <p>
     * 
     * @param data
     *            a byte[] to cache.
     */
    private synchronized LogRecord getTraceData(byte[] data) {
        buffer.append(new String(data));
        LogRecord se = new LogRecord(Level.parse("625"), buffer.toString());
        se.setLoggerName(logger.getName());
        buffer = new StringBuilder();
        ivCacheSize = 0;
        return se;
    }

    /**
     * Add the specified data to the trace cache, then orphan the event and
     * cache, returning the event so it can be forwareded to trace.
     * <p>
     * This method is called when a println (terminating) event is encountered.
     * An event will be created if one does not exist, else the data is added to
     * the cache for an existing event. The entire event is then orphaned and
     * returned to the caller so it can be written to the trace output device.
     * <p>
     * The first operation that initiates a "sequence" will create the initial
     * event that captures the timestamp, etc. As more nonterminating events are
     * encountered, the data is simply added to a cache. A terminating event
     * will orphan the event and cache, and return the event so it can be
     * forwarded.
     * <p>
     * 
     * @param data
     *            a char[] to cache.
     */
    private synchronized LogRecord getTraceData(char[] data) {
        buffer.append(data);
        LogRecord se = new LogRecord(Level.parse("625"), buffer.toString());
        se.setLoggerName(logger.getName());
        buffer = new StringBuilder();
        ivCacheSize = 0;
        return se;
    }

    /**
     * This method redirects the System.* stream to the repository.
     * It will print header if a implementation of logHeader is provided
     */
    public static void redirectStreams() {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    HpelSystemStream newErr = new HpelSystemStream("SystemErr");
                    System.setErr(newErr);
                    HpelSystemStream newOut = new HpelSystemStream("SystemOut");
                    System.setOut(newOut);
                    return null;
                }
            });
        } catch (Throwable t) {
            throw new RuntimeException("Unable to replace System streams", t);
        }

        reset() ;
    }

    /**
     * determine two handlers needed by the repository
     */
    private static void determineHandlers() {
        /*
         * find the handlers that we are dispatching to
         */
    	if (textHandler != null && !logRepositoryConfiguration.isTextEnabled())
    		textHandler = null ;
    			// Don't do this work if only text and text is not enabled (if so, it will always be null)  666241.1
    	if (binaryHandler != null && !logRepositoryConfiguration.isTextEnabled())
    		return ;
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            String name = handler.getClass().getName();
            if (BINLOGGER_HANDLER_NAME.equals(name)) {
                binaryHandler = (LogRecordHandler) handler;
            } else if (TEXTLOGGER_HANDLER_NAME.equals(name)) {
                textHandler = (LogRecordTextHandler) handler;
            }
        }
    }
}
