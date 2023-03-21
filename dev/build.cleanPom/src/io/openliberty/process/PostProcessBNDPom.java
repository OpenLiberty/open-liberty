/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.process;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 */
public class PostProcessBNDPom {

    private static String jarPath;
    private static String pomEntryPath;
    private static List<String> filteredGroups = Arrays.asList("org.springframework", "org.springframework.boot", "com.ibm.ws.common.encoder");
    private static List<String> filteredArtifacts = Arrays.asList("com.ibm.crypto.CmpCrmf",
                                                                  "com.ibm.crypto.ibmjceprovider",
                                                                  "com.ibm.crypto.ibmkeycert",
                                                                  "com.ibm.crypto.ibmpkcs",
                                                                  "com.ibm.mq.commonservices",
                                                                  "com.ibm.ws.eba.blueprint.extensions.interceptors",
                                                                  "com.ibm.ws.eba.blueprint.transform",
                                                                  "com.ibm.ws.eba.jpa.container.annotations",
                                                                  "com.ibm.ws.kernel.feature",
                                                                  "com.ibm.ws.kernel.service",
                                                                  "com.ibm.ws.prereq.rxa",
                                                                  "dhbcore",
                                                                  "java.ibmjgssprovider",
                                                                  "wmq_java");

    /**
     * @param args
     */
    public static void main(String[] args) {

        jarPath = args[0];
        String outputDir = args[1];
        Model pom = readJARPom(jarPath);
        if (pom != null) {
            removeDevDependecies(pom);
            writeTempPom(pom, outputDir);
            replacePomFile(outputDir + "/pom.xml", jarPath);
        }
    }

    /**
     * @param pom
     * @return
     */
    private static void writeTempPom(Model pom, String path) {

        MavenXpp3Writer writer = new MavenXpp3Writer();
        try {
            writer.write(new FileWriter(path + "/pom.xml"), pom);
        } catch (IOException e3) {
            e3.printStackTrace();
        }
    }

    /**
     * @param pom
     * @param jarPath2
     */
    private static void replacePomFile(String pomPath, String jarPath) {

        Path myFilePath = Paths.get(pomPath);

        Path zipFilePath = Paths.get(jarPath);
        try (FileSystem fs = FileSystems.newFileSystem(zipFilePath, null)) {
            Path fileInsideZipPath = fs.getPath(pomEntryPath);
            Files.copy(myFilePath, fileInsideZipPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param pom
     */
    private static void removeDevDependecies(Model pom) {

        List<Dependency> deps = pom.getDependencies();
        for (Iterator iterator = deps.iterator(); iterator.hasNext();) {
            Dependency dependency = (Dependency) iterator.next();
            if (dependency.getGroupId().equals("dev") || dependency.getGroupId().equals("test") || (filteredGroups.contains(dependency.getGroupId()))
                || (filteredArtifacts.contains(dependency.getArtifactId())))
                iterator.remove();
        }

    }

    /**
     * @param jarFile2
     */
    private static Model readJARPom(String path) {

        ZipFile jar = null;
        try {
            jar = new ZipFile(path);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        if (jar != null) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream stream = null;
                if (entry.getName().contains("pom.xml")) {
                    try {
                        pomEntryPath = entry.getName();
                        stream = jar.getInputStream(entry);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    Model model = null;
                    try {
                        if (stream != null)
                            model = reader.read(stream);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }
                    try {
                        jar.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return model;
                }

            }
        }
        try {
            jar.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
