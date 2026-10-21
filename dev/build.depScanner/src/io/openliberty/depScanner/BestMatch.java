/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.depScanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

public class BestMatch {

    /** Maximum number of Maven dependencies written to a single pom.xml file. */
    private static final int MAX_DEPS_PER_POM = 50;

    private static int pomFiles = 1;
    private static final Map<String, List<String>> depVersionMap = new HashMap<>();

    /**
     * Scans a WLP installation for third-party dependencies and writes Maven pom files.
     *
     * @param args[0] wlp directory to scan
     * @param args[1] output directory for generated files
     * @throws Exception if scanning or writing fails
     */
    public static void main(String[] args) throws Exception {
        Repository repo = new Repository(new File(System.getProperty("user.home"), ".ibmartifactory/repository"), false);
        Repository gradleRepo = findGradleCacheRepo();
        String wlpDir = args[0];
        String outputDir = args[1];
        LibertyInstall liberty = new LibertyInstall(new File(wlpDir));
        PrintStream modOut = new PrintStream(new File(outputDir + "/moduleMatches.txt"));
        PrintStream mpOut = new PrintStream(new File(outputDir + "/missingPackages.txt"));
        Set<Module> matched = new TreeSet<>();
        Set<String> uniqueMissingPackages = new HashSet<>();

        liberty.stream()
            .sorted(Comparator.comparing(Jar::getOriginalFile))
            .forEach(jar -> {
                List<Module> modules = repo.stream()
                    .map(moduleInfo -> {
                        List<Module> moduleInfoList = moduleInfo.getValue();
                        moduleInfoList.sort((o1, o2) -> o2.containsCount(jar) - o1.containsCount(jar));
                        return moduleInfoList.get(0);
                    })
                    .filter(jar::contains)
                    .collect(Collectors.toList());

                matched.addAll(modules);

                List<Module> gradleModules = gradleRepo.stream()
                    .map(gradleModuleInfo -> {
                        List<Module> gradleModuleInfoList = gradleModuleInfo.getValue();
                        gradleModuleInfoList.sort((o1, o2) -> o2.containsCount(jar) - o1.containsCount(jar));
                        return gradleModuleInfoList.get(0);
                    })
                    .filter(jar::contains)
                    .collect(Collectors.toList());

                matched.addAll(gradleModules);

                Set<String> matchedNames = new TreeSet<>();
                modules.stream()
                    .map(module -> "\t" + module + "\t" + jar.getPackages(module))
                    .sorted()
                    .forEach(matchedNames::add);
                gradleModules.stream()
                    .map(gradleModule -> "\t" + gradleModule + "\t" + jar.getPackages(gradleModule))
                    .sorted()
                    .forEach(matchedNames::add);

                List<String> foundPackages = modules.stream()
                    .flatMap(module -> jar.getPackages(module).stream())
                    .collect(Collectors.toList());
                List<String> foundGradlePackages = gradleModules.stream()
                    .flatMap(gradleModule -> jar.getPackages(gradleModule).stream())
                    .collect(Collectors.toList());

                Collection<String> missingPackages = jar.getPackages();
                missingPackages.removeAll(foundPackages);
                missingPackages.removeAll(foundGradlePackages);

                missingPackages = missingPackages.stream()
                    .filter(name -> !name.startsWith("com.ibm.ws"))
                    .filter(name -> !name.startsWith("com.ibm.websphere"))
                    .filter(name -> !name.startsWith("com.ibm.wsspi"))
                    .filter(name -> !name.startsWith("io.openliberty"))
                    .filter(name -> !name.startsWith("com.ibm.ejs"))
                    .filter(name -> !name.startsWith("com.ibm.json"))
                    .filter(name -> !name.startsWith("com.ibm.tx"))
                    .filter(name -> !name.startsWith("com.ibm.oauth"))
                    .filter(name -> !name.startsWith("com.ibm.jbatch"))
                    .filter(name -> !name.startsWith("javax.servlet.sip"))
                    .filter(name -> !name.startsWith("com.ibm.sip"))
                    .filter(name -> !name.startsWith("jain.protocol.ip.sip"))
                    .filter(name -> !name.startsWith("javax.batch"))
                    .filter(name -> !name.startsWith("com.ibm"))
                    .collect(Collectors.toList());

                uniqueMissingPackages.addAll(missingPackages);

                if (!matchedNames.isEmpty()) {
                    modOut.println(jar.getOriginalFile().getAbsolutePath());
                    matchedNames.forEach(modOut::println);
                }

                if (!missingPackages.isEmpty()) {
                    mpOut.println(jar.getOriginalFile().getAbsolutePath());
                    missingPackages.stream().map(name -> "\t" + name).forEach(mpOut::println);
                }
            });

        manageWSJars(matched, outputDir);
        writePom(matched, outputDir);
    }

