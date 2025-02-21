/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Allows external contributors a convenient way to build the custom images we
 * use in our build
 */
public class CustomImages {

    // The --build-arg necessary to overwrite the default BASE_IMAGE in the Dockerfile
    // with the mirrored image from an alternative registry
    public static final String BASE_IMAGE = "BASE_IMAGE";

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        if (args == null || args[0] == null || args.length > 1) {
            throw new RuntimeException(
                    "CustomImages expects a single argument (projectPath) which is the path to the io.openliberty.org.testcontainers project.");
        }

        // Get data from calling script
        String projectPath = args[0];
        
        if (System.getenv().containsKey("ARTIFACTORY_DOCKER_SERVER")) {
            System.out.println("Setting fat.test.artifactory.docker.server=" + System.getenv().get("ARTIFACTORY_DOCKER_SERVER"));
            System.setProperty("fat.test.artifactory.docker.server", System.getenv().get("ARTIFACTORY_DOCKER_SERVER"));
        }
        
        if (System.getenv().containsKey("DOCKER_REGISTRY_SERVER")) {
            System.out.println("Setting fat.test.docker.registry.server=" + System.getenv().get("DOCKER_REGISTRY_SERVER"));
            System.setProperty("fat.test.docker.registry.server", System.getenv().get("DOCKER_REGISTRY_SERVER"));
        }

        // Where to find instructions to build images
        Path commonPath = Paths.get(projectPath, "resources", "openliberty", "testcontainers");

        // Find all dockerfiles and attempt to build their corresponding images
        Dockerfile.findDockerfiles(commonPath).stream()
                .map(location -> new Dockerfile(location))
                .sorted() //Sort in case images end up depending on each other
                .forEach(dockerfile -> {
                    // Find or build all images
                    if(dockerfile.isCached()) {
                        System.out.println("Skipping build: " + dockerfile.imageName.asCanonicalNameString());
                        System.out.println("-----");
                        return;
                    }
                    
                    ImageFromDockerfile img = new ImageFromDockerfile(dockerfile.imageName.asCanonicalNameString(), false)
                            .withDockerfile(dockerfile.location)
                            .withBuildArg(BASE_IMAGE, dockerfile.baseImageNameSubstituted.asCanonicalNameString());

                    try {
                        System.out.println("Building image: " + dockerfile.imageName.asCanonicalNameString());
                        img.get();
                        System.out.println("Built image successfully: " + dockerfile.imageName.asCanonicalNameString());
                    } catch (Exception e) {
                        throw new RuntimeException("Could not build or find image " + dockerfile.imageName.asCanonicalNameString(), e);
                    } finally {
                        System.out.println("-----");
                    }
                });

        long end = System.currentTimeMillis();
        System.out.println("Execution time in ms: " + (end - start));
    }
}
