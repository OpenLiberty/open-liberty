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

package com.ibm.ws.microprofile.openapi.impl.parser.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static Path getParentDirectoryOfFile(String fileStr) {
        final String fileScheme = "file:";
        Path file;
        fileStr = fileStr.replaceAll("\\\\", "/");
        if (fileStr.toLowerCase().startsWith(fileScheme)) {
            file = Paths.get(URI.create(fileStr)).toAbsolutePath();
        } else {
            file = Paths.get(fileStr).toAbsolutePath();
        }
        return file.toAbsolutePath().getParent();
    }
}