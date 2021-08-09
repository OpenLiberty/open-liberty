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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

public class DependencyReport {

    private static MessageDigest digest;

    public static void main(String[] args) throws Exception {
        digest = MessageDigest.getInstance("SHA-256");
        File repoDir = new File(System.getProperty("user.home"), ".ibmartifactory/repository");

        Map<String, Map<String, String>> jarMap = new HashMap<>();
        Map<String, List<String>> hashToJar = new HashMap<>();
        Map<String, String> hashToClass = new HashMap<>();
        Map<String, List<String>> versions = new TreeMap<>();
        PrintStream out = new PrintStream(new File("report.txt"));

        for (File f : findJars(repoDir)) {
            Map<String, String> classes = new HashMap<>();
            try {
                out.println("Processing file: " + f.getAbsolutePath());
                JarInputStream jarIn = new JarInputStream(new FileInputStream(f));
                ZipEntry entry = jarIn.getNextEntry();
                do {
                    if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                        String className = entry.getName();
                        className = className.replaceAll("/", ".");
                        className = className.substring(0, className.length() - 6);

                        String hash = computeHash(jarIn);

                        out.print("\t");
                        out.print(className);
                        out.print(" = ");
                        out.println(hash);

                        classes.put(className, hash);
                        hashToClass.put(hash, className);
                        List<String> jars = new ArrayList<>();
                        List<String> prior = hashToJar.putIfAbsent(hash, jars);
                        if (prior != null) {
                            jars = prior;
                        }
                        jars.add(f.getAbsolutePath());

                        // bad practice of reusing jars
                        List<String> hashes = new ArrayList<>();
                        prior = versions.putIfAbsent(className, hashes);
                        if (prior != null) {
                            hashes = prior;
                        }
                        hashes.add(hash);
                    }
                } while ((entry = jarIn.getNextEntry()) != null);
            } catch (IOException e) {
                e.printStackTrace();
            }

            jarMap.put(f.getAbsolutePath(), classes);
        }

        PrintStream multipleVersions = new PrintStream("versions.txt");
        versions.entrySet().stream().filter(entry -> entry.getValue().size() > 1).forEach(entry -> {
            multipleVersions.print(entry.getKey());
            multipleVersions.println(":");
            entry.getValue().stream().forEach(hash -> {
                multipleVersions.print("\t");
                multipleVersions.print(hashToJar.get(hash));
                multipleVersions.print(" = ");
                multipleVersions.println(hash);
            });
        });
/*
 * PrintStream copies = new PrintStream(new File("duplicate.txt"));
 * Set<Map.Entry<String, List<String>>> jarsWithCopies = hashToJar.entrySet().stream().filter(entry -> entry.getValue().size() > 1).collect(Collectors.toSet());
 * 
 * jarsWithCopies.stream().forEach(entry -> {
 * copies.print(hashToClass.get(entry.getKey()));
 * copies.println(":");
 * entry.getValue().stream().forEach(fileName -> copies.println("\t" + fileName));
 * });
 */

    }

    public static String computeHash(JarInputStream jarIn) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        int len;
        while ((len = jarIn.read(buffer)) != -1) {
            digest.update(buffer, 0, len);
        }

        byte[] hash = digest.digest();

        return Base64.getEncoder().encodeToString(hash);
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
