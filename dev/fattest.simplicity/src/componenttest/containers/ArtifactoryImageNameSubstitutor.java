/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package componenttest.containers;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.log.Log;

/**
 * An image name substituter is configured in testcontainers.properties and will transform docker image names.
 * Here we use it to apply a private registry prefix and organization prefix so that in remote builds we use an internal
 * Artifactory mirror of Docker Hub, instead of downloading from Docker Hub in each build which causes rate limiting issues.
 */
public class ArtifactoryImageNameSubstitutor extends ImageNameSubstitutor {

    private static final Class<?> c = ArtifactoryImageNameSubstitutor.class;

    /**
     * Manual override that will allow builds or users to pull from Artifactory instead of DockerHub.
     */
    private static final String substitutionOverride = "fat.test.use.artifactory.substitution";

    /**
     * Artifactory keeps a cache of docker images from DockerHub within
     * this organization specifically for the Liberty builds.
     */
    private static final String mirror = "wasliberty-docker-remote";

    @Override
    public DockerImageName apply(final DockerImageName original) {
        DockerImageName result = null;
        boolean collect = true;
        String reason = "";

        do {
            // Priority 1: If we are using a synthetic image do not substitute nor cache
            if (isSyntheticImage(original)) {
                result = original;
                collect = false;
                reason = "Image name is known to be synthetic, cannot use Artifactory registry.";
                break;
            }

            // Priority 2: If registry was explicit set, do not substitute
            if (original.getRegistry() != null && !original.getRegistry().isEmpty()) {
                result = original;
                reason = "Image name is explicitally set with registry, cannot modify registry.";
                break;
            }

            // Priority 3: If the image is known to only exist in an Artifactory organization
            // No need to check for this in Open Liberty since all images need to be accessible outside of Artifactory.

            // Priority 4: System property use.artifactory.substitution (NOTE: only honor this property if set to true)
            if (Boolean.getBoolean(substitutionOverride)) {
                result = DockerImageName.parse(mirror + '/' + original.asCanonicalNameString())
                                .withRegistry(ArtifactoryRegistry.instance().getRegistry())
                                .asCompatibleSubstituteFor(original);
                reason = "System property [ fat.test.use.artifactory.substitution ] was set to true, must use Artifactory registry.";
                break;
            }

            // Priority 5: Always use Artifactory if using remote docker host.
            if (DockerClientFactory.instance().isUsing(ExternalDockerClientStrategy.class)) {
                result = DockerImageName.parse(mirror + '/' + original.asCanonicalNameString())
                                .withRegistry(ArtifactoryRegistry.instance().getRegistry())
                                .asCompatibleSubstituteFor(original);
                reason = "Using a remote docker host, must use Artifactory registry";
                break;
            }

            //default - use original
            result = original;
            reason = "Default behavior: use default docker registry.";
        } while (false);

        // We determined we need Artifactory, but it is unavailable.
        if (original != result && !ArtifactoryRegistry.instance().isArtifactoryAvailable()) {
            throw new RuntimeException("Need to swap image " + original.asCanonicalNameString() + " --> " + result.asCanonicalNameString()
                                       + System.lineSeparator() + "Reason: " + reason
                                       + System.lineSeparator() + "Error: The Artifactory regsitry was not added to the docker config.", //
                            ArtifactoryRegistry.instance().getSetupException());
        }

        // Alert user that we either added the Artifactory registry or not.
        if (original == result) {
            Log.info(c, "apply", "Keeping original image name: " + original.asCanonicalNameString()
                                 + System.lineSeparator() + "Reason: " + reason);
        } else {
            Log.info(c, "apply", "Swapping docker image name " + original.asCanonicalNameString() + " --> " + result.asCanonicalNameString()
                                 + System.lineSeparator() + "Reason: " + reason);
        }

        // Collect image data for verification after testing
        if (collect) {
            return ImageVerifier.collectImage(original, result);
        } else {
            return original;
        }
    }

    @Override
    protected String getDescription() {
        return "ArtifactoryImageNameSubstitutor";
    }

    /**
     * Docker images that are programmatically constructed at runtime (usually with ImageFromDockerfile)
     * will error out with a 404 if we attempt to do a docker pull from an Artifactory mirror registry.
     * To work around this issue, we will avoid image name substitution for image names that appear to be programmatically
     * generated by Testcontainers. FATs that use ImageFromDockerfile should consider using dedicated images
     * instead of programmatic construction (see com.ibm.ws.cloudant_fat/publish/files/couchdb-ssl/ for an example)
     */
    private static boolean isSyntheticImage(DockerImageName dockerImage) {
        String name = dockerImage.asCanonicalNameString();
        boolean isSynthetic = dockerImage.getRegistry().equals("localhost") && //
                              dockerImage.getRepository().split("/")[0].equals("testcontainers") && //
                              dockerImage.getVersionPart().equals("latest");
        boolean isCommittedImage = dockerImage.getRepository().equals("sha256");
        if (isSynthetic || isCommittedImage) {
            Log.warning(c, "WARNING: Cannot use private registry for programmatically built or committed image " + name +
                           ". Consider using a pre-built image instead.");
        }
        return isSynthetic || isCommittedImage;
    }

}
