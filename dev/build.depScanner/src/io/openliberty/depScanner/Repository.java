/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class Repository {

    private static List<String> testLibs = Arrays.asList("com.ibm.ws:ant-jshint-0.3.6-deps:1.0.0",
                                                         "com.ibm.ws:closure-compiler-v20160315:1.0.0",
                                                         "com.ibm.ws:commons-codec:1.3",
                                                         "com.ibm.ws:commons-codec:1.0.0",
                                                         "com.ibm.ws:commons-logging:1.0.2",
                                                         "com.ibm.ws:fcg:1.0.0",
                                                         "com.ibm.ws:htmlunit-2.20-OSGi:1.0.0",
                                                         "com.ibm.ws:j2ee:1.0.1",
                                                         "com.ibm.ws:j2ee:1.0.2",
                                                         "com.ibm.ws:j2ee:1.0.3",
                                                         "com.ibm.ws:jruby-complete:9.1.17.0",
                                                         "com.ibm.ws:junit:1.0.10",
                                                         "com.ibm.ws:junit:1.0.11",
                                                         "com.ibm.ws:junit:1.0.15",
                                                         "com.ibm.ws:junit:1.0.17",
                                                         "com.ibm.ws:junit:1.0.18",
                                                         "com.ibm.ws:junit:4.9",
                                                         "com.ibm.ws:junit:4.10",
                                                         "com.ibm.ws:rt:1.0.1",
                                                         "com.ibm.ws:apacheds-service-2.0.0-M15:1.0.0",
                                                         "com.ibm.ws:LibertyFeaturesToMavenRepo-0.0.1-SNAPSHOT:11.0.0",
                                                         "com.ibm.ws:mockserver-netty-3.10.7-jar-with-dependencies:1.0.0",
                                                         "com.ibm.ws:objectgrid:1.0.0",
                                                         "com.ibm.ws:org.apache.felix.webconsole-4.2.4-all:1.0.0",
                                                         "com.ibm.ws:org.apache.wink:1.0.1",
                                                         "com.ibm.ws:xmlpull:1.1.3.1",
                                                         "com.ibm.ws:xalan:2.7.1",
                                                         "com.ibm.ws:xalan:1.0.2",
                                                         "io.netty:netty-codec-http:4.1.107.Final",
                                                         "io.netty:netty-codec-http:4.1.93.Final",
                                                         "io.netty:netty-codec-http:4.1.94.Final",
                                                         "io.netty:netty-codec-http:4.1.90.Final",
                                                         "org.bouncycastle:bcprov-jdk18on:1.77",
                                                         "org.apache.cxf:cxf-rt-frontend-jaxrs:3.1.16",
                                                         "org.apache.cxf:cxf-rt-rs-client:3.1.16",
                                                         "org.apache.cxf:cxf-rt-rs-service-description:3.1.16",
                                                         "org.apache.cxf:cxf-rt-transports-http:3.1.16",
                                                         "org.apache.cxf:cxf-tools-common:3.1.16",
                                                         "org.apache.cxf:cxf-tools-wadlto-jaxrs:3.1.16",
                                                         "commons-io:commons-io:2.13.0",
                                                         "org.eclipse.parsson:parsson:1.1.0");

    private final Map<String, List<Module>> moduleMap = new HashMap<>();

    public Repository(File repoDir, boolean isGradleCache) {
        Module.setRoot(repoDir.getAbsolutePath());

        for (File f : Utils.findJars(repoDir)) {
            try {
                Module m = new Module(f, isGradleCache);
                if (!knownTestLibs(m)) {
                    List<Module> modules = moduleMap.computeIfAbsent(m.getModuleId(), k -> new ArrayList<>());
                    modules.add(m);
                    JarInputStream jarIn = new JarInputStream(new FileInputStream(f));
                    ZipEntry entry = jarIn.getNextEntry();
                    if (entry != null) {
                        do {
                            if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                                String className = entry.getName();
                                className = className.replaceAll("/", ".");
                                className = className.substring(0, className.length() - 6);

                                byte[] hash = Utils.computeHash(jarIn);

                                m.addClass(className, hash);
                            }
                        } while ((entry = jarIn.getNextEntry()) != null);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param m
     * @return
     */
    private boolean knownTestLibs(Module m) {
        if (testLibs.contains(m.toString()) || m.getModuleId().equals("com.ibm.ws:junit"))
            return true;
        return false;
    }

    public Stream<Map.Entry<String, List<Module>>> stream() {
        return moduleMap.entrySet().stream();
    }
}