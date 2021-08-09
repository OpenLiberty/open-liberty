/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import java.io.File;

public class FileSystem {

    // NEED TO IMPLEMENT FIX FOR NOVELL. --> not needed since it is no longer
    // supported.
    // public static final boolean isCaseInsensitive=
    // (System.getProperty("os.name").toLowerCase().startsWith("windows") ||
    // System.getProperty("os.name").toLowerCase().startsWith("netware")) ;

    public static final boolean isCaseInsensitive = System.getProperty("os.name").toLowerCase().equals("os/400")
                                                    || System.getProperty("os.name").toLowerCase().startsWith("windows");
    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    // 94578 Security Defect.
    // Prior to this fix, WebSphere on Windows Servers was not checking case
    // sensitivity when serving JSP's or static html files.
    // If a file was secured, as long as the browser request used different
    // capitalization and/or lowercase than the secured file, the file would be
    // served without being challenged.
    // Not a problem on UNIX type systems since the OS handles is case sensitive.

    public static boolean uriCaseCheck(File file, String matchString) throws java.io.IOException {

        if (isCaseInsensitive || isWindows) { // security measure to ensure that this check is only performed on case
            // sensitive servers

            // begin 154268
            matchString = WSUtil.resolveURI(matchString);
            // (removed since it is changed to "//" inside of resolveURI
            // matchString = matchString.replace ('/', '\\'); // change string from
            // url format to Windows format
            // end 154268

            matchString = matchString.replace('/', File.separatorChar);
            String canPath = file.getCanonicalPath();
            // begin 540920 change for drive letter ignore case on Win 2008
            int offset = 0;
            int inx = matchString.length();
            if (isWindows && inx > 1 && canPath.length() > 0 && matchString.charAt(1) == ':') {
                // check that the string to match starts with a drive letter (':'), and
                // the drive letter is the same as canonical
                if (!(matchString.substring(0, 1).equalsIgnoreCase(canPath.substring(0, 1)))) {
                    return false;
                }
                // make the offset 1 for the regionMatches so that we do not compare the
                // drive letters
                offset = 1;
            }
            return canPath.regionMatches(canPath.length() - inx + offset, matchString, offset, inx - offset);
            // end 540920
        }
        return true;
    } // end 94578 Security Defect.
}
