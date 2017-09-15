/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.ws.ffdc.FFDCConfigurator;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;

/**
 * An IncidentStreamImpl is a lightweight implementation of an Incident stream
 * 
 */
public final class IncidentStreamImpl implements IncidentStream, Closeable {
    //private static final TraceComponent tc = Tr.register(IncidentStreamImpl.class, LoggingConstants.GROUP, LoggingConstants.FFDC_NLS);

    private static final int DEFAULT_DEPTH = 1;
    private static final int DEFAULT_MAX_SIZE = 1024 * 1024;

    private static final String EQUALS = " = ";

    protected final PrintStream ffdcLog;
    protected final File ffdcFile;

    /**
     * Construct a new IncidentStreamImpl.
     * 
     * @param ffdcFileName
     *            The name of the FFDC file to be used
     */
    @FFDCIgnore(PrivilegedActionException.class)
    public IncidentStreamImpl(FileLogSet fileLogSet) throws IOException {
        final TextFileOutputStreamFactory factory = FFDCConfigurator.getFileOutputStreamFactory();

        // Creating log files must occur using the server's identity. This is a no-op in most cases.
        Object token = ThreadIdentityManager.runAsServer();
        try {
            // an incident will only have 1 ffdc file associated at any time
            synchronized (fileLogSet) {
                this.ffdcFile = LoggingFileUtils.createNewFile(fileLogSet);
            }

            // This method will throw if the ffdcLog can't be created
            if (System.getSecurityManager() == null)
                this.ffdcLog = new PrintStream(factory.createOutputStream(ffdcFile));
            else
                try {
                    this.ffdcLog = AccessController.doPrivileged(new PrivilegedExceptionAction<PrintStream>() {
                        @Override
                        public PrintStream run() throws IOException {
                            return new PrintStream(factory.createOutputStream(ffdcFile));
                        }
                    });
                } catch (PrivilegedActionException e) {
                    if (e.getCause() instanceof IOException)
                        throw (IOException) e.getCause();
                    else
                        throw new RuntimeException(e);
                }
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Construct a new IncidentStreamImpl, prints to a OutputStream, instead of using a file.
     * 
     * @param ffdcLogStream
     *            The output stream to which logs will be printed
     */
    public IncidentStreamImpl(OutputStream ffdcLogStream) {
        // not associated to a ffdc file.
        this.ffdcFile = null;

        this.ffdcLog = new PrintStream(ffdcLogStream);
    }

    File getFile() {
        return ffdcFile;
    }

    void printStackTrace(Throwable t) {
        t.printStackTrace(ffdcLog);
    }

    @Override
    public void close() {
        ffdcLog.close();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWrite(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    @Override
    public void introspectAndWrite(String text, Object value) {
        ffdcLog.println(text);
        introspect(value, DEFAULT_DEPTH, DEFAULT_MAX_SIZE);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWrite(java.lang.String, java.lang.Object, int)
     * @param text
     * @param value
     * @param depth
     */
    @Override
    public void introspectAndWrite(String text, Object value, int depth) {
        ffdcLog.println(text);
        introspect(value, depth, DEFAULT_MAX_SIZE);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWrite(java.lang.String, java.lang.Object, int, int)
     * @param text
     * @param value
     * @param depth
     * @param maxBytes
     */
    @Override
    public void introspectAndWrite(String text, Object value, int depth, int maxBytes) {
        ffdcLog.println(text);
        introspect(value, depth, maxBytes);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWriteLine(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    @Override
    public void introspectAndWriteLine(String text, Object value) {
        introspectAndWrite(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWriteLine(java.lang.String, java.lang.Object, int)
     * @param text
     * @param value
     * @param depth
     */
    @Override
    public void introspectAndWriteLine(String text, Object value, int depth) {
        introspectAndWrite(text, value, depth);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#introspectAndWriteLine(java.lang.String, java.lang.Object, int, int)
     * @param text
     * @param value
     * @param depth
     * @param maxBytes
     */
    @Override
    public void introspectAndWriteLine(String text, Object value, int depth, int maxBytes) {
        introspectAndWrite(text, value, depth, maxBytes);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, boolean)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, boolean value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, byte)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, byte value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, char)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, char value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, short)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, short value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, int)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, int value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, long)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, long value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, float)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, float value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, double)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, double value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, java.lang.String)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, String value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#write(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    @Override
    public void write(String text, Object value) {
        printValueIntro(text);
        ffdcLog.print(value);
    }

    /**
     * Print the introductory text for a value
     * 
     * @param text
     *            The introductory text (null if there is to be no intro)
     */
    private void printValueIntro(String text) {
        if (text != null) {
            ffdcLog.print(text);
            ffdcLog.print(EQUALS);
        }
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, boolean)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, boolean value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, byte)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, byte value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, char)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, char value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, short)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, short value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, int)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, int value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, long)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, long value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, float)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, float value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, double)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, double value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, java.lang.String)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, String value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @see com.ibm.ws.ffdc.IncidentStream#writeLine(java.lang.String, java.lang.Object)
     * @param text
     * @param value
     */
    @Override
    public void writeLine(String text, Object value) {
        write(text, value);
        ffdcLog.println();
    }

    /**
     * @param value
     *            The object to be introspected
     * @param max_depth
     *            The maximum depth to introspect
     * @param max_size
     *            The maximum amount of data to be produced
     */
    private void introspect(Object value, int max_depth, int max_size) {
        if (value == null) {
            // Easy case!
            ffdcLog.print("null");
        } else {
            IntrospectionLevel rootLevel = new IntrospectionLevel(value);

            IntrospectionLevel currentLevel = rootLevel;
            IntrospectionLevel nextLevel = rootLevel.getNextLevel();
            int totalBytes = currentLevel.getNumberOfBytesinJustThisLevel();
            int actualDepth = 0;
            while (actualDepth < max_depth && nextLevel.hasMembers() && totalBytes <= max_size) {
                totalBytes -= currentLevel.getNumberOfBytesinJustThisLevel();
                totalBytes += currentLevel.getNumberOfBytesInAllLevelsIncludingThisOne();
                currentLevel = nextLevel;
                nextLevel = nextLevel.getNextLevel();
                totalBytes += currentLevel.getNumberOfBytesinJustThisLevel();
                actualDepth++;
            }
            boolean exceededMaxBytes = false;
            if (totalBytes > max_size && actualDepth > 0) {
                actualDepth--;
                exceededMaxBytes = true;
            }
            rootLevel.print(this, actualDepth);
            if (exceededMaxBytes == true) {
                ffdcLog.println("Only " + actualDepth
                                + " levels of object introspection were performed because performing the next level would have exceeded the specified maximum bytes of " + max_size);
            }
        }
    }
}
// End of file

