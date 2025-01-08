/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import java.util.Objects;
import java.util.Optional;

import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * This builder class is an extension of {@link org.testcontainers.images.builder.ImageFromDockerfile}
 * and is intended to allow developers of Open Liberty the ability to create custom images from a Dockerfile.
 *
 * This class will first attempt find a cached version of the image on the docker host.
 * If not found, then we will try to pull the image from a non-local registry.
 * If not found, this class will build the image at test runtime.
 *
 * TODO write unit tests
 */
public class ImageBuilder {

    private static final Class<?> c = ImageBuilder.class;

    // Ensures when external users look for cached images Docker doesn't attempt to reach out to docker.io
    public static final String LOCAL_REGISTRY = "localhost";

    // The repository where all Open Liberty images will be cached
    public static final String REPOSITORY_PREFIX = "openliberty/testcontainers/";

    // The --build-arg necessary to overwrite the default BASE_IMAGE in the Dockerfile
    // with the mirrored image in Artifactory
    public static final String BASE_IMAGE = "BASE_IMAGE";

    // Image to build
    private final DockerImageName image;

    // Image base
    private DockerImageName baseImage;

    private ImageBuilder(DockerImageName image) {
        //builder class
        this.image = image;
    }

    /**
     * The image to build.
     *
     * The Dockerfile with instructions on how to build this image must be saved in source control in directory
     * io.openliberty.org.testcontainers/resources/openliberty/testcontainers/<image-name>/<image-version>/Dockerfile
     *
     * Note: The resulting image will be cached with name:
     * - "localhost/openliberty/testcontainers/<image-name>:<image-version>" for external contributors
     * - "[custom-registry]/openliberty/testcontainers/<image-name>:<image-version>" for internal contributors or builds
     * therefore, you must update the image version whenever a change is made to the corresponding Dockerfile.
     *
     * @param  img the image to build in format "<image-name>:<image-version>" or "openliberty/testcontainers/<image-name>:<image-version>"
     *
     * @return     instance of ImageBuilder
     */
    public static ImageBuilder build(String img) {
        Objects.requireNonNull(img);

        final DockerImageName image = DockerImageName.parse(img);

        if (!image.getRegistry().isEmpty()) {
            throw new RuntimeException("A docker image cannot be built when initialized with a registry: " + image.getRegistry());
        }

        final DockerImageName result;

        if (image.getUnversionedPart().startsWith(REPOSITORY_PREFIX)) {
            result = image.withRegistry(LOCAL_REGISTRY);
        } else {
            result = image.withRepository(REPOSITORY_PREFIX + image.getRepository()).withRegistry(LOCAL_REGISTRY);
        }

        return new ImageBuilder(result);
    }

    /**
     * The base image defined in the Dockerfile.
     *
     * This is required because at runtime we need to apply the ImageNameSubstitutor to whatever image
     * is defined in the FROM field of the Dockerfile.
     *
     * Testcontainers does not automatically do this, and therefore we need to take extra steps to ensure
     * we do not pull images from a registry which may be rate limited.
     *
     * NOTE: this will also ensure we track the FROM image using the
     * {@link componenttest.containers.ImageNameVerifier}
     *
     * @param  img the image in the FROM line of the Dockerfile
     * @return     this
     */
    public ImageBuilder with(String baseImage) {
        Objects.requireNonNull(baseImage);

        //NOTE: this will also ensure we track the FROM image using the ImageVerifier
        this.baseImage = ImageNameSubstitutor.instance().apply(DockerImageName.parse(baseImage));

        return this;
    }

    /**
     * Termination point of this builder class.
     *
     * We will first attempt to find a cached version of the image,
     * if unsuccessful, we will then build the image from the Dockerfile.
     *
     * @return RemoteDockerImage that points to a cached or built image.
     */
    public RemoteDockerImage get() {
        return getCached()
                        .orElse(pullImage()
                                        .orElse(buildFromDockerfile()));
    }

    /*
     * Helper method, attempts to find a cached version of the image.
     */
    private Optional<RemoteDockerImage> getCached() {
        final String m = "getCached";

        RemoteDockerImage cachedImage = new RemoteDockerImage(image);

        if (PullPolicy.defaultPolicy().shouldPull(image)) {
            Log.info(c, m, "Unable to find cached image " + image.asCanonicalNameString());
            return Optional.empty();
        } else {
            Log.info(c, m, "Found cached image " + image.asCanonicalNameString());
            return Optional.of(cachedImage);
        }
    }

    private Optional<RemoteDockerImage> pullImage() {
        final String m = "pullImage";

        if (image.getRegistry().equalsIgnoreCase(LOCAL_REGISTRY)) {
            return Optional.empty();
        }

        try {
            RemoteDockerImage pullableImage = new RemoteDockerImage(image);
            pullableImage.get();

            Log.info(c, m, "Found pullable image " + image.asCanonicalNameString());
            return Optional.of(pullableImage);
        } catch (Exception e) {
            Log.info(c, m, "Unable to pull image " + image.asCanonicalNameString() + " because " + e.getMessage());
            return Optional.empty();
        }
    }

    /*
     * Helper method, constructs an image from a Dockerfile
     */
    private RemoteDockerImage buildFromDockerfile() {
        String resource = constructResource(image);

        ImageFromDockerfile builtImage = new ImageFromDockerfile(image.asCanonicalNameString(), false)
                        .withFileFromClasspath(".", resource)
                        .withBuildArg(BASE_IMAGE, baseImage.asCanonicalNameString());

        return new RemoteDockerImage(builtImage);
    }

    // Helper method, constructs a resource path to the directory that holds the
    // Dockerfile and supporting files that define this image.
    private static String constructResource(DockerImageName image) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("/");
        buffer.append(image.getRepository()).append("/");
        buffer.append(image.getVersionPart()).append("/");

        return buffer.toString();
    }

    public static boolean isBuiltImage(DockerImageName image) {
        return image.getRegistry().equals(LOCAL_REGISTRY) && //
               image.getRepository().startsWith(REPOSITORY_PREFIX);
    }
}
