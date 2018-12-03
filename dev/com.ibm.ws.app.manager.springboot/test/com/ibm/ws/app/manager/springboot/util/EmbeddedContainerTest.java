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

import static com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil.getArtifactId;
import static com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil.getStarterFilter;
import static com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil.stringStream;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil.EmbeddedContainer;
import com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil.StarterFilter;

/**
 * Verify the Liberty SpringBoot runtime excludes, from thinned applications and library caches,
 * any starter artifact dependencies that support embedded containers, such as Tomcat, Jetty, or Undertow.
 */
@SuppressWarnings("serial")
public class EmbeddedContainerTest {

    public static class TestThinUtil extends SpringBootThinUtil {
        TestThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache) throws Exception {
            super(sourceFatJar, targetThinJar, libIndexCache);
        }
    }

    public final static String fs = java.io.File.separator;

    @Rule
    public final TemporaryFolder workingArea = new TemporaryFolder();

    @Test
    public void testGetArtifactId() throws Exception {
        String expected;

        expected = "spring-boot-starter-undertow_spec";
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("SPRING-BOOT-starter-undertow_spec-1.0.SNAPSHOT.jar")));
        expected = "spring-boot-starter-undertow";
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("/BOOT-INF/INF/spring-boot-starter-undertow-2.0.1.RELEASE.jar")));
        expected = ""; // bad version
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("/BOOT-INF/INF/springbootstarterundertow2.0.1.RELEASE.jar")));
        expected = ""; // not jar
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("springbootstarterundertow2.0.1.RELEASE")));
        expected = ""; // empty string
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("")));
        expected = "a"; // no dirPath, smallest artifact
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("a-2.jar")));
        expected = "b"; // dirPath, smallest artifact
        assertTrue("Artifact name is " + expected, expected.equals(getArtifactId("/b-c.jar")));
    }

    @Test
    public void testGetStarterArtifactIdss() throws Exception {
        Set<String> expected;
        int expectedSize;

        Set<String> tomcat15StarterArtifactIds = new HashSet<String>() {
            {
                add("spring-boot-starter-tomcat");
                add("tomcat-embed-websocket");
                add("tomcat-embed-el");
                add("tomcat-annotations-api");
                add("tomcat-embed-core");
            }
        };
        // clone the unmodifiable result
        expected = new HashSet<String>();
        expected.addAll(EmbeddedContainer.getStarterArtifactIds("spring-boot-starter-tomcat-1.5"));
        expectedSize = expected.size();
        tomcat15StarterArtifactIds.forEach(artifactId -> expected.remove(artifactId));

        assertTrue("The exact set of Maven starter dependencies loaded",
                   expected.isEmpty() && expectedSize == tomcat15StarterArtifactIds.size());
    }

    @Test
    public void testStarterFilterGetAndApply() throws Exception {
        JarFile sourceFatJar;
        StarterFilter filter;
        String starterJarName;

        sourceFatJar = new JarFile(getUndertowStarter20AppJar());
        starterJarName = springBoot20UndertowStarterJars.get(random0max(springBoot20UndertowStarterJars.size() - 1));
        filter = getStarterFilter(sourceFatJar);
        assertTrue("JarName " + starterJarName + " is a SB 2.0 UNDERTOW starter artifact, apply() is true", filter.apply(starterJarName));
        sourceFatJar.close();

        sourceFatJar = new JarFile(getJettyStarter20AppJar());
        starterJarName = springBoot20JettyStarterJars.get(random0max(springBoot20JettyStarterJars.size() - 1));
        filter = getStarterFilter(sourceFatJar);
        assertTrue("JarName " + starterJarName + " is a SB 2.0 JETTY starter artifact, apply() is true", filter.apply(starterJarName));
        sourceFatJar.close();

        sourceFatJar = new JarFile(getWebStarter20AppJar());
        starterJarName = springBoot20TomcatStarterJars.get(random0max(springBoot20TomcatStarterJars.size() - 1));
        filter = getStarterFilter(sourceFatJar);
        assertTrue("JarName " + starterJarName + " is a SB 2.0 TOMCAT starter artifact, apply() is true", filter.apply(starterJarName));
        sourceFatJar.close();

        sourceFatJar = new JarFile(getNettyStarter20AppJar());
        starterJarName = springBoot20NettyStarterJars.get(random0max(springBoot20NettyStarterJars.size() - 1));
        filter = getStarterFilter(sourceFatJar);
        assertTrue("JarName " + starterJarName + " is a SB 2.0 NETTY starter artifact, apply() is true", filter.apply(starterJarName));
        sourceFatJar.close();

        starterJarName = springBoot20JettyStarterJars.get(0); // item 0 is always the root starter jar
        assertFalse("JarName " + starterJarName + "is NOT a TOMCAT 2.0 starter artifact, apply() is false", filter.apply(starterJarName));
    }

    int random0max(int max) {
        return ThreadLocalRandom.current().nextInt(0, max + 1);
    };

    @Test
    public void testThinnedWebStarterAppLacksTomcatStarter() throws Exception {

        File fatAppJar = getWebStarter20AppJar();
        File thinAppJar = workingArea.newFile("starterThinJar.jar");
        File appLibsDir = workingArea.newFolder("starterAppLibs");

        try (SpringBootThinUtil util = new TestThinUtil(fatAppJar, thinAppJar, appLibsDir)) {
            util.execute(); // Indirectly exercises SpringBootThinUtil.getStarterFilter() and
                            // StarterFilter.apply()
        }

        verifyJarLacksArtifacts(thinAppJar, springBoot20TomcatStarterJars);
        verifyDirLacksArtifacts(appLibsDir, springBoot20TomcatStarterJars);
    }

    public void verifyJarLacksArtifacts(File jarFile, List<String> artifacts) throws IOException {
        final JarFile jf;
        Stream<String> entries = stringStream((jf = new JarFile(jarFile)));
        entries.forEach(entry -> {
            for (String artifact : artifacts) {
                assertFalse("Unexpected artifact in jar: " + entry, entry.contains(artifact));
            }
        });
        jf.close();
    }

    public void verifyDirLacksArtifacts(File directory, List<String> artifacts) throws IOException {
        int dirPathLen = directory.getAbsolutePath().length();
        List<String> actualPaths = new ArrayList<String>();
        Files.walk(directory.toPath()) //
                        .filter((p) -> p.toFile().getAbsolutePath().length() > dirPathLen) //
                        .map((p) -> {
                            File f = p.toFile();
                            String actualPath = f.getAbsolutePath().substring(dirPathLen + 1);
                            if (f.isDirectory()) {
                                actualPath += fs;
                            }
                            return actualPath;
                        }) //
                        .collect(Collectors.toCollection(() -> actualPaths));

        for (String actualPath : actualPaths) {
            for (String artifact : artifacts) {
                assertFalse("Unexpected artifact found in directory: " + actualPath, actualPath.contains(artifact));
            }
        }
    }

    File webStarter20AppJar; // tomcat starter, fyi
    File jettyStarter20AppJar;
    File undertowStarter20AppJar;
    File nettyStarter20AppJar;

    File getWebStarter20AppJar() throws Exception {
        if (webStarter20AppJar == null) {
            List<String> starterJarEntryPaths = convertToJarEntryPaths("BOOT-INF/lib/", springBoot20TomcatStarterJars);
            List<String> entryPaths = merge(springBoot20WebAppMinusTomcatJarEntryPaths, starterJarEntryPaths);
            Manifest manifest = createManifest(springBoot20WebAppManifestContent);
            webStarter20AppJar = createSourceFatJar(entryPaths, manifest);
        }
        return webStarter20AppJar;
    }

    File getJettyStarter20AppJar() throws Exception {
        if (jettyStarter20AppJar == null) {
            List<String> starterJarEntryPaths = convertToJarEntryPaths("BOOT-INF/lib/", springBoot20JettyStarterJars);
            List<String> entryPaths = merge(springBoot20WebAppMinusTomcatJarEntryPaths, starterJarEntryPaths);
            Manifest manifest = createManifest(springBoot20WebAppManifestContent);
            jettyStarter20AppJar = createSourceFatJar(entryPaths, manifest);
        }
        return jettyStarter20AppJar;
    }

    File getUndertowStarter20AppJar() throws Exception {
        if (undertowStarter20AppJar == null) {
            List<String> starterJarEntryPaths = convertToJarEntryPaths("BOOT-INF/lib/", springBoot20UndertowStarterJars);
            List<String> entryPaths = merge(springBoot20WebAppMinusTomcatJarEntryPaths, starterJarEntryPaths);
            Manifest manifest = createManifest(springBoot20WebAppManifestContent);
            undertowStarter20AppJar = createSourceFatJar(entryPaths, manifest);
        }
        return undertowStarter20AppJar;
    }

    File getNettyStarter20AppJar() throws Exception {
        if (nettyStarter20AppJar == null) {
            List<String> starterJarEntryPaths = convertToJarEntryPaths("BOOT-INF/lib/", springBoot20NettyStarterJars);
            List<String> entryPaths = merge(springBoot20WebAppMinusTomcatJarEntryPaths, starterJarEntryPaths);
            Manifest manifest = createManifest(springBoot20WebAppManifestContent);
            nettyStarter20AppJar = createSourceFatJar(entryPaths, manifest);
        }
        return nettyStarter20AppJar;
    }

    public static List<String> convertToJarEntryPaths(String path, Collection<String> artifacts) {
        List<String> newList = new ArrayList<String>(artifacts.size());
        for (String artifact : artifacts)
            newList.add(path + artifact);
        return newList;
    }

    public static <T> List<T> merge(List<T> l1, List<T>... ln) {
        List<T> newList = new ArrayList<T>(l1);
        for (List<T> l : ln)
            newList.addAll(l);
        return newList;
    }

    public static Manifest createManifest(String content) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes("UTF8"));
        return new Manifest(bais);
    }

    public File createSourceFatJar(List<String> filePaths, Manifest manifest) throws Exception {
        File fatJar = workingArea.newFile("fat.jar");

        JarOutputStream fatJarStream = new JarOutputStream(new FileOutputStream(fatJar), manifest);
        byte i = 0, j = 0;
        for (String filePath : filePaths) {
            ZipEntry ze = new ZipEntry(filePath);
            fatJarStream.putNextEntry(ze);
            if (!filePath.endsWith(fs)) {
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

    static <T> Set<T> union(Set<T> s1, Set<T>... sn) {
        Set<T> newSet = new HashSet<T>(s1);
        for (Set<T> s : sn)
            newSet.addAll(s); // union when collection is Set
        return newSet;
    }

    public static Set<String> getKeysByValue(Map<String, String> map, String value) {
        Set<String> keys = new HashSet<String>();
        for (Entry<String, String> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    final static String springBoot20WebAppManifestContent = "Manifest-Version: 1.0\n" +
                                                            "Implementation-Title: gs-spring-boot\n" +
                                                            "Implementation-Version: 0.1.0\n" +
                                                            "Built-By: Heavy D\n" +
                                                            "Implementation-Vendor-Id: org.springframework\n" +
                                                            "Spring-Boot-Version: 2.0.1.RELEASE\n" +
                                                            "Main-Class: org.springframework.boot.loader.JarLauncher\n" +
                                                            "Start-Class: hello.Application\n" +
                                                            "Spring-Boot-Classes: BOOT-INF/classes/\n" +
                                                            "Spring-Boot-Lib: BOOT-INF/lib/\n" +
                                                            "Created-By: Apache Maven 3.5.3\n" +
                                                            "Build-Jdk: 1.8.0_151\n" +
                                                            "Implementation-URL: https://projects.spring.io/spring-boot/#/spring-bo\n" +
                                                            " ot-starter-parent/gs-spring-boot\n";

    final static List<String> springBoot20WebAppMinusTomcatJarEntryPaths = Arrays.asList(
                                                                                         "BOOT-INF/",
                                                                                         "BOOT-INF/classes/",
                                                                                         "BOOT-INF/classes/hello/",
                                                                                         "BOOT-INF/classes/hello/Application.class",
                                                                                         "BOOT-INF/classes/hello/Greeter.class",
                                                                                         "BOOT-INF/classes/hello/HelloController.class",
                                                                                         "BOOT-INF/classes/hello/HelloWorld.class",
                                                                                         "META-INF/maven/",
                                                                                         "META-INF/maven/org.springframework/",
                                                                                         "META-INF/maven/org.springframework/gs-spring-boot/",
                                                                                         "META-INF/maven/org.springframework/gs-spring-boot/pom.xml",
                                                                                         "META-INF/maven/org.springframework/gs-spring-boot/pom.properties",
                                                                                         "BOOT-INF/lib/",
                                                                                         "BOOT-INF/lib/spring-boot-starter-web-1.5.10.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-boot-starter-1.5.10.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-boot-1.5.10.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-boot-autoconfigure-1.5.10.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-boot-starter-logging-1.5.10.RELEASE.jar",
                                                                                         "BOOT-INF/lib/logback-classic-1.1.11.jar",
                                                                                         "BOOT-INF/lib/logback-core-1.1.11.jar",
                                                                                         "BOOT-INF/lib/slf4j-api-1.7.25.jar",
                                                                                         "BOOT-INF/lib/jcl-over-slf4j-1.7.25.jar",
                                                                                         "BOOT-INF/lib/jul-to-slf4j-1.7.25.jar",
                                                                                         "BOOT-INF/lib/log4j-over-slf4j-1.7.25.jar",
                                                                                         "BOOT-INF/lib/spring-core-4.3.14.RELEASE.jar",
                                                                                         "BOOT-INF/lib/snakeyaml-1.17.jar",
                                                                                         "BOOT-INF/lib/hibernate-validator-5.3.6.Final.jar",
                                                                                         "BOOT-INF/lib/validation-api-1.1.0.Final.jar",
                                                                                         "BOOT-INF/lib/jboss-logging-3.3.1.Final.jar",
                                                                                         "BOOT-INF/lib/classmate-1.3.4.jar",
                                                                                         "BOOT-INF/lib/jackson-databind-2.8.10.jar",
                                                                                         "BOOT-INF/lib/jackson-annotations-2.8.0.jar",
                                                                                         "BOOT-INF/lib/jackson-core-2.8.10.jar",
                                                                                         "BOOT-INF/lib/spring-web-4.3.14.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-aop-4.3.14.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-beans-4.3.14.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-context-4.3.14.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-webmvc-4.3.14.RELEASE.jar",
                                                                                         "BOOT-INF/lib/spring-expression-4.3.14.RELEASE.jar",
                                                                                         "org/",
                                                                                         "org/springframework/",
                                                                                         "org/springframework/boot/",
                                                                                         "org/springframework/boot/loader/",
                                                                                         "org/springframework/boot/loader/LaunchedURLClassLoader$1.class",
                                                                                         "org/springframework/boot/loader/PropertiesLauncher$ArchiveEntryFilter.class",
                                                                                         "org/springframework/boot/loader/PropertiesLauncher$PrefixMatchingArchiveFilter.class",
                                                                                         "org/springframework/boot/loader/Launcher.class",
                                                                                         "org/springframework/boot/loader/ExecutableArchiveLauncher$1.class",
                                                                                         "org/springframework/boot/loader/jar/",
                                                                                         "org/springframework/boot/loader/jar/JarFile$1.class",
                                                                                         "org/springframework/boot/loader/jar/Handler.class",
                                                                                         "org/springframework/boot/loader/jar/JarEntry.class",
                                                                                         "org/springframework/boot/loader/jar/JarFile$3.class",
                                                                                         "org/springframework/boot/loader/jar/CentralDirectoryEndRecord.class",
                                                                                         "org/springframework/boot/loader/jar/CentralDirectoryVisitor.class",
                                                                                         "org/springframework/boot/loader/jar/JarFile$JarFileType.class",
                                                                                         "org/springframework/boot/loader/jar/JarFileEntries.class",
                                                                                         "org/springframework/boot/loader/jar/JarFile.class",
                                                                                         "org/springframework/boot/loader/jar/JarFileEntries$1.class",
                                                                                         "org/springframework/boot/loader/jar/JarURLConnection$1.class",
                                                                                         "org/springframework/boot/loader/jar/JarFile$2.class",
                                                                                         "org/springframework/boot/loader/jar/JarEntryFilter.class",
                                                                                         "org/springframework/boot/loader/jar/AsciiBytes.class",
                                                                                         "org/springframework/boot/loader/jar/CentralDirectoryParser.class",
                                                                                         "org/springframework/boot/loader/jar/Bytes.class",
                                                                                         "org/springframework/boot/loader/jar/ZipInflaterInputStream.class",
                                                                                         "org/springframework/boot/loader/jar/JarFileEntries$EntryIterator.class",
                                                                                         "org/springframework/boot/loader/jar/FileHeader.class",
                                                                                         "org/springframework/boot/loader/jar/JarURLConnection$JarEntryName.class",
                                                                                         "org/springframework/boot/loader/jar/JarURLConnection.class",
                                                                                         "org/springframework/boot/loader/jar/CentralDirectoryFileHeader.class",
                                                                                         "org/springframework/boot/loader/data/",
                                                                                         "org/springframework/boot/loader/data/ByteArrayRandomAccessData.class",
                                                                                         "org/springframework/boot/loader/data/RandomAccessDataFile$DataInputStream.class",
                                                                                         "org/springframework/boot/loader/data/RandomAccessDataFile$FilePool.class",
                                                                                         "org/springframework/boot/loader/data/RandomAccessData$ResourceAccess.class",
                                                                                         "org/springframework/boot/loader/data/RandomAccessDataFile.class",
                                                                                         "org/springframework/boot/loader/data/RandomAccessData.class",
                                                                                         "org/springframework/boot/loader/LaunchedURLClassLoader.class",
                                                                                         "org/springframework/boot/loader/JarLauncher.class",
                                                                                         "org/springframework/boot/loader/MainMethodRunner.class",
                                                                                         "org/springframework/boot/loader/PropertiesLauncher$1.class",
                                                                                         "org/springframework/boot/loader/ExecutableArchiveLauncher.class",
                                                                                         "org/springframework/boot/loader/WarLauncher.class",
                                                                                         "org/springframework/boot/loader/archive/",
                                                                                         "org/springframework/boot/loader/archive/JarFileArchive$EntryIterator.class",
                                                                                         "org/springframework/boot/loader/archive/ExplodedArchive$FileEntryIterator.class",
                                                                                         "org/springframework/boot/loader/archive/ExplodedArchive$FileEntry.class",
                                                                                         "org/springframework/boot/loader/archive/JarFileArchive$JarFileEntry.class",
                                                                                         "org/springframework/boot/loader/archive/Archive$Entry.class",
                                                                                         "org/springframework/boot/loader/archive/JarFileArchive.class",
                                                                                         "org/springframework/boot/loader/archive/ExplodedArchive.class",
                                                                                         "org/springframework/boot/loader/archive/Archive.class",
                                                                                         "org/springframework/boot/loader/archive/ExplodedArchive$FileEntryIterator$EntryComparator.class",
                                                                                         "org/springframework/boot/loader/archive/Archive$EntryFilter.class",
                                                                                         "org/springframework/boot/loader/archive/ExplodedArchive$1.class",
                                                                                         "org/springframework/boot/loader/PropertiesLauncher.class",
                                                                                         "org/springframework/boot/loader/util/",
                                                                                         "org/springframework/boot/loader/util/SystemPropertyUtils.class");

    private final static List<String> springBoot15TomcatStarterJars = Arrays.asList("spring-boot-starter-tomcat-1.5.10.RELEASE.jar",
                                                                                    "tomcat-annotations-api-8.5.27.jar",
                                                                                    "tomcat-embed-core-8.5.27.jar",
                                                                                    "tomcat-embed-el-8.5.27.jar",
                                                                                    "tomcat-embed-websocket-8.5.27.jar");

    private final static List<String> springBoot20TomcatStarterJars = Arrays.asList("spring-boot-starter-tomcat-2.0.1.RELEASE.jar",
                                                                                    "tomcat-embed-core-8.5.29.jar",
                                                                                    "tomcat-embed-el-8.5.29.jar",
                                                                                    "tomcat-embed-websocket-8.5.29.jar");

    private final static List<String> springBoot15UndertowStarterJars = Arrays.asList("spring-boot-starter-undertow-1.5.10.RELEASE.jar",
                                                                                      "javax.el-3.0.0.jar",
                                                                                      "javax.servlet-api-3.1.0.jar",
                                                                                      "jboss-annotations-api_1.2_spec-1.0.0.Final.jar",
                                                                                      "jboss-websocket-api_1.1_spec-1.1.0.Final.jar",
                                                                                      "undertow-core-1.4.22.Final.jar",
                                                                                      "undertow-servlet-1.4.22.Final.jar",
                                                                                      "undertow-websockets-jsr-1.4.22.Final.jar",
                                                                                      "xnio-api-3.3.8.Final.jar",
                                                                                      "xnio-nio-3.3.8.Final.jar");

    private final static List<String> springBoot20UndertowStarterJars = Arrays.asList("spring-boot-starter-undertow-2.0.1.RELEASE.jar",
                                                                                      "javax.el-3.0.0.jar",
                                                                                      "javax.servlet-api-3.1.0.jar",
                                                                                      "jboss-annotations-api_1.2_spec-1.0.2.Final.jar",
                                                                                      "jboss-websocket-api_1.1_spec-1.1.3.Final.jar",
                                                                                      "undertow-core-1.4.23.Final.jar",
                                                                                      "undertow-servlet-1.4.23.Final.jar",
                                                                                      "undertow-websockets-jsr-1.4.23.Final.jar",
                                                                                      "xnio-api-3.3.8.Final.jar",
                                                                                      "xnio-nio-3.3.8.Final.jar");

    private final static List<String> springBoot15JettyStarterJars = Arrays.asList("spring-boot-starter-jetty-1.5.10.RELEASE.jar",
                                                                                   "apache-el-8.0.33.jar",
                                                                                   "asm-6.0.jar",
                                                                                   "asm-commons-6.0.jar",
                                                                                   "asm-tree-6.0.jar",
                                                                                   "javax-websocket-client-impl-9.4.8.v20171121.jar",
                                                                                   "javax-websocket-server-impl-9.4.8.v20171121.jar",
                                                                                   "javax.annotation-api-1.2.jar",
                                                                                   "javax.servlet-api-3.1.0.jar",
                                                                                   "javax.websocket-api-1.0.jar",
                                                                                   "jetty-annotations-9.4.8.v20171121.jar",
                                                                                   "jetty-client-9.4.8.v20171121.jar",
                                                                                   "jetty-continuation-9.4.8.v20171121.jar",
                                                                                   "jetty-http-9.4.8.v20171121.jar",
                                                                                   "jetty-io-9.4.8.v20171121.jar",
                                                                                   "jetty-plus-9.4.8.v20171121.jar",
                                                                                   "jetty-security-9.4.8.v20171121.jar",
                                                                                   "jetty-server-9.4.8.v20171121.jar",
                                                                                   "jetty-servlet-9.4.8.v20171121.jar",
                                                                                   "jetty-servlets-9.4.8.v20171121.jar",
                                                                                   "jetty-util-9.4.8.v20171121.jar",
                                                                                   "jetty-webapp-9.4.8.v20171121.jar",
                                                                                   "jetty-xml-9.4.8.v20171121.jar",
                                                                                   "websocket-api-9.4.8.v20171121.jar",
                                                                                   "websocket-client-9.4.8.v20171121.jar",
                                                                                   "websocket-common-9.4.8.v20171121.jar",
                                                                                   "websocket-server-9.4.8.v20171121.jar",
                                                                                   "websocket-servlet-9.4.8.v20171121.jar");

    private final static List<String> springBoot20JettyStarterJars = Arrays.asList("spring-boot-starter-jetty-2.0.1.RELEASE.jar",
                                                                                   "apache-el-8.5.24.2.jar",
                                                                                   "asm-6.0.jar",
                                                                                   "asm-commons-6.0.jar",
                                                                                   "asm-tree-6.0.jar",
                                                                                   "javax-websocket-client-impl-9.4.9.v20180320.jar",
                                                                                   "javax-websocket-server-impl-9.4.9.v20180320.jar",
                                                                                   "javax.servlet-api-3.1.0.jar",
                                                                                   "javax.websocket-api-1.0.jar",
                                                                                   "jetty-annotations-9.4.9.v20180320.jar",
                                                                                   "jetty-client-9.4.9.v20180320.jar",
                                                                                   "jetty-continuation-9.4.9.v20180320.jar",
                                                                                   "jetty-http-9.4.9.v20180320.jar",
                                                                                   "jetty-io-9.4.9.v20180320.jar",
                                                                                   "jetty-plus-9.4.9.v20180320.jar",
                                                                                   "jetty-security-9.4.9.v20180320.jar",
                                                                                   "jetty-server-9.4.9.v20180320.jar",
                                                                                   "jetty-servlet-9.4.9.v20180320.jar",
                                                                                   "jetty-servlets-9.4.9.v20180320.jar",
                                                                                   "jetty-util-9.4.9.v20180320.jar",
                                                                                   "jetty-webapp-9.4.9.v20180320.jar",
                                                                                   "jetty-xml-9.4.9.v20180320.jar",
                                                                                   "websocket-api-9.4.9.v20180320.jar",
                                                                                   "websocket-client-9.4.9.v20180320.jar",
                                                                                   "websocket-common-9.4.9.v20180320.jar",
                                                                                   "websocket-server-9.4.9.v20180320.jar",
                                                                                   "websocket-servlet-9.4.9.v20180320.jar");

    private final static List<String> springBoot20NettyStarterJars = Arrays.asList("spring-boot-starter-reactor-netty-2.0.1.RELEASE.jar");

}
