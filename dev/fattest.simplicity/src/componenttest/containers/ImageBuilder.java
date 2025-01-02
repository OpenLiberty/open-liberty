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
 * For internal contributors and our CI builds, this class will first attempt find a cached version of the image on the docker host
 * For external contributors, this class will build the image at test runtime.
 *
 * TODO write unit tests
 */
public class ImageBuilder {

    private static final Class<?> c = ImageBuilder.class;

    // Ensures when we look for cached images Docker doesn't attempt to reach out to docker.io
    private static final String LOCAL_REGISTRY = "localhost";

    // The --build-arg necessary to overwrite the default BASE_IMAGE in the Dockerfile
    // with the mirrored image in artifactory
    private static final String BASE_IMAGE = "BASE_IMAGE";

    // The repository where all Open Liberty images will be cached
    static final String REPOSITORY = "openliberty/testcontainers/";

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
     * Note: The resulting image will be cached with name "localhost/openliberty/testcontainers/<image-name>:<image-version>"
     * therefore, you must update the image version whenever a change is made to the corresponding Dockerfile.
     *
     * @param  img the image to build in format "<image-name>:<image-version>" or "openliberty/testcontainers/<image-name>:<image-version>"
     *
     * @return     instance of ImageBuilder
     */
    public static ImageBuilder build(String img) {
        Objects.requireNonNull(img);

        DockerImageName image = DockerImageName.parse(img);

        if (!image.getRegistry().isEmpty()) {
            throw new RuntimeException("DockerImageName with a registry for ImageBuilder is unsupported: " + image.getRegistry());
        }

        if (image.getUnversionedPart().startsWith(REPOSITORY)) {
            return new ImageBuilder(image.withRegistry(LOCAL_REGISTRY));
        } else {
            return new ImageBuilder(DockerImageName.parse(REPOSITORY + img).withRegistry(LOCAL_REGISTRY));
        }
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
        return getCached().orElse(buildFromDockerfile());
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

}
