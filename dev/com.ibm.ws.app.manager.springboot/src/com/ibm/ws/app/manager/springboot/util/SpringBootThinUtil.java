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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
    private final String springBootLibPath;
    private final String springBootLibProvidedPath;
    private final List<String> libEntries = new ArrayList<>();
    private final StarterFilter starterFilter;
    public static final String SPRING_LIB_INDEX_FILE = "META-INF/spring.lib.index";
    private static final String SPRING_BOOT_LOADER_CLASSPATH = "org/springframework/boot/loader/";

    public SpringBootThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache) throws IOException {
        this(sourceFatJar, targetThinJar, libIndexCache, null);
    }

    public SpringBootThinUtil(File sourceFatJar, File targetThinJar, File libIndexCache, File libIndexCacheParent) throws IOException {
        this.sourceFatJar = new JarFile(sourceFatJar);
        this.targetThinJar = targetThinJar;
        this.libIndexCache = libIndexCache;
        this.libIndexCacheParent = libIndexCacheParent;
        SpringBootManifest sbmf = new SpringBootManifest(this.sourceFatJar.getManifest());
        String springBootLibPath = sbmf.getSpringBootLib();
        if (!springBootLibPath.endsWith("/")) {
            springBootLibPath += "/";
        }
        this.springBootLibPath = springBootLibPath;
        String springBootLibProvidedPath = sbmf.getSpringBootLibProvided();
        if (springBootLibProvidedPath != null && !springBootLibProvidedPath.endsWith("/")) {
            springBootLibProvidedPath += "/";
        }
        this.springBootLibProvidedPath = springBootLibProvidedPath;
        this.starterFilter = getStarterFilter(this.sourceFatJar);
    }

    public void execute() throws IOException, NoSuchAlgorithmException {
        thin();
    }

    private void thin() throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        try (JarOutputStream thinJar = new JarOutputStream(new FileOutputStream(targetThinJar), sourceFatJar.getManifest())) {
            Set<String> entryNames = new HashSet<>();
            for (Enumeration<JarEntry> entries = sourceFatJar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (entryNames.add(entry.getName()) && !JarFile.MANIFEST_NAME.equals(entry.getName()) &&
                    !entry.getName().startsWith(SPRING_BOOT_LOADER_CLASSPATH) /* omit spring boot loader classes */) {
                    storeEntry(thinJar, entry);
                }
            }
            addLibIndexFileToThinJar(thinJar);

        }
    }

    private void storeEntry(JarOutputStream thinJar, JarEntry entry) throws IOException, NoSuchAlgorithmException {
        String path = entry.getName();
        boolean isLibPath = isFromLibPath(path);
        boolean isLibProvidedPath = isFromLibProvidedPath(path);
        // check if entry is dependency jar or application class
        if (isLibPath || isLibProvidedPath) {
            if (!starterFilter.apply(entry.getName()) && (!isLibProvidedPath || includeLibProvidedPaths())) {
                String hash = hash(sourceFatJar, entry);
                String hashPrefix = hash.substring(0, 2);
                String hashSuffix = hash.substring(2, hash.length());

                storeLibraryInDir(entry, hashPrefix, hashSuffix);

                String libLine = "/" + path + '=' + hash;
                libEntries.add(libLine);
            }
        } else {
            try (InputStream is = sourceFatJar.getInputStream(entry)) {
                writeEntry(is, thinJar, path);
            }
        }
    }

    private boolean includeLibProvidedPaths() {
        // Always return false for now.
        // May add option to include lib provided paths in the future ... but not now
        return false;
    }

    boolean isFromLibPath(String entryName) {
        return entryName.startsWith(springBootLibPath) && !entryName.endsWith("/");
    }

    boolean isFromLibProvidedPath(String entryName) {
        if (springBootLibProvidedPath != null) {
            return entryName.startsWith(springBootLibProvidedPath) && !entryName.endsWith("/");
        }
        return false;
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

    public static StarterFilter getStarterFilter(JarFile jarFile) {
        return getStarterFilter(stringStream(jarFile));
    }

    public static Stream<String> stringStream(JarFile jarFile) {
        Stream<String> stream = StreamSupport.stream(jarFile.stream().spliterator(), false).map(entry -> entry.getName());
        return stream;
    }

    public static StarterFilter getStarterFilter(Stream<String> entries) {
        final AtomicReference<String> starterRef = new AtomicReference<String>();
        entries.forEach(entry -> {
            if (starterRef.get() == null) {
                String path = entry;
                for (String starterJarNamePrefix : EmbeddedContainer.getSupportedStarters()) {
                    if (path.contains(starterJarNamePrefix)) {
                        starterRef.set(starterJarNamePrefix);
                        break;
                    }
                }
            }
        });
        String springBootStarter = (starterRef.get() != null) ? starterRef.get() : THE_UNKNOWN_STARTER;
        Set<String> starterArtifactIds = EmbeddedContainer.getStarterArtifactIds(springBootStarter);
        return new StarterFilter(springBootStarter, starterArtifactIds);
    }

    private static String THE_UNKNOWN_STARTER = "";
    private static final Set<String> emptySet = new HashSet<String>(0);

    public static class StarterFilter implements Function<String, Boolean> {
        final String starterName;
        final Set<String> starterArtifactIds;

        public StarterFilter(String starterName, Set<String> starterArtifactIds) {
            this.starterName = starterName;
            this.starterArtifactIds = starterArtifactIds;
        }

        @Override
        public Boolean apply(String jarName) {
            // return true iff jarName is a starter artifact
            return starterArtifactIds.contains(getArtifactId(jarName));
        }
    }

    public static String getArtifactId(String jarName) {
        // jarName :: [<dirPath>/]<artifactId>-<version>.jar
        int idxBegAid = jarName.lastIndexOf('/') + 1;
        int idxEndAid = jarName.lastIndexOf('-') - 1;
        return ((idxBegAid <= idxEndAid) && jarName.endsWith(".jar")) ? jarName.substring(idxBegAid, idxEndAid + 1).toLowerCase() : "";
    }

    /**
     *
     */
    static class EmbeddedContainer {

        public static Set<String> getSupportedStarters() {
            return getStartersToDependentArtifactIdsMap().keySet();
        }

        public static Set<String> getStarterArtifactIds(String starter) {
            Set<String> starterArtifactIds = getStartersToDependentArtifactIdsMap().getOrDefault(starter, null);
            if (null == starterArtifactIds) {
                return emptySet;
            }
            return starterArtifactIds;
        }

        // For now mvn dependencies for embedded container starters are provided here.
        private final static List<String> mvnSpringBoot15TomcatStarterDeps = Arrays.asList(
                                                                                           "org.springframework.boot:spring-boot-starter-tomcat:jar:1.5.10.RELEASE:compile",
                                                                                           "org.apache.tomcat.embed:tomcat-embed-websocket:jar:8.5.27:compile",
                                                                                           "org.apache.tomcat.embed:tomcat-embed-el:jar:8.5.27:compile",
                                                                                           "org.apache.tomcat:tomcat-annotations-api:jar:8.5.27:compile",
                                                                                           "org.apache.tomcat.embed:tomcat-embed-core:jar:8.5.27:compile");
        private final static List<String> mvnSpringBoot20TomcatStarterDeps = Arrays.asList(
                                                                                           "org.springframework.boot:spring-boot-starter-tomcat:jar:2.0.1.RELEASE:compile",
                                                                                           "javax.annotation:javax.annotation-api:jar:1.3.2:compile",
                                                                                           "org.apache.tomcat.embed:tomcat-embed-core:jar:8.5.29:compile",
                                                                                           "org.apache.tomcat.embed:tomcat-embed-el:jar:8.5.29:compile",
                                                                                           "org.apache.tomcat.embed:tomcat-embed-websocket:jar:8.5.29:compile");
        private final static List<String> mvnSpringBoot15JettyStarterDeps = Arrays.asList(
                                                                                          "org.springframework.boot:spring-boot-starter-jetty:jar:1.5.10.RELEASE:compile",
                                                                                          "org.eclipse.jetty:jetty-xml:jar:9.4.8.v20171121:compile",
                                                                                          "org.ow2.asm:asm-tree:jar:6.0:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-servlet:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty.websocket:javax-websocket-client-impl:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-server:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-continuation:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-io:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-security:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-servlet:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty.websocket:javax-websocket-server-impl:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-client:jar:9.4.8.v20171121:compile",
                                                                                          "javax.annotation:javax.annotation-api:jar:1.2:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-server:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-common:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-annotations:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-plus:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-util:jar:9.4.8.v20171121:compile",
                                                                                          "org.ow2.asm:asm-commons:jar:6.0:compile",
                                                                                          "javax.servlet:javax.servlet-api:jar:3.1.0:compile",
                                                                                          "org.eclipse.jetty:jetty-webapp:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-client:jar:9.4.8.v20171121:compile",
                                                                                          "javax.websocket:javax.websocket-api:jar:1.0:compile",
                                                                                          "org.mortbay.jasper:apache-el:jar:8.0.33:compile",
                                                                                          "org.eclipse.jetty:jetty-servlets:jar:9.4.8.v20171121:compile",
                                                                                          "org.eclipse.jetty:jetty-http:jar:9.4.8.v20171121:compile",
                                                                                          "org.ow2.asm:asm:jar:6.0:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-api:jar:9.4.8.v20171121:compile");
        private final static List<String> mvnSpringBoot20JettyStarterDeps = Arrays.asList(
                                                                                          "org.springframework.boot:spring-boot-starter-jetty:jar:2.0.1.RELEASE:compile",
                                                                                          "org.eclipse.jetty:jetty-servlets:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-continuation:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-http:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-util:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-io:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-webapp:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-xml:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-servlet:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-security:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-server:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-server:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-common:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-api:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-client:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-client:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty.websocket:websocket-servlet:jar:9.4.9.v20180320:compile",
                                                                                          "javax.servlet:javax.servlet-api:jar:3.1.0:compile",
                                                                                          "org.eclipse.jetty.websocket:javax-websocket-server-impl:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-annotations:jar:9.4.9.v20180320:compile",
                                                                                          "org.eclipse.jetty:jetty-plus:jar:9.4.9.v20180320:compile",
                                                                                          "javax.annotation:javax.annotation-api:jar:1.3.2:compile",
                                                                                          "org.ow2.asm:asm:jar:6.0:compile",
                                                                                          "org.ow2.asm:asm-commons:jar:6.0:compile",
                                                                                          "org.ow2.asm:asm-tree:jar:6.0:compile",
                                                                                          "org.eclipse.jetty.websocket:javax-websocket-client-impl:jar:9.4.9.v20180320:compile",
                                                                                          "javax.websocket:javax.websocket-api:jar:1.0:compile",
                                                                                          "org.mortbay.jasper:apache-el:jar:8.5.24.2:compile");
        private final static List<String> mvnSpringBoot15UndertowStarterDeps = Arrays.asList(
                                                                                             "org.jboss.xnio:xnio-api:jar:3.3.8.Final:compile",
                                                                                             "org.jboss.logging:jboss-logging:jar:3.3.1.Final:compile",
                                                                                             "javax.servlet:javax.servlet-api:jar:3.1.0:compile",
                                                                                             "org.springframework.boot:spring-boot-starter-undertow:jar:1.5.10.RELEASE:compile",
                                                                                             "org.jboss.spec.javax.annotation:jboss-annotations-api_1.2_spec:jar:1.0.0.Final:compile",
                                                                                             "io.undertow:undertow-websockets-jsr:jar:1.4.22.Final:compile",
                                                                                             "org.glassfish:javax.el:jar:3.0.0:compile",
                                                                                             "org.jboss.spec.javax.websocket:jboss-websocket-api_1.1_spec:jar:1.1.0.Final:compile",
                                                                                             "io.undertow:undertow-core:jar:1.4.22.Final:compile",
                                                                                             "org.jboss.xnio:xnio-nio:jar:3.3.8.Final:runtime",
                                                                                             "io.undertow:undertow-servlet:jar:1.4.22.Final:compile");
        private final static List<String> mvnSpringBoot20UndertowStarterDeps = Arrays.asList(
                                                                                             "org.springframework.boot:spring-boot-starter-undertow:jar:2.0.1.RELEASE:compile",
                                                                                             "io.undertow:undertow-core:jar:1.4.23.Final:compile",
                                                                                             "org.jboss.logging:jboss-logging:jar:3.3.2.Final:compile",
                                                                                             "org.jboss.xnio:xnio-api:jar:3.3.8.Final:compile",
                                                                                             "org.jboss.xnio:xnio-nio:jar:3.3.8.Final:runtime",
                                                                                             "io.undertow:undertow-servlet:jar:1.4.23.Final:compile",
                                                                                             "org.jboss.spec.javax.annotation:jboss-annotations-api_1.2_spec:jar:1.0.2.Final:compile",
                                                                                             "io.undertow:undertow-websockets-jsr:jar:1.4.23.Final:compile",
                                                                                             "org.jboss.spec.javax.websocket:jboss-websocket-api_1.1_spec:jar:1.1.3.Final:compile",
                                                                                             "javax.servlet:javax.servlet-api:jar:3.1.0:compile",
                                                                                             "org.glassfish:javax.el:jar:3.0.0:compile");

        // NOTE that we leave netty itself on the classpath in order to allow WebClient to still be used.
        private final static List<String> mvnSpringBoot20NettyStarterDeps = Arrays.asList(
                                                                                          "org.springframework.boot:spring-boot-starter-reactor-netty:jar:2.0.1.RELEASE:compile");

        public static final String TOMCAT = "tomcat";
        public static final String JETTY = "jetty";
        public static final String UNDERTOW = "undertow";
        public static final String LIBERTY = "liberty";
        public static final String NETTY = "netty";

        public static final String SPRING_BOOT_STARTER = "spring-boot-starter";
        public static final String SPRING_BOOT_STARTER_REACTOR = "spring-boot-starter-reactor";

        public static Map<String, Set<String>> getStartersToDependentArtifactIdsMap() {
            return startersToDependentArtifactIdsMap;
        }

        @SuppressWarnings("serial")
        private static final Map<String, Set<String>> startersToDependentArtifactIdsMap;

        static {
            Map<String, Set<String>> theMap = new HashMap<String, Set<String>>(6);
            theMap.put(starterJarNamePrefix(TOMCAT, "1.5"), loadStarterMvnDeps(mvnSpringBoot15TomcatStarterDeps));
            theMap.put(starterJarNamePrefix(TOMCAT, "2.0"), loadStarterMvnDeps(mvnSpringBoot20TomcatStarterDeps));
            theMap.put(starterJarNamePrefix(JETTY, "1.5"), loadStarterMvnDeps(mvnSpringBoot15JettyStarterDeps));
            theMap.put(starterJarNamePrefix(JETTY, "2.0"), loadStarterMvnDeps(mvnSpringBoot20JettyStarterDeps));
            theMap.put(starterJarNamePrefix(UNDERTOW, "1.5"), loadStarterMvnDeps(mvnSpringBoot15UndertowStarterDeps));
            theMap.put(starterJarNamePrefix(UNDERTOW, "2.0"), loadStarterMvnDeps(mvnSpringBoot20UndertowStarterDeps));
            theMap.put(starterJarNamePrefix(NETTY, "2.0"), loadStarterMvnDeps(mvnSpringBoot20NettyStarterDeps));
            startersToDependentArtifactIdsMap = Collections.unmodifiableMap(theMap);
        }

        private static String starterJarNamePrefix(String embeddedContainer, String versionInfo) {
            // e.g. spring-boot-starter-jetty-2.0
            if (NETTY.equals(embeddedContainer)) {
                return SPRING_BOOT_STARTER_REACTOR + "-" + embeddedContainer + "-" + versionInfo;
            }
            return SPRING_BOOT_STARTER + "-" + embeddedContainer + "-" + versionInfo;
        }

        public static Set<String> loadStarterMvnDeps(List<String> mvnStarterDeps) {
            // mvnDep :: groupId:artifactId:version:scope
            Set<String> starterArtifactIds = new HashSet<String>();
            mvnStarterDeps.forEach(mvnDep -> starterArtifactIds.add(mvnDep.split(":")[1].toLowerCase()));
            return Collections.unmodifiableSet(starterArtifactIds);
        }

    };

} // EmbeddedContainers
