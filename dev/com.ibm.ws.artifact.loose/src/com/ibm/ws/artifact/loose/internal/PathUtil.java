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
package com.ibm.ws.artifact.loose.internal;

import java.io.File;

import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

//TODO: class to become common .. issue to resolve with location service.
public class PathUtil {

    /**
     * Gets the parent section of a given path.
     * <p>
     * Eg, for /wibble/fish/monkey, will return /wibble/fish<br>
     * for /wibble, will return /<br>
     * for / will return null
     * 
     * @param path the path to be interpreted.
     * @return parent path, or null if there is no parent path possible.
     */
    public static String getParent(String path) {
        String parent = null;
        if (path.lastIndexOf('/') != -1) {
            if (path.length() == 1) {
                parent = null;
            } else if (path.lastIndexOf('/') == path.indexOf("/") && path.indexOf("/") == 0) {
                parent = "/";
            } else {
                parent = path.substring(0, path.lastIndexOf("/"));
            }
        }
        return parent;
    }

    /**
     * Obtains the 1st component of a path<p>
     * Eg, for /wibble/fish will return wibble<br>
     * for /we/like/pie will return we<br>
     * for /fish will return fish<br>
     * for sheep will return sheep<br>
     * for / will return ""<br>
     * 
     * @param path path to parse
     * @return 1st path component.
     */
    public static String getFirstPathComponent(String path) {
        File localf = new File(path);
        File last = localf;
        while (localf.getParentFile() != null) {
            last = localf;
            localf = localf.getParentFile();
        }
        return last.getName();
    }

    /**
     * Given a path, and a super path, return the immediate child of the super path in the path.<p>
     * Eg, for /we/like/pies and /we/like will return pies<br>
     * for /fish/are/friends/not/food and /fish/are/friends will return not<br>
     * <p>
     * <em>Will explode in fiery ball of death if parentPath is not a parent of path, you have been warned.</em>
     * 
     * @param path full path
     * @param parentPath path segment that is a super path within path
     * @return 1st child name.
     */
    public static String getChildUnder(String path, String parentPath) {
        int start = parentPath.length();
        String local = path.substring(start, path.length());
        String name = PathUtil.getFirstPathComponent(local);
        return name;
    }

    public static String getName(String path) {
        return new File(path).getName();
    }

    public static boolean containsSymbol(String s) {
        return s.length() > 3 &&
               (s.contains(WsLocationConstants.SYMBOL_PREFIX));
    }
}
