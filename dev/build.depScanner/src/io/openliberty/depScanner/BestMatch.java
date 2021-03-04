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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

public class BestMatch {

    /**
     * @param args[0] = wlp dir to scan args[1] = dependency pom file location.
     * @throws Exception
     */
    private static int pomFiles = 1;
    private static final Map<String, List<String>> depVersionMap = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Repository repo = new Repository(new File(System.getProperty("user.home"), ".ibmartifactory/repository"));
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

                            List<String> moduleNames = modules.stream()
                                            .map(module -> "\t" + module + "\t" + jar.getPackages(module))
                                            .sorted()
                                            .collect(Collectors.toList());

                            List<String> foundPackages = modules.stream().flatMap(module -> jar.getPackages(module).stream()).collect(Collectors.toList());

                            Collection<String> missingPackages = jar.getPackages();

                            missingPackages.removeAll(foundPackages);

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
                            if (!moduleNames.isEmpty()) {
                                modOut.println(jar.getOriginalFile().getAbsolutePath());
                                moduleNames.forEach(modOut::println);
                            }

                            if (!missingPackages.isEmpty()) {
                                mpOut.println(jar.getOriginalFile().getAbsolutePath());
                                missingPackages.stream().map(name -> "\t" + name).forEach(mpOut::println);
                            }

                        });

        writePom(matched, outputDir);

    }

    /**
     * @param matched
     */
    private static void writePom(Set<Module> matched, String path) {

        matched.forEach(library -> {
            if (!library.getGroupId().equals("com.ibm.ws")) {
                List<String> versions = depVersionMap.computeIfAbsent((library.getGroupId() + library.getArtifactId()), k -> new ArrayList<>());
                versions.add(library.getVersion());
                if (versions.size() > pomFiles)
                    pomFiles = versions.size();
            }
        });

        for (AtomicInteger count = new AtomicInteger(0); count.intValue() < pomFiles; count.incrementAndGet()) {

            Model model = new Model();
            model.setModelVersion("4.0.0");
            model.setVersion("1.0-SNAPSHOT");
            model.setGroupId("liberty");
            model.setArtifactId("dependency-report");

            matched.forEach(library -> {
                if (!library.getGroupId().equals("com.ibm.ws")) {
                    List<String> versions = depVersionMap.get((library.getGroupId() + library.getArtifactId()));
                    if (versions.size() > count.intValue()) {

                        class ComparedDependency extends Dependency {

                            /*
                             * (non-Javadoc)
                             *
                             * @see java.lang.Object#equals(java.lang.Object)
                             */
                            @Override
                            public boolean equals(Object obj) {

                                return this.getGroupId().equals(((Dependency) obj).getGroupId())
                                       && this.getArtifactId().equals(((Dependency) obj).getArtifactId());
                            }

                            /*
                             * (non-Javadoc)
                             *
                             * @see java.lang.Object#hashCode()
                             */
                            @Override
                            public int hashCode() {

                                int result = 17;
                                result = 31 * result + getGroupId().hashCode();
                                result = 31 * result + getArtifactId().hashCode();
                                return result;
                            }

                        }
                        ComparedDependency dependency = new ComparedDependency();
                        dependency.setGroupId(library.getGroupId());
                        dependency.setArtifactId(library.getArtifactId());
                        dependency.setVersion(versions.get(count.intValue()));

                        if (!model.getDependencies().contains(dependency))
                            model.addDependency(dependency);
                    }
                }
            });

            MavenXpp3Writer writer = new MavenXpp3Writer();
            try {
                writer.write(new FileWriter(path + "/pom_" + count.intValue() + ".xml"), model);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private static String toMavenCoords(String coords) {
        return coords.substring(10).replace("/", ":").replace("/", ":").replace("@", ":");
    }
}
