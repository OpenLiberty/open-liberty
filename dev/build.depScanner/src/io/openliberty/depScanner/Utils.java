/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.depScanner;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class Utils {
    private final static MessageDigest digest;

    static {
        MessageDigest tmp = null;
        try {
            tmp = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        digest = tmp;
    }

    public static byte[] computeHash(JarInputStream jarIn) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        int len;
        while ((len = jarIn.read(buffer)) != -1) {
            digest.update(buffer, 0, len);
        }

        return digest.digest();
    }

    public static String computeHashBase64(JarInputStream jarIn) throws IOException {
        return Base64.getEncoder().encodeToString(computeHash(jarIn));
    }

    public static byte[] computeHash(List<byte[]> hashes) throws IOException {
        hashes.stream().forEach(hash -> digest.update(hash));
        return digest.digest();
    }

    public static List<File> findJars(File repoDir) {
        List<File> files = new ArrayList<>();
        File[] children = repoDir.listFiles();
        if (children != null) {
            for (File f : children) {
                if (f.isDirectory()) {
                    files.addAll(findJars(f));
                } else if (f.getName().endsWith(".jar")) {
                    try {
                        JarFile jar = new JarFile(f);
                        jar.close();
                        files.add(f);
                    } catch (IOException e) {
                        // not a jar file
                    }
                }
            }
        }

        return files;
    }
}
