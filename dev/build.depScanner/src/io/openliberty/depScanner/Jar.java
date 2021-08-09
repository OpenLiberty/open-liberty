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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Jar {
    // Map of class hashes. Key is the name, hash is the value
    private final Map<String, byte[]> classHashes = new TreeMap<>();
    private final Map<String, byte[]> packageHashes = new TreeMap<>();
    private boolean dirty = false;

    private final File originalFile;

    public Jar(File f) {
        originalFile = f;
    }

    public void addClass(String name, byte[] hash) {
        dirty = true;
        classHashes.put(name, hash);
    }

    public File getOriginalFile() {
        return originalFile;
    }

    public boolean contains(Jar m) {
        computePackageHash();
        m.computePackageHash();

        List<String> packages = packageHashes.entrySet()
                        .stream()
                        .filter(entry -> m.packageHashes.containsKey(entry.getKey()) && Arrays.equals(m.packageHashes.get(entry.getKey()), entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        return !packages.isEmpty();
    }

    public int containsCount(Jar m) {
        computePackageHash();
        m.computePackageHash();

        List<String> packages = packageHashes.entrySet()
                        .stream()
                        .filter(entry -> m.packageHashes.containsKey(entry.getKey()) && Arrays.equals(m.packageHashes.get(entry.getKey()), entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

        return packages.size();
    }

    @Override
    public String toString() {
        return originalFile.getAbsolutePath();
    }

    private void computePackageHash() {
        if (dirty) {
            packageHashes.clear();
            Map<String, List<byte[]>> hashesForPackages = new HashMap<>();

            classHashes.forEach((key, value) -> {
                if (key.length() > 0) {
                    int index = key.lastIndexOf('.');
                    String packageName;
                    if (index == -1) {
                        packageName = "";
                    } else {
                        packageName = key.substring(0, key.lastIndexOf('.'));
                    }

                    List<byte[]> tmp = new ArrayList<>();
                    List<byte[]> prior = hashesForPackages.putIfAbsent(packageName, tmp);
                    if (prior != null) {
                        tmp = prior;
                    }
                    tmp.add(value);
                } else {
                    System.out.println(originalFile + " has a class hash with an empty string key");
                }
            });

            hashesForPackages.forEach((pName, hashes) -> {

                byte[] hash;
                try {
                    hash = Utils.computeHash(hashes);
                    packageHashes.put(pName, hash);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
            dirty = false;
        }
    }

    public List<String> getPackages(Jar m) {
        computePackageHash();
        m.computePackageHash();

        return packageHashes.entrySet()
                        .stream()
                        .filter(entry -> m.packageHashes.containsKey(entry.getKey()) && Arrays.equals(m.packageHashes.get(entry.getKey()), entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
    }

    public Collection<String> getPackages() {
        computePackageHash();
        return packageHashes.keySet();
    }
}
