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
package com.ibm.ws.kernel.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Various file system utilities.
 */
public class FileSystem {
    /**
     * Reads all lines in the file specified by {@code path}. Obviously be careful not
     * to call this on potentially large files.
     *
     * @param path Path to read.
     * @return List of lines from the file.
     * @throws FileNotFoundException File cannot be found.
     * @throws IOException Error reading the file.
     */
    public static List<String> readAllLines(String path) throws FileNotFoundException, IOException {
        List<String> result = new ArrayList<String>();
        try (FileInputStream fis = new FileInputStream(path)) {
            try (InputStreamReader isr = new InputStreamReader(fis)) {
                try (BufferedReader br = new BufferedReader(isr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        result.add(line);
                    }
                }
            }
        }
        return result;
    }
}
