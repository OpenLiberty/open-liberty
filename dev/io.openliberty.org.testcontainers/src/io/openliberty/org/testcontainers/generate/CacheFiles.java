/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.org.testcontainers.generate;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

public class CacheFiles {
    
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
          
        if(args == null || args[0] == null || args.length > 1) {
            throw new RuntimeException("CacheFiles expects a single argument (projectPath) which is the path to the io.openliberty.org.testcontainers project.");
        }
          
        //Get data from calling script
        String projectPath = args[0];
        File thisProject = new File(projectPath);
                    
        //Create the cache directory and files
        File projectList = new File(projectPath, "cache/projects");
        projectList.mkdirs();
        projectList.delete();
        
        File imageList = new File(projectPath, "cache/images");
        imageList.delete();
        
        File externalsList = new File(projectPath, "cache/externals");
        externalsList.delete();
        
        //Create structures to store data
        HashSet<String> projects = new HashSet<>();
        HashSet<String> externals = new HashSet<>();
        HashSet<String> images = new HashSet<>();
        
        //Investigate all bnd.bnd files
        File rootDir = thisProject.getParentFile();
        for(File nextDir : rootDir.listFiles()) {
            if (!nextDir.isDirectory()) {
                continue;
            }
            
            File bndFile = new File(nextDir.getAbsolutePath(), "bnd.bnd");
            if (!bndFile.exists()) {
                continue;
            }
                
            File gradleFile = new File(nextDir.getAbsolutePath(), "build.gradle");
            if (!gradleFile.exists()) {
                continue;
            }
            
            try {
                if (!nextDir.getName().equals("fattest.simplicity") && !Files.lines(gradleFile.toPath()).anyMatch(line -> line.contains("copyTestContainers"))) {
                    continue;
                }
                
                //Found a project that uses TestContainers
                projects.add(nextDir.getName());
                
                List<String> bndLines = Files.lines(bndFile.toPath()).collect(Collectors.toList());
                if (!bndLines.stream().anyMatch(line -> line.contains("fat.test.container.images"))) {
                    continue;
                }
                
                AtomicBoolean getNext = new AtomicBoolean(false);
                bndLines.stream()
                        .forEach(line -> {
                            if (line.contains("fat.test.container.images") || getNext.get()) {
                                String trimmedLine = line.trim()
                                        .replaceAll("\\s", "")
                                        .replaceAll("fat.test.container.images:", "")
                                        .replaceAll("fat.test.container.images=", "");
                                if(trimmedLine.endsWith("\\")) {
                                    getNext.set(true);
                                } else {
                                    getNext.set(false);
                                }
                                
                                trimmedLine = trimmedLine.replaceAll("\\\\", "");
                                
                                externals.addAll(Arrays.asList(trimmedLine.split(",")));
                            }
                        });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        String header = "# NOTICE: This file was automatically updated to reflect changes made to test projects." + System.lineSeparator() +  
                        "# Please check these changes into GitHub" + System.lineSeparator();
        
        externals.remove(""); //Remove any blank lines that made it into the list.
        
        // Duplicate the externals list with internal organizations        
        externals.stream()
            .forEach(image -> {
                DockerImageName applied = ImageNameSubstitutor.instance().apply(DockerImageName.parse(image));
                images.add(applied.asCanonicalNameString());
            });

        
        //Save projects and images to files
        try (BufferedWriter projectWriter = Files.newBufferedWriter(projectList.toPath(), Charset.defaultCharset());
             BufferedWriter imageWriter = Files.newBufferedWriter(imageList.toPath(), Charset.defaultCharset());
             BufferedWriter externalsWriter = Files.newBufferedWriter(externalsList.toPath(), Charset.defaultCharset());) {
            
            projectWriter.append(header);
            for(String project : new TreeSet<String>(projects)) {
                projectWriter.append(project + System.lineSeparator());
            }
            
            imageWriter.append(header);
            for(String image : new TreeSet<String>(images)) {
                imageWriter.append(image + System.lineSeparator());
            }
            
            externalsWriter.append(header);
            for(String external : new TreeSet<String>(externals)) {
                externalsWriter.append(external + System.lineSeparator());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // Average execution time is 300 ms
        long end = System.currentTimeMillis();
        System.out.println( "Execution time in ms: " + ( end - start ));
    }
}
