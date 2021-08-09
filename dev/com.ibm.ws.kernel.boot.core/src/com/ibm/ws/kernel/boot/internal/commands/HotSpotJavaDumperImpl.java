/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.ibm.ws.kernel.boot.cmdline.Utils;

class HotSpotJavaDumperImpl extends JavaDumper {
    /**
     * The current process ID, or null if it could not be determined.
     */
    private static final String PID = getPID();

    /**
     * Return the current process ID.
     */
    private static String getPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int index = name.indexOf('@');
        if (index == -1) {
            return null;
        }

        String pid = name.substring(0, index);
        if (!pid.matches("[0-9]+")) {
            return null;
        }

        return pid;
    }

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

    /**
     * Lazily-initialized VirtualMachine for generating Java dumps.
     * 
     * @see #getVirtualMachine
     */
    private VirtualMachine vm;

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
     * @param prefix the prefix for the filename
     * @param extension the file extension, not including a leading "."
     * @return the created file
     */
    private File createNewFile(File outputDir, String prefix, String extension) throws IOException {
        String dateTime = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
        File outputFile;
        do {
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
        VirtualMachine vm = null;
        try {
            vm = getAttachedVirtualMachine();
            if (vm == null && diagnosticCommandName == null) {
                // Neither Java Attach nor DiagnosticCommand are available, so
                // it's not possible to create a thread dump.
                return null;
            }
        } catch (VirtualMachineException e) {
            // Sometimes Java Attach fails spuriously.  If DiagnosticCommand is
            // available, try that.  Otherwise, propagate the failure.
            if (diagnosticCommandName == null) {
                Throwable cause = e.getCause();
                throw cause instanceof RuntimeException ? (RuntimeException) cause : new RuntimeException(cause);
            }
        }

        File outputFile = null;
        InputStream input = null;
        OutputStream output = null;
        boolean success = false;

        try {
            // Use a filename that resembles an IBM javacore.
            outputFile = createNewFile(outputDir, "javadump", "txt");

            if (vm != null) {
                input = vm.remoteDataDump();
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

    /**
     * Returns sun.tools.attach.HotSpotVirtualMachine, if possible.
     */
    private synchronized VirtualMachine getAttachedVirtualMachine() throws VirtualMachineException {
        if (PID == null) {
            // Java Attach requires a PID.
            return null;
        }

        if (vm == null) {
            vm = createVirtualMachine();
        }
        return vm.isAttached() ? vm : null;
    }

    /**
     * Create a VirtualMachine wrapper.
     */
    private VirtualMachine createVirtualMachine() throws VirtualMachineException {
        ClassLoader toolsClassLoader;

        File toolsJar = getToolsJar();
        if (toolsJar == null) {
            // The attach classes are on the boot classpath on Mac.
            toolsClassLoader = HotSpotJavaDumperImpl.class.getClassLoader();
        } else {
            try {
                toolsClassLoader = new URLClassLoader(new URL[] { toolsJar.getAbsoluteFile().toURI().toURL() });
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Class<?> vmClass = toolsClassLoader.loadClass("com.sun.tools.attach.VirtualMachine");
            Method attachMethod = vmClass.getMethod("attach", new Class<?>[] { String.class });
            Object toolsVM = attachMethod.invoke(null, new Object[] { PID });
            Method remoteDataDumpMethod = toolsVM.getClass().getMethod("remoteDataDump", new Class<?>[] { Object[].class });
            return new VirtualMachine(toolsVM, remoteDataDumpMethod);
        } catch (ClassNotFoundException e) {
            // The class isn't found, so we won't be able to create dumps.
        } catch (InvocationTargetException e) {
            throw new VirtualMachineException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new VirtualMachine(null, null);
    }

    /**
     * Gets tools.jar from the JDK, or null if not found (for example, when
     * running with a JRE rather than a JDK, or when running on Mac).
     */
    private static File getToolsJar() {
        String javaHome = System.getProperty("java.home");
        File file = new File(javaHome, "../lib/tools.jar");
        if (!file.exists()) {
            file = new File(javaHome, "lib/tools.jar");
            if (!file.exists()) {
                return null;
            }
        }

        return file;
    }

    @SuppressWarnings("serial")
    private static class VirtualMachineException extends Exception {
        VirtualMachineException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Create a wrapper for sun.tools.attach.HotSpotVirtualMachine.
     */
    private static class VirtualMachine {
        /**
         * sun.tools.attach.HotSpotVirtualMachine
         */
        private final Object vm;

        /**
         * InputStream sun.tools.attach.HotSpotVirtualMachine.remoteDataDump(Object[])
         */
        private final Method remoteDataDumpMethod;

        VirtualMachine(Object vm, Method remoteDataDumpMethod) {
            this.vm = vm;
            this.remoteDataDumpMethod = remoteDataDumpMethod;
        }

        public boolean isAttached() {
            return vm != null;
        }

        public InputStream remoteDataDump() throws IOException {
            try {
                // Pass -l, which is the jstack argument for a "long listing".
                Object[] dumpArgs = new Object[] { "-l" };
                return (InputStream) remoteDataDumpMethod.invoke(vm, new Object[] { dumpArgs });
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
}
