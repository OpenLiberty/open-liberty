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
package com.ibm.ws.kernel;

import java.io.File;

import com.ibm.ws.kernel.boot.internal.commands.JavaDumpAction;
import com.ibm.ws.kernel.boot.internal.commands.JavaDumper;

/**
 * Access to some of the commands.
 */
public class Commands {
    /**
     * Generate a thread dump of the current process.
     *
     * @param outputDir Optional. The server output directory. Use null for default.
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return the file containing the dump, or null if the action is not supported by this JVM
     * @throws RuntimeException if an error occurs while dumping
     */
    public static File threadDump(File outputDir, String nameToken, int maximum) {
        return JavaDumper.getInstance().dump(JavaDumpAction.THREAD, outputDir, nameToken, maximum);
    }

    /**
     * Generate a heap dump of the current process.
     *
     * @param outputDir Optional. The server output directory. Use null for default.
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return the file containing the dump, or null if the action is not supported by this JVM
     * @throws RuntimeException if an error occurs while dumping
     */
    public static File heapDump(File outputDir, String nameToken, int maximum) {
        return JavaDumper.getInstance().dump(JavaDumpAction.HEAP, outputDir, nameToken, maximum);
    }

    /**
     * Generate a system dump of the current process.
     *
     * @param outputDir Optional. The server output directory. Use null for default.
     * @param nameToken Optional. An additional token to place at the end of the file name before the extension. If null, no additional token.
     * @param maximum Optional. If there are already this many dump files in the target directory, then remove all but (maximum - 1) of them before performing the
     *            dump. If {@code nameToken} is specified, this only applies to dump files with that {@code nameToken}.
     * @return the file containing the dump, or null if the action is not supported by this JVM
     * @throws RuntimeException if an error occurs while dumping
     */
    public static File systemDump(File outputDir, String nameToken, int maximum) {
        return JavaDumper.getInstance().dump(JavaDumpAction.SYSTEM, outputDir, nameToken, maximum);
    }
}
