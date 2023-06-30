/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.jmx.service.VirtualMachineHelper;

class HotSpotJavaDumperImpl extends JavaDumper {

    /**
     * The next sequence number to use for dump file names.
     */
    private static final AtomicInteger nextSequenceNumber = new AtomicInteger(1);

    /**
     * The platform MBean server.
     */
    private final MBeanServer platformMBeanServer;

    /**
     * Name for "com.sun.management:type=HotSpotDiagnostic".
     */
    private final ObjectName hotSpotDiagnosticName;

    /**
     * Name for "com.sun.management:type=DiagnosticCommand".
     */
    private final ObjectName diagnosticCommandName;

    HotSpotJavaDumperImpl(MBeanServer platformMBeanServer, ObjectName diagName, ObjectName diagCommandName) {
        this.platformMBeanServer = platformMBeanServer;
        this.hotSpotDiagnosticName = diagName;
        this.diagnosticCommandName = diagCommandName;
    }

    @Override
    public File dump(JavaDumpAction action, File outputDir) {
        switch (action) {
            case HEAP:
                return createHeapDump(outputDir);

            case THREAD:
                return createThreadDump(outputDir);

            default:
                return null;
        }
    }

    /**
     * Create a dump file with a unique name.
     *
     * @param outputDir the directory to contain the file
     * @param prefix    the prefix for the filename
     * @param extension the file extension, not including a leading "."
     * @return the created file
     */
    private File createNewFile(File outputDir, String prefix, String extension) throws IOException {
        String dateTime = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
        File outputFile;
        do {
            String PID = VirtualMachineHelper.getPID();
            String pid = PID == null ? "" : PID + '.';
            int sequenceNumber = nextSequenceNumber.getAndIncrement();
            outputFile = new File(outputDir, String.format("%s.%s.%s%04d.%s", prefix, dateTime, pid, sequenceNumber, extension));
        } while (outputFile.exists());

        return outputFile;
    }

    /**
     * Create a heap dump. This is the same as jmap -dump:file=...
     *
     * @param outputDir the server output directory
     * @return the resulting file
     */
    private File createHeapDump(File outputDir) {
        if (hotSpotDiagnosticName == null) {
            return null;
        }

        File outputFile;
        try {
            // The default dump name is "java.hprof".
            outputFile = createNewFile(outputDir, "java", "hprof");

            platformMBeanServer.invoke(hotSpotDiagnosticName,
                                       "dumpHeap",
                                       new Object[] { outputFile.getAbsolutePath(), false },
                                       new String[] { String.class.getName(), boolean.class.getName() });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return outputFile;
    }

    /**
     * Create a thread dump. This is the same output normally printed to the
     * console when a kill -QUIT or Ctrl-Break is sent to the process.
     *
     * @param outputDir the server output directory
     * @return the resulting file, or null if the dump could not be created
     */
    private File createThreadDump(File outputDir) {
        // Determine if we're using Java Attach (VirtualMachine.remoteDataDump)
        // or the DiagnosticCommand MBean.  Java Attach is only available on
        // JREs (not JDKs) and sometimes fails to connect, but we prefer it when
        // available because DiagnosticCommand returns the entire thread dump as
        // a single String, which risks OutOfMemoryError.
        Method remoteDataDumpMethod = null;
        try {
            remoteDataDumpMethod = VirtualMachineHelper.getRemoteDumpMethod();
            if (remoteDataDumpMethod == null && diagnosticCommandName == null) {
                // Neither Java Attach nor DiagnosticCommand are available, so
                // it's not possible to create a thread dump.
                return null;
            }
        } catch (RuntimeException e) {
            // Sometimes Java Attach fails spuriously.  If DiagnosticCommand is
            // available, try that.  Otherwise, propagate the failure.
            if (diagnosticCommandName == null) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
            }
        }

        File outputFile = null;
        InputStream input = null;
        OutputStream output = null;
        boolean success = false;

        try {
            // Use a filename that resembles an IBM javacore.
            outputFile = createNewFile(outputDir, "javadump", "txt");

            if (remoteDataDumpMethod != null) {
                input = remoteDataDump(remoteDataDumpMethod);
                input = new BufferedInputStream(input);

                output = new FileOutputStream(outputFile);
                output = new BufferedOutputStream(output);

                byte[] buf = new byte[8192];
                for (int read; (read = input.read(buf)) != -1;) {
                    output.write(buf, 0, read);
                }
            } else {
                String outputString;
                try {
                    outputString = (String) platformMBeanServer.invoke(diagnosticCommandName,
                                                                       "threadPrint",
                                                                       new Object[] { new String[] { "-l" } },
                                                                       new String[] { String[].class.getName() });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                output = new FileOutputStream(outputFile);
                Writer writer = new OutputStreamWriter(output, "UTF-8");
                writer.write(outputString);
                writer.close();
            }

            success = true;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            Utils.tryToClose(input);
            Utils.tryToClose(output);

            if (!success && outputFile != null && !outputFile.delete()) {
                // Avoid FindBugs warning.  We .delete() as a best effort.
                outputFile = null;
            }
        }

        return outputFile;
    }

    private InputStream remoteDataDump(Method remoteDataDumpMethod) throws IOException {
        try {
            // Pass -l, which is the jstack argument for a "long listing".
            Object[] dumpArgs = new Object[] { "-l" };
            return (InputStream) remoteDataDumpMethod.invoke(VirtualMachineHelper.getVirtualMachine(), new Object[] { dumpArgs });
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }
}
