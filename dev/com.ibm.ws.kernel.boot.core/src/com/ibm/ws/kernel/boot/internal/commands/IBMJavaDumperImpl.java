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
    private final Method systemDumpToFileMethod;

    IBMJavaDumperImpl(Method javaDumpToFileMethod, Method heapDumpToFileMethod, Method systemDumpToFileMethod) {
        this.javaDumpToFileMethod = javaDumpToFileMethod;
        this.heapDumpToFileMethod = heapDumpToFileMethod;
        this.systemDumpToFileMethod = systemDumpToFileMethod;
    }

    @Override
    public File dump(JavaDumpAction action, File outputDir) {
        Method method;

        switch (action) {
            case HEAP:
                method = heapDumpToFileMethod;
                break;

            case SYSTEM:
                method = systemDumpToFileMethod;
                break;

            case THREAD:
                method = javaDumpToFileMethod;
                break;

            default:
                return null;
        }

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
