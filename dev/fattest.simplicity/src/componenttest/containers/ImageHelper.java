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

import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.registry.InternalRegistry;

/**
 * A helper class that can determine the type of docker image based
 * off of heristics of the docker image name.
 */
public class ImageHelper {

    private static final Class<?> c = ImageHelper.class;

    /**
     * Test to see if an image is synthetic.
     * This is the result of using ImageFromDockerFile without providing a docker image name.
     *
     * @param  dockerImage the docker image name
     * @return             true if the docker image name was synthetically produced by Testcontainers, false otherwise.
     */
    public static boolean isSyntheticImage(DockerImageName dockerImage) {
        boolean result = dockerImage.getRegistry().equals("localhost") && //
                         dockerImage.getRepository().split("/")[0].equals("testcontainers") && //
                         dockerImage.getVersionPart().equals("latest");

        if (result) {
            Log.warning(c, "WARNING: Cannot use alternative registry for programmatically built image " + dockerImage.asCanonicalNameString() +
                           ". Consider using the ImageBuilder in fattest.simplicity instead.");
        }

        return result;
    }

    /**
     * Test to see if an image is a commit hash.
     * This is the result of accessing the docker client directly to commit an image
     * without providing a docker image name.
     * Depending on your docker host configuration this hash could be one of SHA256, AES, or XOR.
     *
     * @param  dockerImage the docker image name
     * @return             true if the docker image name is a commit hash, false otherwise.
     */
    public static boolean isCommittedImage(DockerImageName dockerImage) {
        boolean result = dockerImage.getRepository().equals("sha256") ||
                         dockerImage.getRepository().equals("aes") ||
                         dockerImage.getRepository().equals("xor");

        if (result) {
            Log.warning(c, "WARNING: Cannot use alternative registry for programmatically committed image " + dockerImage.asCanonicalNameString() +
                           ". Consider using the ImageBuilder in fattest.simplicity instead.");
        }

        return result;
    }

    /**
     * Test to see if an image was built using the {@link componenttest.containers.ImageBuilder}
     * These images are cached either locally or within an internal registry.
     *
     * @param  dockerImage the docker image name
     * @return             true if the docker image name was built with our internal builder, false otherwise.
     */
    public static boolean isBuiltImage(DockerImageName dockerImage) {
        boolean matchesRegistry = !dockerImage.getRegistry().isEmpty() &&
                                  (dockerImage.getRegistry().equals("localhost") ||
                                   dockerImage.getRegistry().equals(InternalRegistry.instance().getRegistry()));
        boolean matchesRepository = dockerImage.getRepository().contains("openliberty/testcontainers");
        boolean nonLatestTag = !dockerImage.getVersionPart().equals("latest");
        return matchesRegistry && matchesRepository && nonLatestTag;
    }
}
