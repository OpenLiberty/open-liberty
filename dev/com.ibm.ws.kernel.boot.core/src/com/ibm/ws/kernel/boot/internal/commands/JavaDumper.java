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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

public abstract class JavaDumper {
    private static JavaDumper instance = createInstance();

    /**
     * Create a dumper for the current JVM.
     */
    private static JavaDumper createInstance() {

        Debug.traceEntry(JavaDumper.class, "createInstance");

        JavaDumper result;

        try {
            // Try to find IBM Java dumper class.
            Class<?> dumpClass = Class.forName("com.ibm.jvm.Dump");
            try {
                // Try to find the IBM Java 7.1 dump methods.
                Class<?>[] paramTypes = new Class<?>[] { String.class };
                Method javaDumpToFileMethod = dumpClass.getMethod("javaDumpToFile", paramTypes);
                Method heapDumpToFileMethod = dumpClass.getMethod("heapDumpToFile", paramTypes);
                Method systemDumpToFileMethod = dumpClass.getMethod("systemDumpToFile", paramTypes);
                result = new IBMJavaDumperImpl(javaDumpToFileMethod, heapDumpToFileMethod, systemDumpToFileMethod);
            } catch (NoSuchMethodException e) {
                result = new IBMLegacyJavaDumperImpl(dumpClass);
            }
        } catch (ClassNotFoundException ex) {
            // Try to find HotSpot MBeans.
            ObjectName diagName;
            ObjectName diagCommandName;
            try {
                diagName = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
                diagCommandName = new ObjectName("com.sun.management:type=DiagnosticCommand");
            } catch (MalformedObjectNameException ex2) {
                throw new IllegalStateException(ex2);
            }

            MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
            if (!mbeanServer.isRegistered(diagName)) {
                diagName = null;
            }
            if (!mbeanServer.isRegistered(diagCommandName)) {
                diagCommandName = null;
            }

            result = new HotSpotJavaDumperImpl(mbeanServer, diagName, diagCommandName);
        }

        Debug.traceExit(JavaDumper.class, "createInstance", result);

        return result;
    }

    /**
     * Obtains a dumper for the current JVM.
     */
    public static JavaDumper getInstance() {
        return instance;
    }

    /**
     * Generate a java dump of the current process.
     *
     * @param action the dump action
     * @param outputDir the server output directory
     * @return the file containing the dump, or null if the action is not supported by this JVM
     * @throws RuntimeException if an error occurs while dumping
     */
    public File dump(JavaDumpAction action, File outputDir) {
        return dump(action, outputDir, null, -1);
    }

    /**
     * Generate a java dump of the current process.
     *
     * @param action the dump action
     * @param outputDir the server output directory
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many thread dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return the file containing the dump, or null if the action is not supported by this JVM
     * @throws RuntimeException if an error occurs while dumping
     */
    public abstract File dump(JavaDumpAction action, File outputDir, String nameToken, int maximum);

    /**
     * Helper method to rename a created dump artifict to include a {@code nameToken} if specified.
     *
     * @param createdFile The created dump artifact file.
     * @param nameToken An optional name token to add.
     * @return If the file is renamed, the new file; otherise, {@code createdFile}.
     */
    protected File addNameToken(File createdFile, String nameToken) {
        Debug.traceEntry(JavaDumper.class, "addNameToken", createdFile, nameToken);

        if (nameToken != null && nameToken.length() > 0) {
            String fileName = createdFile.getName();
            int i = fileName.lastIndexOf('.');

            Debug.trace(JavaDumper.class, "pruneFiles", "Searching for extension", fileName, i);

            if (i != -1) {
                String extension = fileName.substring(i);
                fileName = fileName.substring(0, i);

                File newFile = new File(createdFile.getParent(), fileName + nameToken + extension);

                Debug.trace(JavaDumper.class, "pruneFiles", "Renaming", newFile, extension, fileName);

                if (createdFile.renameTo(newFile)) {
                    createdFile = newFile;
                } else {
                    Debug.trace(JavaDumper.class, "pruneFiles", "Rename returned false");
                    throw new RuntimeException(MessageFormat.format(BootstrapConstants.messages.getString("error.dump.rename.fail"), createdFile.getAbsolutePath(),
                                                                    newFile.getAbsolutePath()));
                }
            }
        }

        Debug.traceExit(JavaDumper.class, "addNameToken", createdFile);

        return createdFile;
    }

