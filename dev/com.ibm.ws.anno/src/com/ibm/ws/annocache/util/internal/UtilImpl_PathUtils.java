/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.util.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wsspi.annocache.util.Util_Factory;

public class UtilImpl_PathUtils {
    public static final String NORMALIZED_SEP = Util_Factory.NORMALIZED_SEP;
    public static final char NORMALIZED_SEP_CHAR = Util_Factory.NORMALIZED_SEP_CHAR;

    public static String append(String headPath, String tailPath) {
        if ( (headPath == null) || headPath.isEmpty() ) {
            return tailPath;
        } else if ((tailPath == null) || tailPath.isEmpty() ) {
            return headPath;
        }

        if ( headPath.endsWith(File.separator) ) {
            if ( tailPath.startsWith(File.separator) ) {
                return headPath + tailPath.substring(1);
            } else {
                return headPath + tailPath;
            }
        } else if ( tailPath.startsWith(File.separator)) {
            return headPath + tailPath;
        } else {
            return headPath + File.separator + tailPath;
        }
    }

    public static String n_append(String n_headPath, String n_tailPath) {
        if ( (n_headPath == null) || n_headPath.isEmpty() ) {
            return n_tailPath;
        } else if ((n_tailPath == null) || n_tailPath.isEmpty() ) {
            return n_headPath;
        }

        if ( n_headPath.endsWith(NORMALIZED_SEP) ) {
            if ( n_tailPath.startsWith(NORMALIZED_SEP) ) {
                return n_headPath + n_tailPath.substring(1);
            } else {
                return n_headPath + n_tailPath;
            }
        } else if ( n_tailPath.startsWith(NORMALIZED_SEP)) {
            return n_headPath + n_tailPath;
        } else {
            return n_headPath + NORMALIZED_SEP + n_tailPath;
        }
    }

    public static String normalize(String path) {
        if ( (path == null) || path.isEmpty() ) {
            return path;
        }

        if ( File.separatorChar == NORMALIZED_SEP_CHAR) {
            return path;
        } else {
            return path.replace(File.separatorChar, NORMALIZED_SEP_CHAR);
        }
    }

    public static String denormalize(String n_path) {
        if ( (n_path == null) || n_path.isEmpty() ) {
            return n_path;
        }

        if ( File.separatorChar == NORMALIZED_SEP_CHAR ) {
            return n_path;
        } else {
            return n_path.replace(NORMALIZED_SEP_CHAR, File.separatorChar);
        }
    }

    public static String subtractPath(String basePath, String fullPath) {
        if ( basePath.startsWith(File.separator) ) {
            if ( !fullPath.startsWith(basePath) ) {
                return null;
            } else {
                return fullPath.substring(basePath.length());
            }
        } else {
            if ( !fullPath.startsWith(basePath) ) {
                return null;
            } else if ( fullPath.length() <= basePath.length() ) {
                return null;
            } else if ( fullPath.charAt( basePath.length() ) != File.separatorChar) {
                return null;
            } else {
                return fullPath.substring( basePath.length() + 1 );
            }
        }
    }

    public static String n_subtractPath(String n_basePath, String n_fullPath) {
        if ( n_basePath.startsWith(NORMALIZED_SEP) ) {
            if ( !n_fullPath.startsWith(n_basePath) ) {
                return null;
            } else {
                return n_fullPath.substring(n_basePath.length());
            }
        } else {
            if ( !n_fullPath.startsWith(n_basePath) ) {
                return null;
            } else if ( n_fullPath.length() <= n_basePath.length() ) {
                return null;
            } else if ( n_fullPath.charAt( n_basePath.length() ) != NORMALIZED_SEP_CHAR ) {
                return null;
            } else {
                return n_fullPath.substring( n_basePath.length() + 1 );
            }
        }
    }

    public static UtilImpl_RelativePath addRelativePath(String basePath, String relativePath) {
        String n_basePath = normalize(basePath);
        String n_relativePath = normalize(relativePath);
        String n_fullPath = n_append(n_basePath, n_relativePath);

        return new UtilImpl_RelativePath(n_basePath, n_relativePath, n_fullPath);
    }

    public static UtilImpl_RelativePath n_addRelativePath(String n_basePath, String n_relativePath) {
        String n_fullPath = n_append(n_basePath, n_relativePath);

        return new UtilImpl_RelativePath(n_basePath, n_relativePath, n_fullPath);
    }

    public static UtilImpl_RelativePath subtractRelativePath(String basePath, String fullPath) {
        String n_basePath = normalize(basePath);
        String n_fullPath = normalize(fullPath);
        String n_relativePath = n_subtractPath(n_basePath, n_fullPath);

        return new UtilImpl_RelativePath(n_basePath, n_relativePath, n_fullPath);
    }

    public static UtilImpl_RelativePath n_subtractRelativePath(String n_basePath, String n_fullPath) {
        String n_relativePath = n_subtractPath(n_basePath, n_fullPath);

        return new UtilImpl_RelativePath(n_basePath, n_relativePath, n_fullPath);
    }

    //

    public static List<UtilImpl_RelativePath> selectJars(String basePath) {
        String n_basePath = normalize(basePath);

        List<UtilImpl_RelativePath> selectedJars = new ArrayList<UtilImpl_RelativePath>();

        selectJars( n_basePath, new File(basePath), selectedJars );

        return selectedJars;
    }

    public static void selectJars(String n_basePath,
                                  File currentFile,
                                  List<UtilImpl_RelativePath> selectedJars) {

        if ( !UtilImpl_FileUtils.exists(currentFile) ) {
            return;
        }

        if ( UtilImpl_FileUtils.isDirectory(currentFile) ) {
            // Using 'listFiles' would be more work:
            //
            // The files returned from list files do not include the
            // path from the parent file.  A call to create a new file
            // for each returned file would still be necessary, meaning,
            // double the necessary file creates would be performed.

            String[] childNames = UtilImpl_FileUtils.list(currentFile);
            if ( (childNames == null) || (childNames.length == 0) ) {
                return;
            }

            for ( String childName : childNames ) {
                selectJars( n_basePath,
                            new File(currentFile, childName),
                            selectedJars );
            }

        } else {
            String currentName = currentFile.getName();
            if ( !currentName.toUpperCase().endsWith(".JAR") ) {
                return;
            }

            String currentPath = currentFile.getPath();
            String n_currentPath = normalize(currentPath);

            UtilImpl_RelativePath relativePath = n_subtractRelativePath(n_basePath, n_currentPath);

            selectedJars.add(relativePath);
        }
    }
}
