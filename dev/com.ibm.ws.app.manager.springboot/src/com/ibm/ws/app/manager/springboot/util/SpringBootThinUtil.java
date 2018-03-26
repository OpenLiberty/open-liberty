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

/**
 * A utility class for thinning an uber jar by separating application code in a separate jar
 * and libraries(dependencies) in a zip or a directory.
 */
public class SpringBootThinUtil {

    private final JarFile sourceFatJar;
    private final File targetThinJar;
    private final File libIndexCache;
    private final File libIndexCacheParent;
    private final boolean putLibCacheInDirectory;
    private final String springBootLibPath;
    private final List<String> libEntries = new ArrayList<>();
    private final Set<String> hashPrefixes = new HashSet<>();
    public static final String SPRING_LIB_INDEX_FILE = "META-INF/spring.lib.index";
    private static final String SPRING_BOOT_LOADER_CLASSPATH = "org/springframework/boot/loader/";

    public SpringBootThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache, boolean putLibCacheInDirectory) throws IOException {
        this(sourceFatJar, targetThinJar, libIndexCache, null, putLibCacheInDirectory);
    }

    public SpringBootThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache, File libIndexCacheParent, boolean putLibCacheInDirectory) throws IOException {
        this.sourceFatJar = new JarFile(sourceFatJar);
        this.targetThinJar = targetThinJar;
        this.libIndexCache = libIndexCache;
        this.libIndexCacheParent = libIndexCacheParent;
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

            Set<String> entryNames = new HashSet<>();
            for (Enumeration<JarEntry> entries = sourceFatJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (entryNames.add(entry.getName()) && !JarFile.MANIFEST_NAME.equals(entry.getName()) &&
                    !entry.getName().startsWith(SPRING_BOOT_LOADER_CLASSPATH) /* omit spring boot loader classes */) {
                    storeEntry(thinJar, libZip, entry);
                }
            }
            addLibIndexFileToThinJar(thinJar);

        }
    }

    private void storeEntry(JarOutputStream thinJar, ZipOutputStream libZip, JarEntry entry) throws IOException, NoSuchAlgorithmException {
        String path = entry.getName();
        // check if entry is dependency jar or application class
        if (entry.getName().startsWith(springBootLibPath) && !entry.getName().equals(springBootLibPath)) {

            String hash = hash(sourceFatJar, entry);
            String hashPrefix = hash.substring(0, 2);
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

    protected String hash(JarFile jf, ZipEntry entry) throws IOException, NoSuchAlgorithmException {
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
        // Note that for the in zip case we always store the library
        // without checking the parent.  This is because we want to
        // create a complete cache that may have no parent.
        String path = entry.getName();
        try (InputStream is = sourceFatJar.getInputStream(entry)) {
            //zip format require trailing '/' for directory entries
            String uniqueDirectoryPath = hashPrefix + '/' + hashSuffix + '/';
            if (!hashPrefixes.contains(uniqueDirectoryPath)) {
                if (!hashPrefixes.contains(hashPrefix + '/')) {
                    libZip.putNextEntry(new ZipEntry(hashPrefix + '/'));
                    libZip.closeEntry();
                    hashPrefixes.add(hashPrefix + '/');
                }
                libZip.putNextEntry(new ZipEntry(uniqueDirectoryPath));
                libZip.closeEntry();
                hashPrefixes.add(uniqueDirectoryPath);
            }
            path = hashPrefix + '/' + hashSuffix + entry.getName().substring(entry.getName().lastIndexOf('/'));
            writeEntry(is, libZip, path);
        }
    }

    private void storeLibraryInDir(JarEntry entry, String hashPrefix, String hashSuffix) throws IOException, NoSuchAlgorithmException {
        String hashPath = hashPrefix + '/' + hashSuffix;
        String libName = entry.getName();
        int lastSlash = libName.lastIndexOf('/');
        if (lastSlash >= 0) {
            libName = libName.substring(lastSlash + 1);
        }

        if (libIndexCacheParent != null) {
            // if there is a parent cache look to see if the lib name exists there
            File libDirParent = new File(libIndexCacheParent, hashPath);
            File libFileParent = new File(libDirParent, libName);
            if (libFileParent.exists()) {
                // no need to store since the lib exists in the parent cache
                return;
            }
        }

        if (!libIndexCache.exists()) {
            libIndexCache.mkdirs();
        }

        File libDir = new File(libIndexCache, hashPath);
        File libFile = new File(libDir, libName);
        if (!libFile.exists()) {
            if (!libDir.exists()) {
                libDir.mkdirs();
            }
            InputStream is = sourceFatJar.getInputStream(entry);
            try (OutputStream libJar = new FileOutputStream(libFile)) {
                copyStream(is, libJar);
            } finally {
                is.close();
            }
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
        thinJar.putNextEntry(new ZipEntry(SPRING_LIB_INDEX_FILE));
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
