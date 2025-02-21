/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * A record that represents a Dockerfile
 */
public class Dockerfile implements Comparable<Dockerfile> {
    public final Path location;
    public final DockerImageName imageName;
    public final DockerImageName baseImageName;
    public final DockerImageName baseImageNameSubstituted;
    
    public Dockerfile(Path location) {
        this.location = location;
        this.imageName = constructImageName(this.location);
        this.baseImageName = findBaseImageFrom(this.location);
        this.baseImageNameSubstituted = ImageNameSubstitutor.instance().apply(baseImageName);
    }
    
    public boolean isCached() {
        if (PullPolicy.defaultPolicy().shouldPull(imageName)) {
            System.out.println("Did not find image locally: " + imageName.asCanonicalNameString());
            return false;
        } else {
            System.out.println("Found image locally: " + imageName.asCanonicalNameString());
            return true;
        }
    }
    
    /**
     * Using the Dockerfile's path, parse the directory structure to construct a
     * fully qualified DockerImageName to be associated with this Dockerfile.
     * 
     * @param location of Dockerfile to be built
     * @return The DockerImageName for this Dockerfile
     */
    private static DockerImageName constructImageName(Path location) {

        // io.openliberty.org.testcontainers/resources/openliberty/testcontainers/[repository]/[version]/Dockerfile
        final String fullPath = location.toString();
        
        System.out.println("Full path to dockerfile is: " + fullPath);

        // Find version (between the last two separator characters)
        int end = fullPath.lastIndexOf(File.separator);
        int start = fullPath.substring(0, end).lastIndexOf(File.separator) + 1;
        final String version = fullPath.substring(start, end);

        // Find repository (between "resources/" and version)
        start = fullPath.lastIndexOf("resources/") + 10;
        end = fullPath.indexOf(version) - 1;
        final String repository = fullPath.substring(start, end);

        // Construct and return name
        DockerImageName name = ImageBuilderSubstitutor.instance()
                .apply(DockerImageName.parse(repository).withTag(version));
        System.out.println("DockerImageName from path: " + name.asCanonicalNameString());
        return name;
    }
    
    /**
     * Walk through all files nested within a shared path and find every Dockerfile.
     * 
     * @param commonPath the shared path within which all Dockerfiles are nested
     *                   within
     * @return A list of paths to every Dockerfile
     */
    public static List<Path> findDockerfiles(Path commonPath) {
        final String FILE_NAME = "Dockerfile";
        final List<Path> Dockerfiles = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(commonPath)) {
            paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(FILE_NAME))
                    .forEach(Dockerfiles::add);
        } catch (IOException e) {
            throw new RuntimeException("Error searching files: " + e.getMessage());
        }

        return Dockerfiles;
    }

    /**
     * Similar logic to ImageBuilder.findBaseImageFrom(resource)
     * 
     * However, in this case we can only use the ArtifactoryMirrorSubstitutor so we
     * have to manually put in the Artifactory registry (when available)
     * 
     * @param location of Dockerfile the resource path of the Dockerfile
     * @return The substituted docker image of the BASE_IMAGE argument
     */
    private DockerImageName findBaseImageFrom(Path location) {
        final String BASE_IMAGE_PREFIX = "ARG BASE_IMAGE=\"";

        Stream<String> dockerfileLines;
        try {
            dockerfileLines = Files.readAllLines(location).stream();
        } catch (IOException e) {
            throw new RuntimeException("Could not read or find Dockerfile in " + location.toString(), e);
        }

        String errorMessage = "The Dockerfile did not contain a BASE_IMAGE argument declaration. "
                + "This is required to allow us to pull and substitute the BASE_IMAGE using the ImageNameSubstitutor.";

        String baseImageLine = dockerfileLines.filter(line -> line.startsWith(BASE_IMAGE_PREFIX)).findFirst()
                .orElseThrow(() -> new IllegalStateException(errorMessage));

        String baseImageName = baseImageLine.substring(BASE_IMAGE_PREFIX.length(), baseImageLine.lastIndexOf('"'));

        return DockerImageName.parse(baseImageName);

    }
    
    /**
     * A ImageNameSubstitutor for images built by this outer class.
     * TODO figure out if there is a way to use the ImageBuilderSubstitutor from the ImageBuilder class of fattest.simplicity
     */
    private static class ImageBuilderSubstitutor extends ImageNameSubstitutor {

        private static final String INTERNAL_REGISTRY_ENV = "DOCKER_REGISTRY_SERVER";

        // Ensures when we look for cached images Docker only attempt to find images
        // locally or from an internally configured registry.
        private static final String REGISTRY = System.getenv().getOrDefault(INTERNAL_REGISTRY_ENV, "localhost");

        // The repository where all Open Liberty images are located
        private static final String REPOSITORY_PREFIX = "openliberty/testcontainers/";

        @Override
        public DockerImageName apply(final DockerImageName original) {
            Objects.requireNonNull(original);

            if (!original.getRegistry().isEmpty()) {
                throw new IllegalArgumentException("DockerImageName with the registry " + original.getRegistry()
                        + " cannot be substituted with registry " + REGISTRY);
            }

            if (original.getRepository().startsWith(REPOSITORY_PREFIX)) {
                return original.withRegistry(REGISTRY);
            } else {
                return original.withRepository(REPOSITORY_PREFIX + original.getRepository()).withRegistry(REGISTRY);
            }
        }

        @Override
        protected String getDescription() {
            return "ImageBuilderSubstitutor with registry " + REGISTRY;
        }
        
        // Hide instance method from parent class.
        // which will choose the ImageNameSubstitutor based on environment.
        private static ImageBuilderSubstitutor instance;

        public static synchronized ImageNameSubstitutor instance() {
            if (Objects.isNull(instance)) {
                instance = new ImageBuilderSubstitutor();
            }
            return instance;
        }
    }

    @Override
    public int compareTo(Dockerfile o) {
        if(Objects.isNull(o)) {
            throw new IllegalStateException("Cannot compare a Dockerfile object to a null object");
        }
        
        return this.imageName.asCanonicalNameString().compareTo(o.imageName.asCanonicalNameString());
    }
}