    /**
     * Locates the Gradle file cache repository under the user's home directory.
     *
     * @return a {@link Repository} backed by the Gradle files cache
     */
    private static Repository findGradleCacheRepo() {
        File cacheFolder = new File(System.getProperty("user.home"), ".gradle/caches");

        String[] moduleFiles = cacheFolder.list((dir, name) -> name.startsWith("modules-"));
        File modulesFolder = (moduleFiles != null && moduleFiles.length > 0)
                ? new File(cacheFolder.getPath(), moduleFiles[0]) : null;
        if (modulesFolder == null) {
            System.err.println("No module found in user gradle repo [ " + cacheFolder.getAbsolutePath() + " ]");
        }

        String[] files = (modulesFolder != null) ? modulesFolder.list((dir, name) -> name.startsWith("files-")) : null;
        File filesFolder = (files != null && files.length > 0)
                ? new File(modulesFolder.getPath(), files[0]) : null;
        if (filesFolder == null) {
            System.err.println("No file found in gradle module folder [ "
                               + (modulesFolder != null ? modulesFolder.getAbsolutePath() : cacheFolder.getAbsolutePath()) + " ]");
        }

        return new Repository(filesFolder, true);
    }

    /**
     * Copies IBM WebSphere rebundled jars from the matched set into the wsJars output directory.
     *
     * @param matched   the full set of matched modules
     * @param outputDir the root output directory
     */
    private static void manageWSJars(Set<Module> matched, String outputDir) {
        new File(outputDir + "/wsJars").mkdirs();
        matched.stream()
            .filter(BestMatch::wsLibraries)
            .forEach(library -> manageLibrary(library, outputDir));
    }

