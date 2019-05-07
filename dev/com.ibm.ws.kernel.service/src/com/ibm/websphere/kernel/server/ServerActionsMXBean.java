/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.kernel.server;

/**
 * The ServerActionsMXBean exposes various actions on the server.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 *
 * @ibm-api
 */
public interface ServerActionsMXBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    String OBJECT_NAME = "WebSphere:feature=kernel,name=ServerActions";

    /**
     * Perform a thread dump, if possible. By default, the dump goes to the current working directory: on IBM Java, javacore*.txt, and on HotSpot, javadump*.txt.
     *
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String threadDump();

    /**
     * Perform a thread dump, if possible. By default, the dump goes to the current working directory: on IBM Java, javacore*.txt, and on HotSpot, javadump*.txt.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String threadDump(String targetDirectory);

    /**
     * Perform a thread dump, if possible. By default, the dump goes to the current working directory: on IBM Java, javacore*.txt, and on HotSpot, javadump*.txt.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String threadDump(String targetDirectory, String nameToken);

    /**
     * Perform a thread dump, if possible. By default, the dump goes to the current working directory: on IBM Java, javacore*.txt, and on HotSpot, javadump*.txt.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String threadDump(String targetDirectory, String nameToken, int maximum);

    /**
     * Perform a heap dump, if possible.
     *
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String heapDump();

    /**
     * Perform a heap dump, if possible.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String heapDump(String targetDirectory);

    /**
     * Perform a heap dump, if possible.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String heapDump(String targetDirectory, String nameToken);

    /**
     * Perform a heap dump, if possible.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String heapDump(String targetDirectory, String nameToken, int maximum);

    /**
     * Perform a system dump, if possible.
     *
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String systemDump();

    /**
     * Perform a system dump, if possible.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String systemDump(String targetDirectory);

    /**
     * Perform a system dump, if possible.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String systemDump(String targetDirectory, String nameToken);

    /**
     * Perform a system dump, if possible.
     *
     * @param targetDirectory Optional. The target directory to place the file. If null, use default (which may depend on JVM arguments and disk space).
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return The absolute path of the generated dump, or null if unsupported by this JVM.
     * @throws RuntimeException if an error occurs while dumping
     */
    String systemDump(String targetDirectory, String nameToken, int maximum);
}
