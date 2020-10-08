/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Utilities for working with files
 */
public class FileUtils {

    /**
     * Recursively deletes files within the supplied directory and then deletes the directory itself.
     *
     * @param dir The directory to delete
     */
    public static void recursiveDelete(File dir) {
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    recursiveDelete(f);
                } else {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    /**
     * Copies the contents of a source file into a destination file
     *
     * @param  sourceFile
     * @param  destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        // Creates the destination file if it doesn't exists
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException e) {
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void copyDirectory(File source, File target) throws IOException {
        if (source.isDirectory()) {
            if (!target.exists()) {
                target.mkdir();
            }

            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(source, children[i]),
                              new File(target, children[i]));
            }
        } else {
            copyFile(source, target);
        }
    }

    public static String readFile(String file) throws IOException {
        File f = new File(file);
        if (!f.exists() || f.isDirectory())
            throw new FileNotFoundException(file);

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append('\n');
        } finally {
            br.close();
        }
        return sb.toString();
    }

}
