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
package com.ibm.ws.springboot.utility.tasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.ws.app.manager.springboot.internal.SpringConstants;
import com.ibm.ws.springboot.utility.IFileUtility;
import com.ibm.ws.springboot.utility.utils.ConsoleWrapper;
import com.ibm.ws.springboot.utility.utils.FileUtility;

/**
 *
 */
public class ThinAppTaskTest {
    @Rule
    public final TemporaryFolder workingArea = new TemporaryFolder();

    private static final String MYAPP_JAR = "myapp.jar";
    private static final String MYAPP_SPRING = "myapp." + SpringConstants.SPRING_APP_TYPE;

    private File FAT_APP_JAR;
    private File EXPECTED_DEFAULT_TARGET_THIN_APP_FILE;
    private File EXPECTED_DEFAULT_TARGET_LIB_INDEX_CACHE_DIR;
    private File EXPECTED_SET_TARGET_THIN_APP_FILE;
    private File EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR;
    private File EXPECTED_PARENT_LIB_INDEX_CACHE_DIR;
    private final Map<String, String> hashes = new HashMap<>();

    private Mockery mock;

    private ConsoleWrapper stdin;
    private PrintStream stdout;
    private PrintStream stderr;
    private IFileUtility fileUtil;
    private BaseCommandTask task;