    /**
     * Copies a single IBM WebSphere rebundled jar into the wsJars output directory.
     *
     * @param library   the module to copy
     * @param outputDir the root output directory
     */
    private static void manageLibrary(Module library, String outputDir) {
        // If the proper group name can be detected in the rebundled ibm ws jar, then we will use it for scanning purposes
        String fileName = library.getArtifactId() + "-" + library.getVersion() + ".jar";
        System.out.println(library);

        Path copied = Paths.get(outputDir + "/wsJars/" + fileName);
        Path originalPath = library.getOriginalFile().toPath();
        try {
            Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes Maven pom.xml files for all matched dependencies, grouped by version slot and
     * split into files of at most {@value #MAX_DEPS_PER_POM} dependencies each.
     * <p>
     * Output directories use zero-padded indices for clean lexicographic sorting, e.g.
     * {@code proj_00}, {@code proj_01}, {@code proj_00_01} for overflow chunks.
     *
     * @param matched the full set of matched modules
     * @param path    the root output directory
     */
    private static void writePom(Set<Module> matched, String path) {
        matched.forEach(library -> {
            if (!filteredLibraries(library)) {
                List<String> versions = depVersionMap.computeIfAbsent(library.getModuleId(), k -> new ArrayList<>());
                versions.add(library.getVersion());
                if (versions.size() > pomFiles)
                    pomFiles = versions.size();
            }
        });

        for (int versionSlot = 0; versionSlot < pomFiles; versionSlot++) {
            final int slot = versionSlot;

            class ComparedDependency extends Dependency {
                @Override
                public boolean equals(Object obj) {
                    return this.getGroupId().equals(((Dependency) obj).getGroupId())
                           && this.getArtifactId().equals(((Dependency) obj).getArtifactId());
                }

                @Override
                public int hashCode() {
                    int result = 17;
                    result = 31 * result + getGroupId().hashCode();
                    result = 31 * result + getArtifactId().hashCode();
                    return result;
                }
            }

            // Collect all unique dependencies for this version slot into a model,
            // then chunk the model's dependency list for writing.
            Model baseModel = new Model();
            baseModel.setModelVersion("4.0.0");
            baseModel.setVersion("1.0-SNAPSHOT");
            baseModel.setGroupId("liberty");
            baseModel.setArtifactId("dependency-report");

            matched.forEach(library -> {
                if (!filteredLibraries(library)) {
                    List<String> versions = depVersionMap.get(library.getModuleId());
                    if (versions.size() > slot) {
                        ComparedDependency dependency = new ComparedDependency();
                        dependency.setGroupId(library.getGroupId());
                        dependency.setArtifactId(library.getArtifactId());
                        dependency.setVersion(versions.get(slot));
                        if (!baseModel.getDependencies().contains(dependency)) {
                            baseModel.addDependency(dependency);
                        }
                    }
                }
            });

            List<Dependency> allDeps = baseModel.getDependencies();

            // Compute zero-padded widths from the maximum possible indices so that
            // directory names sort correctly in a file listing (proj_00, proj_01, ...).
            int slotWidth = String.valueOf(pomFiles - 1).length();
            int maxChunks = (allDeps.size() + MAX_DEPS_PER_POM - 1) / MAX_DEPS_PER_POM;
            int chunkWidth = String.valueOf(Math.max(maxChunks - 1, 0)).length();
            String slotPart = String.format("%0" + slotWidth + "d", slot);

            // Split into chunks of MAX_DEPS_PER_POM and write a separate pom for each chunk
            int chunkIndex = 0;
            for (int start = 0; start < allDeps.size(); start += MAX_DEPS_PER_POM, chunkIndex++) {
                List<Dependency> chunk = allDeps.subList(start, Math.min(start + MAX_DEPS_PER_POM, allDeps.size()));

                String projectDir = path + "/proj_" + slotPart
                                    + (maxChunks > 1 ? "_" + String.format("%0" + chunkWidth + "d", chunkIndex) : "");
                new File(projectDir).mkdirs();

                Model model = new Model();
                model.setModelVersion(baseModel.getModelVersion());
                model.setVersion(baseModel.getVersion());
                model.setGroupId(baseModel.getGroupId());
                model.setArtifactId(baseModel.getArtifactId());
                chunk.forEach(model::addDependency);

                MavenXpp3Writer writer = new MavenXpp3Writer();
                try {
                    writer.write(new FileWriter(projectDir + "/pom.xml"), model);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns {@code true} for libraries that are known false positives (transitive test/build
     * dependencies whose packages coincidentally overlap with shipped code) and should be
     * excluded from the dependency report.
     *
     * @param library the module to test
     * @return {@code true} if the library should be excluded
     */
    private static boolean filteredLibraries(Module library) {
        String artifactId = library.getArtifactId();
        String version = library.getVersion();

        return (library.getGroupId().equals("org.glassfish") && artifactId.equals("javax.faces"))
               || artifactId.equals("tomcat-embed-core")
               || artifactId.equals("mockserver-netty")
               || (artifactId.equals("woodstox-core") && (version.equals("6.2.6") || version.equals("6.2.4")))
               || (artifactId.equals("commons-io") && version.equals("2.13.0"))
               || (artifactId.equals("netty-codec-http2") && version.equals("4.1.110.Final"))
               || (artifactId.equals("netty-codec-http")
                   && (version.equals("4.1.73.Final") || version.equals("4.1.78.Final") || version.equals("4.1.92.Final")))
               || artifactId.equals("wlp-docGen");
    }

    /**
     * Returns {@code true} if the library is an IBM WebSphere rebundled jar that should be
     * copied to the wsJars output directory.
     *
     * @param library the module to test
     * @return {@code true} if the library is an IBM WebSphere jar
     */
    private static boolean wsLibraries(Module library) {
        return library.getGroupId().startsWith("com.ibm.ws") && !library.getArtifactId().equals("wlp-docGen");
    }
}