    /**
     * Helper method to prune to {@code maximum} - 1 number of files in the {@code targetDirectory} that contains {@code uniqueFilePrefix}, end with a period followed by
     * {@code extension}, and, if {@code nameToken} is specified, contain {@nameToken}. If {@code maximum} is less than or equal to 0, do nothing.
     *
     * @param maximum Optional maximum number of files
     * @param targetDirectory The directory to check
     * @param uniqueFilePrefix The prefix pattern to search for
     * @param extension The extension to search for
     * @param nameToken Optional token to search for
     * @return The number of files deleted
     */
    protected int pruneFiles(final int maximum, File targetDirectory, final String uniqueFilePrefix, final String extension, final String nameToken) {
        Debug.traceEntry(JavaDumper.class, "pruneFiles", maximum, targetDirectory, uniqueFilePrefix, extension, nameToken);

        int filesDeleted = 0;
        if (maximum > 0) {
            if (targetDirectory == null) {
                // Get the current directory
                targetDirectory = new File(System.getProperty("user.dir"));

                Debug.trace(JavaDumper.class, "pruneFiles", "Current working directory", targetDirectory.getAbsolutePath());
            }

            File[] filteredFiles = targetDirectory.listFiles(new java.io.FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {

                    Debug.trace(JavaDumper.class, "pruneFiles", "Checking", dir.getAbsolutePath(), name);

                    if (name.contains(uniqueFilePrefix) && name.endsWith(extension)) {
                        if (nameToken == null) {
                            Debug.trace(JavaDumper.class, "pruneFiles", "Return first true");
                            return true;
                        } else {
                            if (name.contains(nameToken)) {
                                Debug.trace(JavaDumper.class, "pruneFiles", "Return second true");
                                return true;
                            }
                        }
                    }

                    Debug.trace(JavaDumper.class, "pruneFiles", "Return false");

                    return false;
                }
            });

            List<File> filteredFilesList = new ArrayList<File>(Arrays.asList(filteredFiles));

            Collections.sort(filteredFilesList, new Comparator<File>() {
                @Override
                public int compare(File x, File y) {
                    // Tried using lastModified but for some reason sometimes files created later would
                    // have an older lastModified time and thus the wrong file could be deleted,
                    // so given that the date is in the file name, we can just sort by name.
                    return x.getName().compareTo(y.getName());
                }
            });

            Debug.trace(JavaDumper.class, "pruneFiles", "Sorted files", filteredFilesList.size(), filteredFilesList);

            // Use >= because we need to make room for the one file we're going to create
            while (filteredFilesList.size() >= maximum) {
                File existingFile = filteredFilesList.remove(0);
                Debug.trace(JavaDumper.class, "pruneFiles", "Deleting", existingFile.getAbsolutePath());
                if (existingFile.delete()) {
                    filesDeleted++;
                } else {
                    Debug.trace(JavaDumper.class, "pruneFiles", "Delete returned false");
                    throw new RuntimeException(MessageFormat.format(BootstrapConstants.messages.getString("error.dump.delete.fail"), existingFile.getAbsolutePath(),
                                                                    filteredFilesList.size()));
                }
            }
        }

        Debug.traceExit(JavaDumper.class, "pruneFiles", filesDeleted);

        return filesDeleted;
    }

    /**
     * Helper method to move a created artifact to the specified {@code outputDir}.
     *
     * @param dump The path to the dump if created.
     * @param outputDir Optional output directory.
     * @return The {@code dump} or the new path if it is moved.
     */
    protected File moveDump(File dump, File outputDir) {
        Debug.traceEntry(JavaDumper.class, "moveDump", dump, outputDir);

        if (dump != null && outputDir != null && outputDir.exists() && outputDir.isDirectory() && !dump.getParentFile().equals(outputDir)) {
            File target = new File(outputDir, dump.getName());

            Debug.trace(JavaDumper.class, "moveDump", "Renaming", target);

            if (dump.renameTo(target)) {
                dump = target;
            } else {
                Debug.trace(JavaDumper.class, "moveDump", "Rename returned false");
                throw new RuntimeException(MessageFormat.format(BootstrapConstants.messages.getString("error.dump.rename.fail"), dump.getAbsolutePath(),
                                                                target.getAbsolutePath()));
            }
        }

        Debug.traceExit(JavaDumper.class, "moveDump", dump);

        return dump;
    }
}