    private static final List<String> FILE_PATHS = Arrays.asList("org/",
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

    private static final Set<String> LIB_PATHS = new HashSet<>(Arrays.asList("BOOT-INF/lib/jboss-logging-3.3.2.Final.jar",
                                                                             "BOOT-INF/lib/hibernate-jpa-2.1-api-1.0.0.Final.jar",
                                                                             "BOOT-INF/lib/hibernate-commons-annotations-5.0.1.Final.jar"));
    private static final Collection<String> THIN_PATHS = Arrays.asList("META-INF/MANIFEST.MF",
                                                                       "META-INF/spring.lib.index",
                                                                       "BOOT-INF/",
                                                                       "BOOT-INF/classes/",
                                                                       "BOOT-INF/classes/org/",
                                                                       "BOOT-INF/classes/org/petclinic/",
                                                                       "BOOT-INF/classes/org/petclinic/Vets.class",
                                                                       "BOOT-INF/classes/org/petclinic/PetValidator.class",
                                                                       "BOOT-INF/classes/org/petclinic/OwnerController.class",
                                                                       "BOOT-INF/lib/",
                                                                       "org/",
                                                                       "org/springframework/",
                                                                       "org/springframework/boot/");

    private File createSourceFatJar(File parentLibCache) throws Exception {
        hashes.clear();

        String manifestContents = "Manifest-Version: 1.0\n" +
                                  "Main-Class: org.springframework.boot.loader.JarLauncher\n" +
                                  "Start-Class: org.springframework.samples.petclinic.PetClinicApplicatio\n" +
                                  " n\n" +
                                  "Spring-Boot-Classes: BOOT-INF/classes/\n" +
                                  "Spring-Boot-Lib: BOOT-INF/lib/\n";
        ByteArrayInputStream bais = new ByteArrayInputStream(manifestContents.getBytes("UTF8"));
        Manifest manifest = new Manifest(bais);

        File fatJar = workingArea.newFile(MYAPP_JAR);
        JarOutputStream fatJarStream = new JarOutputStream(new FileOutputStream(fatJar), manifest);
        byte i = 0, j = 0;
        for (String filePath : FILE_PATHS) {
            ZipEntry ze = new ZipEntry(filePath);
            fatJarStream.putNextEntry(ze);
            if (!filePath.endsWith("/")) {
                if (LIB_PATHS.contains(filePath)) {
                    byte[] libContent = createBootInfLibContent(manifest, j++);
                    try (InputStream is = new ByteArrayInputStream(libContent)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fatJarStream.write(buffer, 0, bytesRead);
                        }
                    }
                    hashes.put(filePath, hash(libContent));
                    if (filePath.equals("BOOT-INF/lib/hibernate-jpa-2.1-api-1.0.0.Final.jar")) {
                        String hash = hashes.get(filePath);
                        String prefix = hash.substring(0, 2);
                        String suffix = hash.substring(2);
                        File libraryFile = new File(parentLibCache, prefix);
                        libraryFile = new File(libraryFile, suffix);
                        libraryFile = new File(libraryFile, "hibernate-jpa-2.1-api-1.0.0.Final.jar");
                        libraryFile.getParentFile().mkdirs();
                        Files.write(libraryFile.toPath(), libContent);
                    }
                } else {
                    //if this is an actual file entry write some data. The content is irrelevant
                    // to the test. We only care about the structure of the zip file.
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
            libJarStream.write(new byte[] { 'H', 'e', 'l', 'l', 'o', j }, 0, 6);
        }
        return out.toByteArray();
    }

    private static String hash(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("sha-256");
        digest.update(content, 0, content.length);
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

    @Before
    public void setUp() throws Exception {
        final File destination = new File(workingArea.getRoot(), "destination");
        destination.mkdirs();
        EXPECTED_PARENT_LIB_INDEX_CACHE_DIR = new File(destination, "parent.lib.index.cache");
        FAT_APP_JAR = createSourceFatJar(EXPECTED_PARENT_LIB_INDEX_CACHE_DIR);

        mock = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        stdin = mock.mock(ConsoleWrapper.class, "stdin");
        stdout = mock.mock(PrintStream.class, "stdout");
        stderr = mock.mock(PrintStream.class, "stderr");
        fileUtil = new FileUtility();

        task = new ThinAppTask(fileUtil, "myScript");

        EXPECTED_DEFAULT_TARGET_LIB_INDEX_CACHE_DIR = new File(FAT_APP_JAR.getParentFile(), SpringConstants.SPRING_LIB_CACHE_NAME);
        EXPECTED_DEFAULT_TARGET_LIB_INDEX_CACHE_DIR.mkdirs();
        EXPECTED_DEFAULT_TARGET_THIN_APP_FILE = new File(FAT_APP_JAR.getParentFile(), MYAPP_SPRING);

        EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR = new File(destination, "test.cache");
        EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR.mkdirs();
        EXPECTED_SET_TARGET_THIN_APP_FILE = new File(destination, "testThinApp.jar");

    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void getTaskHelp() {
        assertNotNull(task.getTaskHelp());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.ThinAppTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_noArguments() throws Exception {
        String[] args = new String[] { task.getTaskName() };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.ThinAppTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unDashknownArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "-unknown" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.ThinAppTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unknownArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--unknown" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.ThinAppTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unknownValue() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "unknown" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.ThinAppTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_noApplicationArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--targetThinAppPath=" + EXPECTED_SET_TARGET_THIN_APP_FILE.getCanonicalPath() };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.ThinAppTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_justRequiredFlagsNoValues() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Create the expectations for a successful execution of the task.
     *
     * @throws Exception
     */
    private void createSuccessfulStdoutExpectations(final boolean defaultCache, final boolean defaultThinApp) throws Exception {
        mock.checking(new Expectations() {
            {
                one(stdout).println("Creating a thin application from: " + FAT_APP_JAR.getCanonicalPath());
                File expectedCache = defaultCache ? EXPECTED_DEFAULT_TARGET_LIB_INDEX_CACHE_DIR : EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR;
                one(stdout).println("Library cache: " + expectedCache.getCanonicalPath());
                File expectedThin = defaultThinApp ? EXPECTED_DEFAULT_TARGET_THIN_APP_FILE : EXPECTED_SET_TARGET_THIN_APP_FILE;
                one(stdout).println("Thin application: " + expectedThin.getCanonicalPath());
            }
        });
    }

    @Test
    public void handleTask_allRequiredFlagsWithDefaults() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath() };

        createSuccessfulStdoutExpectations(true, true);
        task.handleTask(stdin, stdout, stderr, args);
        validateThinnedApp(EXPECTED_DEFAULT_TARGET_THIN_APP_FILE, EXPECTED_DEFAULT_TARGET_LIB_INDEX_CACHE_DIR);
    }

    @Test
    public void handleTask_allRequiredFlagsWithTargets() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath(),
                                       "--targetThinAppPath=" + EXPECTED_SET_TARGET_THIN_APP_FILE.getCanonicalPath(),
                                       "--targetLibCachePath=" + EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR.getCanonicalPath() };

        createSuccessfulStdoutExpectations(false, false);
        task.handleTask(stdin, stdout, stderr, args);
        validateThinnedApp(EXPECTED_SET_TARGET_THIN_APP_FILE, EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR);
    }

    @Test
    public void handleTask_withParentCache() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath(),
                                       "--targetThinAppPath=" + EXPECTED_SET_TARGET_THIN_APP_FILE.getCanonicalPath(),
                                       "--targetLibCachePath=" + EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR.getCanonicalPath(),
                                       "--parentLibCachePath=" + EXPECTED_PARENT_LIB_INDEX_CACHE_DIR.getCanonicalPath() };

