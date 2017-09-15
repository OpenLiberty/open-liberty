/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.Closeable;
import java.io.IOException;

public class FileUtils {
    public static boolean tryToClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the supplied <code>fileName</code> has an
     * extension that matches the expected extension. This is not case
     * sensitive.
     * 
     * @param expectedFileExtension
     *            The expected file name extension in lower case and including
     *            the '.'
     * @param fileName
     *            The file name to test
     * @return <code>true</code> if the file extension matches
     */
    public static boolean matchesFileExtension(String expectedFileExtension,
                                               String fileName) {
        // case insensitive match of the last x characters.
        return fileName.regionMatches(true,
                                      fileName.length() - expectedFileExtension.length(),
                                      expectedFileExtension,
                                      0,
                                      expectedFileExtension.length());
    }
}
