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
package com.ibm.ws.app.manager.springboot.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ibm.ws.app.manager.springboot.internal.SpringConstants;

/**
 * A utility class for thinning an uber jar by separating application code in a separate jar
 * and libraries(dependencies) in a zip or a directory.
 */
public class SpringBootThinUtil {

    private final JarFile sourceFatJar;
    private final File targetThinJar;
    private final File libIndexCache;
    private final boolean putLibCacheInDirectory;
    private final String springBootLibPath;
    private final List<String> libEntries = new ArrayList<>();
    private final Set<String> hashPrefixes = new HashSet<>();

    public SpringBootThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache, boolean putLibCacheInDirectory) throws IOException {
        this.sourceFatJar = new JarFile(sourceFatJar);
        this.targetThinJar = targetThinJar;
        this.libIndexCache = libIndexCache;
        this.putLibCacheInDirectory = putLibCacheInDirectory;
        String springBootLibPath = new SpringBootManifest(this.sourceFatJar.getManifest()).getSpringBootLib();
        if (!springBootLibPath.endsWith("/")) {
            springBootLibPath += "/";
        }
        this.springBootLibPath = springBootLibPath;
    }

    public void execute() throws IOException, NoSuchAlgorithmException {
        thin();
    }

    private void thin() throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        try (JarOutputStream thinJar = new JarOutputStream(new FileOutputStream(targetThinJar), sourceFatJar.getManifest());
                        ZipOutputStream libZip = putLibCacheInDirectory ? null : new ZipOutputStream(new FileOutputStream(libIndexCache))) {

            for (Enumeration<JarEntry> entries = sourceFatJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!JarFile.MANIFEST_NAME.equals(entry.getName()) && !entry.getName().startsWith("org")) { // hack to omit spring boot loader
                    storeEntry(thinJar, libZip, entry);
                }
            }
            addLibIndexFileToThinJar(thinJar);

        }
    }

    private void storeEntry(JarOutputStream thinJar, ZipOutputStream libZip, JarEntry entry) throws IOException, NoSuchAlgorithmException {
        String path = entry.getName();

        if (entry.getName().startsWith(springBootLibPath) && !entry.getName().equals(springBootLibPath)) {

            String hash = hash(sourceFatJar, entry);
            String hashPrefix = hash.substring(0, 2) + "/";
            String hashSuffix = hash.substring(2, hash.length());

            if (putLibCacheInDirectory) {
                storeLibraryInDir(entry, hashPrefix, hashSuffix);
            } else {
                storeLibraryInZip(libZip, entry, hashPrefix, hashSuffix);
            }

            String libLine = "/" + path + '=' + hash;
            libEntries.add(libLine);
        } else {
            try (InputStream is = sourceFatJar.getInputStream(entry)) {
                writeEntry(is, thinJar, path);
            }
        }
    }

    private static String hash(JarFile jf, ZipEntry entry) throws IOException, NoSuchAlgorithmException {
        InputStream eis = jf.getInputStream(entry);
        MessageDigest digest = MessageDigest.getInstance("sha-256");
        byte[] buffer = new byte[4096];
        int read = -1;

        while ((read = eis.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        byte[] digested = digest.digest();
        return convertToHexString(digested);
    }

    private static String convertToHexString(byte[] digested) {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < digested.length; i++) {
            stringBuffer.append(Integer.toString((digested[i] & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

    private void storeLibraryInZip(ZipOutputStream libZip, JarEntry entry, String hashPrefix, String hashSuffix) throws IOException, NoSuchAlgorithmException {
        String path = entry.getName();
        try (InputStream is = sourceFatJar.getInputStream(entry)) {
            if (!hashPrefixes.contains(hashPrefix)) {
                libZip.putNextEntry(new ZipEntry(hashPrefix));
                libZip.closeEntry();
                hashPrefixes.add(hashPrefix);
            }
            path = hashPrefix + hashSuffix + ".jar";
            writeEntry(is, libZip, path);
        }
    }

    private void storeLibraryInDir(JarEntry entry, String hashPrefix, String hashSuffix) throws IOException, NoSuchAlgorithmException {
        if (!libIndexCache.exists()) {
            libIndexCache.mkdirs();
        }
        File libDir = new File(libIndexCache, hashPrefix);
        if (!libDir.exists()) {
            libDir.mkdirs();
        }
        File libFile = new File(libDir, hashSuffix + ".jar");
        InputStream is = sourceFatJar.getInputStream(entry);

        try (OutputStream libJar = new FileOutputStream(libFile)) {
            copyStream(is, libJar);
        } finally {
            is.close();
        }
    }

    private void writeEntry(InputStream is, ZipOutputStream zos, String entryName) throws IOException {
        try {
            zos.putNextEntry(new ZipEntry(entryName));
            copyStream(is, zos);
        } finally {
            zos.closeEntry();
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        int read = -1;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
    }

    private void addLibIndexFileToThinJar(JarOutputStream thinJar) throws IOException {
        thinJar.putNextEntry(new ZipEntry(SpringConstants.SPRING_LIB_INDEX_FILE));
        try {
            for (String libEntry : libEntries) {
                thinJar.write(libEntry.getBytes(StandardCharsets.UTF_8));
                thinJar.write('\n');
            }
        } finally {
            thinJar.closeEntry();
        }
    }
}
