/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

class IBMLegacyJavaDumperImpl extends JavaDumper {
    private static final Pattern HEAPDUMP_PATTERN = createPattern("heapdump", "phd");
    private static final Pattern JAVACORE_PATTERN = createPattern("javacore", "txt");
    private static final Pattern CORE_DUMP_PATTERN = createPattern("core", "dmp");

    private static Pattern createPattern(String prefix, String suffix) {
        // yyyyMMdd.HHmmss.pid.sequenceNumber
        return Pattern.compile(prefix + "\\.\\d{8}\\.\\d{6}\\..+?\\.\\d+\\." + suffix);
    }

    private final Class<?> dumpClass;

    IBMLegacyJavaDumperImpl(Class<?> dumpClass) {
        this.dumpClass = dumpClass;
    }

    @Override
    public File dump(JavaDumpAction action, File outputDir) {
        Pattern pattern;
        String dumpDirVar;
        String methodName;

        switch (action) {
            case HEAP:
                // -Xdump:heap
                dumpDirVar = "IBM_HEAPDUMPDIR";
                pattern = HEAPDUMP_PATTERN;
                methodName = "HeapDump";
                break;

            case SYSTEM:
                // -Xdump:system
                dumpDirVar = "IBM_COREDIR";
                pattern = CORE_DUMP_PATTERN;
                methodName = "SystemDump";
                break;

            case THREAD:
                // -Xdump:java
                dumpDirVar = "IBM_JAVACOREDIR";
                pattern = JAVACORE_PATTERN;
                methodName = "JavaDump";
                break;

            default:
                return null;
        }

        // There is no API for generating a dump to a specific file, so
        // determine the directory where the JVM will put the dump, find
        // existing files in the directory matching the dump name, generate
        // the dump, and then find the newly created file. 

        File dumpDir;

        String dumpDirName = System.getenv(dumpDirVar);
        if (dumpDirName != null) {
            dumpDir = new File(dumpDirName);
        } else {
            dumpDir = new File(System.getProperty("user.dir"));
        }

        Set<String> existingDumps = new TreeSet<String>();

        String[] fileNames = dumpDir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                if (pattern.matcher(fileName).matches()) {
                    existingDumps.add(fileName);
                }
            }
        }

        try {
            dumpClass.getMethod(methodName).invoke(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        fileNames = dumpDir.list();
        if (fileNames != null) {
            // Sort to prefer earliest timestamp.
            Arrays.sort(fileNames);

            for (String fileName : fileNames) {
                if (!existingDumps.contains(fileName) && pattern.matcher(fileName).matches()) {
                    return new File(dumpDir, fileName);
                }
            }
        }

        if (ServerDumpUtil.isZos() && (JavaDumpAction.SYSTEM == action)) {
            // We dont get a core*.dmp file as it goes to a dataset
            return null;
        } else {
            throw new IllegalStateException("failed to find generated dump file");
        }
    }
}
