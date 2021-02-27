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
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class BestMatch {

    public static final String LIBERTY_INSTALL_DIR = "/Users/cbridgha@us.ibm.com/git/OL/dev/build.image/wlp";

    public static void main(String[] args) throws Exception {
        Repository repo = new Repository(new File(System.getProperty("user.home"), ".ibmartifactory/repository"));
        LibertyInstall liberty = new LibertyInstall(new File(LIBERTY_INSTALL_DIR));

        PrintStream modOut = new PrintStream(new File("moduleMatches.txt"));
        PrintStream mpOut = new PrintStream(new File("missingPackages.txt"));
        PrintStream foundLibs = new PrintStream(new File("foundDeps.txt"));

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

        matched.forEach(foundLibs::println);

        writePom(matched);

        mpOut.println();
        mpOut.print("Missing Packages: ");
        mpOut.println(uniqueMissingPackages.size());
    }

    /**
     * @param matched
     */
    private static void writePom(Set<Module> matched) {
        // TODO Auto-generated method stub

    }

    private static String toMavenCoords(String coords) {
        return coords.substring(10).replace("/", ":").replace("/", ":").replace("@", ":");
    }
}