        createSuccessfulStdoutExpectations(false, false);
        task.handleTask(stdin, stdout, stderr, args);
        validateThinnedApp(EXPECTED_SET_TARGET_THIN_APP_FILE, EXPECTED_SET_TARGET_LIB_INDEX_CACHE_DIR, "BOOT-INF/lib/hibernate-jpa-2.1-api-1.0.0.Final.jar");
    }

    private void validateThinnedApp(File thinApp, File libCache, String... expectMissing) throws IOException {
        assertTrue("Expected thin app does is not a file.", thinApp.isFile());
        assertTrue("Expected lib cache is not a directory.", libCache.isDirectory());
        Set<String> expectedThinPaths = new HashSet<>(THIN_PATHS);
        try (JarFile thinJar = new JarFile(thinApp)) {
            for (Enumeration<JarEntry> entries = thinJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!expectedThinPaths.remove(entry.getName())) {
                    fail("Thinned JAR contains unexpected entry: " + entry.getName());
                }
            }
        }
        if (!expectedThinPaths.isEmpty()) {
            fail("Thin application does not contain all expected paths: " + expectedThinPaths.toString());
        }

        for (Entry<String, String> expectedHash : hashes.entrySet()) {
            String libName = expectedHash.getKey();
            libName = libName.substring(libName.lastIndexOf('/') + 1);
            String hashPrefix = expectedHash.getValue().substring(0, 2);
            String hashSuffix = expectedHash.getValue().substring(2);
            File libFile = new File(libCache, hashPrefix);
            libFile = new File(libFile, hashSuffix);
            libFile = new File(libFile, libName);
            if (Arrays.binarySearch(expectMissing, expectedHash.getKey()) >= 0) {
                assertFalse("Expected library to not exist:" + libFile.getCanonicalPath(), libFile.isFile());
            } else {
                assertTrue("Expected library does not exist: " + libFile.getCanonicalPath(), libFile.isFile());
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void handleTask_extraValueFront() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "extraValue",
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath() };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        } ;

    }

    @Test(expected = IllegalArgumentException.class)
    public void handleTask_extraValueMiddle() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath(),
                                       "extraValue",
                                       "--targetThinAppPath=" + EXPECTED_SET_TARGET_THIN_APP_FILE.getCanonicalPath() };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        } ;
    }

    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unknownArgumentMiddle() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath(),
                                       "--unknown",
                                       "--targetThinAppPath=" + EXPECTED_SET_TARGET_THIN_APP_FILE.getCanonicalPath() };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        } ;
    }

    @Test
    public void handleTask_applicationDoesntExist() throws Exception {
        final String DOES_NOT_EXIST = "doesNotExist";
        final File DOES_NOT_EXIST_FILE = new File(workingArea.getRoot(), DOES_NOT_EXIST);
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + DOES_NOT_EXIST_FILE.getCanonicalPath() };

        mock.checking(new Expectations() {
            {
                one(stdout).println("Aborting application thin task:");
                one(stdout).println("Specified application could not be found at location " + DOES_NOT_EXIST_FILE.getCanonicalPath());
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    @Test
    public void handleTask_thinAppIsDirectory() throws Exception {
        final File isDirectory = workingArea.newFolder();
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath(),
                                       "--targetThinAppPath=" + isDirectory.getCanonicalPath() };

        mock.checking(new Expectations() {
            {
                one(stdout).println("Aborting application thin task:");
                one(stdout).println("Specified application target is a directory " + isDirectory.getCanonicalPath());
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    @Test
    public void handleTask_libCacheIsFile() throws Exception {
        final File isFile = workingArea.newFile();
        String[] args = new String[] { task.getTaskName(),
                                       "--sourceAppPath=" + FAT_APP_JAR.getCanonicalPath(),
                                       "--targetLibCachePath=" + isFile.getCanonicalPath() };

        mock.checking(new Expectations() {
            {
                one(stdout).println("Aborting application thin task:");
                one(stdout).println("Specified library cache target is a file " + isFile.getCanonicalPath());
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }
}
