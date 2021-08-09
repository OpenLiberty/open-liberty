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
package com.ibm.ws.springboot.utility.utils;

import java.io.File;
import java.io.PrintStream;

import com.ibm.ws.springboot.utility.IFileUtility;

/**
 *
 */
public class FileUtility implements IFileUtility {

    @Override
    public boolean isFile(String path) {
        return new File(path).isFile();
    }

    @Override
    public boolean isDirectory(String path) {
        return new File(path).isDirectory();
    }

    @Override
    public boolean mkDirs(File f, PrintStream stdout) {
        if (f.exists()) {
            if (f.isDirectory()) {
                return true;
            }
            stdout.println(CommandUtils.getMessage("file.failedDirCreate", f.getAbsolutePath()));
            return false;
        }
        File p = f.getParentFile();
        if (p != null) {
            if (!mkDirs(p, stdout)) {
                return false;
            }
        }
        if (!f.mkdir()) {
            stdout.println(CommandUtils.getMessage("file.failedDirCreate", f.getAbsolutePath()));
            return false;
        }
        return true;
    }
}
