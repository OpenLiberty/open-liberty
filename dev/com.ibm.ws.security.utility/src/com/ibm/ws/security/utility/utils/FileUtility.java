/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import com.ibm.ws.security.utility.IFileUtility;

/**
 *
 */
public class FileUtility implements IFileUtility {
    static final String SLASH = String.valueOf(File.separatorChar);
    private final String WLP_USER_DIR;
    private final String WLP_OUTPUT_DIR;

    /**
     * Construct the FileUtility class based on the values for the various
     * environment variables.
     * <p>
     * The supported environment variables are: WLP_USER_DIR and WLP_OUTPUT_DIR
     *
     * @param WLP_USER_DIR The value of WLP_USER_DIR environment variable. {@code null} is supported.
     * @param WLP_OUTPUT_DIR The value of WLP_OUTPUT_DIR environment variable. {@code null} is supported.
     */
    public FileUtility(String WLP_USER_DIR, String WLP_OUTPUT_DIR) {
        this.WLP_USER_DIR = WLP_USER_DIR;
        this.WLP_OUTPUT_DIR = WLP_OUTPUT_DIR;
    }

    /** {@inheritDoc} */
    @Override
    public String getServersDirectory() {
        if (WLP_OUTPUT_DIR != null) {
            return WLP_OUTPUT_DIR + SLASH;
        } else if (WLP_USER_DIR != null) {
            return WLP_USER_DIR + SLASH + "servers" + SLASH;
        } else {
            return System.getProperty("user.dir") + SLASH + "usr" + SLASH + "servers" + SLASH;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getClientsDirectory() {
        if (WLP_USER_DIR != null) {
            return WLP_USER_DIR + SLASH + "clients" + SLASH;
        } else {
            return System.getProperty("user.dir") + SLASH + "usr" + SLASH + "clients" + SLASH;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean createParentDirectory(PrintStream stdout, File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return true;
        }
        if (!parent.exists()) {
            if (!createParentDirectory(stdout, parent)) {
                return false;
            }
            if (!parent.mkdir()) {
                stdout.println(CommandUtils.getMessage("fileUtility.failedDirCreate", resolvePath(parent)));
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String resolvePath(String path) {
        return resolvePath(new File(path));
    }

    /** {@inheritDoc} */
    @Override
    public String resolvePath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            // If we can't resolve the canonical path, just use absolute
            return f.getAbsolutePath();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(String path) {
        File fPath = new File(path);
        return fPath.exists();
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(File file) {
        return file.exists();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirectory(File file) {
        return file.isDirectory();
    }

    /** {@inheritDoc} */
    @Override
    public boolean writeToFile(PrintStream stderr, String toWrite, File outFile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            fos.write(toWrite.getBytes(Charset.forName("UTF-8")));
            fos.flush();
            return true;
        } catch (FileNotFoundException e) {
            stderr.println(e.getMessage());
        } catch (IOException e) {
            stderr.println(e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // Ignored, can not do anything about this
                }
            }
        }
        return false;
    }

}
