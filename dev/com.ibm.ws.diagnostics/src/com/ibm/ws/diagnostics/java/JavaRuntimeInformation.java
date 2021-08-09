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
package com.ibm.ws.diagnostics.java;

import java.io.File;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.logging.Introspector;

/**
 * Diagnostic handler to capture the system properties.
 */
public class JavaRuntimeInformation implements Introspector {
    /**
     * White space to use for indenting.
     */
    private final static String INDENT = "    ";

    @Override
    public String getIntrospectorName() {
        return "JavaRuntimeInformation";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Java runtime information";
    }

    /**
     * Grab a snapshot of the current system properties.
     * 
     * @param out the output stream to write diagnostics to
     */
    @Override
    public void introspect(PrintWriter writer) {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        introspectUptime(runtime, writer);
        introspectVendorVersion(runtime, writer);
        introspectInputArguments(runtime, writer);
        Map<String, String> props = introspectSystemProperties(runtime, writer);
        //introspectPaths(runtime, writer);
        introspectDirsFromSystemProperties(runtime, writer, "java.ext.dirs", props);
        introspectDirsFromSystemProperties(runtime, writer, "java.endorsed.dirs", props);
    }

    private void introspectUptime(RuntimeMXBean runtime, PrintWriter writer) {
        writeHeader("Uptime", writer);

        StringBuilder sb = new StringBuilder();
        sb.append("JVM active ").append(formatDuration(runtime.getUptime()));
        sb.append(" [Started ").append(new Date(runtime.getStartTime())).append("]");

        writer.println(sb.toString());
    }

    private void introspectVendorVersion(RuntimeMXBean runtime, PrintWriter writer) {
        writeHeader("JVM Vendor and Version", writer);
        writer.println(" JVM Vendor: " + runtime.getVmVendor());
        writer.println("   JVM Name: " + runtime.getVmName());
        writer.println("JVM Version: " + runtime.getVmVersion());

        writeHeader("Java Specification Levels", writer);
        writer.println(" VM Spec Vendor: " + runtime.getSpecVendor());
        writer.println("   VM Spec Name: " + runtime.getSpecName());
        writer.println("VM Spec Version: " + runtime.getSpecVersion());
    }

    private void introspectInputArguments(RuntimeMXBean runtime, PrintWriter writer) {
        writeHeader("Input Arguments", writer);

        List<String> args = runtime.getInputArguments();
        for (int i = 0; i < args.size(); i++) {
            StringBuilder sb = new StringBuilder(INDENT);
            sb.append("arg[").append(i).append("] = ");
            sb.append(args.get(i));
            writer.println(sb.toString());
        }
    }

    private Map<String, String> introspectSystemProperties(RuntimeMXBean runtime, PrintWriter writer) {
        writeHeader("Java System Properties", writer);

        // Get the keys into a sorted map for display
        Map<String, String> props = AccessController.doPrivileged(new GetSysPropsAction(runtime));

        // Write the values
        for (Map.Entry<String, String> entry : props.entrySet()) {
            writer.print(INDENT);
            writer.print(entry.getKey());
            writer.print("=");
            writer.println(entry.getValue().replaceAll("\\\n", "<nl>"));
        }
        return props;
    }

    private static class GetSysPropsAction implements PrivilegedAction<Map<String, String>> {
        private final RuntimeMXBean runtime;

        private GetSysPropsAction(RuntimeMXBean runtime) {
            this.runtime = runtime;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.security.PrivilegedAction#run()
         */
        @Override
        public Map<String, String> run() {
            return new TreeMap<String, String>(runtime.getSystemProperties());
        }

    }

    private void introspectDirsFromSystemProperties(RuntimeMXBean runtime, PrintWriter writer, String propertyName, Map<String, String> props) {
        String pathSeparator = props.get("path.separator");

        String dirsFromSysProps = props.get(propertyName);
        if (dirsFromSysProps != null) {
            writeHeader("Contents of " + propertyName, writer);
            String[] dirs = dirsFromSysProps.split(pathSeparator);
            for (String dir : dirs) {
                File dirFile = new File(dir);
                writer.println(INDENT + dir);
                for (File f : listFiles(dirFile, writer)) {
                    writer.println(INDENT + INDENT + f.getName());
                }
            }
        }
    }

    @FFDCIgnore(PrivilegedActionException.class)
    private File[] listFiles(final File dir, PrintWriter writer) {
        File[] files;
        try {
            files = AccessController.doPrivileged(new PrivilegedExceptionAction<File[]>() {

                @Override
                public File[] run() throws Exception {
                    File[] files = dir.listFiles();
                    return files == null ? new File[] {} : files;
                }
            });
        } catch (PrivilegedActionException e) {
            writer.println(INDENT + "Failed to read contents of directory - " + dir.getAbsolutePath());
            e.printStackTrace(writer);
            files = new File[] {};
        }
        return files;
    }

    //    private void introspectPaths(RuntimeMXBean runtime, PrintWriter writer) {
    //        writeHeader("JVM Path Information", writer);
    //
    //        writer.print(formatPath("java.class.path", runtime.getClassPath()));
    //        writer.print(formatPath("java.library.path", runtime.getLibraryPath()));
    //    }
    //
    //    private String formatPath(String pathName, String path) {
    //        return "";
    //    }

    private void writeHeader(String header, PrintWriter writer) {
        writer.println();
        writer.println(header);
        for (int i = header.length(); i > 0; i--) {
            writer.print("-");
        }
        writer.println();
    }

    private final long MILLIS_PER_SECOND = 1000L;
    private final long MILLIS_PER_MINUTE = 60 * 1000L;
    private final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    private final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
    private final long MILLIS_PER_WEEK = 7 * MILLIS_PER_DAY;

    private String formatDuration(long millis) {
        StringBuilder sb = new StringBuilder();

        long weeks = millis / MILLIS_PER_WEEK;
        if (weeks != 0) {
            sb.append(weeks).append("w");
        }

        long days = (millis % MILLIS_PER_WEEK) / MILLIS_PER_DAY;
        if (days != 0) {
            sb.append(days).append("d");
        }

        long hours = (millis % MILLIS_PER_DAY) / MILLIS_PER_HOUR;
        if (hours != 0) {
            sb.append(hours).append("h");
        }

        long minutes = (millis % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE;
        if (minutes != 0) {
            sb.append(minutes).append("m");
        }

        long seconds = (millis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND;
        sb.append(seconds).append(".").append(millis % MILLIS_PER_SECOND).append("s");

        return sb.toString();
    }
}
