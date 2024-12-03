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

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * Allows external contributors a convenient way to build the custom images we use in our build
 */
public class CustomImages {
    
    public static void main(String[] args) {
          long start = System.currentTimeMillis();
          
          if(args == null || args[0] == null || args.length > 1) {
              throw new RuntimeException("CustomImages expects a single argument (projectPath) which is the path to the io.openliberty.org.testcontainers project.");
          }
          
          //Get data from calling script
          String projectPath = args[0];
          
          // Get image list if it exists
          File imageList = new File(projectPath, "cache/externals");
          if(!imageList.exists()) {
              System.out.println("Could not find file: " + imageList.getAbsolutePath());
              return;
          }
          
          List<DockerImageName> images = Arrays.asList();
          try {
              images = Files.lines(imageList.toPath())
                      .filter(line -> !line.startsWith("#"))
                      .map(line -> DockerImageName.parse(line))
                      .collect(Collectors.toList());
          } catch (Exception e) {
              System.out.println("Could not read file: " + imageList.getAbsolutePath());
              return;
          }
          
          // Try to pull all images in the list and generate a list of images that need to be created
          final Set<DockerImageName> unpullableImageNames = new HashSet<>(images);
          images.stream()
          .map(name -> new RemoteDockerImage(name))
          .forEach(image -> {
              try {
                  unpullableImageNames.remove(DockerImageName.parse(image.get()));
              } catch (Exception e) {
                  System.out.println("Could not pull image " + image.toString() + " because " + e.getMessage());
              }
          });
          
          
          File dockerfiles = new File(projectPath, "dockerfiles");
          if(!dockerfiles.exists() && !dockerfiles.isDirectory()) {
              System.out.println("Could not find directory: " + dockerfiles.getAbsolutePath());
              return;
          }
          
          // Try to find a Dockerfile for each unpullable image and build the image
          final Set<DockerImageName> unbuildableImageNames = new HashSet<>(unpullableImageNames);
          
          unpullableImageNames.stream()
              .map(name -> new ImageFromDockerfile(name.asCanonicalNameString(), false)
                              .withDockerfile(
                                      new File(dockerfiles.getAbsolutePath(), name.getUnversionedPart() + "/" + name.getVersionPart() + "/Dockerfile").toPath()))
              .forEach(image ->  {
                  try {
                      unbuildableImageNames.remove(DockerImageName.parse(image.get()));
                  } catch (Exception e) {
                      System.out.println("Could not build image " + image.getDockerImageName() + " because " + e.getMessage());
                  }
              });
          
          System.out.println("Could not pull or build " + unbuildableImageNames.size() + " image(s)");
        
        long end = System.currentTimeMillis();
        System.out.println( "Execution time in ms: " + ( end - start ));
    }
}
