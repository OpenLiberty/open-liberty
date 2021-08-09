/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.lang.reflect.Method;

class IBMJavaDumperImpl extends JavaDumper {
    private final Method javaDumpToFileMethod;
    private final Method heapDumpToFileMethod;
    private final Method triggerDumpMethod;

    IBMJavaDumperImpl(Method javaDumpToFileMethod, Method heapDumpToFileMethod, Method triggerDumpMethod) {
        this.javaDumpToFileMethod = javaDumpToFileMethod;
        this.heapDumpToFileMethod = heapDumpToFileMethod;
        this.triggerDumpMethod = triggerDumpMethod;
    }

    @Override
    public File dump(JavaDumpAction action, File outputDir) {
        switch (action) {
            case HEAP:
                return processReturnedPath(action, invokeToFileMethod(heapDumpToFileMethod));

            case SYSTEM:
                // "if you intend to use the system dump file for Java heap memory analysis, use the
                // following option to request exclusive access when the dump is taken:
                // -Xdump:system:defaults:request=exclusive+compact+prepwalk"
                // https://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/openj9/xdump/index.html#requestrequests

                // We don't want to use compact because there are times when it's interesting
                // to look at the trash in older regions. By default, the Memory Analyzer Tool
                // performs a GC when loading a core dump and doesn't show such trash. Avoiding
                // compact also avoids some GC overhead and moving that to the analysis machine.

                try {
                    String resultPath = (String) triggerDumpMethod.invoke(null, new Object[] { "system:request=exclusive+serial+prepwalk" });
                    return processReturnedPath(action, resultPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            case THREAD:
                return processReturnedPath(action, invokeToFileMethod(javaDumpToFileMethod));

            default:
                return null;
        }
    }

    private String invokeToFileMethod(Method method) {
        String resultPath;
        try {
            // Passing a null file name will cause the JVM to write the dump to
            // the default location based on the current dump settings and
            // return that path.
            String fileNamePattern = null;
            resultPath = (String) method.invoke(null, new Object[] { fileNamePattern });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resultPath;
    }

    private File processReturnedPath(JavaDumpAction action, String resultPath) {
        // After Java 8 JDK updates on z/OS, the Legacy path is no longer used in JavaDumper.
        // Updating this path to return null for z/OS so that the correct audit message is
        // returned to the user.
        if (ServerDumpUtil.isZos() && (JavaDumpAction.SYSTEM == action)) {
            // We dont get a core*.dmp file as it goes to a dataset
            return null;
        } else {
            return new File(resultPath);
        }
    }
}
