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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 */
public class LibIndexCreateTest {

    private final static String sep = java.io.File.separator;

    public static class TestThinUtil extends SpringBootThinUtil {

        TestThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache) throws Exception {
            super(sourceFatJar, targetThinJar, libIndexCache);
        }

        public Stack<String> hashValues;

        @Override
        protected String hash(JarFile jf, ZipEntry entry) throws IOException, NoSuchAlgorithmException {
            return hashValues.pop();
        }
    }

    @Rule
    public final TemporaryFolder workingArea = new TemporaryFolder();

    @Test
    public void testWriteToOutputDir() throws Exception {

        File thinJar = workingArea.newFile("thinJar.jar");
        File applicationLibs = workingArea.newFolder("AppLibs");

        Stack<String> hashes = new Stack<String>();
        for (String s : Arrays.asList("aa003", "aa002", "aa001")) {
            hashes.push(s);
        }
        try (SpringBootThinUtil util = new TestThinUtil(createSourceFatJar(null, null), thinJar, applicationLibs)) {
            ((TestThinUtil) util).hashValues = hashes;
            util.execute();
        }
        //verify thin jar contents;
        HashSet<String> expectedThinJarContents = new HashSet<String>() {
            {
                //we stripped out the spring bootloader classes but the directory entries will remain
                add("org/");
                add("org/springframework/");
                add("org/springframework/boot/");

                add("META-INF/MANIFEST.MF");
                add("BOOT-INF/");
                add("BOOT-INF/classes/");
                add("BOOT-INF/classes/org/");
                add("BOOT-INF/classes/org/petclinic/");
                add("BOOT-INF/classes/org/petclinic/Vets.class");
                add("BOOT-INF/classes/org/petclinic/PetValidator.class");
                add("BOOT-INF/classes/org/petclinic/OwnerController.class");
                add("BOOT-INF/lib/");
                add("META-INF/spring.lib.index");
            }
        };
        verifyJarEntryPaths(thinJar, expectedThinJarContents);

        //verify dependencies jar contents
        HashSet<String> expectedDepsZipContents = new HashSet<String>() {
            {
                add("aa" + sep + "");
                add("aa" + sep + "001" + sep + "");
                add("aa" + sep + "001" + sep + "jboss-logging-3.3.2.Final.jar");
                add("aa" + sep + "002" + sep + "");
                add("aa" + sep + "002" + sep + "hibernate-jpa-2.1-api-1.0.0.Final.jar");
                add("aa" + sep + "003" + sep + "");
                add("aa" + sep + "003" + sep + "hibernate-commons-annotations-5.0.1.Final.jar");
            }
        };
        verifyDirEntryPaths(applicationLibs, expectedDepsZipContents);

    }

    @Test
    public void testMultLibJarsWithSameContents() throws Exception {
        File thinJar = workingArea.newFile("thinJar.jar");
        File applicationLibsDir = workingArea.newFolder("AppLibs");

        Stack<String> hashes = new Stack<String>();
        for (String s : Arrays.asList("bb001", "aa001", "aa001")) {
            hashes.push(s);
        }
        try (SpringBootThinUtil util = new TestThinUtil(createSourceFatJar(null, null), thinJar, applicationLibsDir)) {
            ((TestThinUtil) util).hashValues = hashes;
            util.execute();
        }
        //verify thin jar contents;
        HashSet<String> expectedThinJarContents = new HashSet<String>() {
            {
                //we stripped out the spring bootloader classes but the directory entries will remain
                add("org/");
                add("org/springframework/");
                add("org/springframework/boot/");

                add("META-INF/MANIFEST.MF");
                add("BOOT-INF/");
                add("BOOT-INF/classes/");
                add("BOOT-INF/classes/org/");
                add("BOOT-INF/classes/org/petclinic/");
                add("BOOT-INF/classes/org/petclinic/Vets.class");
                add("BOOT-INF/classes/org/petclinic/PetValidator.class");
                add("BOOT-INF/classes/org/petclinic/OwnerController.class");
                add("BOOT-INF/lib/");
                add("META-INF/spring.lib.index");
            }
        };
        verifyJarEntryPaths(thinJar, expectedThinJarContents);

        //verify dependencies jar contents
        HashSet<String> expectedDepsZipContents = new HashSet<String>() {
            {
                add("aa" + sep + "");
                add("aa" + sep + "001" + sep + "");
                add("aa" + sep + "001" + sep + "jboss-logging-3.3.2.Final.jar");
                add("aa" + sep + "001" + sep + "hibernate-jpa-2.1-api-1.0.0.Final.jar");
                add("bb" + sep + "");
                add("bb" + sep + "001" + sep + "");
                add("bb" + sep + "001" + sep + "hibernate-commons-annotations-5.0.1.Final.jar");
            }
        };
        verifyDirEntryPaths(applicationLibsDir, expectedDepsZipContents);

    }

    private File createSourceFatJar(List<String> filePaths, Manifest manifest) throws Exception {

        if (filePaths == null) {
            filePaths = Arrays.asList("org/",
                                      "org/springframework/",
                                      "org/springframework/boot/",
                                      "org/springframework/boot/loader/",
                                      "org/springframework/boot/loader/Archive",
                                      "org/springframework/boot/loader/Archive$Entry.class",
                                      "BOOT-INF/",
                                      "BOOT-INF/classes/",
                                      "BOOT-INF/classes/org/",
                                      "BOOT-INF/classes/org/petclinic/",
                                      "BOOT-INF/classes/org/petclinic/Vets.class",
                                      "BOOT-INF/classes/org/petclinic/PetValidator.class",
                                      "BOOT-INF/classes/org/petclinic/OwnerController.class",
                                      "BOOT-INF/lib/",
                                      "BOOT-INF/lib/jboss-logging-3.3.2.Final.jar",
                                      "BOOT-INF/lib/hibernate-jpa-2.1-api-1.0.0.Final.jar",
                                      "BOOT-INF/lib/hibernate-commons-annotations-5.0.1.Final.jar");

        }
        if (manifest == null) {
            String manifestContents = "Manifest-Version: 1.0\n" +
                                      "Main-Class: org.springframework.boot.loader.JarLauncher\n" +
                                      "Start-Class: org.springframework.samples.petclinic.PetClinicApplicatio\n" +
                                      " n\n" +
                                      "Spring-Boot-Classes: BOOT-INF/classes/\n" +
                                      "Spring-Boot-Lib: BOOT-INF/lib/\n";
            ByteArrayInputStream bais = new ByteArrayInputStream(manifestContents.getBytes("UTF8"));
            manifest = new Manifest(bais);
        }

        File fatJar = workingArea.newFile("fat.jar");
        JarOutputStream fatJarStream = new JarOutputStream(new FileOutputStream(fatJar), manifest);
        byte i = 0, j = 0;
        for (String filePath : filePaths) {
            ZipEntry ze = new ZipEntry(filePath);
            fatJarStream.putNextEntry(ze);
            if (!filePath.endsWith(sep)) {
                if (filePath.endsWith(".jar")) {
                    byte[] libContent = createBootInfLibContent(manifest, j++);
                    try (InputStream is = new ByteArrayInputStream(libContent)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fatJarStream.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    // If this is an actual file entry write some data. The content is not
                    // relevant to the test. We only care about the structure of the zip file.
                    fatJarStream.write(new byte[] { 'H', 'e', 'l', 'l', 'o', i++ }, 0, 6);
                }
            }
        }
        fatJarStream.close();
        return fatJar;
    }

    private byte[] createBootInfLibContent(Manifest manifest, byte j) throws IOException, FileNotFoundException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JarOutputStream libJarStream = new JarOutputStream(out, manifest)) {
            libJarStream.putNextEntry(new ZipEntry("hello.txt"));
            libJarStream.write(new byte[] { 'h', 'e', 'l', 'l', '0', j }, 0, 6);
        }
        return out.toByteArray();
    }

    //verify that a passed in jarfile contains EXACTLY the set of expected entries.
    public void verifyJarEntryPaths(File jarFile, Set<String> expectedEntries) throws IOException {
        Enumeration<? extends ZipEntry> entries = new ZipFile(jarFile).entries();
        ZipEntry zipEntry;
        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement();
            assertTrue("Unexpected path found in zip: " + zipEntry.toString(), expectedEntries.remove(zipEntry.toString()));
        }
        assertTrue("Missing " + expectedEntries.size() + " expected paths from jar.", expectedEntries.isEmpty());
    }

    public void verifyDirEntryPaths(File directory, Set<String> expectedEntries) throws IOException {
        int dirPathLen = directory.getAbsolutePath().length();
        List<String> actualPaths = new ArrayList<String>();
        Files.walk(directory.toPath()) //
                        .filter((p) -> p.toFile().getAbsolutePath().length() > dirPathLen) //
                        .map((p) -> {
                            File f = p.toFile();
                            String actualPath = f.getAbsolutePath().substring(dirPathLen + 1);
                            if (f.isDirectory()) {
                                actualPath += sep;
                            }
                            return actualPath;
                        }) //
                        .collect(Collectors.toCollection(() -> actualPaths));

        for (String actualPath : actualPaths) {
            assertTrue("Unexpected path found in directory: " + actualPath, expectedEntries.remove(actualPath));
        }
        assertTrue("Missing " + expectedEntries.size() + " expected paths from jar.", expectedEntries.isEmpty());
    }
}
